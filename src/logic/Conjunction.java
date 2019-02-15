package logic;

import models.Channel;
import java.util.*;

public class Conjunction extends TransitionSystem {
    List<TransitionSystem> systems;

    public Conjunction(List<TransitionSystem> systems) {
        super();

        this.systems = systems;

        for (TransitionSystem ts : systems) {
            clocks.addAll(ts.getClocks());
        }
        dbmSize = clocks.size() + 1;
    }

    public Set<Channel> getInputs() {
        Set<Channel> inputs = new HashSet<>();
        inputs.addAll(systems.get(0).getInputs());

        for (int i = 1; i < systems.size(); i++) {
            inputs.retainAll(systems.get(i).getInputs());
        }

        return inputs;
    }

    public Set<Channel> getOutputs() {
        Set<Channel> outputs = new HashSet<>();
        outputs.addAll(systems.get(0).getOutputs());

        for (int i = 1; i < systems.size(); i++) {
            outputs.retainAll(systems.get(i).getOutputs());
        }

        return outputs;
    }

    public SymbolicLocation getInitialLocation() {
        return getInitialLocation(systems);
    }

    public List<Transition> getNextTransitions(State currentState, Channel channel) {
        List<SymbolicLocation> locations = ((ComplexLocation) currentState.getLocation()).getLocations();

        // these will store the locations of the target states and the corresponding transitions
        List<Move> resultMoves = systems.get(0).getNextMoves(locations.get(0), channel);

        if (resultMoves.isEmpty())
            return new ArrayList<>();

        for (int i = 1; i < systems.size(); i++) {
            List<Move> moves = systems.get(i).getNextMoves(locations.get(i), channel);

            if (moves.isEmpty())
                // no transitions are possible from this state
                return new ArrayList<>();

            resultMoves = moveProduct(resultMoves, moves, i == 1);
        }

        return createNewTransitions(currentState, resultMoves);
    }

    public List<Move> getNextMoves(SymbolicLocation symLocation, Channel channel) {
        return getNextMoves(symLocation, channel, systems);
    }
}
