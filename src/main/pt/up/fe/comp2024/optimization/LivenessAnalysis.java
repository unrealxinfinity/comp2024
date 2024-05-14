package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class LivenessAnalysis {

    private final Map<String, Map<Integer, Set<String>>> outs = new HashMap<>();
    private final Map<String, Map<Integer, Set<String>>> uses = new HashMap<>();

    public void buildLivenessSets(OllirResult ollirResult) {
        ollirResult.getOllirClass().buildCFGs();
        ollirResult.getOllirClass().buildVarTables();
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            outs.put(method.getMethodName(), new HashMap<>());
            uses.put(method.getMethodName(), new HashMap<>());
            this.buildLivenessSets(method);
        }
    }

    private void buildLivenessSets(Method method) {
        Queue<Node> queue = new ArrayDeque<>(Collections.singletonList(method.getBeginNode()));
        Map<Integer, Set<String>> useSets = uses.get(method.getMethodName());
        Map<Integer, Set<String>> defSets = new HashMap<>();
        Map<Integer, Set<String>> inSets = new HashMap<>();
        Map<Integer, Set<String>> outSets = outs.get(method.getMethodName());

        while (!queue.isEmpty()) {
            Node curr = queue.remove();
            queue.addAll(curr.getSuccessors());
            if (!curr.getNodeType().equals(NodeType.INSTRUCTION)) continue;

            if (!useSets.containsKey(curr.getId())) useSets.put(curr.getId(), this.getUse(curr));
            if (!defSets.containsKey(curr.getId())) defSets.put(curr.getId(), this.getDef(curr));
            Set<String> newOut = new TreeSet<>();

            Set<String> outTemp = new TreeSet<>(outSets.getOrDefault(curr.getId(), new TreeSet<>()));
            Set<String> useTemp = new TreeSet<>(useSets.get(curr.getId()));
            outTemp.removeAll(defSets.get(curr.getId()));
            useTemp.addAll(outTemp);

            for (Node successor : curr.getSuccessors()) {
                newOut.addAll(inSets.getOrDefault(successor.getId(), new TreeSet<>()));
            }

            inSets.put(curr.getId(), useTemp);
            outSets.put(curr.getId(), newOut);
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
            if (element == null || element.isLiteral()) continue;

            use.add(((Operand) element).getName());
        }
        return use;
    }


    private Map<Integer, Set<String>> getOuts(String method) {
        return outs.get(method);
    }

    private Map<Integer, Set<String>> getUses(String method) {
        return uses.get(method);
    }
}
