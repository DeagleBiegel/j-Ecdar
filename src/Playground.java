import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import logic.TransitionSystem;
import models.*;
import parser.JSONParser;
import parser.XMLParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Playground {

    public static void main(String[] args) throws IOException, FileNotFoundException {
        Automaton[] automaton = JSONParser.parse("samples/json/casecd.json", true);

            for (var a : automaton) {
                Clock z = new Clock("z", a.getName());
                a.getClocks().add(z);
                SimpleTransitionSystem STS = new SimpleTransitionSystem(a);

                List<Transition> lst = STS.fastestPath("L1");

                //STS.realFastestTrace(STS.fastestPath());
            }



    }

}
