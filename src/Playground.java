import logic.*;
import models.*;
import parser.JSONParser;
import testsuite.TestSuite;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Playground {

    public static void main(String[] args) throws IOException, FileNotFoundException {

        Automaton[] automaton = JSONParser.parse("samples/json/CoffeeTeaMachine", false);

        for (var a : automaton) {
            Clock z = new Clock("g", a.getName());
            a.getClocks().add(z);

            TestSuite TS = new TestSuite(a,
                    "@Test \npublic void test",
                    "}\n",
                    "assertTrue(",
                    ");\n",
                    "wait(",
                    ");\n");

            TS.createTestSuite(true, true, false);

        }
}

}
