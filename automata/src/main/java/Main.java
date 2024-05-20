import com.mxgraph.swing.*;
import com.mxgraph.util.*;
import com.mxgraph.view.mxGraph;
import org.jgrapht.*;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.*;

import javax.swing.*;
import java.util.*;

public class Main {

    public static final String ERROR_STATE = "[Error]";
    public static final String INIT_AUTOMATON = "[INICIO]";

    static boolean isNotNfa = true;

    public static void main(String[] args) {
        // Definir el autómata no determinista (AFN)
        InputAutomaton input = initAutomaton();

        Set<String> nfaStates = input.getStates();
        Map<String, Map<Character, Set<String>>> nfaTransitions = input.getTransitions();
        /** new HashMap<>();
         nfaTransitions.put("A", Map.of( '1', Set.of("A", "B")));
         nfaTransitions.put("B", Map.of('0', Set.of("B"), '1', Set.of("B")));*/
        String nfaStartState = input.getStartState();
        Set<String> nfaAcceptStates = input.getAcceptStates();

        Map<Pair<String, Character>, Set<String>> transitions = new HashMap<>();
        nfaTransitions.forEach((s, characterSetMap) -> {
                    characterSetMap.forEach((symbol, nextState) -> {
                        transitions.put(new Pair<>("[" + s + "]", symbol), nextState);

                    });
                }
        );
        Set<String> nfaPrintStates = new HashSet<>();
        nfaStates.forEach(s -> nfaPrintStates.add("[" + s + "]"));
        Set<String> printStateNfaAcceptStates = new HashSet<>();
        nfaAcceptStates.forEach(s -> printStateNfaAcceptStates.add("["+s+"]") );


        System.out.println(transitions);

        /**
         * Valida si el automata es no determinista, a partir de las transiciones
         * a multiples estados desde un mismo estado
         */
        transitions.forEach((stringCharacterPair, strings) -> isNotNfa = isNotNfa && strings.size() == 1);

        if(isNotNfa){
            JOptionPane.showMessageDialog(null,"El automata ingresado, no es un (AFND)");
            System.exit(0);
        }

        // Mostrar el NAFD graficamente
        drawAutomaton("Automata No Deterministico", false, nfaPrintStates, transitions, "[" + nfaStartState + "]", printStateNfaAcceptStates);

        // Convertir el AFN a un autómata finito determinista (AFD)
        AutomatonConversionResult conversionResult = convertNFAToDFA(nfaStates, nfaTransitions, nfaStartState, nfaAcceptStates);

        // Mostrar el AFD graficamente
        drawAutomaton("Automata Deterministico", true, conversionResult.getDfaStates(), conversionResult.getDfaTransitions(), conversionResult.getDfaStartState(), conversionResult.getDfaAcceptStates());
    }

    public static InputAutomaton initAutomaton() {
        String[] inputStates = (javax.swing.JOptionPane.showInputDialog("Ingrese los estados del Automata no deterministico, separados por (,)")).split(",");
        Set<String> setStates = new HashSet<>(Arrays.asList(inputStates));
        Set<String> nfaAcceptStates = new HashSet<>();
        Map<String, Map<Character, Set<String>>> nfaTransitions = new HashMap<>();
        String inputStartState = null;
        String[] inputAcceptStates;
        int reintentos = 3;
        do {
            if (reintentos <= 0) {
                javax.swing.JOptionPane.showMessageDialog(null, "Hasta la proxima mi pequeño Padawan");
                System.exit(0);
            }
            String finalStarState = (javax.swing.JOptionPane.showInputDialog("Ingrese el estado inicial"));
            long exist = setStates.stream().filter(state -> state.equals(finalStarState)).count();
            if (exist > 0) {
                inputStartState = finalStarState;
                reintentos = 3;
            } else {
                javax.swing.JOptionPane.showMessageDialog(null, "El estado ingresado no existe " + setStates.toString());
            }
            --reintentos;
        } while (Objects.isNull(inputStartState));
        do {
            if (reintentos <= 0) {
                javax.swing.JOptionPane.showMessageDialog(null, "Hasta la proxima mi pequeño Padawan");
                System.exit(0);
            }
            inputAcceptStates = (javax.swing.JOptionPane.showInputDialog("Ingrese los estados de aceptación, separados por (,)")).split(",");
            long exist = 0;
            for (String startState : inputAcceptStates) {
                exist = setStates.stream().filter(state -> state.equals(startState)).count();
                if (exist <= 0) {
                    javax.swing.JOptionPane.showMessageDialog(null, "El estado ingresado no existe " + setStates.toString());
                    break;
                }
            }
            if (exist > 0) {
                nfaAcceptStates = new HashSet<>(Arrays.asList(inputAcceptStates));
                reintentos = 3;
            } else {
                inputAcceptStates = null;
            }
            --reintentos;
        } while (Objects.isNull(inputAcceptStates));
        String[] inputSymbols = (javax.swing.JOptionPane.showInputDialog("Ingrese los simbolos de entrada para el automata, separados por (,)")).split(",");

        setStates.forEach(state -> {
            Map<Character, Set<String>> transitions = new HashMap<>();
            for (int i = 0; i < inputSymbols.length; i++) {
                String[] transitionsState = (javax.swing.JOptionPane.showInputDialog("Hacia donde viaja el estado [" + state + "] con el simbolo [" + inputSymbols[i] + "], si son varios ingreselos separados por (,)")).split(",");
                if (transitionsState.length > 0 && !transitionsState[0].isEmpty()) {
                    transitions.put(inputSymbols[i].charAt(0), Set.of(transitionsState));
                }
            }
            nfaTransitions.put(state, transitions);

        });

        return new InputAutomaton(setStates, inputStartState, nfaAcceptStates, nfaTransitions);
    }

    public static AutomatonConversionResult convertNFAToDFA(Set<String> nfaStates, Map<String, Map<Character, Set<String>>> nfaTransitions, String nfaStartState, Set<String> nfaAcceptStates) {
        Set<String> dfaStates = new HashSet<>();
        Map<Pair<String, Character>, Set<String>> dfaTransitions = new HashMap<>();
        Set<String> dfaAcceptStates = new HashSet<>();
        Queue<Set<String>> pending = new LinkedList<>();

        // Calcular cierre épsilon del estado inicial
        Set<String> startEpsilonClosure = epsilonClosure(Set.of(nfaStartState), nfaTransitions);

        // Inicializar el estado inicial del DFA
        dfaStates.add(startEpsilonClosure.toString());
        pending.add(startEpsilonClosure);

        // Mientras haya estados pendientes por procesar en el DFA
        while (!pending.isEmpty()) {
            Set<String> currentState = pending.poll();

            // Si el estado actual contiene al menos un estado de aceptación del AFN, es un estado de aceptación del DFA
            if (currentState.stream().anyMatch(nfaAcceptStates::contains)) {
                dfaAcceptStates.add(currentState.toString());
            }

            // Para cada símbolo del alfabeto
            for (char symbol : getAlphabet(nfaTransitions)) {
                Set<String> nextState = new HashSet<>();

                // Calcular el conjunto de estados alcanzables desde el estado actual con el símbolo actual
                for (String state : currentState) {
                    if (nfaTransitions.containsKey(state) && nfaTransitions.get(state).containsKey(symbol)) {
                        nextState.addAll(epsilonClosure(nfaTransitions.get(state).get(symbol), nfaTransitions));
                    }
                }

                // Agregar transición al DFA
                dfaTransitions.put(new Pair<>(currentState.toString(), symbol), nextState);

                // Si el nuevo estado no ha sido visitado, agregarlo a la lista de estados pendientes
                if (!dfaStates.contains(nextState.toString())) {
                    dfaStates.add(nextState.toString());
                    pending.add(nextState);
                }
            }
        }

        return new AutomatonConversionResult(dfaStates, dfaTransitions, startEpsilonClosure.toString(), dfaAcceptStates);
    }

    public static Set<String> epsilonClosure(Set<String> states, Map<String, Map<Character, Set<String>>> transitions) {
        Set<String> closure = new HashSet<>(states);
        Queue<String> pending = new LinkedList<>(states);

        while (!pending.isEmpty()) {
            String state = pending.poll();
            if (transitions.containsKey(state) && transitions.get(state).containsKey("\0")) {
                Set<String> epsilonTransitions = transitions.get(state).get("\0");
                for (String nextState : epsilonTransitions) {
                    if (!closure.contains(nextState)) {
                        closure.add(nextState);
                        pending.add(nextState);
                    }
                }
            }
        }
        System.out.println("Cierre epsilon: "+ states+" cierre: " +closure);

        return closure;
    }

    public static Set<Character> getAlphabet(Map<String, Map<Character, Set<String>>> transitions) {
        Set<Character> alphabet = new HashSet<>();
        for (Map<Character, Set<String>> transition : transitions.values()) {
            alphabet.addAll(transition.keySet());
        }
        alphabet.remove("\0"); // Remove epsilon transitions
        return alphabet;
    }

    public static void drawAutomaton(String txt_title, boolean isDeterministic, Set<String> dfaStates, Map<Pair<String, Character>, Set<String>> transitions, String startState, Set<String> acceptStates) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Set<String> states = new HashSet<>();
        dfaStates.forEach(state -> {
            if ("[]".equals(state)) {
                states.add(ERROR_STATE);
            } else {
                states.add(state);
            }
        });
        for (String state : states) {
            graph.addVertex(state);
            String[] list = state.replace("[", "").replace("]", "").split(",");
            for (String s : list) {
                graph.addVertex(s.trim());
            }

        }

        graph.addVertex(INIT_AUTOMATON);

        for (Map.Entry<Pair<String, Character>, Set<String>> entry : transitions.entrySet()) {
            Pair<String, Character> key = entry.getKey();
            for (String nextState : entry.getValue()) {
                if ("[]".equals(key.getFirst())) {
                    graph.addEdge(ERROR_STATE, nextState);
                } else {
                    graph.addEdge(key.getFirst(), nextState);
                }
            }
        }
        graph.addEdge(INIT_AUTOMATON, startState);
        // Crear un objeto JGraphX para visualizar el grafo
        mxGraph mGraph = new mxGraph();
        Object parent = mGraph.getDefaultParent();

        mGraph.getModel().beginUpdate();
        try {
            Map<String, Object> vertexMap = new HashMap<>();
            int x = 121, y = 10, index = 1;

            for (String state : states) {
                if (startState.equals(state)) {
                    x = 171;
                    y = 230;
                }
                Object vertex = mGraph.insertVertex(parent, null, state, x, y, 80, 30);
                vertexMap.put(state, vertex);
                if (acceptStates.contains(state)) {
                    mGraph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#90EE90", new Object[]{vertex});
                }
                if (ERROR_STATE.equals(state)) {
                    mGraph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FF5733", new Object[]{vertex});
                }

                x = x + 90;
                y = y + 110;
                ++index;
            }

            Object vertexInit = mGraph.insertVertex(parent, null, INIT_AUTOMATON, 10, 220, 100, 50);
            vertexMap.put(INIT_AUTOMATON, vertexInit);
            mGraph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#BBBFD6", new Object[]{vertexInit});

            Object sourceVertexInit = vertexMap.get(INIT_AUTOMATON);
            Object targetVertexInit = vertexMap.get(startState);
            mGraph.insertEdge(parent, null, "", sourceVertexInit, targetVertexInit);

            for (Pair<String, Character> transition : transitions.keySet()) {
                Object sourceVertex;
                if ("[]".equals(transition.getFirst())) {
                    sourceVertex = vertexMap.get(ERROR_STATE);
                    Object targetVertex = vertexMap.get(ERROR_STATE);
                    mGraph.insertEdge(parent, null, transition.getSecond(), sourceVertex, targetVertex);
                } else {
                    sourceVertex = vertexMap.get(transition.getFirst());
                    Object targetVertex = vertexMap.get(transitions.get(transition).toString());
                    if (!isDeterministic && Objects.isNull(targetVertex)) {
                        transitions.get(transition).forEach(s -> {
                            Object vertex = vertexMap.get("[" + s + "]");
                            mGraph.insertEdge(parent, null, transition.getSecond(), sourceVertex, vertex);
                        });
                    } else {
                        if ("[]".equals(transitions.get(transition).toString())) {
                            targetVertex = vertexMap.get(ERROR_STATE);
                        }
                        mGraph.insertEdge(parent, null, transition.getSecond(), sourceVertex, targetVertex);
                    }

                }


            }

            mGraph.getModel().endUpdate();
        } finally {
            mGraph.setAllowDanglingEdges(false);
        }

        // Visualizar el grafo en un JFrame
        mxGraphComponent graphComponent = new mxGraphComponent(mGraph);
        JFrame frame = new JFrame();
        frame.setTitle(txt_title);
        graphComponent.setCenterZoom(true);
        graphComponent.setToolTipText(txt_title);
        frame.getContentPane().add(graphComponent);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    static class InputAutomaton {
        private final Set<String> states;
        private final String startState;
        private final Set<String> acceptStates;
        private final Map<String, Map<Character, Set<String>>> transitions;


        public InputAutomaton(Set<String> states, String startState, Set<String> acceptStates, Map<String, Map<Character, Set<String>>> transitions) {
            this.states = states;
            this.startState = startState;
            this.acceptStates = acceptStates;
            this.transitions = transitions;
        }

        public Set<String> getStates() {
            return states;
        }

        public String getStartState() {
            return startState;
        }

        public Set<String> getAcceptStates() {
            return acceptStates;
        }

        public Map<String, Map<Character, Set<String>>> getTransitions() {
            return transitions;
        }
    }

    static class AutomatonConversionResult {
        private final Set<String> dfaStates;
        private final Map<Pair<String, Character>, Set<String>> dfaTransitions;
        private final String dfaStartState;
        private final Set<String> dfaAcceptStates;

        public AutomatonConversionResult(Set<String> dfaStates, Map<Pair<String, Character>, Set<String>> dfaTransitions, String dfaStartState, Set<String> dfaAcceptStates) {
            this.dfaStates = dfaStates;
            this.dfaTransitions = dfaTransitions;
            this.dfaStartState = dfaStartState;
            this.dfaAcceptStates = dfaAcceptStates;
        }

        public Set<String> getDfaStates() {
            return dfaStates;
        }

        public Map<Pair<String, Character>, Set<String>> getDfaTransitions() {
            return dfaTransitions;
        }

        public String getDfaStartState() {
            return dfaStartState;
        }

        public Set<String> getDfaAcceptStates() {
            return dfaAcceptStates;
        }
    }
}
