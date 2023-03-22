package testsuite;

import models.Location;

import java.util.ArrayList;
import java.util.List;

public class BoundaryValues {
    private final String location;
    private final int delay;
    private List<Integer> values = new ArrayList<>();

    public BoundaryValues(String location, int delay) {
        this.location = location;
        this.delay = delay;
        computeBoundaryValues();
    }

    private void computeBoundaryValues() {
        if (delay == 0) {
            values.add(delay);
            values.add(delay+1);
        }
        else if (delay > 0) {
            values.add(0);
            values.add(delay);
            values.add(delay+1);
        }
    }

    public List<Integer> getValues() {
        return values;
    }

    public String getLocation() {
        return location;
    }
}
