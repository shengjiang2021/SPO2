package ru.mipt.rml;

public class SpO2Event {
    public double start;
    public double duration;
    public double o2Before;
    public double o2min;
    public String stage;
    public double endTime;
    public double area;

    public SpO2Event(double start, double duration, double o2Before, double o2min) {
        this.start = start;
        this.duration = duration;
        this.o2Before = o2Before;
        this.o2min = o2min;
    }

    @Override
    public String toString() {
        return "SpO2Event{" +
                "start=" + start +
                ", duration=" + duration +
                ", o2Before=" + o2Before +
                ", o2min=" + o2min +
                ", stage='" + stage + '\'' +
                ", endTime=" + endTime +
                ", area=" + area +
                '}';
    }
}
