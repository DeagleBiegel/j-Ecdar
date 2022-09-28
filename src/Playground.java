import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import logic.TransitionSystem;
import models.Automaton;
import models.Edge;
import models.Location;
import parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Playground {

    public static void main(String[] args) {
        try {
            Automaton[] automaton = JSONParser.parse("samples/json/FastestTraceMultiplePathsMultipleAssignExample", true);

            for (var a : automaton) {
                SimpleTransitionSystem STS = new SimpleTransitionSystem(a);
                STS.allPaths();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
