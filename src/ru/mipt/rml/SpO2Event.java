package ru.mipt.rml;

public class SpO2Event {
    public int start;
    public int duration;
    public int o2Before;
    public int o2min;

    public SpO2Event(int start, int duration, int o2Before, int o2min) {
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
                '}';
    }
}
