import logic.SimpleTransitionSystem;
import logic.TransitionSystem;
import models.Automaton;
import models.Location;
import parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Playground {

    public static void main(String[] args) {
        try {
            Automaton[] automaton = JSONParser.parse("samples/json/ShortestTraceExample", true);

            for (var a : automaton) {
                SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

                /*
                for (var x : a.getLocations()) {
                    System.out.println(STS.isReachable(x.getName()));
                }
                 */

                STS.generateTrace("L6");
                //System.out.println(STS.isStateReachable("L1", "x >= 3 && x <= 6"));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
