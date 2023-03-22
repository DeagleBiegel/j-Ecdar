package testsuite;

import logic.State;
import logic.Transition;
import models.*;

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

    public TestCase(List<Transition> trace, TestSettings testSettings, List<Clock> clocks) {
        this.trace = trace;
        this.testSettings = testSettings;
        this.clocks = clocks;
    }

    public TestCase(TestCase testCase) {
        this.trace = testCase.getTrace();
        this.testSettings = testCase.getTestSettings();
        this.clocks = testCase.clocks;
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
        StringBuilder sb = new StringBuilder(testSettings.prefix + "\n");

        sb.append(testCodeInitClocks());
        HashMap<String, Boolean> booleans = new HashMap<>();

        //initialise hashmap of boolean variables
        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        //create test code for initial location
        sb.append(testCodeAssertClocks(trace.get(0).getSource()));
        sb.append(parseTestCode(new StringBuilder(trace.get(0).getSource().getLocation().getEnterTestCode()), booleans, trace.get(0).getSource().getInvariant()));

        for (Transition tran : trace) {
            sb.append(parseTestCode(new StringBuilder(tran.getSource().getLocation().getExitTestCode()), booleans, tran.getSource().getInvariant()));
            sb.append(testCodeAssertClocks(tran.getSource()));
            sb.append(parseTestCode(new StringBuilder(tran.getEdges().get(0).getTestCode()), booleans, tran.getEdges().get(0).getGuardCDD()));

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
            sb.append(parseTestCode(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), booleans, tran.getTarget().getInvariant()));

        }
        sb.append(testSettings.postfix);

        this.testCode = sb;
    }

    public void createTestCode(String location, int delay) {
        // Start the test case by declaring and initialising all clocks
        StringBuilder sb = new StringBuilder(testSettings.prefix + "\n");

        sb.append(testCodeInitClocks());
        HashMap<String, Boolean> booleans = new HashMap<>();

        //initialise hashmap of boolean variables
        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        //create test code for initial location
        sb.append(testCodeAssertClocks(trace.get(0).getSource()));
        sb.append(parseTestCode(new StringBuilder(trace.get(0).getSource().getLocation().getEnterTestCode()), booleans, trace.get(0).getSource().getInvariant()));

        for (Transition tran : trace) {
            if(tran.getSource().getLocation().getName().equals(location) && tran.getEdges().get(0).getStatus().equals("INPUT")) {
                sb.append(testSettings.delayPre + delay + testSettings.delayPost);
            }

            sb.append(parseTestCode(new StringBuilder(tran.getSource().getLocation().getExitTestCode()), booleans, tran.getSource().getInvariant()));
            sb.append(testCodeAssertClocks(tran.getSource()));
            sb.append(parseTestCode(new StringBuilder(tran.getEdges().get(0).getTestCode()), booleans, tran.getEdges().get(0).getGuardCDD()));

            // If there are updates in the edge it is added to the test code (only for clocks right now)
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
            sb.append(parseTestCode(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), booleans, tran.getTarget().getInvariant()));

        }
        sb.append(testSettings.postfix);

        this.testCode = sb;
    }


    private String testCodeAssertClocks(State state) {
        String s = state.getInvariant().toString();

        for (Clock c : clocks) {
            s = s.replaceAll("(\\W)" + c.getOriginalName() + "(\\W)", "$1 "+ c.getOriginalName() + "-timestamp $2");
        }

        return "timeStamp = " + testSettings.timeStampFunc + testSettings.assertPre + s + testSettings.assertPost;
    }

    private String testCodeInitClocks() {
        String s = testSettings.clockType + " timeStamp = " + testSettings.timeStampFunc;

        for (Clock c : clocks) {
            s += testSettings.clockType + " " + c.getOriginalName() + " = " + "timeStamp;\n";
        }

        return s;
    }
    
    private String testCodeUpdateClock(Clock clock, Integer value) {
        String s = "";

        if (value == 0) {
            s += clock.getOriginalName() + " = " + testSettings.timeStampFunc;
        }
        else {
            s += clock.getOriginalName() + " = " + testSettings.timeStampFunc;
        }

        return s;
    }

    private StringBuilder parseTestCode(StringBuilder sb, HashMap<String, Boolean> booleans, CDD cdd) {
        String testCode = "";

        //If checks if there is a variable to be "replaced", denoted by $
        if (sb.indexOf("$") != -1) {
            int startIndex = sb.indexOf("$");
            int endIndex = 0;

            Pattern pattern = Pattern.compile("\\$[a-z,A-Z,0-9]+");
            Matcher m = pattern.matcher(sb.toString());
            while (m.find()){
                endIndex = m.end();
            }
            String key = sb.subSequence(startIndex+1, endIndex).toString();

            if (booleans.containsKey(key)) {
                testCode = booleans.get(key).toString();
            }

            for (Clock c : clocks) {
                if (sb.subSequence(startIndex+1, endIndex).equals(c.getOriginalName())) {
                    testCode = filterCDD(cdd.toString(), sb.subSequence(startIndex+1, endIndex).toString()).toString();
                }
            }

            sb.replace(startIndex, endIndex, testCode);
            return parseTestCode(sb, booleans, cdd);
        }
        return sb;
    }

    private StringBuilder filterCDD (String s, String name) {
        StringBuilder sb = new StringBuilder();
        s  = s.replace("(", "").replace(")", "");
        String[] parts = s.split("&&");

        List filteredParts = Arrays.stream(parts).filter(x -> containsClock(x, name) || containsClock(x, "z")).collect(Collectors.toList());

        for (int i = 0; i < filteredParts.size(); i++) {
            if (i == filteredParts.size()-1) {
                sb.append(filteredParts.get(i).toString());
            }
            else {
                sb.append(filteredParts.get(i)).append("&&");
            }
        }

        return sb;
    }

    private boolean containsClock(String s, String name) {
        Pattern pattern = Pattern.compile("([^A-Za-z0-9_]*" + name + "[<>-]+)");
        Matcher m = pattern.matcher(s);

        return m.find();
    }
}
