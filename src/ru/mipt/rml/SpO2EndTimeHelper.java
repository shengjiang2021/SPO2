package ru.mipt.rml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SpO2EndTimeHelper {
    public static double endTime(SpO2Event event, String filePath) {
        int endTimeUsing92 = 0;
        int endTimeUsingHalfRecover = 0;
        int endTimeUsingHalfDuration = (int) (event.start + event.duration / 2);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // skip lines before the starting line
            for (int i = 0; i < event.start - 1; i++) {
                br.readLine();
            }

            // read the file line by line
            double halfRecoverThreshold = (event.o2Before - event.o2min) / 2;

            String line;
            for (int i = 0; i < event.duration; i++) {
                Double current = Double.parseDouble(br.readLine());
                if (current > 92 && endTimeUsing92 == 0) {
                    endTimeUsing92 = (int) event.start + i;
                }
                if (current > halfRecoverThreshold && endTimeUsingHalfRecover == 0) {
                    endTimeUsingHalfRecover = (int) event.start + i;
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
