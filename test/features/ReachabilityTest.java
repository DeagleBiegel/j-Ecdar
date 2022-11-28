package features;

import com.google.gson.JsonParser;
import logic.SimpleTransitionSystem;
import logic.Transition;
import models.Automaton;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Target;
import java.util.List;

public class ReachabilityTest {

    private static SimpleTransitionSystem sts1, sts2, sts3;

    @BeforeClass
    public static void setUpBeforeClass() throws FileNotFoundException {
        Automaton[] aut1 = JSONParser.parse("samples/json/isReachableTest", false);
        sts1 = new SimpleTransitionSystem(aut1[0]);
        sts2 = new SimpleTransitionSystem(aut1[1]);
        Automaton[] aut2 = JSONParser.parse("samples/json/FastestTraceMultiplePathsMultipleAssignExample", false);
        sts3 = new SimpleTransitionSystem(aut2[0]);
    }

    @Test
    public void simpleReachability(){
        assert sts1.isReachable("L1");
        assert sts1.isReachable("L2") == false;
    }

    @Test
    public void ReachabilityTest(){
        assert sts2.isStateReachable("L4", "x == 6");
        assert sts2.isStateReachable("L4", "x < 6") == false;
        assert sts2.isStateReachable("L5", "") == false;

    }

    @Test
    public void FastestTrace() throws IOException {
        List<Transition> fastestPath;


        sts3.fastestPath("L4");
    }

    public ReachabilityTest() throws FileNotFoundException {
    }


}
