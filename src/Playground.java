import models.Automaton;
import parser.JSONParser;

import java.io.FileNotFoundException;

public class Playground {

    public static void main(String[] args) throws FileNotFoundException {
        Automaton[] list = JSONParser.parse("samples/json/AG",true);

        for (Automaton a: list) {
            System.out.println(a.getName());
        }

    }

}
