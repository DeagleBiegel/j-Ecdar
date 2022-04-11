package models;

import java.util.List;
import java.util.Objects;

public class ClockUpdate extends Update{

    private final Clock clock;
    private final int value;

    public ClockUpdate(Clock clock, int value) {
        this.clock = clock;
        this.value = value;
    }

    public ClockUpdate(ClockUpdate copy, List<Clock> clocks){
        this.clock = clocks.get(clocks.indexOf(copy.clock));
        this.value = copy.value;
    }

    public Clock getClock() {
        return clock;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClockUpdate)) return false;
        ClockUpdate update = (ClockUpdate) o;
        return value == update.value &&
                clock.equals(update.clock);
    }

    @Override
    public String toString() {
        return "Update{" +
                "clock=" + clock +
                ", value=" + value +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(clock, value);
    }
}