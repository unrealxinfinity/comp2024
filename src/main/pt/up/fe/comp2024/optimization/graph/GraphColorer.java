package pt.up.fe.comp2024.optimization.graph;

import org.specs.comp.ollir.Descriptor;

import java.util.*;

public class GraphColorer {

    private InterferenceGraph graph;
    public GraphColorer(InterferenceGraph graph) {
        this.graph = graph;
    }

    public boolean colorGraph(int colors) {
        for (GraphNode node : graph.getNodes().values()) {
            node.setColor(0);
        }
        int nodesLeft = graph.getNodes().size();
        Stack<String> stack = new Stack<>();

        while (stack.size() < graph.getNodes().size()) {
            int currSize = stack.size();
            for (Map.Entry<String, GraphNode> entry : graph.getNodes().entrySet()) {
                if (graph.getDegree(entry.getKey()) >= colors) continue;
                if (entry.getValue().getColor() == -1) continue;

                stack.add(entry.getKey());
                entry.getValue().setColor(-1);
            }
            if (currSize == stack.size()) return false;
        }

        while (!stack.isEmpty()) {
            String curr = stack.pop();
            int minColor = 1;
            GraphNode currNode = graph.getNodes().get(curr);
            List<Boolean> foundColors = new ArrayList<>(Collections.nCopies(colors, false));

            for (Iterator<GraphNode> it = currNode.getAdj().stream().map(name -> graph.getNodes().get(name)).iterator(); it.hasNext(); ) {
                GraphNode adj = it.next();
                if (adj.getColor() == -1) continue;
                foundColors.set(adj.getColor()-1, true);

                while (foundColors.get(minColor-1)) {
                    minColor++;
                }
            }
            currNode.setColor(minColor);
        }

        return true;
    }
}
