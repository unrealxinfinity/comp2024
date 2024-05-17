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

    public RegisterAllocator(Map<String, Descriptor> varTable, InterferenceGraph graph, int colors) {
        this.varTable = varTable;
        this.graph = graph;
        this.colors = colors;
        countParams();
        nextReg = params+1;
    }

    private void countParams() {
        for (Descriptor descriptor : varTable.values()) {
            if (descriptor.getScope().equals(VarScope.PARAMETER)) this.params++;
        }
    }

    public void allocateRegisters() {
        for (int i = 0; i < colors; i++) {
            int finalI = i;
            List<GraphNode> nodes = graph.getNodes().values().stream().filter(node -> finalI == node.getColor()).toList();
            int reg = nextReg;

            for (GraphNode node : nodes) {
                if (varTable.get(node.getName()).getScope().equals(VarScope.PARAMETER)) {
                    reg = varTable.get(node.getName()).getVirtualReg();
                }
            }

            for (GraphNode node : nodes) {
                varTable.get(node.getName()).setVirtualReg(reg);
            }

            if (reg == nextReg) nextReg++;
        }
    }
}
