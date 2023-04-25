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

    public TestSuite(Automaton automaton, String prefix, String postfix, String assertPre, String assertPost, String delayPre, String delayPost) {
        this.automaton = automaton;
        this.testSettings = new TestSettings(prefix, postfix,assertPre, assertPost, delayPre, delayPost);
    }


    public void createTestSuite() throws IOException {
        BVA bva = new BVA(automaton);

        for (Edge e : automaton.getEdges()) {
            String boolName = "xd";
            BoolVar bv = new BoolVar(boolName, boolName, false);
            automaton.getBVs().add(bv);
            e.getUpdates().add(new BoolUpdate(bv, true));
            ts = new SimpleTransitionSystem(automaton);

            boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());
            TestCase tc = new TestCase(ts.explore(e.getTarget().getName(), bv.getOriginalName() + " == true"), testSettings, ts.getClocks(), ts.getBVs());

            tc.setTrace(ts.expandTrace(tc.getTrace()));

            bva.computeInvariantDelays(tc.getTrace());

            tc.createTestCode();
            testCases.add(tc);

            e.getUpdates().remove(e.getUpdates().size()-1);
            automaton.getBVs().remove(automaton.getBVs().size()-1);


            //testCases = testCases.stream().sorted(Comparator.comparingInt(List::size)).collect(Collectors.toList());
            List<BoundaryValues> remove_list = new ArrayList<>();

            for (BoundaryValues boundaryValues : bva.getBoundaryValues()) {
                TestCase temp = findApplicableTrace(boundaryValues.getLocation());
                if (temp != null && temp.getTrace() != null) {
                    for (Integer i : boundaryValues.getValues()) {
                        TestCase testCase = new TestCase(temp);
                        testCase.createTestCode(boundaryValues.getLocation(), i);
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
        /*
        StringBuilder originalTrace = new StringBuilder();
        for (Transition transition : testCase.getTrace()) {
            originalTrace.append(transition.getSource().getLocation().getName()).append(transition.getTarget().getLocation().getName());
        }
         */

        for (TestCase t : allTraces) {
            if (t.getTrace().size() > testCase.getTrace().size()) {
                boolean prefix = true;
                for (int i = 0; i < testCase.getTrace().size(); i++) {
                    String org = testCase.getTestCode().toString();//testCase.getTrace().get(i).getSource().getLocation().getName() + testCase.getTrace().get(i).getTarget().getLocation().getName();
                    String borg = t.getTestCode().toString();//t.getTrace().get(i).getSource().getLocation().getName() + t.getTrace().get(i).getTarget().getLocation().getName();

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

    public void printSingleToFile(TestCase tc, Integer number) {
        StringBuilder sb = new StringBuilder();
        sb.append(testSettings.prefix + number + "() {\n");
        sb.append(tc.getTestCode());
        try {
            FileWriter myWriter = new FileWriter("testcodes.txt", true);
            myWriter.write(sb.toString());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
