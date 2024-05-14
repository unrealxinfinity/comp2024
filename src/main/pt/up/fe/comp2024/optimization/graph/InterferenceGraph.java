package pt.up.fe.comp2024.optimization.graph;

import java.util.*;

public class InterferenceGraph {
    private Map<String, GraphNode> nodes = new HashMap<>();

    public InterferenceGraph(List<String> vars) {
        for (String var : vars) {
            nodes.put(var, new GraphNode(var));
        }
    }

    public void buildEdges(Map<Integer, Set<String>> uses, Map<Integer, Set<String>> outs) {
        for (Integer id : uses.keySet()) {
            Set<String> union = new TreeSet<>(uses.get(id));
            union.addAll(outs.get(id));
            for (String s : union) {
                for (String t : union) {
                    if (s == t) continue;
                    nodes.get(s).addAdj(t);
                    nodes.get(t).addAdj(s);
                }
            }
        }
    }
}
