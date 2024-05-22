package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.*;

public class LivenessAnalysis {

    private final Map<String, Map<Integer, Set<String>>> outs = new HashMap<>();
    private final Map<String, Map<Integer, Set<String>>> defs = new HashMap<>();
    private final Map<String, Map<Integer, Set<String>>> ins = new HashMap<>();
    private Method currMethod;
    public void buildLivenessSets(OllirResult ollirResult) {
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            currMethod = method;
            outs.put(method.getMethodName(), new HashMap<>());
            defs.put(method.getMethodName(), new HashMap<>());
            ins.put(method.getMethodName(), new HashMap<>());

            this.buildLivenessSets(method);
        }
    }

    private void buildLivenessSets(Method method) {
        boolean changed = true;

        Map<Integer, Set<String>> useSets = new HashMap<>();
        Map<Integer, Set<String>> defSets = defs.get(method.getMethodName());
        Map<Integer, Set<String>> inSets = ins.get(method.getMethodName());
        Map<Integer, Set<String>> outSets = outs.get(method.getMethodName());

        while (changed) {
            Set<Integer> visited = new TreeSet<>();
            changed = false;
            Queue<Node> queue = new ArrayDeque<>(Collections.singletonList(method.getBeginNode()));

            while (!queue.isEmpty()) {
                Node curr = queue.remove();
                if (visited.contains(curr.getId())) continue;
                visited.add(curr.getId());

                queue.addAll(curr.getSuccessors());
                if (!curr.getNodeType().equals(NodeType.INSTRUCTION)) continue;

                if (!useSets.containsKey(curr.getId())) useSets.put(curr.getId(), this.getUse(curr));
                if (!defSets.containsKey(curr.getId())) defSets.put(curr.getId(), this.getDef(curr));
                Set<String> newOut = new TreeSet<>();

                Set<String> outTemp = new TreeSet<>(outSets.getOrDefault(curr.getId(), new TreeSet<>()));
                outTemp.removeAll(defSets.get(curr.getId()));
                outTemp.addAll(useSets.get(curr.getId()));

                for (Node successor : curr.getSuccessors()) {
                    newOut.addAll(inSets.getOrDefault(successor.getId(), new TreeSet<>()));
                }

                if (!inSets.getOrDefault(curr.getId(), new TreeSet<>()).equals(outTemp)) changed = true;
                if (!outSets.getOrDefault(curr.getId(), new TreeSet<>()).equals(newOut)) changed = true;
                inSets.put(curr.getId(), outTemp);
                outSets.put(curr.getId(), newOut);
            }
        }
    }

    private Set<String> getDef(Node curr) {
        Set<String> def = new TreeSet<>();
        if (!curr.getNodeType().equals(NodeType.INSTRUCTION)) return def;

        Instruction inst = curr.toInstruction();
        if (!inst.getInstType().equals(InstructionType.ASSIGN)) return def;
        AssignInstruction assign = (AssignInstruction) inst;

        Operand dest = (Operand) assign.getDest();
        if (!(dest instanceof ArrayOperand)) {
            def.add(dest.getName());
        }

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
            case ASSIGN -> this.getUseFromAssign((AssignInstruction) inst);
            case CALL -> this.getUseOp(((CallInstruction) inst).getOperands());
            case GOTO -> new TreeSet<>();
            case BRANCH -> this.getUseFromInstruction(((CondBranchInstruction) inst).getCondition());
            case RETURN -> this.getUseOp(Collections.singletonList(((ReturnInstruction) inst).getOperand()));
            case PUTFIELD, GETFIELD -> this.getUseOp(((FieldInstruction) inst).getOperands());
            case UNARYOPER, BINARYOPER -> this.getUseOp(((OpInstruction) inst).getOperands());
            case NOPER -> this.getUseOp(Collections.singletonList(((SingleOpInstruction) inst).getSingleOperand()));
        };
    }

    private Set<String> getUseFromAssign(AssignInstruction inst) {
        Set<String> rhs = this.getUseFromInstruction(inst.getRhs());
        if (inst.getDest() instanceof ArrayOperand operand) {
            Set<String> lhs = this.getUseOp(operand.getIndexOperands());
            lhs.add(operand.getName());
            rhs.addAll(lhs);
        }
        return rhs;
    }

    private Set<String> getUseOp(List<Element> operands) {
        Set<String> use = new TreeSet<>();

        for (Element element : operands) {
            if (element == null || element.isLiteral()) continue;
            String name = ((Operand) element).getName();
            if (name.equals("array")) {
                continue;
            }
            if (name.equals("this")) {
                continue;
            }
            if (currMethod.getVarTable().containsKey(name) && currMethod.getVarTable().get(name).getScope().equals(VarScope.FIELD)) {
                continue;
            }

            use.add(((Operand) element).getName());
        }
        return use;
    }


    public Map<Integer, Set<String>> getOuts(String method) {
        return outs.get(method);
    }
    public Map<Integer, Set<String>> getIns(String method) {
        return ins.get(method);
    }
    public Map<Integer, Set<String>> getDefs(String method) {
        return defs.get(method);
    }
}
