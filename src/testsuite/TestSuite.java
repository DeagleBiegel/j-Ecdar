package testsuite;


import logic.SimpleTransitionSystem;
import logic.Transition;
import models.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestSuite {

    public SimpleTransitionSystem ts;

    public Automaton automaton;
    private TestSettings testSettings;
    public List<TestCase> testCases = new ArrayList<>();

    public TestSuite(Automaton automaton, String prefix, String postfix, String timeStampFunc, String clockType, String assertPre, String assertPost, String delayPre, String delayPost) {
        this.automaton = automaton;
        this.testSettings = new TestSettings(prefix, postfix, timeStampFunc, clockType,assertPre, assertPost, delayPre, delayPost);
    }

    public void createTestSuite() throws IOException {
        BVA bva = new BVA(automaton);

        for (Edge e : automaton.getEdges()) {
            String boolName = e.getSource().getName() + e.getTarget().getName();
            BoolVar bv = new BoolVar(boolName, boolName, false);
            automaton.getBVs().add(bv);
            e.getUpdates().add(new BoolUpdate(bv, true));
            ts = new SimpleTransitionSystem(automaton);

            boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());
            TestCase tc = new TestCase(ts.explore(e.getTarget().getName(), bv.getOriginalName() + "== true"), testSettings, ts.getClocks(), ts.getBVs());

            if (tc.getTrace().get(tc.getTrace().size()-1).getEdges().get(0).getStatus().equals("INPUT")) {
                tc.setTrace(ts.expandTrace(tc.getTrace()));
            }

            bva.computeInvariantDelays(tc.getTrace());

            tc.createTestCode();
            testCases.add(tc);

            //testCases = testCases.stream().sorted(Comparator.comparingInt(List::size)).collect(Collectors.toList());
            e.getUpdates().remove(e.getUpdates().size()-1);
            automaton.getBVs().remove(automaton.getBVs().size()-1);

            List<BoundaryValues> remove_list = new ArrayList<>();
            for (BoundaryValues boundaryValues : bva.getBoundaryValues()) {
                TestCase temp = findApplicableTrace(boundaryValues.getLocation());
                if (temp != null) {
                    for (Integer i : boundaryValues.getValues()) {
                        TestCase testCase = new TestCase(temp);
                        if (testCase.getTrace().get(testCase.getTrace().size()-1).getEdges().get(0).getStatus().equals("INPUT")) {
                            tc.setTrace(ts.expandTrace(tc.getTrace()));
                        }
                        testCase.createTestCode(boundaryValues.getLocation(), i);
                        testCase.getTestCode().append("\n//failing test maybe\n");
                        testCases.add(testCase);
                    }
                    remove_list.add(boundaryValues);
                }
            }

            for (BoundaryValues boundaryValues : remove_list) {
                bva.getBoundaryValues().remove(boundaryValues);
            }

            if (initialisedCdd) {
                CDD.done();
            }
        }

        List<TestCase> finalTraces = testCases;
        testCases = testCases.stream().filter(s -> isPrefix(s, finalTraces)).collect(Collectors.toList());

        boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());

        findTransition("L6", "L1", testCases);
        if (initialisedCdd) {
            CDD.done();
        }

        printToFile();

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
        String originalTrace = "";
        for (Transition transition : testCase.getTrace()) {
            originalTrace += transition.getSource().getLocation().getName() + transition.getTarget().getLocation().getName();
        }

        for (TestCase t : allTraces) {
            String newTrace = "";
            for (Transition t1 : t.getTrace()) {
                newTrace += t1.getSource().getLocation().getName() + t1.getTarget().getLocation().getName();
            }
            //if original trace is in newTrace then bam.
            if (newTrace.contains(originalTrace) && !newTrace.equals(originalTrace)) {
                return false;
            }
        }
        return true;
    }


    public void printToFile() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < testCases.size(); i++) {
            sb.append(testSettings.prefix + i + "() {\n");
            sb.append(testCases.get(i).getTestCode());
        }

            try {
                FileWriter myWriter = new FileWriter("testcodes.txt");
                myWriter.write(sb.toString());
                myWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


    }

}
