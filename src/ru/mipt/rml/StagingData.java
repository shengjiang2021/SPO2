package ru.mipt.rml;

public class StagingData {
    public int start;
    public String type;

    public StagingData(int start, String type) {
        this.start = start;
        this.type = type;
    }

    @Override
    public String toString() {
        return "StagingData{" +
                "start=" + start +
                ", type='" + type + '\'' +
                '}';
    }
}
