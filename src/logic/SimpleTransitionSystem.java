package logic;

import models.*;
import parser.XMLFileWriter;

import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

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

    public boolean isReachableHelper(Location location) throws IOException {
        Set<Channel> actions = getActions();
        HashMap<String, ArrayList<Transition>> passedTransitions = new HashMap<>();

        waiting = new ArrayDeque<>();
        passed = new ArrayList<>();
        waiting.add(getInitialState());

        System.out.println("------------");
        System.out.println("Is " + location.getName() + " reachable?");

        if (location.getName().equals(getInitialLocation().getName())){
            return true;
        }

        while (!waiting.isEmpty()) {
            State currState = new State(waiting.pop());
            State toStore = new State(currState);

            toStore.extrapolateMaxBounds(this.getMaxBounds(), clocks.getItems());
            passed.add(toStore);

            for (Channel action : actions){

                List<Transition> tempTrans = getNextTransitions(currState, action);

                for (Transition trans : tempTrans) {

                    if (!passedTransitions.containsKey(trans.getTarget().getLocation().getName())) {
                        passedTransitions.put(trans.getTarget().getLocation().getName(), new ArrayList<>());
                        passedTransitions.get(trans.getTarget().getLocation().getName()).add(trans);
                    }
                    else {
                        passedTransitions.get(trans.getSource().getLocation().getName()).add(trans);
                    }

                    if (trans.getTarget().getLocation().getName().equals(location.getName())){
                        findTrace(passedTransitions, location);
                        return true;
                    }

                }

                List<State> toAdd = tempTrans.stream().map(Transition::getTarget).
                        filter(s -> !passedContainsState(s) && !waitingContainsState(s)).collect(Collectors.toList());

                waiting.addAll(toAdd);

            }

        }

        return false;
    }

    public void findTrace(HashMap<String, ArrayList<Transition>> passedTransitions, Location loc)  {
        Transition newLoc;
        ArrayList<Transition> trace = new ArrayList();
        ArrayList<Transition> temp;

        newLoc = passedTransitions.get(loc.getName()).get(0);
        trace.add(passedTransitions.get(loc.getName()).get(0));

        while (!newLoc.getSource().getLocation().getName().equals(getInitialLocation().getName())) {
            temp = passedTransitions.get(newLoc.getSource().getLocation().getName());

            for (Transition t : temp) {
                if (t.getTarget().getLocation().getName().equals(newLoc.getSource().getLocation().getName())) {
                    trace.add(t);
                    newLoc = t;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Transition tran : trace){
            sb.append(tran.getSource().getLocation().getExitTestCode());
            sb.append(tran.getEdges().get(0).getTestCode());
            sb.append(tran.getTarget().getLocation().getEnterTestCode());
        }

        try {
            FileWriter writer = new FileWriter("testcode.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        
    }
}
