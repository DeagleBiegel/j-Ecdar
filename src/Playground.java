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
            Automaton[] automaton = JSONParser.parse("samples/json/cofmachine", true);

            for (var a : automaton) {
                SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

                for (var x : a.getLocations()) {
                    System.out.println(STS.isStateReachable(x.getName(), "x == 2"));
                    //STS.generateTrace(x.getName());
                }

            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}
