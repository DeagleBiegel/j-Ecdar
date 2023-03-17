import logic.*;
import models.*;
import parser.JSONParser;
import testsuite.TestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Playground {

    public static void main(String[] args) throws IOException, FileNotFoundException {

        Automaton[] automaton = JSONParser.parse("samples/json/casecdfactor10", false);

        for (var a : automaton) {
            Clock z = new Clock("globalclock", a.getName());
            a.getClocks().add(z);

            a.getBVs().add(new BoolVar(a.getName(), "coolBool", false));

            SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

            TestSuite TS = new TestSuite(STS,
                    "test {\n",
                    "}",
                    "System.currentTimeMillis();\n",
                    "double",
                    "assert(",
                    ");");

            TS.createTestSuite();
            //STS.explore();
            //STS.allFastestPaths();
        }
}

}
