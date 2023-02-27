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

            /*
            for (Location loc : a.getLocations()) {
                List<Edge> edges = a.getEdgesFromLocation(loc);
                for (Edge e : edges) {
                    if (!e.getSource().getName().equals(e.getTarget().getName())) {
                        BoolVar bv = new BoolVar(e.getSource().getName() + e.getTarget().getName(), a.getName(), false );
                        a.getBVs().add(bv);
                        Update update = new BoolUpdate(bv,true);
                        e.addToUpdates(update);
                    }
                }
            }
            */

            SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

            STS.explore();
            //STS.allFastestPaths();
        }
}

}
