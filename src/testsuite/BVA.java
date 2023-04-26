package testsuite;

import logic.Transition;
import models.*;
import parser.GuardParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BVA {
    Automaton automaton;
    private HashMap<String, Boolean> invariants = new HashMap<>();
    private HashMap<String, Integer> invariantDelays = new HashMap<>();

    private List<BoundaryValues> boundaryValues = new ArrayList<>();

    public BVA(Automaton automaton) {
        this.automaton = automaton;
        for (Location loc : automaton.getLocations()) {
            if (!loc.getInvariant().toString().equals("true")) {
                this.invariants.put(loc.getName(), false);
            }
        }
    }

    public void computeInvariantDelays(List<Transition> trace) {
        for (String key : invariants.keySet()) {
            if (!invariants.get(key)) {
                for (Transition t : trace) {
                    if (t.getSource().getLocation().getName().equals(key) && !invariants.get(key)) {
                        int min = minClockValue(t.getSource().getInvariant(), getClock(t.getSource().getLocationInvariant()));
                        int max = maxClockValue(t.getSource().getInvariant(), getClock(t.getSource().getLocationInvariant()));
                        invariantDelays.put(key, max-min);
                        invariants.put(key, true);
                        boundaryValues.add(new BoundaryValues(key, invariantDelays.get(key)));
                    }
                }
            }
        }
    }

    public List<BoundaryValues> getBoundaryValues() {
        return boundaryValues;
    }

    private Clock getClock(CDD locInvar) {

        Pattern pattern = Pattern.compile("([A-Za-z])");
        Matcher m = pattern.matcher(locInvar.toString());

        while (m.find()){

            for (Clock c : automaton.getClocks()) {
                if (c.getOriginalName().equals(m.group())) {
                    return c;
                }
            }
        }

        return null;
    }
    private CDD helperConjoin(String guardString, CDD orgCDD) {
        Guard g = GuardParser.parse(guardString, automaton.getClocks(), automaton.getBVs());
        CDD cdd = orgCDD.conjunction(new CDD(g));

        return cdd;
    }
    private int minClockValue(CDD orgCDD, Clock clock){

        String guardTemplate = clock.getOriginalName() + " == ";
        int min = 0;
        while (true) {
            CDD cdd = helperConjoin(guardTemplate + min, orgCDD);
            if (cdd.isNotFalse()) {
                break;
            }
            min++;
        }
        return min;

    }

    public int  maxClockValue(CDD orgCDD, Clock clock){

        String guardTemplate = clock.getOriginalName() + " == ";
        int min = 1000;
        while (true) {
            CDD cdd = helperConjoin(guardTemplate + min, orgCDD);
            if (cdd.isNotFalse()) {
                break;
            }
            min--;
        }
        return min;

    }
    public HashMap<String, Integer> getInvariantDelays() {
        return invariantDelays;
    }
}
