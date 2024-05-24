package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.VarScope;
import pt.up.fe.comp2024.optimization.graph.GraphNode;
import pt.up.fe.comp2024.optimization.graph.InterferenceGraph;

import java.util.List;
import java.util.Map;

public class RegisterAllocator {
    private Map<String, Descriptor> varTable;
    private int params = 0;
    private int nextReg;
    private InterferenceGraph graph;
    private int colors;

    public RegisterAllocator(Map<String, Descriptor> varTable, InterferenceGraph graph, int colors, boolean isStatic) {
        this.varTable = varTable;
        this.graph = graph;
        this.colors = colors;
        if (isStatic) nextReg = 0;
        else nextReg = 1;
        countParams();
    }

    private void countParams() {
        for (Descriptor descriptor : varTable.values()) {
            if (descriptor.getScope().equals(VarScope.PARAMETER)) this.nextReg = Math.max(this.nextReg, descriptor.getVirtualReg()+1);
        }
    }

    public void allocateRegisters() {
        for (int i = 1; i <= colors; i++) {
            int finalI = i;
            List<GraphNode> nodes = graph.getNodes().values().stream().filter(node -> finalI == node.getColor()).toList();
            int reg = nextReg;

            for (GraphNode node : nodes) {
                varTable.get(node.getName()).setVirtualReg(reg);
            }

            if (reg == nextReg) nextReg++;
        }
    }
}
