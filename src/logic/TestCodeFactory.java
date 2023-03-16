package logic;

import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestCodeFactory {

    public SimpleTransitionSystem ts;
    private String prefix;
    private String postfix;
    private String clockType;
    private String timeStampFunc;
    private String assertPre;
    private String assertPost;
    public TestCodeFactory(SimpleTransitionSystem ts, String prefix, String postfix, String timeStampFunc, String clockType, String assertPre, String assertPost) {
        this.ts = ts;
        this.prefix = prefix;
        this.postfix = postfix;
        this.clockType = clockType;
        this.timeStampFunc = timeStampFunc;
        this.assertPre = assertPre;
        this.assertPost = assertPost;
    }

    public void createTestSuite() throws IOException {
        boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());

        List<List<Transition>> traces = ts.explore();

        for (List<Transition> ts : traces) {
            generateTestCode(ts);
        }

        if (initialisedCdd) {
            CDD.done();
        }

    }
    public void generateTestCode(List<Transition> trace) {
        // Start the test case by declaring and initialising all clocks
        StringBuilder sb = new StringBuilder(prefix + "\n");

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

            //check the transition for updates
            // Clock updates are being considered by adding a "clock reset" to the test code
            // What do we do with the test code in the edge? pre-update values or post?
            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof BoolUpdate) {
                        booleans.put(((BoolUpdate)update).getBV().getUniqueName(), ((BoolUpdate)update).getValue());
                    }
                    else if (update instanceof ClockUpdate) {
                        //how to approach resets that are not 0?
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                        //sb.append(new StringBuilder("clock " + ((ClockUpdate) update).getClock().toString() + "is set to " + ((ClockUpdate) update).getValue()) + "\n");
                    }
                }
            }
            sb.append(testCodeAssertClocks(tran.getTarget()));
            sb.append(parseTestCode(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), booleans, tran.getTarget().getInvariant()));

        }
        sb.append(postfix);
        sb.append("\nend of test code\n");
        try {
            //PrintWriter printerWriter = new PrintWriter("testcode.txt");
            //printerWriter.println("");
            //printerWriter.close();
            FileWriter writer = new FileWriter("testcode.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String testCodeEditClocks(State state) {

        return "";
    }
    private String testCodeAssertClocks(State state) {
        //String s = "double currentTime = time.now();\n";

        //s += "assert("+ state.getInvariant().toString() + ");\n";
        String s = state.getInvariant().toString();

        for (Clock c : ts.getClocks()) {
            String patternString = "(\\W)" + c.getOriginalName() + "(\\W)";
            s = s.replaceAll(patternString, "$1" + c.getOriginalName() + ".value(temp)$2");
            //s = s.replaceAll("(?:( |-|\\()" + c.getOriginalName() + ")" , " " + c.getOriginalName() + ".value(temp) ");
        }


        //\b[A-Z, a-z]\s
        String temp = "temp = System.currentTimeMillis();\n" + "assert("+ s +");\n";

        return temp;
    }

    private String testCodeInitClocks() {
        String s = clockType + " timeStamp = System.currentTimeMillis();\n";
        for (Clock c : ts.getClocks()) {
            s += clockType + " " + c.getOriginalName() + " = " + "timeStamp\n";
        }
        s += clockType + " temp =" + timeStampFunc + ";\n";
        return s;
    }

    private String testCodeUpdateClock(Clock clock, Integer value) {
        String s = "";
        if (value == 0) {
            s += clock.getOriginalName() + ".time = System.currentTimeMillis();\n";
        }
        else {
            s += clock.getOriginalName() + ".time = System.currentTimeMillis() -" + value + ";\n";
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

            for (Clock c : ts.getMaxBounds().keySet()) {
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

        while (m.find()){

            return true;
        }
        return false;
    }


}
