package testsuite;

import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestSuite {

    public SimpleTransitionSystem ts;

    public Automaton automaton;
    private TestSettings testSettings;
    public List<TestCase> testCases = new ArrayList<>();

    public TestSuite(Automaton automaton, String prefix, String postfix, String timeStampFunc, String clockType, String assertPre, String assertPost) {
        this.automaton = automaton;
        this.testSettings = new TestSettings(prefix, postfix, timeStampFunc, clockType,assertPre,assertPost);
    }

    public void createTestSuite() throws IOException {

        for (Edge e : automaton.getEdges()) {
            String boolName = e.getSource().getName() + e.getTarget().getName();
            BoolVar bv = new BoolVar(automaton.getName(), boolName, false);
            automaton.getBVs().add(bv);
            e.getUpdates().add(new BoolUpdate(bv, true));
            ts = new SimpleTransitionSystem(automaton);

            boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());
            TestCase tc = new TestCase(ts.explore(e.getTarget().getName(), bv.getOriginalName() + "== true"), testSettings, ts.getClocks());
            tc.createTestCode();
            testCases.add(tc);

            //testCases = testCases.stream().sorted(Comparator.comparingInt(List::size)).collect(Collectors.toList());
            e.getUpdates().remove(e.getUpdates().size()-1);
            automaton.getBVs().remove(automaton.getBVs().size()-1);

            if (initialisedCdd) {
                CDD.done();
            }
        }

        List<TestCase> finalTraces = testCases;
        testCases = testCases.stream().filter(s -> isPrefix(s, finalTraces)).collect(Collectors.toList());

        printToFile();
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

        for (int i = 0; i < testCases.size(); i++) {
            try {
                FileWriter writer = new FileWriter("testcode" + i + ".txt", true);
                writer.write(testCases.get(i).getTestCode().toString());
                writer.write("\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
