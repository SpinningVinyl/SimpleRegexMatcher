package net.prsv.rengine;

import java.util.HashMap;
import java.util.HashSet;

public class StateMachine {
    private final HashSet<String> states;
    private final HashSet<String> acceptStates;
    private final HashMap<Pair, HashSet<String>> transitions;
    private final HashMap<String, HashSet<String>> nullTransitions;

    private final HashMap<String, String> anyCharTransitions;

    private final HashSet<String> startStates;

    public StateMachine(HashSet<String> states,
                        HashSet<String> startStates, HashSet<String> acceptStates,
                        HashMap<Pair, HashSet<String>> transitions,
                        HashMap<String, HashSet<String>> nullTransitions,
                        HashMap<String, String> anyCharTransitions) {
        this.states = states;
        this.acceptStates = acceptStates;
        this.transitions = transitions;
        this.nullTransitions = nullTransitions;
        this.startStates = startStates;
        this.anyCharTransitions = anyCharTransitions;
    }

    public String config() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== NFA configuration =====\n");
        sb.append("States: ");
        states.forEach(state -> sb.append(state).append(" "));
        sb.append("\nStart states: ");
        startStates.forEach(state -> sb.append(state).append(" "));
        sb.append("\nAccept states: ");
        acceptStates.forEach(state -> sb.append(state).append(" "));
        sb.append("\nTransitions:\n");
        transitions.keySet().forEach(key -> {
            sb.append("(").append(key).append(") -> ");
            transitions.get(key).forEach(state -> sb.append(state).append(" "));
            sb.append("\n");
        });
        if (!nullTransitions.isEmpty()) {
            sb.append("\nNull transitions:\n");
            nullTransitions.keySet().forEach(key -> {
                sb.append(key).append(" -> ");
                nullTransitions.get(key).forEach(state -> sb.append(state).append(" "));
                sb.append("\n");
            });
        }
        return sb.toString();
    }

    public String summary() {
        return "===== NFA summary =====\n" +
                "States: " +
                states.size() +
                ", accept states: " +
                acceptStates.size() +
                ", transitions: " +
                transitions.size() +
                ", null transitions: " +
                nullTransitions.size() +
                "\n";
    }

    private HashSet<String> followNullTransitions(String state) {
        HashSet<String> result = new HashSet<>();
        if (nullTransitions.containsKey(state)) {
            result.addAll(nullTransitions.get(state));
            for (String s : nullTransitions.get(state)) {
                result.addAll(followNullTransitions(s));
            }
        }
        return result;
    }

    public boolean run(String input) {

        // if any of the start states have null transitions defined,
        // add them to the start states recursively
        for (String startState: startStates) {
            if (nullTransitions.containsKey(startState)) {
                startStates.addAll(followNullTransitions(startState));
            }
        }

//        System.out.print("Start states: ");
//        startStates.forEach(state -> System.out.print(state + " "));
//        System.out.println();
        HashSet<String> currentStates = new HashSet<>(startStates);
        if(!input.equals("")) {
            for (int i = 0; i < input.length(); i++) {

//                System.out.print("Current states: ");
//                for (String state : currentStates) {
//                    System.out.print(state + " ");
//                }
//                System.out.println();
                HashSet<String> newStates = new HashSet<>();
                char symbol = input.charAt(i);
//                System.out.print("Current symbol: '" + symbol + "'. ");
                for (String state : currentStates) {
                    HashSet<String> states = transitions.get(new Pair(state, symbol));
                    if (states != null) {
                        newStates.addAll(states);
                    }
                    if (anyCharTransitions.containsKey(state)) {
                        newStates.add(anyCharTransitions.get(state));
                    }
                }
                // if any of the current states have null transitions defined,
                // add them to the current states recursively
                HashSet<String> tmp = new HashSet<>();
                for (String state : newStates) {
                    if (nullTransitions.containsKey(state)) {
                        tmp.addAll(followNullTransitions(state));
                    }
                }
                newStates.addAll(tmp);
                currentStates = newStates;
//                System.out.print("Moving to states: ");
//                newStates.forEach(state -> System.out.print(state + " "));
//                System.out.println();
            }
        }
        currentStates.retainAll(acceptStates);
        boolean result = !currentStates.isEmpty();
        currentStates.clear();
        return result;
    }

}