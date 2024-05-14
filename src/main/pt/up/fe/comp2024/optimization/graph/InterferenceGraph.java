package pt.up.fe.comp2024.optimization.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterferenceGraph {
    private Map<String, GraphNode> nodes = new HashMap<>();

    public InterferenceGraph(List<String> vars) {
        for (String var : vars) {
            nodes.put(var, new GraphNode(var));
        }
    }

    public void buildEdges(Map<Integer, Set<String>> ins, Map<Integer, Set<String>> outs) {
        for (Integer id : ins.keySet()) {

        }
    }
}
