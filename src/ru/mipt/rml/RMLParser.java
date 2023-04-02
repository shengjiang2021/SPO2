package ru.mipt.rml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RMLParser {

    private static String RML_FILENAME = "";
    private static String SPO2_DATA_FILE = "";
    private static List<SpO2Event> events = new ArrayList<SpO2Event>();
    private static List<String> typeFilter = List.of("Wake", "NonREM1", "NonREM2", "NonREM3", "REM", "Total");

    public static void process(String rmlFilePath, String dataFilePath, int totalLine) {
        RML_FILENAME = rmlFilePath;
        SPO2_DATA_FILE = dataFilePath;

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File(RML_FILENAME));

            // optional, but recommended
            // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());
            System.out.println("------");

            // get <staff>
            NodeList list = doc.getElementsByTagName("Event");

            for (int temp = 0; temp < list.getLength(); temp++) {

                Node node = list.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get staff's attribute
                    String id = element.getAttribute("Family");

                    if (id.equals("SpO2")) {
                        double start = Double.parseDouble(element.getAttribute("Start"));
                        double duration = Double.parseDouble(element.getAttribute("Duration"));

                        double o2Before = Double.parseDouble(element.getElementsByTagName("O2Before").item(0).getFirstChild().getNodeValue());
                        double o2min = Double.parseDouble(element.getElementsByTagName("O2Min").item(0).getFirstChild().getNodeValue());
                        SpO2Event event = new SpO2Event(start, duration, o2Before, o2min);
                        events.add(event);
                        //System.out.println(event);
                    }
                }
            }

            System.out.println("SpO2 Events number: " + events.size());

            /*
            Element userStaging = (Element) doc.getElementsByTagName("UserStaging").item(0);

            // Get the NeuroAdultAASMStaging element within the UserStaging element
            Element neuroAdultAASMStaging = (Element) userStaging.getElementsByTagName("NeuroAdultAASMStaging").item(0);

            // Get the list of Stage elements within the NeuroAdultAASMStaging element
            NodeList stages = neuroAdultAASMStaging.getElementsByTagName("Stage");

            // Loop through the list of Stage elements and print the attributes
            for (int i = 0; i < stages.getLength(); i++) {
                Element stage = (Element) stages.item(i);
                String type = stage.getAttribute("Type");
                String start = stage.getAttribute("Start");
                System.out.println("Type: " + type + ", Start: " + start);
            }
            */

            Element userStaging = (Element) doc.getElementsByTagName("UserStaging").item(0);

            // Get the NeuroAdultAASMStaging element within the UserStaging element
            Element neuroAdultAASMStaging = (Element) userStaging.getElementsByTagName("NeuroAdultAASMStaging").item(0);

            // Get the list of Stage elements within the NeuroAdultAASMStaging element
            NodeList stages = neuroAdultAASMStaging.getElementsByTagName("Stage");

            Map<String, List<DoubleInterval>> stageToInterval = new HashMap<>();
            Map<String, Double> stageTotalTime = new HashMap<>() {{
                put("Wake", 0.0);
                put("NonREM1", 0.0);
                put("NonREM2", 0.0);
                put("NonREM3", 0.0);
                put("REM", 0.0);
                put("Total", 0.0);
            }};
            Element duration = (Element) doc.getElementsByTagName("Duration").item(0);
            double endFinal = Double.parseDouble(duration.getTextContent());

            // Loop through the list of Stage elements and print the attributes
            for (int i = 0; i < stages.getLength(); i++) {
                Element stage = (Element) stages.item(i);
                String type = stage.getAttribute("Type");
                if (!typeFilter.contains(type)) {
                    System.out.println("Type: " + type + " is not in the type list, skipping it.");
                    continue;
                }
                String start = stage.getAttribute("Start");
                //System.out.println("Type: " + type + ", Start: " + start);

                double startTime = Double.parseDouble(start);
                double endTime;
                if (i + 1 < stages.getLength()) {
                    Element nextStage = (Element) stages.item(i + 1);
                    endTime = Double.parseDouble(nextStage.getAttribute("Start")) - 1;
                } else {
                    endTime = endFinal;
                }
                DoubleInterval interval = new DoubleInterval(startTime, endTime);
                stageToInterval.computeIfAbsent(type, x -> new ArrayList<DoubleInterval>()).add(interval);

                stageTotalTime.put(type, stageTotalTime.get(type) + (endTime - startTime) / 3600);
                if (!type.equals("Wake")) {
                    stageTotalTime.put("Total", stageTotalTime.get("Total") + (endTime - startTime) / 3600);
                }
            }
            System.out.println("Finish building stage to interval map");

            Map<String, List<SpO2Event>> stageToEvents = new HashMap<>();
            Map<String, Double> stageToArea = new HashMap<>() {{
                put("NonREM1", 0.0);
                put("NonREM2", 0.0);
                put("NonREM3", 0.0);
                put("REM", 0.0);
                put("Total", 0.0);
            }};
            outer: for (SpO2Event event : events) {
                inner: for (Map.Entry<String, List<DoubleInterval>> entry : stageToInterval.entrySet()) {
                    if (DoubleIntervalChecker.isDoubleInIntervals(event.start, entry.getValue())) {
                        stageToEvents.computeIfAbsent(entry.getKey(), x -> new ArrayList<>()).add(event);
                        event.stage = entry.getKey();
                        break inner;
                    }
                }

                if (event.stage == null) {
                    System.out.println("Spo2 event stage is not in the stage list");
                    continue;
                }

                if (!event.stage.equals("Wake")) {
                    double endTime = SpO2EndTimeHelper.endTime(event, SPO2_DATA_FILE, totalLine);
                    event.area = (endTime - event.start) / 60 * (event.o2Before - event.o2min) * 0.5;

                    stageToArea.put(event.stage, stageToArea.get(event.stage) + event.area);
                    stageToArea.put("Total", stageToArea.get("Total") + event.area);
                }
            }
            System.out.println("Finish building stage to events map");

            double totalHb = stageToArea.get("Total") / stageTotalTime.get("Total");
            double n1Hb = stageToArea.get("NonREM1") / stageTotalTime.get("NonREM1");
            double n2Hb = stageToArea.get("NonREM2") / stageTotalTime.get("NonREM2");
            double n3Hb = stageToArea.get("NonREM3") / stageTotalTime.get("NonREM3");
            double nTotal = (stageToArea.get("NonREM1") + stageToArea.get("NonREM2") + stageToArea.get("NonREM3")) /
                    (stageTotalTime.get("NonREM1") + stageTotalTime.get("NonREM2") + stageTotalTime.get("NonREM3"));
            double rem = stageToArea.get("REM") / stageTotalTime.get("REM");

            System.out.println("totalHb: " + totalHb);
            System.out.println("n1Hb: " + n1Hb);
            System.out.println("n2Hb: " + n2Hb);
            System.out.println("n3Hb: " + n3Hb);
            System.out.println("nTotal: " + nTotal);
            System.out.println("rem: " + rem);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }
}
