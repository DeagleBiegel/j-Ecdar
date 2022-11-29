package features;

import javafx.util.Pair;
import logic.SimpleTransitionSystem;
import logic.State;
import logic.Transition;
import models.Automaton;
import models.CDD;
import models.Clock;
import models.Guard;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import parser.GuardParser;
import parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class ReachabilityTest {

    private static SimpleTransitionSystem sts1, sts2, sts3, sts4;

    @BeforeClass
    public static void setUpBeforeClass() throws FileNotFoundException {
        Automaton[] aut1 = JSONParser.parse("samples/json/isReachableTest", true);
        sts1 = new SimpleTransitionSystem(aut1[0]);
        sts2 = new SimpleTransitionSystem(aut1[1]);

        Automaton[] aut2 = JSONParser.parse("samples/json/fastestPatrh", true);
        Clock z = new Clock("z", aut2[0].getName());
        aut2[0].getClocks().add(z);
        sts3 = new SimpleTransitionSystem(aut2[0]);

        Automaton[] aut3 = JSONParser.parse("samples/json/ShortestTraceExample", true);
        sts4 = new SimpleTransitionSystem(aut3[0]);
    }

    @Test
    public void simpleReachability(){
        assert sts1.isReachable("L1");
        assert sts1.isReachable("L2") == false;
    }

    @Test
    public void ReachabilityTest(){
        assert sts2.isStateReachable("L4", "x == 6");
        assert sts2.isStateReachable("L4", "x > 6");
        assert sts2.isStateReachable("L4", "x < 6") == false;
        assert sts2.isStateReachable("L5", "") == false;

    }

    @Test
    public void ShortestTrace() {
        List<Transition> shortestPath = sts4.shortestPath("L4");

        assert shortestPath.get(0).getSource().getLocation().getName().equals("L7");
        assert shortestPath.get(0).getTarget().getLocation().getName().equals("L0");
        assert shortestPath.get(1).getSource().getLocation().getName().equals("L0");
        assert shortestPath.get(1).getTarget().getLocation().getName().equals("L1");
        assert shortestPath.get(2).getSource().getLocation().getName().equals("L1");
        assert shortestPath.get(2).getTarget().getLocation().getName().equals("L4");
    }

    @Test
    public void FastestTrace() throws IOException {
        List<Transition> fastestPath = sts3.fastestPath("L1");
        boolean initialisedCdd = CDD.tryInit(sts3.getClocks(), sts3.getBVs());
        Guard g = GuardParser.parse("z == 6", sts3.getClocks(), sts3.getBVs());

        assert fastestPath.get(0).getSource().getLocation().getName().equals("L0");
        assert fastestPath.get(0).getTarget().getLocation().getName().equals("L2");
        assert !fastestPath.get(0).getTarget().getInvariant().conjunction(new CDD(g)).isTrue();

        assert fastestPath.get(1).getSource().getLocation().getName().equals("L2");
        assert fastestPath.get(1).getTarget().getLocation().getName().equals("L2");
        assert !fastestPath.get(1).getTarget().getInvariant().conjunction(new CDD(g)).isTrue();

        assert fastestPath.get(2).getSource().getLocation().getName().equals("L2");
        assert fastestPath.get(2).getTarget().getLocation().getName().equals("L1");
        assert !fastestPath.get(2).getTarget().getInvariant().conjunction(new CDD(g)).isTrue();

        if (initialisedCdd) {
            CDD.done();
        }
    }
}
