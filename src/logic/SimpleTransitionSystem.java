package logic;

import javafx.util.Pair;
import models.*;
import parser.GuardParser;
import parser.XMLFileWriter;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.IOException;
import connection.Client;

public class SimpleTransitionSystem extends TransitionSystem{

    private boolean printComments = false;

    private final Automaton automaton;
    private Deque<State> waiting;
    private List<State> passed;
    private HashMap<String, List<Pair<Transition, Integer>>> transitionHashMap;
    private HashMap<Clock,Integer> maxBounds;
    private HashMap<Clock, Integer> clockValues;



    public SimpleTransitionSystem(Automaton automaton) {
        this.automaton = automaton;
        clocks.addAll(automaton.getClocks());
        BVs.addAll(automaton.getBVs());
        this.waiting = new ArrayDeque<>();
        this.passed = new ArrayList<>();
        setMaxBounds();
    }

    public Set<Channel> getInputs() {
        return automaton.getInputAct();
    }

    public Set<Channel> getOutputs() {
        return automaton.getOutputAct();
    }

    public SymbolicLocation getInitialLocation() {
        return new SimpleLocation(automaton.getInitial());
    }

    public List<SimpleTransitionSystem> getSystems() {
        return Collections.singletonList(this);
    }

    public String getName() {
        return automaton.getName();
    }

    public Automaton getAutomaton() {
        return automaton;
    }

    public void setMaxBounds()
    {
        // System.out.println("Max bounds: " + automaton.getMaxBoundsForAllClocks());
        HashMap<Clock,Integer> res = new HashMap<>();

        res.putAll(automaton.getMaxBoundsForAllClocks());
        //res.replaceAll(e -> e==0 ? 1 : e);
        maxBounds = res;
    }
    public HashMap<Clock,Integer> getMaxBounds(){
        return maxBounds;
    }

    // Checks if automaton is deterministic
    public boolean isDeterministicHelper() {

        Set<Channel> actions = getActions();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);


            toStore.extrapolateMaxBounds(this.getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions) {

                List<Transition> tempTrans = getNextTransitions(currState, action);
                if (checkMovesOverlap(tempTrans)) {
                    for (Transition t: tempTrans) {
                        System.out.println("next trans");
                        for (Edge e : t.getEdges())
                            System.out.println(e);
                    }
                    return false;
                }

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList()); // TODO I added waitingConstainsState... Okay??
                toAdd.forEach(e->e.extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                waiting.addAll(toAdd);
            }
        }

        return true;
    }

    // Check if zones of moves for the same action overlap, that is if there is non-determinism
    public boolean checkMovesOverlap(List<Transition> trans) {
        if (trans.size() < 2) return false;
        //System.out.println("check moves overlap -------------------------------------------------------------------");
        for (int i = 0; i < trans.size(); i++) {
            for (int j = i + 1; j < trans.size(); j++) {
                if (trans.get(i).getTarget().getLocation().equals(trans.get(j).getTarget().getLocation())
                        && trans.get(i).getEdges().get(0).hasEqualUpdates(trans.get(j).getEdges().get(0)))
                    continue;

                State state1 = new State(trans.get(i).getSource());
                State state2 = new State(trans.get(j).getSource());

                // TODO: do a transitionBack here, in case the target invariant was not included into the guards

                state1.applyGuards(trans.get(i).getGuardCDD());
                state2.applyGuards(trans.get(j).getGuardCDD());



                if (state1.getInvariant().isNotFalse() && state2.getInvariant().isNotFalse()) {
                    if(state1.getInvariant().intersects(state2.getInvariant())) {
                        /*System.out.println(CDD.toGuardList(trans.get(i).getGuardCDD(),clocks));
                        System.out.println(CDD.toGuardList(trans.get(j).getGuardCDD(),clocks));
                        System.out.println(trans.get(0).getEdges().get(0).getChannel());
                        System.out.println(trans.get(0).getEdges().get(0));
                        System.out.println(trans.get(1).getEdges().get(0));
                        System.out.println(CDD.toGuardList(state1.getInvarCDD(),clocks));
                        System.out.println(CDD.toGuardList(state2.getInvarCDD(),clocks));
                        // trans.get(j).getGuardCDD().printDot();
                        System.out.println(CDD.toGuardList(trans.get(i).getEdges().get(0).getGuardCDD(),clocks));
                        System.out.println(CDD.toGuardList(trans.get(j).getEdges().get(0).getGuardCDD(),clocks));
                        System.out.println("they intersect??!");*/
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public boolean isConsistentHelper(boolean canPrune) {
        //if (!isDeterministic()) // TODO: this was commented out, I added it again
        //    return false;
        passed = new ArrayList<>();
        waiting = new ArrayDeque<>();
        boolean result = checkConsistency(getInitialState(), getInputs(), getOutputs(), canPrune);
        return result;
    }

    public boolean checkConsistency(State currState, Set<Channel> inputs, Set<Channel> outputs, boolean canPrune) {

        if (passedContainsState(currState))
            return true;

        State toStore = new State(currState);

        toStore.extrapolateMaxBounds(getMaxBounds(),clocks.getItems());
        System.out.println(getMaxBounds());
        //if (passedContainsState(toStore))
        //    return true;
        passed.add(toStore);
        // Check if the target of every outgoing input edge ensures independent progress
        for (Channel channel : inputs) {
            List<Transition> tempTrans = getNextTransitions(currState, channel);
            for (Transition ts : tempTrans) {
                boolean inputConsistent = checkConsistency(ts.getTarget(), inputs, outputs, canPrune);
                if (!inputConsistent) {
                    System.out.println("Input inconsistent");
                    return false;
                }
            }
        }
        boolean outputExisted = false;
        // If delaying indefinitely is possible -> Prune the rest
        if (canPrune && currState.getInvariant().canDelayIndefinitely()) {
            return true;
        }
        // Else if independent progress does not hold through delaying indefinitely,
        // we must check for being able to output and satisfy independent progress
        else {
            for (Channel channel : outputs) {
                List<Transition> tempTrans = getNextTransitions(currState, channel);
                for (Transition ts : tempTrans) {
                    if(!outputExisted) outputExisted = true;
                    boolean outputConsistent = checkConsistency(ts.getTarget(), inputs, outputs, canPrune);
                    if (outputConsistent && canPrune)
                        return true;
                    if(!outputConsistent && !canPrune) {
                        return false;
                    }
                }
            }
            if(!canPrune) {
                if (outputExisted)
                    return true;
                return currState.getInvariant().canDelayIndefinitely();

            }
            // If by now no locations reached by output edges managed to satisfy independent progress check
            // or there are no output edges from the current location -> Independent progress does not hold

            else
            {
                return false;
            }
        }
    }

    public boolean isImplementationHelper(){
        Set<Channel> outputs = getOutputs();
        Set<Channel> actions = getActions();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);
            toStore.extrapolateMaxBounds(getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions){
                List<Transition> tempTrans = getNextTransitions(currState, action);

                if(!tempTrans.isEmpty() && outputs.contains(action)){
                    if(!outputsAreUrgent(tempTrans))
                        return false;
                }

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s)).collect(Collectors.toList());

                toAdd.forEach(s -> s.extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                waiting.addAll(toAdd);
            }
        }

        return true;
    }

    public boolean outputsAreUrgent(List<Transition> trans){
        for (Transition ts : trans){
            State state = new State(ts.getSource());
            state.applyGuards(ts.getGuardCDD());

            if(!state.getInvariant().isUrgent())
                return false;
        }
        return true;
    }

    private boolean passedContainsState(State state1) {
        State state = new State(state1);
        state.extrapolateMaxBounds(maxBounds, clocks.getItems());


        for (State passedState : passed) {
            //    System.out.print(" "+passedState.getLocation() + " " + CDD.toGuardList(passedState.getInvarCDD(),clocks));
            if (state.getLocation().equals(passedState.getLocation()) &&
                    state.getInvariant().isSubset((passedState.getInvariant()))) {
                return true;
            }
        }


        return false;
    }

    private boolean waitingContainsState(State state1) {
        State state = new State(state1);
        state.extrapolateMaxBounds(maxBounds, clocks.getItems());

        for (State passedState : waiting) {
            // check for zone inclusion
            if (state.getLocation().equals(passedState.getLocation()) &&
                    state.getInvariant().isSubset(passedState.getInvariant())) {
                return true;
            }
        }

        return false;
    }

    public List<Transition> getNextTransitions(State currentState, Channel channel, List<Clock> allClocks) {
        List<Move> moves = getNextMoves(currentState.getLocation(), channel);
        return createNewTransitions(currentState, moves, allClocks);
    }

    protected List<Move> getNextMoves(SymbolicLocation symLocation, Channel channel) {
        List<Move> moves = new ArrayList<>();

        Location location = ((SimpleLocation) symLocation).getActualLocation();
        List<Edge> edges = automaton.getEdgesFromLocationAndSignal(location, channel);

        for (Edge edge : edges) {
            SymbolicLocation target = new SimpleLocation(edge.getTarget());
            Move move = new Move(symLocation, target, Collections.singletonList(edge));
            moves.add(move);
        }

        return moves;
    }

    public void toXML(String filename)
    {
        XMLFileWriter.toXML(filename,this);
    }

    public void toJson(String filename)
    {
        JsonAutomatonEncoder.writeToJson(this.getAutomaton(),filename);
    }

    public SimpleTransitionSystem pruneReachTimed(){
        boolean initialisedCdd = CDD.tryInit(clocks.getItems(), BVs.getItems());

        //TODO: this function is not correct yet. // FIXED: 05.1.2021
        // In the while loop, we should collect all edges associated to transitions (not just all locations associated to states), and remove all that were never associated
        Set<Channel> outputs = getOutputs();
        Set<Channel> actions = getActions();
        // the set to store all locations we met during the exploration. All others will be removed afterwards.
        Set<Location> metLocations = new HashSet<Location>();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        List<Edge> passedEdges = new ArrayList<Edge>();


        // explore until waiting is empty, and add all locations that ever are in waiting to metLocations
        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            passed.add(new State(currState));
            metLocations.add(((SimpleLocation) currState.getLocation()).getActualLocation());
            for (Channel action : actions){
                List<Transition> tempTrans = getNextTransitions(currState, action);
                for (Transition t: tempTrans)
                {
                    for (Edge e : t.getEdges())
                        passedEdges.add(e);
                }
                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s)).collect(Collectors.toList());
                waiting.addAll(toAdd);
            }
        }

        List<Edge> edges = new ArrayList<Edge>();
        List<Location> locations = new ArrayList<Location>();

        // we only want to add edges to our new automaton, if their target and source were to / from explored locations
        for (Edge e : getAutomaton().getEdges())
        {
            boolean sourceMatched=false;
            boolean targetMatched=false;
            for (Location l: metLocations) {
                if (e.getTarget().getName().equals(l.getName()))
                    targetMatched= true;
                if (e.getSource().getName().equals(l.getName())) {
                    sourceMatched = true;
                }

            }
            if (sourceMatched && targetMatched && passedEdges.contains(e))    edges.add(e);
        }

        // add all explored locations
        for (Location l: metLocations)
        {
            locations.add(l);
        }

        Automaton aut = new Automaton(getName(), locations, edges, getClocks(),getAutomaton().getBVs(), false);

        if (initialisedCdd) {
            CDD.done();
        }
        return new SimpleTransitionSystem(aut);
    }

    private int getIndexOfClock(Clock clock, List<Clock> clocks) {
        for (int i = 0; i < clocks.size(); i++){
            if(clock.hashCode() == clocks.get(i).hashCode()) return i+1;
        }
        return 0;
    }


    //Reachability stuff
    public boolean isReachableHelper(String name) {
        System.out.println("------------");
        System.out.println("Is " + name + " reachable?");

        Set<Channel> actions = getActions();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(), clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions){
                List<Transition> tempTrans = getNextTransitions(currState, action);

                if (currState.getLocation().getName().equals(name)) {
                    return true;
                }

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList());

                waiting.addAll(toAdd);
            }
        }

        return false;
    }

    public boolean isStateReachableHelper(String name, String state) {
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(), clocks.getItems());
            passed.add(toStore);

            for (Channel action : getActions()){
                List<Transition> tempTrans = getNextTransitions(currState, action);

                if (currState.getLocation().getName().equals(name)) {
                    Guard g = GuardParser.parse(state,getClocks(),getBVs());
                    CDD cdd = new CDD(g);
                    CDD cdd1 = currState.getInvariant().conjunction(cdd);

                    if (!cdd1.toString().equals("false")) {
                        System.out.println(cdd1);
                        return true;
                    }
                    return false;
                }
                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList());

                waiting.addAll(toAdd);
            }
        }
        return false;
    }

    public int minClockValue(CDD guard, Clock clock) {
        String guardTemplate = clock.getOriginalName() + " == ";
        int min = 0;
        while (true) {
            String guardCopy = guardTemplate;
            guardCopy += String.valueOf(min);
            Guard g = GuardParser.parse(guardCopy, getClocks(), getBVs());
            CDD cdd = guard.conjunction(new CDD(g));

            if (!cdd.isFalse()) {
                break;
            }
            min++;
        }
        return min;
    }

    public List<Transition> shortestTraceHelper(String destination) {
        Set<Channel> actions = getActions();
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());
        transitionHashMap = new HashMap<>(262144);

        List<Transition> st = new ArrayList<>();

        boolean check = true;

        while (!waiting.isEmpty() && check) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> tempTrans = getNextTransitions(currState, action);

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList());
                toAdd.forEach(e->e.extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<Transition> newTransitions = tempTrans.stream().
                        filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget())).collect(Collectors.toList());
                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                for (Transition t : newTransitions) {
                    if (t.getTarget().getLocation().getName().equals(destination)) {
                        check = false;
                    }
                    if (!transitionHashMap.containsKey(t.getTarget().getLocation().getName())) {
                        transitionHashMap.put(t.getTarget().getLocation().getName(), new ArrayList<>());
                    }
                    transitionHashMap.get(t.getTarget().getLocation().getName()).add(new Pair<>(t, minClockValue(t.getTarget().getInvariant(), getClocks().get(getClocks().size()-1))));
                }
                waiting.addAll(toAdd);
            }
        }

        Transition shortestTrans = transitionHashMap.get(destination).get(0).getKey();
        st.add(shortestTrans);

        while (true) {
            if (transitionHashMap.containsKey(shortestTrans.getSource().getLocation().getName()) && !shortestTrans.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
                for (Pair<Transition, Integer> p : transitionHashMap.get(shortestTrans.getSource().getLocation().getName())) {
                    if (p.getKey().getTarget().toString().equals(shortestTrans.getSource().toString())) {
                        shortestTrans = p.getKey();
                        st.add(shortestTrans);
                    }
                }
            }
            else {
                break;
            }
        }
        Collections.reverse(st);
        return st;
    }

    public List<Transition> fastestPathHelper(String destination) throws IOException {
        Set<Channel> actions = getActions();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());
        transitionHashMap = new HashMap<>(262144);

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> tempTrans = getNextTransitions(currState, action);

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList());
                toAdd.forEach(e->e.extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<Transition> newTransitions = tempTrans.stream().
                        filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget())).collect(Collectors.toList());
                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                for (Transition t : newTransitions) {
                    if (!transitionHashMap.containsKey(t.getTarget().getLocation().getName())) {
                        transitionHashMap.put(t.getTarget().getLocation().getName(), new ArrayList<>());
                    }
                    transitionHashMap.get(t.getTarget().getLocation().getName()).add(new Pair<>(t, minClockValue(t.getTarget().getInvariant(), getClocks().get(getClocks().size()-1))));
                }

                waiting.addAll(toAdd);
            }
        }

        //find the the destination with the smallest global clock value
        int min = Integer.MAX_VALUE;
        Transition fastestTrans = null;
        for (String s : transitionHashMap.keySet()) {
            if (s.equals(destination)) {
                for (Pair<Transition, Integer> p : transitionHashMap.get(s)){
                    if (p.getValue() < min) {
                        min = p.getValue();
                        fastestTrans = p.getKey();
                    }
                }
            }
        }
        System.out.println(min);
        List<Transition> ft = new ArrayList<>();
        ft.add(fastestTrans);

        //follow trace   to start
        while (true) {
            if (transitionHashMap.containsKey(fastestTrans.getSource().getLocation().getName()) && !fastestTrans.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
                for (Pair<Transition, Integer> p : transitionHashMap.get(fastestTrans.getSource().getLocation().getName())) {
                    if (p.getKey().getTarget().toString().equals(fastestTrans.getSource().toString())) {
                        fastestTrans = p.getKey();
                        ft.add(fastestTrans);
                    }
                }
            }
            else {
                break;
            }
        }
        Collections.reverse(ft);

        //realFastestTrace(ft);

        return ft;
    }

    public void realFastestTrace(List<Transition> path) throws IOException {
        clockValues = new HashMap<>();

        for (Clock c : getClocks()) {
            clockValues.put(c,0);
        }

        Client client = new Client();
        client.startConnection("127.0.0.1", 6666);

        for (Transition t : path) {
            // if it is an output edge, we call the implementation and measure the time it takes.
            if(t.getEdges().get(0).getStatus().equals("OUTPUT")){
                client.writeString(t.getSource().getLocation().getName() + " " + t.getTarget().getLocation().getName());
                long start = System.currentTimeMillis();
                client.readString();
                long end = System.currentTimeMillis();
                updateClocks((int)Math.round((end - start)*0.001));
            }
            // if it is an input edge, we take the fastest transition
            else {
                //case: INPUT edge -> if the previous transition has exceeded the minimum clock value, otherwise we use min value
                for (Clock c : getClocks()) {
                    int temp = minClockValue(t.getTarget().getInvariant(), c);
                    if (clockValues.get(c) < temp) {
                        clockValues.put(c,temp);
                    }
                }
            }

            helperConjoin(t);
        }
        client.writeString("done");
        client.stopConnection();
    }

    public void updateClocks(int value) {
        for (Clock c : clockValues.keySet())  {
            int temp = clockValues.get(c);
            clockValues.put(c, temp + value);
        }
    }

    public CDD helperConjoin(Transition t) {
        StringBuilder sb = new StringBuilder();
        for (Clock c : getClocks()) {
            //String s = String.format("%s <= %2d && %s >= %2d", c.getOriginalName(), clockValues.get(c), c.getOriginalName(), clockValues.get(c)-1);
            String s = String.format("%s == %2d", c.getOriginalName(), clockValues.get(c));
            sb.append(s);
            if (!c.getOriginalName().equals("z")) {
                sb.append(" && ");
            }
        }

        System.out.println(sb);
        Guard g = GuardParser.parse(sb.toString(), getClocks(), getBVs());
        CDD cdd = t.getTarget().getInvariant().conjunction(new CDD(g));

        if (cdd.isFalse()) {
            System.out.println("CONSTRAINTS BROKEN");
            System.out.println(t.getEdges().get(0).getChan().getName());
            System.out.println("Guard: " + t.getTarget().getInvariant() + "\n");
        }
        else {
            System.out.println("CONSTRAINTS HELD");
            System.out.println(t.getEdges().get(0).getChan().getName());
            System.out.println("Guard: " + t.getTarget().getInvariant() + "\n");
        }

        return cdd;
    }



    //Testcode generation stuff
    public void generateTestCode(ArrayList<Transition> trace) {
        StringBuilder sb = new StringBuilder();
        HashMap<String, Boolean> booleans = new HashMap<>();

        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        sb.append(replaceVar(new StringBuilder(trace.get(0).getSource().getLocation().getEnterTestCode()), booleans, trace.get(0).getSource().getLocationInvariant()));

        for (Transition tran : trace) {
            sb.append(replaceVar(new StringBuilder(tran.getSource().getLocation().getExitTestCode()), booleans, tran.getGuardCDD()));
            sb.append(replaceVar(new StringBuilder(tran.getEdges().get(0).getTestCode()), booleans, tran.getGuardCDD()));

            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof BoolUpdate) {
                        booleans.put(((BoolUpdate)update).getBV().getUniqueName(), ((BoolUpdate)update).getValue());
                    }
                    else if (update instanceof ClockUpdate) {
                        sb.append(replaceClockVarUpdate(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), ((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                    }
                }
                sb.append(replaceVar(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), booleans, tran.getGuardCDD()));
            }

            sb.append("\n");

        }

        try {
            PrintWriter printerWriter = new PrintWriter("testcode.txt");
            printerWriter.println("");
            printerWriter.close();
            FileWriter writer = new FileWriter("testcode.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StringBuilder replaceVar (StringBuilder sb, HashMap<String, Boolean> booleans, CDD cdd) {
        String V = "";

        if (sb.indexOf("$") != -1) {
            int startIndex = sb.indexOf("$");
            int endIndex = 0;

            Pattern pattern = Pattern.compile("\\$[a-z,A-Z,0-9]+");
            Matcher m = pattern.matcher(sb.toString());
                while (m.find()){
                    endIndex = m.end();
                }
            for (String key : booleans.keySet()){
                if (sb.subSequence(startIndex+1, endIndex).equals(key)){
                    V = booleans.get(key).toString();
                }
            }
            for (Clock key : maxBounds.keySet()) {
                if (sb.subSequence(startIndex+1, endIndex).equals(key.getOriginalName())) {
                    V = String.valueOf(minClockValue(cdd,key));
                }
            }

            sb.replace(startIndex, endIndex, V);
            return replaceVar(sb, booleans, cdd);
        }
        return sb;
    }

    public StringBuilder replaceClockVarUpdate (StringBuilder sb, Clock x, Integer val) {
        String V = "";

        if (sb.indexOf("$") != -1) {
            int startIndex = sb.indexOf("$");
            int endIndex = 0;

            Pattern pattern = Pattern.compile("\\$[a-z,A-Z,0-9]+");
            Matcher m = pattern.matcher(sb.toString());
            while (m.find()){
                endIndex = m.end();
            }

            if (sb.subSequence(startIndex+1, endIndex).equals(x.getOriginalName())) {
                V = String.valueOf(val);
            }

            sb.replace(startIndex, endIndex, V);
            return replaceClockVarUpdate(sb, x, val);
        }
        return sb;
    }
}
