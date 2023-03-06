package logic;

import models.*;
import parser.GuardParser;
import parser.XMLFileWriter;

import java.io.FileWriter;
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
    private HashMap<Clock,Integer> maxBounds;



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
        res.put(getClocks().get(getClocks().size()-1), Integer.MAX_VALUE);
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
    public CDD helperConjoin(String guardString, CDD orgCDD) {
        Guard g = GuardParser.parse(guardString, getClocks(), getBVs());
        CDD cdd = orgCDD.conjunction(new CDD(g));

        return cdd;
    }
    public int minClockValue(CDD orgCDD, Clock clock){

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

    public List<Transition> shortestTraceHelper(String destination) {
        Set<Channel> actions = getActions();
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        HashMap<String, List<Transition>> transitions = new HashMap<>();
        List<Transition> shortestTrace = new ArrayList<>();
        boolean check = true;

        waiting.add(getInitialState());

        while (!waiting.isEmpty() && check) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> newTransitions = getNextTransitions(currState, action).stream().
                        filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget())).collect(Collectors.toList());

                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<State> toAdd = newTransitions.stream().map(Transition::getTarget).collect(Collectors.toList());

                for (Transition t : newTransitions) {

                    if (t.getTarget().getLocation().getName().equals(destination)) {
                        check = false;
                    }

                    if (!transitions.containsKey(t.getTarget().getLocation().getName())) {
                        transitions.put(t.getTarget().getLocation().getName(), new ArrayList<>());
                    }

                    transitions.get(t.getTarget().getLocation().getName()).add(t);

                }
                waiting.addAll(toAdd);
            }
        }

        Transition shortestTrans = transitions.get(destination).get(0);
        shortestTrace.add(shortestTrans);

        while (true) {
            if (transitions.containsKey(shortestTrans.getSource().getLocation().getName()) && !shortestTrans.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
                for (Transition t : transitions.get(shortestTrans.getSource().getLocation().getName())) {
                    if (t.getTarget().toString().equals(shortestTrans.getSource().toString())) {
                        shortestTrans = t;
                        shortestTrace.add(shortestTrans);
                    }
                }
            }
            else {
                break;
            }
        }
        Collections.reverse(shortestTrace);
        return shortestTrace;
    }
    public List<Transition> fastestTraceHelper(String destination) throws IOException {
        Set<Channel> actions = getActions();
        HashMap<String, List<Transition>> transitionHashMap = new HashMap<>();
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        boolean destinationFound = false;
        int minClockValue = Integer.MAX_VALUE;
        List<Transition> fastestTrace = new ArrayList<>();

        waiting.add(getInitialState());

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> newTransitions;
                if (destinationFound) {
                    int finalMinClockValue = minClockValue;
                    newTransitions = getNextTransitions(currState, action).stream().
                            filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget())
                                    && minClockValue(s.getTarget().getInvariant(),getClocks().get(getClocks().size()-1)) < finalMinClockValue).collect(Collectors.toList());
                }
                else {
                    newTransitions = getNextTransitions(currState, action).stream().
                            filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget())).collect(Collectors.toList());
                }
                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<State> toAdd = newTransitions.stream().map(Transition::getTarget).collect(Collectors.toList());

                for (Transition t : newTransitions) {
                    if (t.getTarget().getLocation().getName().equals(destination)) {
                        destinationFound = true;
                        minClockValue = minClockValue(t.getTarget().getInvariant(),getClocks().get(getClocks().size()-1));

                    }

                    if (!transitionHashMap.containsKey(t.getTarget().getLocation().getName())) {
                        transitionHashMap.put(t.getTarget().getLocation().getName(), new ArrayList<>());
                    }
                    transitionHashMap.get(t.getTarget().getLocation().getName()).add(t);
                }

                waiting.addAll(toAdd);
            }
        }

        //find the destination state with the smallest global clock value
        if (transitionHashMap.containsKey(destination)) {
            int min = minClockValue(transitionHashMap.get(destination).get(0).getTarget().getInvariant(), getClocks().get(getClocks().size() - 1));
            Transition fastestTrans = transitionHashMap.get(destination).get(0);
            for (Transition t : transitionHashMap.get(destination)) {
                int temp = minClockValue(t.getTarget().getInvariant(), getClocks().get(getClocks().size() - 1));
                if (temp < min) {
                    min = temp;
                    fastestTrans = t;
                }
            }
            fastestTrace.add(fastestTrans);

            //follow trace to initial state
            while (true) {
                if (transitionHashMap.containsKey(fastestTrans.getSource().getLocation().getName()) && !fastestTrans.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
                    for (Transition t : transitionHashMap.get(fastestTrans.getSource().getLocation().getName())) {
                        if (t.getTarget().toString().equals(fastestTrans.getSource().toString())) {
                            fastestTrans = t;
                            fastestTrace.add(fastestTrans);
                        }
                    }
                }
                else {
                    break;
                }
            }
        }
        else {
            return null;
        }

        Collections.reverse(fastestTrace);

        //fastestTraceReal(fastestTrace, destination);
        generateTestCode(fastestTrace);

        return fastestTrace;
    }

    //Testcode generation stuff
    public void generateTestCode(List<Transition> trace) {
        // Start the test case by declaring and initialising all clocks
        StringBuilder sb = new StringBuilder("class Clock {\n" +
                " double now;\n " +
                "Clock(double now) {" +
                "\n this.now = now;\n}" +
                "\n" +
                " double value(double now) {\n" +
                "    return this.now - now;\n" +
                " }\n}\n");
        sb.append(testCodeInitClocks());
        HashMap<String, Boolean> booleans = new HashMap<>();

        //initialise hashmap of boolean variables
        for (BoolVar bv : CDD.BVs) {
            booleans.put(bv.getOriginalName(), bv.getInitialValue());
        }

        //create test code for initial location
        sb.append(testCodeAssertClocks(trace.get(0).getSource()));
        sb.append(parseTestCode(new StringBuilder(trace.get(0).getSource().getLocation().getEnterTestCode()), booleans, trace.get(0).getSource().getInvariant()));

        for (Transition tran : trace) {
            sb.append(parseTestCode(new StringBuilder(tran.getSource().getLocation().getExitTestCode()), booleans, tran.getSource().getInvariant()));
            sb.append(testCodeAssertClocks(tran.getSource()));
            sb.append(parseTestCode(new StringBuilder(tran.getEdges().get(0).getTestCode()), booleans, tran.getEdges().get(0).getGuardCDD()));

            //check the transition for updates
            // Clock updates are being considered by adding a "clock reset" to the test code
            // What do we do with the test code in the edge? pre-update values or post?
            if (tran.getUpdates().size() > 0 ){
                for (Update update : tran.getUpdates()){
                    if (update instanceof BoolUpdate) {
                        booleans.put(((BoolUpdate)update).getBV().getUniqueName(), ((BoolUpdate)update).getValue());
                    }
                    else if (update instanceof ClockUpdate) {
                        //how to approach resets that are not 0?
                        sb.append(testCodeUpdateClock(((ClockUpdate) update).getClock(), ((ClockUpdate) update).getValue()));
                        //sb.append(new StringBuilder("clock " + ((ClockUpdate) update).getClock().toString() + "is set to " + ((ClockUpdate) update).getValue()) + "\n");
                    }
                }
            }
            sb.append(testCodeAssertClocks(tran.getTarget()));
            sb.append(parseTestCode(new StringBuilder(tran.getTarget().getLocation().getEnterTestCode()), booleans, tran.getTarget().getInvariant()));

        }
        sb.append("\nend of test code\n");
        try {
            //PrintWriter printerWriter = new PrintWriter("testcode.txt");
            //printerWriter.println("");
            //printerWriter.close();
            FileWriter writer = new FileWriter("testcode.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String testCodeEditClocks(State state) {


        return "";
    }
    private String testCodeAssertClocks(State state) {
        //String s = "double currentTime = time.now();\n";

        //s += "assert("+ state.getInvariant().toString() + ");\n";
        String s = state.getInvariant().toString();


        for (Clock c : getClocks()) {
            s = s.replaceAll("("+c.getOriginalName()+")", "" + c.getOriginalName() + ".value(temp) ");
        }

        String temp = "double temp = time.now();\n" + "assert("+ s +");\n";

        return temp;
    }

    private String testCodeInitClocks() {
        String s = "double timeStamp = time.now();\n";
        for (Clock c : getClocks()) {
            s += "Clock " + c.getOriginalName() + " = new Clock(timeStamp);\n";
        }

        return s;
    }

    private String testCodeUpdateClock(Clock clock, Integer value) {
        String s = "";
        if (value == 0) {
            s += clock.getOriginalName() + ".time = time.now();\n";
        }
        else {
            s += clock.getOriginalName() + ".time = time.now() -" + value + ";\n";
        }

        return s;
    }

    private StringBuilder parseTestCode(StringBuilder sb, HashMap<String, Boolean> booleans, CDD cdd) {
        String testCode = "";

        //If checks if there is a variable to be "replaced", denoted by $
        if (sb.indexOf("$") != -1) {
            int startIndex = sb.indexOf("$");
            int endIndex = 0;

            Pattern pattern = Pattern.compile("\\$[a-z,A-Z,0-9]+");
            Matcher m = pattern.matcher(sb.toString());
                while (m.find()){
                    endIndex = m.end();
                }
                String key = sb.subSequence(startIndex+1, endIndex).toString();

                if (booleans.containsKey(key)) {
                    testCode = booleans.get(key).toString();
                }

                for (Clock c : maxBounds.keySet()) {
                    if (sb.subSequence(startIndex+1, endIndex).equals(c.getOriginalName())) {
                        testCode = filterCDD(cdd.toString(), sb.subSequence(startIndex+1, endIndex).toString()).toString();
                    }
                }

            sb.replace(startIndex, endIndex, testCode);
            return parseTestCode(sb, booleans, cdd);
        }
        return sb;
    }

    private StringBuilder filterCDD (String s, String name) {
        StringBuilder sb = new StringBuilder();
        s  = s.replace("(", "").replace(")", "");
        String[] parts = s.split("&&");
        List filteredParts = Arrays.stream(parts).filter(x -> containsClock(x, name) || containsClock(x, "z")).collect(Collectors.toList());

        for (int i = 0; i < filteredParts.size(); i++) {
            if (i == filteredParts.size()-1) {
                sb.append(filteredParts.get(i).toString());
            }
            else {
                sb.append(filteredParts.get(i)).append("&&");
            }
        }

        return sb;
    }

    private boolean containsClock(String s, String name) {
        Pattern pattern = Pattern.compile("([^A-Za-z0-9_]*" + name + "[<>-]+)");
        Matcher m = pattern.matcher(s);

        while (m.find()){

            return true;
        }
        return false;
    }

    public HashMap<String, List<Transition>> exploreHelper() throws IOException {
        Set<Channel> actions = getActions();
        HashMap<String, List<Transition>> transitionHashMap = new HashMap<>();
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();

        waiting.add(getInitialState());

        HashMap<String, Boolean> visitedEdges = new HashMap<>();

        for (Edge e : automaton.getEdges()){
            visitedEdges.put(e.toString(), false);
        }

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),getClocks());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> newTransitions;

                newTransitions = getNextTransitions(currState, action).stream().
                        filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget()) || !visitedEdges.get(s.getEdges().get(0).toString())).collect(Collectors.toList());

                newTransitions.forEach(e->visitedEdges.put(e.getEdges().get(0).toString(), true));

                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<State> toAdd = newTransitions.stream().map(Transition::getTarget).collect(Collectors.toList());

                for (Transition t : newTransitions) {

                    if (!transitionHashMap.containsKey(t.getTarget().getLocation().getName())) {
                        transitionHashMap.put(t.getTarget().getLocation().getName(), new ArrayList<>());
                    }
                    transitionHashMap.get(t.getTarget().getLocation().getName()).add(t);
                }

                waiting.addAll(toAdd);
            }
        }
        return transitionHashMap;

    }

    public Transition fastestTransition(List<Transition> transitions) {
        Transition fastestTrans = transitions.get(0);
        int min = minClockValue(fastestTrans.getGuardCDD(), getClocks().get(getClocks().size()-1));
        for (Transition t : transitions){
            int temp = minClockValue(t.getTarget().getInvariant(), getClocks().get(getClocks().size()-1));
            //CDD cdd = helperConjoin(state, t.getTarget().getInvariant());

            if (temp <= min) {
                min = temp;
                fastestTrans = t;
            }
            else if (temp == min) {
                fastestTrans = t;
            }
        }
        return fastestTrans;
    }

    public List<Transition> createTrace(Transition trans, HashMap<String, List<Transition>> transitionHashMap) {
        List<Transition> trace = new ArrayList<>();
        trace.add(trans);

        while (!trans.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
            if (transitionHashMap.containsKey(trans.getSource().getLocation().getName())) {
                for (Transition t : transitionHashMap.get(trans.getSource().getLocation().getName())) {
                    if (t.getTarget().getInvariant().equals(trans.getSource().getInvariant())) {
                        trans = t;
                        trace.add(trans);
                    }
                }
            }
        }

        Collections.reverse(trace);
        return trace;
    }

    public List<Transition> expandTrace(List<Transition> trace) {
        Set<Channel> actions = getActions();
        List<Transition> transitions = new ArrayList<>();
        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        Transition startTrans = trace.get(trace.size()-1);
        State state = new State(trace.get(trace.size()-1).getTarget());

        waiting.add(state);
        boolean foundOutput = false;


        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(),getClocks());
            passed.add(toStore);

            for (Channel action : actions) {
                List<Transition> newTransitions;

                boolean finalFoundOutput = foundOutput;
                newTransitions = getNextTransitions(currState, action).stream().
                        filter(s -> !passedContainsState(s.getTarget()) && !waitingContainsState(s.getTarget()) && !finalFoundOutput).collect(Collectors.toList());

                newTransitions.forEach(e->e.getTarget().extrapolateMaxBounds(getMaxBounds(),clocks.getItems()));

                List<State> toAdd = newTransitions.stream().map(Transition::getTarget).collect(Collectors.toList());

                for (Transition t : newTransitions) {

                    if (t.getEdges().get(0).getStatus().equals("OUTPUT")) {
                        foundOutput = true;
                        transitions.add(t);
                    }

                    if (!foundOutput) {
                        transitions.add(t);
                    }

                }
                if (foundOutput) {
                    break;
                }
                waiting.addAll(toAdd);
            }
        }

        Transition temp = transitions.get(transitions.size()-1);
        List<Transition> tempList = new ArrayList<>();
        tempList.add(temp);

        for (int i = 0; i < transitions.size(); ++i){
            if (i == transitions.size()-1 && startTrans.getGuardCDD().equals(temp.getGuardCDD())) {
                i = 0;
                break;
            }
            if (temp.getSource().getInvariant().equals(transitions.get(i).getTarget().getInvariant())) {
                temp = transitions.get(i);
                tempList.add(temp);
            }
        }



        Collections.reverse(tempList);
        trace.addAll(tempList);
        return trace;
    }
}
