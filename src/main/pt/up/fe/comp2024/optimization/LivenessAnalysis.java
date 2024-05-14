package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

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

        return this.getUseFromInstruction(inst);

    }

    private Set<String> getUseFromInstruction(Instruction inst) {
        return switch (inst.getInstType()) {
            case ASSIGN -> this.getUseFromInstruction(((AssignInstruction) inst).getRhs());
            case CALL -> this.getUseOp(((CallInstruction) inst).getOperands());
            case GOTO -> null;
            case BRANCH -> this.getUseFromInstruction(((CondBranchInstruction) inst).getCondition());
            case RETURN -> this.getUseOp(Collections.singletonList(((ReturnInstruction) inst).getOperand()));
            case PUTFIELD, GETFIELD -> this.getUseOp(((FieldInstruction) inst).getOperands());
            case UNARYOPER, BINARYOPER -> this.getUseOp(((OpInstruction) inst).getOperands());
            case NOPER -> this.getUseOp(Collections.singletonList(((SingleOpInstruction) inst).getSingleOperand()));
        };
    }

    private Set<String> getUseOp(List<Element> operands) {
        Set<String> use = new TreeSet<>();

        for (Element element : operands) {
            if (element.isLiteral()) continue;

            use.add(((Operand) element).getName());
        }
        return use;
    }
}
