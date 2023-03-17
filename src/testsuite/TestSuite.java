package testsuite;

import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import models.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestSuite {

    public SimpleTransitionSystem ts;

    private TestSettings testSettings;
    public List<TestCase> testCases = new ArrayList<>();

    public TestSuite(SimpleTransitionSystem ts, String prefix, String postfix, String timeStampFunc, String clockType, String assertPre, String assertPost) {
        this.ts = ts;
        this.testSettings = new TestSettings(prefix, postfix, timeStampFunc, clockType,assertPre,assertPost);
    }

    public void createTestSuite() throws IOException {
        boolean initialisedCdd = CDD.tryInit(ts.getAutomaton().getClocks(), ts.getAutomaton().getBVs());

        List<List<Transition>> traces = ts.explore();

        for (List<Transition> trace : traces) {
            testCases.add(new TestCase(trace, testSettings, this.ts.getClocks()));
        }

        if (initialisedCdd) {
            CDD.done();
        }
        
        printToFile();
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
