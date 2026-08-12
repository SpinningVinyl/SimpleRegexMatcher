package net.prsv.rengine;

import java.util.*;

public class NFABuilder {

    private static final class StateAllocator {
        private int counter;

        String next() {
            return "q" + counter++;
        }
    }

    static class Fragment {
        private final HashSet<String> states;
        private final String acceptState;
        private final String startState;
        private final HashMap<Pair, HashSet<String>> transitions;
        private final HashMap<String, HashSet<String>> nullTransitions;

        private final HashMap<String, String> anyCharTransitions;

        public Fragment(HashSet<String> states,
                        String startState, String acceptState,
                        HashMap<Pair, HashSet<String>> transitions,
                        HashMap<String, HashSet<String>> nullTransitions,
                        HashMap<String, String> anyCharTransitions) {
            this.states = states;
            this.acceptState = acceptState;
            this.transitions = transitions;
            this.nullTransitions = nullTransitions;
            this.anyCharTransitions = anyCharTransitions;
            this.startState = startState;
        }

        public HashSet<String> getStates() {
            return states;
        }

        public String getAcceptState() {
            return acceptState;
        }

        public String getStartState() {
            return startState;
        }

        public HashMap<Pair, HashSet<String>> getTransitions() {
            return transitions;
        }

        public HashMap<String, HashSet<String>> getNullTransitions() {
            return nullTransitions;
        }

        public HashMap<String, String> getAnyCharTransitions() {
            return anyCharTransitions;
        }
    }

    private static void addNullTransition(HashMap<String, HashSet<String>> nullTransitions,
                                          String fromState,
                                          String toState) {
        nullTransitions.computeIfAbsent(fromState, ignored -> new HashSet<>()).add(toState);
    }

    private static HashMap<Pair, HashSet<String>> copyTransitions(
            HashMap<Pair, HashSet<String>> source) {
        HashMap<Pair, HashSet<String>> copy = new HashMap<>();
        for (Map.Entry<Pair, HashSet<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    private static HashMap<String, HashSet<String>> copyNullTransitions(
            HashMap<String, HashSet<String>> source) {
        HashMap<String, HashSet<String>> copy = new HashMap<>();
        for (Map.Entry<String, HashSet<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    private static void mergeTransitions(HashMap<Pair, HashSet<String>> destination,
                                         HashMap<Pair, HashSet<String>> source) {
        for (Map.Entry<Pair, HashSet<String>> entry : source.entrySet()) {
            destination.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>())
                    .addAll(entry.getValue());
        }
    }

    private static void mergeNullTransitions(HashMap<String, HashSet<String>> destination,
                                             HashMap<String, HashSet<String>> source) {
        for (Map.Entry<String, HashSet<String>> entry : source.entrySet()) {
            destination.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>())
                    .addAll(entry.getValue());
        }
    }

    private static Fragment concatenate(Fragment left, Fragment right) {
        HashSet<String> states = new HashSet<>(left.getStates());
        states.addAll(right.getStates());

        HashMap<Pair, HashSet<String>> transitions = copyTransitions(left.getTransitions());
        mergeTransitions(transitions, right.getTransitions());

        HashMap<String, HashSet<String>> nullTransitions =
                copyNullTransitions(left.getNullTransitions());
        mergeNullTransitions(nullTransitions, right.getNullTransitions());
        addNullTransition(nullTransitions, left.getAcceptState(), right.getStartState());

        HashMap<String, String> anyCharTransitions =
                new HashMap<>(left.getAnyCharTransitions());
        anyCharTransitions.putAll(right.getAnyCharTransitions());

        return new Fragment(states,
                left.getStartState(),
                right.getAcceptState(),
                transitions,
                nullTransitions,
                anyCharTransitions);
    }

    private static Fragment concatenateAll(List<Fragment> fragments) {
        if (fragments.isEmpty()) {
            throw new IllegalArgumentException("Cannot concatenate an empty fragment list");
        }

        HashSet<String> states = new HashSet<>();
        HashMap<Pair, HashSet<String>> transitions = new HashMap<>();
        HashMap<String, HashSet<String>> nullTransitions = new HashMap<>();
        HashMap<String, String> anyCharTransitions = new HashMap<>();

        Fragment previous = null;
        for (Fragment fragment : fragments) {
            states.addAll(fragment.getStates());
            mergeTransitions(transitions, fragment.getTransitions());
            mergeNullTransitions(nullTransitions, fragment.getNullTransitions());
            anyCharTransitions.putAll(fragment.getAnyCharTransitions());
            if (previous != null) {
                addNullTransition(nullTransitions,
                        previous.getAcceptState(),
                        fragment.getStartState());
            }
            previous = fragment;
        }

        return new Fragment(states,
                fragments.get(0).getStartState(),
                fragments.get(fragments.size() - 1).getAcceptState(),
                transitions,
                nullTransitions,
                anyCharTransitions);
    }

    private static Fragment optional(Fragment operand, StateAllocator allocator) {
        String startState = allocator.next();
        String acceptState = allocator.next();
        HashSet<String> states = new HashSet<>(operand.getStates());
        states.add(startState);
        states.add(acceptState);

        HashMap<String, HashSet<String>> nullTransitions =
                copyNullTransitions(operand.getNullTransitions());
        addNullTransition(nullTransitions, startState, operand.getStartState());
        addNullTransition(nullTransitions, startState, acceptState);
        addNullTransition(nullTransitions, operand.getAcceptState(), acceptState);

        return new Fragment(states,
                startState,
                acceptState,
                copyTransitions(operand.getTransitions()),
                nullTransitions,
                new HashMap<>(operand.getAnyCharTransitions()));
    }

    private static Fragment star(Fragment operand, StateAllocator allocator) {
        String startState = allocator.next();
        String acceptState = allocator.next();
        HashSet<String> states = new HashSet<>(operand.getStates());
        states.add(startState);
        states.add(acceptState);

        HashMap<String, HashSet<String>> nullTransitions =
                copyNullTransitions(operand.getNullTransitions());
        addNullTransition(nullTransitions, startState, operand.getStartState());
        addNullTransition(nullTransitions, startState, acceptState);
        addNullTransition(nullTransitions, operand.getAcceptState(), startState);

        return new Fragment(states,
                startState,
                acceptState,
                copyTransitions(operand.getTransitions()),
                nullTransitions,
                new HashMap<>(operand.getAnyCharTransitions()));
    }

    private static Fragment oneOrMore(Fragment operand, StateAllocator allocator) {
        String startState = allocator.next();
        String acceptState = allocator.next();
        HashSet<String> states = new HashSet<>(operand.getStates());
        states.add(startState);
        states.add(acceptState);

        HashMap<String, HashSet<String>> nullTransitions =
                copyNullTransitions(operand.getNullTransitions());
        addNullTransition(nullTransitions, startState, operand.getStartState());
        addNullTransition(nullTransitions, operand.getAcceptState(), acceptState);
        addNullTransition(nullTransitions, acceptState, startState);

        return new Fragment(states,
                startState,
                acceptState,
                copyTransitions(operand.getTransitions()),
                nullTransitions,
                new HashMap<>(operand.getAnyCharTransitions()));
    }

    private static Fragment epsilon(StateAllocator allocator) {
        String state = allocator.next();
        return new Fragment(new HashSet<>(Collections.singleton(state)),
                state,
                state,
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>());
    }

    private static Fragment copyOf(Fragment source, StateAllocator allocator) {
        HashMap<String, String> stateMapping = new HashMap<>();
        for (String state : source.getStates()) {
            stateMapping.put(state, allocator.next());
        }

        HashMap<Pair, HashSet<String>> transitions = new HashMap<>();
        for (Map.Entry<Pair, HashSet<String>> entry : source.getTransitions().entrySet()) {
            Pair oldTransition = entry.getKey();
            Pair newTransition = new Pair(stateMapping.get(oldTransition.getState()),
                    oldTransition.getSymbol());
            HashSet<String> destinations = new HashSet<>();
            for (String destination : entry.getValue()) {
                destinations.add(stateMapping.get(destination));
            }
            transitions.put(newTransition, destinations);
        }

        HashMap<String, HashSet<String>> nullTransitions = new HashMap<>();
        for (Map.Entry<String, HashSet<String>> entry : source.getNullTransitions().entrySet()) {
            HashSet<String> destinations = new HashSet<>();
            for (String destination : entry.getValue()) {
                destinations.add(stateMapping.get(destination));
            }
            nullTransitions.put(stateMapping.get(entry.getKey()), destinations);
        }

        HashMap<String, String> anyCharTransitions = new HashMap<>();
        for (Map.Entry<String, String> entry : source.getAnyCharTransitions().entrySet()) {
            anyCharTransitions.put(stateMapping.get(entry.getKey()),
                    stateMapping.get(entry.getValue()));
        }

        return new Fragment(new HashSet<>(stateMapping.values()),
                stateMapping.get(source.getStartState()),
                stateMapping.get(source.getAcceptState()),
                transitions,
                nullTransitions,
                anyCharTransitions);
    }

    public static StateMachine build(List<RToken> tokenStream) {
        Deque<Fragment> stack = new ArrayDeque<>();
        StateAllocator allocator = new StateAllocator();
        HashSet<String> states;
        String startState;
        String acceptState;
        HashMap<Pair, HashSet<String>> transitions;
        HashMap<String, HashSet<String>> nullTransitions;
        HashMap<String, String> anyCharTransitions;
        Fragment fragment;
        Fragment e1, e2, e;
        for (RToken t : tokenStream) {
            states = new HashSet<>();
            transitions = new HashMap<>();
            nullTransitions = new HashMap<>();
            anyCharTransitions = new HashMap<>();
            switch (t.type) {
                case ANY_CHAR:
                case LITERAL:
                    // create a new fragment which has two states and one transition
                    startState = allocator.next();
                    acceptState = allocator.next();
                    states.add(startState);
                    states.add(acceptState);
                    if (t.type == RToken.RTokenType.LITERAL) {
                        transitions.put(new Pair(startState, t.literal), new HashSet<>(Collections.singleton(acceptState)));
                    } else {
                        anyCharTransitions.put(startState, acceptState);
                    }
                    fragment = new Fragment(states, startState, acceptState, transitions, nullTransitions, anyCharTransitions);
                    // push the fragment onto the stack
                    stack.push(fragment);
                    break;
                case CONCAT:
                    // pop two fragments from the stack
                    if (stack.size() < 2) {
                        throw new IllegalArgumentException("Compilation error: CONCAT requires two operands");
                    }
                    e2 = stack.pop();
                    e1 = stack.pop();
                    stack.push(concatenate(e1, e2));
                    break;
                case UNION:
                    // pop two fragments from the stack
                    if (stack.size() < 2) {
                        throw new IllegalArgumentException("Compilation error: UNION requires two operands");
                    }
                    e2 = stack.pop();
                    e1 = stack.pop();
                    // create a new start state and a new accept state
                    startState = allocator.next();
                    acceptState = allocator.next();
                    states.add(startState);
                    states.add(acceptState);
                    states.addAll(e1.getStates());
                    states.addAll(e2.getStates());
                    transitions = e1.getTransitions();
                    transitions.putAll(e2.getTransitions());
                    anyCharTransitions = e1.getAnyCharTransitions();
                    anyCharTransitions.putAll(e2.getAnyCharTransitions());
                    nullTransitions = e1.getNullTransitions();
                    nullTransitions.putAll(e2.getNullTransitions());
                    // create new null transitions from the new start state to start states of e1 and e2
                    nullTransitions.put(startState, new HashSet<>(List.of(e1.getStartState(), e2.getStartState())));

                    // create a new null transition from the accept state of e1 to the new accept state
                    if (!nullTransitions.containsKey(e1.getAcceptState())) {
                        nullTransitions.put(e1.getAcceptState(), new HashSet<>(Collections.singleton(acceptState)));
                    } else {
                        nullTransitions.get(e1.getAcceptState()).add(acceptState);
                    }
                    // create a new null transition from the accept state of e2 to the new accept state
                    if (!nullTransitions.containsKey(e2.getAcceptState())) {
                        nullTransitions.put(e2.getAcceptState(), new HashSet<>(Collections.singleton(acceptState)));
                    } else {
                        nullTransitions.get(e2.getAcceptState()).add(acceptState);
                    }
                    fragment = new Fragment(states, startState, acceptState, transitions, nullTransitions, anyCharTransitions);

                    // push the resulting fragment onto the stack
                    stack.push(fragment);
                    break;
                case QMARK:
                case STAR:
                    // pop a fragment from the stack
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Compilation error: " +
                                ((t.type == RToken.RTokenType.QMARK) ? "QMARK" : "STAR") +
                                " requires an operand");
                    }
                    e = stack.pop();
                    stack.push(t.type == RToken.RTokenType.QMARK
                            ? optional(e, allocator)
                            : star(e, allocator));
                    break;
                case PLUS:
                    // pop a fragment from the stack
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Compilation error: PLUS requires an operand");
                    }
                    stack.push(oneOrMore(stack.pop(), allocator));
                    break;
                case QUANTIFIER:
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException(
                                "Compilation error: QUANTIFIER requires an operand");
                    }
                    Fragment template = stack.pop();
                    List<Fragment> repetitions = new ArrayList<>();
                    boolean templateUsed = false;

                    for (int i = 0; i < t.min; i++) {
                        Fragment copy = templateUsed ? copyOf(template, allocator) : template;
                        templateUsed = true;
                        repetitions.add(copy);
                    }

                    if (t.isUnbounded()) {
                        Fragment copy = templateUsed ? copyOf(template, allocator) : template;
                        repetitions.add(star(copy, allocator));
                    } else {
                        for (int i = t.min; i < t.max; i++) {
                            Fragment copy = templateUsed ? copyOf(template, allocator) : template;
                            templateUsed = true;
                            repetitions.add(optional(copy, allocator));
                        }
                    }

                    stack.push(repetitions.isEmpty()
                            ? epsilon(allocator)
                            : concatenateAll(repetitions));
                    break;
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Compilation error: malformed regex pattern");
        }
        Fragment finalFragment = stack.pop();
        // create a new state machine using info from the final fragment
        return new StateMachine(finalFragment.getStates(),
                new HashSet<>(Collections.singleton(finalFragment.getStartState())),
                new HashSet<>(Collections.singleton(finalFragment.getAcceptState())),
                finalFragment.getTransitions(),
                finalFragment.getNullTransitions(),
                finalFragment.getAnyCharTransitions());
    }

}
