package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class LivenessAnalysis {
    
    Map<String, Map<Integer, Set<String>>> ins;
    Map<String, Map<Integer, Set<String>>> outs;

    public void buildLivenessSets(OllirResult ollirResult) {
        ollirResult.getOllirClass().buildCFGs();
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            this.buildLivenessSets(method);
        }
    }

    private void buildLivenessSets(Method method) {
        Node curr = method.getBeginNode();
        Map<Integer, Set<String>> useSets = new HashMap<>();
        Map<Integer, Set<String>> defSets = new HashMap<>();

        while (curr != method.getEndNode()) {
            if (!useSets.containsKey(curr.getId())) useSets.put(curr.getId(), this.getUse(curr));
            if (!defSets.containsKey(curr.getId())) defSets.put(curr.getId(), this.getDef(curr));
            curr = curr.getSucc1();
        }
    }

    private Set<String> getDef(Node curr) {
        Set<String> def = new TreeSet<>();
        if (!curr.getNodeType().equals(NodeType.INSTRUCTION)) return def;

        Instruction inst = curr.toInstruction();
        if (!inst.getInstType().equals(InstructionType.ASSIGN)) return def;
        AssignInstruction assign = (AssignInstruction) inst;

        Operand dest = (Operand) assign.getDest();
        def.add(dest.getName());

        return def;
    }

    private Set<String> getUse(Node curr) {
        Set<String> use = new TreeSet<>();
        if (!curr.getNodeType().equals(NodeType.INSTRUCTION)) return use;

        Instruction inst = curr.toInstruction();

        return switch (inst.getInstType()) {
            case ASSIGN -> this.getUseAssign(inst);
            case CALL -> this.getUseCall(inst);
            case GOTO -> this.getUseGoto(inst);
            case BRANCH -> this.getUseBranch(inst);
            case RETURN -> this.getUseReturn(inst);
            case PUTFIELD -> this.getUsePutfield(inst);
            case GETFIELD -> this.getUseGetfield(inst);
            case UNARYOPER -> this.getUseUnaryOp(inst);
            case BINARYOPER -> this.getUseBinaryOp((BinaryOpInstruction) inst);
            case NOPER -> this.getUseNoper(inst);
        };
    }

    private Set<String> getUseBinaryOp(BinaryOpInstruction inst) {

    }
}
