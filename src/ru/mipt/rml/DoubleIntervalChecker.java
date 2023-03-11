package ru.mipt.rml;

import java.util.List;

public class DoubleIntervalChecker {
    public static boolean isDoubleInIntervals(double value, List<DoubleInterval> integerIntervals) {
        for (DoubleInterval interval : integerIntervals) {
            if (value >= interval.getStart() && value < interval.getEnd()) {
                return true;
            }
        }
        return false;
    }
}
