package features;

import logic.SimpleTransitionSystem;
import logic.Transition;
import models.*;
import org.junit.BeforeClass;
import org.junit.Test;
import parser.GuardParser;
import parser.JSONParser;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MinClockValue {

    private static SimpleTransitionSystem sts;

    @BeforeClass
    public static void setUpBeforeClass() throws FileNotFoundException {
        Automaton[] aut1 = JSONParser.parse("samples/json/MinClockTest", false);
        sts = new SimpleTransitionSystem(aut1[0]);
    }

    @Test
    public void minClockValueTest() {
        boolean initialisedCdd = CDD.tryInit(sts.getClocks(), sts.getBVs());
        List<Transition> path = null;
        for (Channel action : sts.getActions()){
            path = sts.getNextTransitions(sts.getInitialState(), action, sts.getClocks());
        }

        assert sts.binaryMinClockValue(path.get(0).getTarget().getInvariant(), sts.getClocks().get(0)) == 11;
        if (initialisedCdd) {
            CDD.done();
        }
    }

}
