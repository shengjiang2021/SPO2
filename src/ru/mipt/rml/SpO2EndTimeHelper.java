package ru.mipt.rml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SpO2EndTimeHelper {
    public static double endTime(SpO2Event event, String filePath, int totalLine) {
        int endTimeUsing92 = 0;
        int endTimeUsingHalfRecover = 0;
        int endTimeUsingHalfDuration = (int) (event.start + event.duration) + (int) (event.duration / 2);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // skip lines before the starting line
            for (int i = 1; i <= event.start + event.duration; i++) {
                br.readLine();
            }

            // read the file line by line
            double halfRecoverThreshold = event.o2min + (event.o2Before - event.o2min) / 2;

            String line;
            for (int i = 1; i <= event.duration && event.start + event.duration + i <= totalLine; i++) {
                line = br.readLine();
                Double current = Double.parseDouble(line);
                if (current > 95 && endTimeUsing92 == 0) {
                    endTimeUsing92 = (int) event.start + (int) event.duration + i;
                }
                if (current > halfRecoverThreshold && endTimeUsingHalfRecover == 0) {
                    endTimeUsingHalfRecover = (int) event.start + (int) event.duration + i;
                }
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

        if (endTimeUsing92 != 0) return endTimeUsing92;
        else if (endTimeUsingHalfRecover != 0) return endTimeUsingHalfRecover;
        else return endTimeUsingHalfDuration;
    }
}
