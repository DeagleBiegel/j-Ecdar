import logic.SimpleTransitionSystem;
import logic.TransitionSystem;
import models.Automaton;
import models.Location;
import parser.JSONParser;

import java.io.FileNotFoundException;

public class Playground {

    public static void main(String[] args) throws FileNotFoundException {
        Automaton[] list = JSONParser.parse("samples/json/TestProject1",true);

        for (Automaton a: list) {
            SimpleTransitionSystem s = new SimpleTransitionSystem(a);
            System.out.println(s.isReachable(a.getLocations().get(0)));
        }

    }

}
