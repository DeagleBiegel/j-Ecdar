package features;

import logic.SimpleTransitionSystem;
import logic.Transition;
import models.*;
import org.junit.BeforeClass;
import org.junit.Test;
import parser.JSONParser;
import java.io.FileNotFoundException;
import java.util.List;



public class MinClockValue {

    private static SimpleTransitionSystem sts1, sts2;

    @BeforeClass
    public static void setUpBeforeClass() throws FileNotFoundException {
        Automaton[] aut1 = JSONParser.parse("samples/json/minClockTest", false);
        sts1 = new SimpleTransitionSystem(aut1[0]);
        sts2 = new SimpleTransitionSystem(aut1[1]);
    }

    @Test
    public void minClockValueGreaterThanTest() {
        boolean initialisedCdd = CDD.tryInit(sts1.getClocks(), sts1.getBVs());
        List<Transition> path = null;
        for (Channel action : sts1.getActions()){
            path = sts1.getNextTransitions(sts1.getInitialState(), action, sts1.getClocks());
        }

        //System.out.println(sts1.minClockValue(path.get(0).getTarget().getInvariant(), sts1.getClocks().get(0)));
        assert sts1.minClockValue(path.get(0).getTarget().getInvariant(), sts1.getClocks().get(0)) == 11;
        if (initialisedCdd) {
            CDD.done();
        }
    }

    @Test
    public void minClockValueLessThanTest(){
        boolean initialisedCdd = CDD.tryInit(sts2.getClocks(), sts2.getBVs());
        List<Transition> path = null;
        for (Channel action : sts2.getActions()){
            path = sts2.getNextTransitions(sts2.getInitialState(), action, sts2.getClocks());
        }

        assert sts2.minClockValue(path.get(0).getTarget().getInvariant(), sts2.getClocks().get(0)) == 0;
        if (initialisedCdd) {
            CDD.done();
        }
    }

}
