import logic.SimpleTransitionSystem;
import logic.TransitionSystem;
import models.Automaton;
import models.Location;
import parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Playground {

    public static void main(String[] args) throws IOException {
        Automaton[] list = JSONParser.parse("samples/json/TestProject1",true);

        for (Automaton a: list) {
            SimpleTransitionSystem s = new SimpleTransitionSystem(a);
            for (Location x: a.getLocations()) {
                System.out.println(s.isReachable(x));
            }

        }

    }

}
