import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import logic.TransitionSystem;
import models.*;
import parser.JSONParser;
import parser.XMLParser;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Playground {

    public static void main(String[] args) throws IOException, FileNotFoundException {

        Automaton[] automaton = JSONParser.parse("samples/json/casecdfactor10", false);


        for (var a : automaton) {
            Clock z = new Clock("z", a.getName());
            a.getClocks().add(z);

            SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

            STS.explore();
            //STS.allFastestPaths();
        }
}

}
