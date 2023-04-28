package testsuite;

import logic.*;
import models.*;
import parser.GuardParser;
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
    private SimpleTransitionSystem STS;

    public TestCase(List<Transition> trace, TestSettings testSettings, SimpleTransitionSystem STS) {
        this.trace = trace;
        this.testSettings = testSettings;
        this.STS = STS;
    }

    public TestCase(TestCase testCase) {
        this.trace = testCase.getTrace();
        this.testSettings = testCase.getTestSettings();
        this.STS = testCase.getSTS();
    }

    public SimpleTransitionSystem getSTS() {
        return STS;
    }

    public List<Clock> getClocks() {
        return STS.getClocks();
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
        //create test code for initial location
        //assert clocks then get the test code from enter location
        sb.append(testCodeAssertClocks(trace.get(0).getSource().getInvariant()));
        sb.append(trace.get(0).getSource().getLocation().getEnterTestCode());

        for (Transition tran : trace) {
            int x  = (minClockValue(tran.getTarget().getInvariant(), getClocks().get(getClocks().size() - 1)) - minClockValue(tran.getSource().getInvariant(), getClocks().get(getClocks().size() - 1)));
            sb.append("cas.wait(" + x + ");\n");
            //get exit test code
            sb.append(tran.getSource().getLocation().getExitTestCode());
            sb.append(testCodeAssertClocks(tran.getSource().getInvariant()));

            //get test code for edge
            sb.append(tran.getEdges().get(0).getTestCode());

            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof ClockUpdate) {
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                    }
                }
            }

            sb.append(testCodeAssertClocks(tran.getTarget().getInvariant()));
            sb.append(tran.getTarget().getLocation().getEnterTestCode()); // .conjunction(tran.getTarget().getInvariant()

        }
        sb.append(testSettings.postfix);

        this.testCode = sb;
    }

    public void createTestCode(String location, int delay) {
        StringBuilder sb = new StringBuilder();

        //create test code for initial location
        //assert clocks then get the test code from enter location
        sb.append(testCodeAssertClocks(trace.get(0).getSource().getInvariant()));
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
            sb.append(testCodeAssertClocks(tran.getSource().getInvariant()));

            //get test code for edge
            sb.append(tran.getEdges().get(0).getTestCode());

            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof ClockUpdate) {
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                    }
                }
            }

            sb.append(testCodeAssertClocks(tran.getTarget().getInvariant()));
            sb.append(tran.getTarget().getLocation().getEnterTestCode());
        }
        sb.append(testSettings.postfix);
        sb.append("//BVA Variant\n");

        this.testCode = sb;
    }

    //creates test code for clock assertions
    private String testCodeAssertClocks(CDD state) {
        String s = filterCDD(state.toString());

        for (Clock c : getClocks()) {
            s = s.replaceAll("(\\W)" + c.getOriginalName() + "(\\W)", "$1" + "cas."+ c.getOriginalName() + "$2");
        }

        s = s.replaceFirst(" ", "");
        return testSettings.assertPre + s + testSettings.assertPost;
    }

    //creates test code for clock assignments
    private String testCodeUpdateClock(Clock clock, Integer value) {
        return "cas." + clock.getOriginalName() + " = " + value + ";\n";
    }

    //Filters out boolean variables from a CDD string
    private String filterCDD (String s) {
        StringBuilder sb = new StringBuilder();
        s  = s.replace("(", "").replace(")", "");
        String[] parts = s.split("&&");


        List filteredParts = Arrays.stream(parts).filter(this::containsClock).collect(Collectors.toList());

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

    //Returns true if the string contains a clock variable. Used to filter out subexpression from a CDD that contain boolean variables
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


    private Clock LocInvariantClock(String part) {
        for (Clock c : getClocks()) {
            Pattern pattern = Pattern.compile("([^A-Za-z0-9_]*" + c.getOriginalName() + "[<>-]+)");
            Matcher m = pattern.matcher(part);
            if (m.find()) {
                return c;
            }
        }
        return null;
    }

    //conjoins 2 CDD's
    public CDD helperConjoin(String guardString, CDD orgCDD) {
        Guard g = GuardParser.parse(guardString, getClocks(), getBVs());
        CDD cdd = orgCDD.conjunction(new CDD(g));

        return cdd;
    }

    //Finds the smallest clock value for a given state and clock
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

    public int getTraceSize() {
        return getTrace().size();
    }
    public List<BoolVar> getBVs() {
        return STS.getBVs();
    }
}
