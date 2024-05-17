package pt.up.fe.comp2024.optimization.graph;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    private final String name;
    private int color = -1;
    private List<String> adj = new ArrayList<>();

    public GraphNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getAdj() {
        return adj;
    }

    public void addAdj(String neighbor) {
        if (!adj.contains(neighbor))
            adj.add(neighbor);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
