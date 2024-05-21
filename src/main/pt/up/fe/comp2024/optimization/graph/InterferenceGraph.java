package pt.up.fe.comp2024.optimization.graph;

import java.util.*;

public class InterferenceGraph {
    private Map<String, GraphNode> nodes = new HashMap<>();

    public InterferenceGraph(List<String> vars) {
        for (String var : vars) {
            if (var.equals("this")) continue;
            nodes.put(var, new GraphNode(var));
        }
    }

    public void buildEdges(Map<Integer, Set<String>> defs, Map<Integer, Set<String>> outs, Map<Integer, Set<String>> ins) {
        for (Integer id : defs.keySet()) {
            Set<String> union = new TreeSet<>(defs.get(id));
            union.addAll(outs.get(id));
            Set<String> in = ins.get(id);

            for (String s : union) {
                for (String t : union) {
                    if (s == t) continue;
                    if (!nodes.containsKey(s) || !nodes.containsKey(t)) {
                        continue;
                    }
                    nodes.get(s).addAdj(t);
                }
            }
            for (String s : in) {
                for (String t : in) {
                    if (s == t) continue;
                    if (!nodes.containsKey(s) || !nodes.containsKey(t)) {
                        continue;
                    }
                    nodes.get(s).addAdj(t);
                }
            }
        }
    }

    public Map<String, GraphNode> getNodes() {
        return nodes;
    }

    public int getDegree(String name) {
        int degree = 0;
        GraphNode node = nodes.get(name);
        for (String adj : node.getAdj()) {
            GraphNode neighbor = nodes.get(adj);
            if (neighbor.getColor() != -1) degree++;
        }

        return degree;
    }
}
