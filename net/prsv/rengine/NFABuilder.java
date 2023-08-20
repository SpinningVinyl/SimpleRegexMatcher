package net.prsv.rengine;

import java.util.*;

public class NFABuilder {

    static class Fragment {
        private final HashSet<String> states;
        private final String acceptState;
        private final String startState;
        private final HashMap<Pair, HashSet<String>> transitions;
        private final HashMap<String, HashSet<String>> nullTransitions;

        public Fragment(HashSet<String> states,
                            String acceptState,
                            HashMap<Pair, HashSet<String>> transitions,
                            HashMap<String, HashSet<String>> nullTransitions,
                            String startState) {
            this.states = states;
            this.acceptState = acceptState;
            this.transitions = transitions;
            this.nullTransitions = nullTransitions;
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
    }

    public static StateMachine build(List<RToken> tokenStream) {
        Deque<Fragment> stack = new ArrayDeque<>();
        int stateCounter = 0;
        HashSet<String> states;
        String startState;
        String acceptState;
        HashMap<Pair, HashSet<String>> transitions;
        HashMap<String, HashSet<String>> nullTransitions;
        Fragment fragment;
        Fragment e1, e2, e;
        for (RToken t : tokenStream) {
            states = new HashSet<>();
            transitions = new HashMap<>();
            nullTransitions = new HashMap<>();
            switch (t.type) {
                case LITERAL:
                    // create a new fragment which has two states and one transition
                    startState = "q" + stateCounter++;
                    acceptState = "q" + stateCounter++;
                    states.add(startState);
                    states.add(acceptState);
                    transitions.put(new Pair(startState, t.literal), new HashSet<>(Collections.singleton(acceptState)));
                    fragment = new Fragment(states, acceptState, transitions, nullTransitions, startState);
                    // push the fragment onto the stack
                    stack.push(fragment);
                    break;
                case CONCAT:
                    // pop two fragments from the stack
                    e2 = stack.pop();
                    e1 = stack.pop();
                    // states = all states from e1 and e2
                    states = e1.getStates();
                    states.addAll(e2.getStates());
                    startState = e1.getStartState();
                    acceptState = e2.getAcceptState();
                    // transitions = all transitions from e1 and e2
                    transitions = e1.getTransitions();
                    transitions.putAll(e2.getTransitions());
                    nullTransitions = e1.getNullTransitions();
                    nullTransitions.putAll(e2.getNullTransitions());
                    // add a null transition between the accept state of e1 and the start state of e2
                    String e1AcceptState = e1.getAcceptState();
                    String e2StartState = e2.getStartState();
                    if (!nullTransitions.containsKey(e1AcceptState)) {
                        nullTransitions.put(e1AcceptState, new HashSet<>(Collections.singleton(e2StartState)));
                    } else {
                        nullTransitions.get(e1AcceptState).add(e2StartState);
                    }
                    fragment = new Fragment(states, acceptState, transitions, nullTransitions, startState);
                    // push the resulting fragment onto the stack
                    stack.push(fragment);
                    break;
                case UNION:
                    // pop two fragments from the stack
                    e2 = stack.pop();
                    e1 = stack.pop();
                    // create a new start state and a new accept state
                    startState = "q" + stateCounter++;
                    acceptState = "q" + stateCounter++;
                    states.add(startState);
                    states.add(acceptState);
                    states.addAll(e1.getStates());
                    states.addAll(e2.getStates());
                    transitions = e1.getTransitions();
                    transitions.putAll(e2.getTransitions());
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
                    fragment = new Fragment(states, acceptState, transitions, nullTransitions, startState);

                    // push the resulting fragment onto the stack
                    stack.push(fragment);
                    break;
                case QMARK:
                case STAR:
                    // pop a fragment from the stack
                    e = stack.pop();
                    // create a new start state and a new accept state
                    startState = "q" + stateCounter++;
                    acceptState = "q" + stateCounter++;
                    states = e.getStates();
                    states.add(startState);
                    states.add(acceptState);
                    transitions = e.getTransitions();
                    nullTransitions = e.getNullTransitions();
                    // add a null transition from the new start state to the start state of e and the new accept state
                    nullTransitions.put(startState, new HashSet<>(List.of(e.getStartState(), acceptState)));

                    // create a null transition from the accept state of e to the accept state or to the start state,
                    // depending on the token
                    String patchState = (t.type == RToken.RTokenType.QMARK) ? acceptState : startState ;
                    if (!nullTransitions.containsKey(e.getAcceptState())) {
                        nullTransitions.put(e.getAcceptState(), new HashSet<>(Collections.singleton(patchState)));
                    } else {
                        nullTransitions.get(e.getAcceptState()).add(patchState);
                    }
                    fragment = new Fragment(states, acceptState, transitions, nullTransitions, startState);

                    // push the resulting fragment onto the stack
                    stack.push(fragment);
                    break;
                case PLUS:
                    // pop a fragment from the stack
                    e = stack.pop();
                    // create a new start state and a new accept state
                    startState = "q" + stateCounter++;
                    acceptState = "q" + stateCounter++;
                    states = e.getStates();
                    states.add(startState);
                    states.add(acceptState);
                    transitions = e.getTransitions();
                    nullTransitions = e.getNullTransitions();
                    // add new null transitions from the start state to the start state of e
                    // and from the accept state of e to the new accept state
                    nullTransitions.put(startState, new HashSet<>(Collections.singleton(e.getStartState())));
                    nullTransitions.put(acceptState, new HashSet<>(Collections.singleton(startState)));

                    // add a null transition from the accept state of e to the new accept state
                    if (!nullTransitions.containsKey(e.getAcceptState())) {
                        nullTransitions.put(e.getAcceptState(), new HashSet<>(Collections.singleton(acceptState)));
                    } else {
                        nullTransitions.get(e.getAcceptState()).add(acceptState);
                    }
                    fragment = new Fragment(states, acceptState, transitions, nullTransitions, startState);

                    // push the resulting fragment onto the stack
                    stack.push(fragment);
                    break;
            }
        }
        assert(stack.size() == 1); // in the end there should be only one fragment left on the stack
        Fragment finalFragment = stack.pop();
        // create a new state machine using info from the final fragment
        return new StateMachine(finalFragment.getStates(),
                new HashSet<>(Collections.singleton(finalFragment.getAcceptState())),
                finalFragment.getTransitions(),
                finalFragment.getNullTransitions(),
                new HashSet<>(Collections.singleton(finalFragment.getStartState())));
    }

}
