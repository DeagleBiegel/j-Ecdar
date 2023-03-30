package testsuite;

import logic.State;
import logic.Transition;
import models.*;
import parser.GuardParser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestCase {
    private List<Transition> trace;
    private StringBuilder testCode;
    private final TestSettings testSettings;
    private final List<Clock> clocks;
    private final List<BoolVar> BVs;



    public TestCase(List<Transition> trace, TestSettings testSettings, List<Clock> clocks, List<BoolVar> BVs) {
        this.trace = trace;
        this.testSettings = testSettings;
        this.clocks = clocks;
        this.BVs = BVs;
    }

    public TestCase(TestCase testCase) {
        this.trace = testCase.getTrace();
        this.testSettings = testCase.getTestSettings();
        this.clocks = testCase.getClocks();
        this.BVs = testCase.getBVs();
    }

    public List<Clock> getClocks() {
        return clocks;
    }

    public TestSettings getTestSettings() {
        return testSettings;
    }

    public StringBuilder getTestCode() {
        return testCode;
    }

    public List<Transition> getTrace() {
        return trace;
    }

    public void setTrace(List<Transition> trace) {
        this.trace = trace;
    }

    public void createTestCode() {
        // Start the test case by declaring and initialising all clocks
        StringBuilder sb = new StringBuilder();

        //sb.append(testCodeInitClocks());
        HashMap<String, Boolean> booleans = new HashMap<>();

        //initialise hashmap of boolean variables
        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        //create test code for initial location
        //assert clocks then get the test code from enter location
        sb.append(testCodeAssertClocks(trace.get(0).getSource()));
        sb.append(trace.get(0).getSource().getLocation().getEnterTestCode());

        for (Transition tran : trace) {
            //wait for x time units, based on the maximum delay for a location. If there is not max the delay is 0.
            int x = (minClockValue(tran.getTarget().getInvariant(), getClocks().get(getClocks().size()-1)) - minClockValue(tran.getSource().getInvariant(), getClocks().get(getClocks().size()-1)));
            sb.append("cas.wait(" + x + ");\n");

            //get exit test code
            sb.append(tran.getSource().getLocation().getExitTestCode());
            sb.append(testCodeAssertClocks(tran.getSource()));

            //get test code for edge
            sb.append(tran.getEdges().get(0).getTestCode());

            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof BoolUpdate) {
                        booleans.put(((BoolUpdate)update).getBV().getUniqueName(), ((BoolUpdate)update).getValue());
                    }
                    else if (update instanceof ClockUpdate) {
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                    }
                }
            }

            sb.append(testCodeAssertClocks(tran.getTarget()));
            sb.append(tran.getTarget().getLocation().getEnterTestCode()); // .conjunction(tran.getTarget().getInvariant()

        }
        sb.append(testSettings.postfix);

        this.testCode = sb;
    }

    public void createTestCode(String location, int delay) {
        // Start the test case by declaring and initialising all clocks
        StringBuilder sb = new StringBuilder();

        //sb.append(testCodeInitClocks());
        HashMap<String, Boolean> booleans = new HashMap<>();

        //initialise hashmap of boolean variables
        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        //create test code for initial location
        //assert clocks then get the test code from enter location
        sb.append(testCodeAssertClocks(trace.get(0).getSource()));
        sb.append(trace.get(0).getSource().getLocation().getEnterTestCode());

        for (Transition tran : trace) {
            //wait for x time units, based on the maximum delay for a location. If there is not max the delay is 0.
            if(tran.getSource().getLocation().getName().equals(location) && tran.getEdges().get(0).getStatus().equals("INPUT")) {
                sb.append("cas.wait(" + delay + ");\n");
            }
            else {
                int x = (minClockValue(tran.getTarget().getInvariant(), getClocks().get(getClocks().size()-1)) - minClockValue(tran.getSource().getInvariant(), getClocks().get(getClocks().size()-1)));
                sb.append("cas.wait(" + x + ");\n");
            }

            //get exit test code
            sb.append(tran.getSource().getLocation().getExitTestCode());
            sb.append(testCodeAssertClocks(tran.getSource()));

            //get test code for edge
            sb.append(tran.getEdges().get(0).getTestCode());

            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof BoolUpdate) {
                        booleans.put(((BoolUpdate)update).getBV().getUniqueName(), ((BoolUpdate)update).getValue());
                    }
                    else if (update instanceof ClockUpdate) {
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                    }
                }
            }

            sb.append(testCodeAssertClocks(tran.getTarget()));
            sb.append(tran.getTarget().getLocation().getEnterTestCode()); // .conjunction(tran.getTarget().getInvariant()

        }
        sb.append(testSettings.postfix);

        this.testCode = sb;
    }

    private String testCodeAssertClocks(State state) {
        String s = filterCDD(state.getInvariant().toString());

        for (Clock c : clocks) {
            s = s.replaceAll("(\\W)" + c.getOriginalName() + "(\\W)", "$1 cas."+ c.getOriginalName() + "$2");
        }

        return testSettings.assertPre + s + testSettings.assertPost;
        //return testSettings.assertPre + s + testSettings.assertPost;
    }

    private String testCodeUpdateClock(Clock clock, Integer value) {
        String s = "";

        s += "cas." + clock.getOriginalName() + " = " + value + ";\n";

        return s;
    }

    private String filterCDD (String s) {
        StringBuilder sb = new StringBuilder();
        s  = s.replace("(", "").replace(")", "");
        String[] parts = s.split("&&");


        List filteredParts = Arrays.stream(parts).filter(x -> containsClock(x)).collect(Collectors.toList());

        for (int i = 0; i < filteredParts.size(); i++) {
            if (i == filteredParts.size()-1) {
                sb.append(filteredParts.get(i).toString());
            }
            else {
                sb.append(filteredParts.get(i)).append("&&");
            }
        }

        return sb.toString();
    }

    private boolean containsClock(String part) {
        for (Clock c : getClocks()) {
            Pattern pattern = Pattern.compile("([^A-Za-z0-9_]*" + c.getOriginalName() + "[<>-]+)");
            Matcher m = pattern.matcher(part);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    public CDD helperConjoin(String guardString, CDD orgCDD) {
        Guard g = GuardParser.parse(guardString, getClocks(), getBVs());
        CDD cdd = orgCDD.conjunction(new CDD(g));

        return cdd;
    }
    public int minClockValue(CDD orgCDD, Clock clock){

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

    public List<BoolVar> getBVs() {
        return BVs;
    }
}
