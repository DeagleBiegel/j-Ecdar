package testsuite;

import logic.Transition;
import models.*;
import parser.GuardParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BVA {
    Automaton automaton;
    private HashMap<String, Boolean> guards = new HashMap<>();
    public List<TestCase> testcases = new ArrayList<>();


    public BVA(List<TestCase> testCases, Automaton automaton) {
        this.automaton = automaton;
       for (TestCase tc : testCases) {
           for (Transition trans : tc.getTrace()) {
               if (trans.getEdges().get(0).getGuard().toString() != "true") {
                   if (!guards.containsKey(trans.getEdges().get(0).toString())) {
                       guards.put(trans.getEdges().get(0).toString(), true);
                       TestCase newTestCase = new TestCase(tc);
                       testcases.addAll(computeEdgeDelays(newTestCase, trans));
                   }
               }
           }
       }
    }


    public List<TestCase> computeEdgeDelays(TestCase tc, Transition trans) {
        List<TestCase> tcs = new ArrayList<>();
        int ub = maxClockValue(trans.getGuardCDD(), getClock(trans.getEdges().get(0).getGuard().toString()));
        int ub1 = maxClockValue(trans.getGuardCDD(), getClock(trans.getEdges().get(0).getGuard().toString())) + 1;
        int lb = minClockValue(trans.getGuardCDD(), getClock(trans.getEdges().get(0).getGuard().toString()));
        int lb1 = 0;
        if (lb > 0) {
            lb1 = minClockValue(trans.getGuardCDD(), getClock(trans.getEdges().get(0).getGuard().toString())) - 1;
        }
        System.out.print(trans.getEdges().get(0));
        System.out.println(lb1 + " " + lb + " " + ub + " " + ub1);

        if (ub == lb) {
            TestCase testCaseUB = new TestCase(tc);
            testCaseUB.createTestCode(trans.getEdges().get(0).toString(), ub, false);
            tcs.add(testCaseUB);
        } else {
            TestCase testCaseUB = new TestCase(tc);
            testCaseUB.createTestCode(trans.getEdges().get(0).toString(), ub, false);
            tcs.add(testCaseUB);
            TestCase testCaseLB = new TestCase(tc);
            testCaseLB.createTestCode(trans.getEdges().get(0).toString(), lb, false);
            tcs.add(testCaseLB);
        }
        TestCase testCaseUB1 = new TestCase(tc);
        testCaseUB1.createTestCode(trans.getEdges().get(0).toString(), ub1, true);
        tcs.add(testCaseUB1);
        if (lb > 0) {
            TestCase testCaseLB1 = new TestCase(tc);
            testCaseLB1.createTestCode(trans.getEdges().get(0).toString(), lb1, true);
            tcs.add(testCaseLB1);
        }
        return tcs;
    }


    private Clock getClock(String guard) {
        Pattern pattern = Pattern.compile("([A-Za-z])");
        Matcher m = pattern.matcher(guard.toString());

        while (m.find()){

            for (Clock c : automaton.getClocks()) {
                if (c.getOriginalName().equals(m.group())) {
                    return c;
                }
            }
        }

        return null;
    }


    private CDD helperConjoin(String guardString, CDD orgCDD) {
        Guard g = GuardParser.parse(guardString, automaton.getClocks(), automaton.getBVs());
        CDD cdd = orgCDD.conjunction(new CDD(g));

        return cdd;
    }
    private int minClockValue(CDD orgCDD, Clock clock){
        String guardTemplate = clock.getOriginalName() + " == ";
        int min = 0;
        while (true) {
            CDD cdd = helperConjoin(guardTemplate + min, orgCDD);
            if (cdd.isNotFalse()) {
                break;
            }
            min++;
        }
        return min;

    }

    public int  maxClockValue(CDD orgCDD, Clock clock){
        String guardTemplate = clock.getOriginalName() + " == ";
        int min = 1000;
        while (true) {
            CDD cdd = helperConjoin(guardTemplate + min, orgCDD);
            if (cdd.isNotFalse()) {
                break;
            }
            min--;
        }
        return min;

    }

}
