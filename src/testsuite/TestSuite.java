package testsuite;

import logic.SimpleTransitionSystem;
import logic.Transition;
import models.*;;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestSuite {

    private SimpleTransitionSystem ts;
    private Automaton automaton;
    private TestSettings testSettings;
    private List<TestCase> testCases = new ArrayList<>();

    public TestSuite(Automaton automaton, String prefix, String postfix, String assertPre, String assertPost, String delayPre, String delayPost) {
        this.automaton = automaton;
        this.testSettings = new TestSettings(prefix, postfix,assertPre, assertPost, delayPre, delayPost);
    }


    public void createTestSuite(boolean prefix, boolean extend, boolean bound) throws IOException {
        String boolName = "edgeBoolean";
        BoolVar bv = new BoolVar(boolName, boolName, false);
        automaton.getBVs().add(bv);
        boolean initialisedCdd = CDD.tryInit(automaton.getClocks(),automaton.getBVs());

        for (Edge e : automaton.getEdges()) {
            //add bool assignment to the edge
            e.getUpdates().add(new BoolUpdate(bv, true));

            //Initialise a STS so that it contains the new bool assignment
            ts = new SimpleTransitionSystem(automaton);

            //Explore the state space and create a trace to the target of the edge
            //Expand the trace to next output edge and create test code
            TestCase tc = new TestCase(ts.createCoverEdgeTrace(e.getTarget().getName(), bv.getOriginalName() + " == true"), testSettings, ts);
            if (extend) {
                tc.setTrace(ts.expandTrace(tc.getTrace()));
            }
            tc.createTestCode();
            testCases.add(tc);

            //remove the bool assignment from the edge
            e.getUpdates().remove(e.getUpdates().size()-1);
        }

        //Remove traces that are a prefix of another trace
        if (prefix) {
            List<TestCase> finalTraces = testCases;
            testCases = testCases.stream().filter(s -> isPrefix(s, finalTraces)).collect(Collectors.toList());
        }

        //Create BVA variants of existing traces
        if (bound) {
            BVA bva = new BVA(testCases, automaton);
            testCases.addAll(bva.testcases);
        }

        if (initialisedCdd) {
            CDD.done();
        }
        printAllToFile();
    }

    private void findTransition(String source, String Target, List<TestCase> testCases) {
        for (TestCase c : testCases) {
            for (Transition t : c.getTrace()) {
                if (t.getSource().getLocation().getName().equals(source) && t.getTarget().getLocation().getName().equals(Target)) {
                    printTransition(c.getTrace());
                }
            }
        }
    }

    private void printTransition(List<Transition> trans) {
        for (Transition t : trans) {
            System.out.println(t.getSource().getLocation().getName() + " " + t.getTarget().getLocation().getName());
        }
        System.out.println();
    }

    private TestCase findApplicableTrace(String location) {
        for (TestCase tc : testCases) {
            for (Transition transition : tc.getTrace()) {
                if (transition.getSource().getLocation().getName().equals(location) && transition.getEdges().get(0).getStatus().equals("INPUT")) {
                    return tc;
                }
            }
        }
        return null;
    }

    public boolean isPrefix(TestCase testCase, List<TestCase> allTraces) {
        for (TestCase t : allTraces) {
            if (t.getTrace().size() > testCase.getTrace().size()) {
                boolean prefix = true;
                for (int i = 0; i < testCase.getTrace().size(); i++) {
                    String org = testCase.getTrace().get(i).getSource().getLocation().getName() + testCase.getTrace().get(i).getTarget().getLocation().getName();
                    String borg = t.getTrace().get(i).getSource().getLocation().getName() + t.getTrace().get(i).getTarget().getLocation().getName();

                    if (!org.equals(borg)) {
                            prefix = false;
                    }
                }
                if (prefix) {
                    return false;
                }
            }
        }
        return true;
    }

    public void printAllToFile() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < testCases.size(); i++) {
            sb.append(testSettings.prefix + i + "() {\n");
            sb.append(testCases.get(i).getTestCode());
        }

            try {
                FileWriter myWriter = new FileWriter("testcodes.txt", false);
                myWriter.write(sb.toString());
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


    }

}
