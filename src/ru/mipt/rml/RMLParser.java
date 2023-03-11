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
import java.util.List;

public class RMLParser {

    private static final String FILENAME = "/Users/jiangsheng/Documents/rml/00005295-111034.rml";
    private static List<SpO2Event> events = new ArrayList<SpO2Event>();

    public static void main(String[] args) {

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {

            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File(FILENAME));

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
                        int start = Integer.parseInt(element.getAttribute("Start"));
                        int duration = Integer.parseInt(element.getAttribute("Duration"));

                        int o2Before = Integer.parseInt(element.getElementsByTagName("O2Before").item(0).getFirstChild().getNodeValue());
                        int o2min = Integer.parseInt(element.getElementsByTagName("O2Min").item(0).getFirstChild().getNodeValue());
                        SpO2Event event = new SpO2Event(start, duration, o2Before, o2min);
                        events.add(event);
                        System.out.println(event);
                    }
                }
            }

            System.out.println("SpO2 Events number: " + events.size());

            NodeList stagingData = doc.getElementsByTagName("Stage");

            for (int temp = 0; temp < stagingData.getLength(); temp++) {
                Node node = stagingData.item(temp);

                Element element = (Element) node;
                int start = Integer.parseInt(element.getAttribute("Start"));
                String type = element.getAttribute("Type");

                StagingData data = new StagingData(start, type);
                System.out.println(data);
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

}
