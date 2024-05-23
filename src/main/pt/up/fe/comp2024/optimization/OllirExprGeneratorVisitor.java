package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private boolean rootOfAssign = false;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void setRootOfAssign(boolean val) {
        rootOfAssign = val;
    }

    public boolean rootCondition(JmmNode node) {
        if (!node.getParent().getJmmChild(0).isInstance(VAR_REF_LITERAL)) return true;
        else return node.getParent().getJmmChild(0).getObject("type", Type.class).getObject("level", Integer.class) != 0;
    }

    @Override
    protected void buildVisitor() {
        //addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(VAR_REF_LITERAL, this::visitVarRefLiteral);
        addVisit(CLASS_FUNCTION_CALL_EXPR, this::visitMethodCall);
        addVisit(THIS, this::visitThis);
        addVisit(NEW_CLASS_EXPR, this::visitNewObj);
        addVisit(LOGICAL_EXPR, this::visitLogicalExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitArrayNew);
        addVisit(ARRAY_EXPR, this::visitArrayExpr);
        addVisit(LENGTH_FUNCTION_EXPR, this::visitLengthFunction);
        addVisit(INDEXED_EXPR, this::visitIndexedExpr);

        addVisit("ParensExpr", this::visitParens);
        setDefaultVisit(this::defaultVisit);
    }
    private OllirExprResult visitIndexedExpr(JmmNode jmmNode, Void unused){
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        Type type = jmmNode.getJmmChild(0).getObject("type", Type.class);
        String arrayOllirType= OptUtils.toOllirType(type);

        OllirExprResult array = visit(jmmNode.getJmmChild(0));
        OllirExprResult index = visit(jmmNode.getJmmChild(1));
        Type sizetype= jmmNode.getObject("type", Type.class);
        String intOllirType = OptUtils.toOllirType(sizetype);
        String temp = OptUtils.getTemp();
        computation.append(array.getComputation());

        computation.append(temp).append(arrayOllirType).append(SPACE).append(ASSIGN).append(arrayOllirType).append(SPACE).append(array.getCode()).append(END_STMT);
        computation.append(index.getComputation());
        String indexedComputation = computation.toString();

        code.append(temp).append('[').append(index.getCode()).append(']').append(intOllirType);
        String indexedCode = code.toString();

        String temp2 = OptUtils.getTemp();
        computation.append(temp2).append(intOllirType).append(SPACE).append(ASSIGN).append(intOllirType)
                .append(SPACE).append(indexedCode).append(END_STMT);

        OllirExprResult result = new OllirExprResult(temp2 + intOllirType, computation);
        result.setIndexedCode(indexedCode);
        result.setIndexedComputation(indexedComputation);

        return result;
    }
    private OllirExprResult visitArrayExpr(JmmNode jmmNode, Void unused){
        StringBuilder computation = new StringBuilder();
        Type type = jmmNode.getObject("type", Type.class);
        Type sizetype = new Type(type.getName(), false);
        //Type sizetype= jmmNode.getJmmChild(0).getObject("type", Type.class);
        String intOllirType = OptUtils.toOllirType(sizetype);
        String arrayOllirType= OptUtils.toOllirType(type);
        String ollir_size = jmmNode.getChildren().size() + intOllirType;
        String arrayTemp = OptUtils.getTemp() + arrayOllirType;
        String varArgsArray = OptUtils.getVarArgsCounter();
        computation.append(arrayTemp + SPACE + ASSIGN + arrayOllirType + " new( array," + ollir_size + ")" + arrayOllirType + END_STMT);
        computation.append(varArgsArray + arrayOllirType + SPACE + ASSIGN + arrayOllirType + SPACE+ arrayTemp + END_STMT);
        for (int i = 0; i < jmmNode.getChildren().size(); i++) {
            OllirExprResult res = visit(jmmNode.getJmmChild(i));
            computation.append(res.getComputation());
            computation.append(varArgsArray).append('[').append(i).append(intOllirType).append(']')
                            .append(intOllirType).append(SPACE).append(ASSIGN).append(intOllirType)
                            .append(SPACE).append(res.getCode()).append(END_STMT);
            //computation.append(varArgsArray+res.getComputation()+intOllirType+ SPACE +ASSIGN + intOllirType+ SPACE + res.getCode() + END_STMT);

        }

        return new OllirExprResult(varArgsArray+arrayOllirType, computation);
    }
    private OllirExprResult visitLengthFunction(JmmNode jmmNode, Void unused) {
        StringBuilder computation = new StringBuilder();

        Type type = jmmNode.getObject("type", Type.class);

        String intOllirType = OptUtils.toOllirType(type);

        String temp = OptUtils.getTemp() + intOllirType;
        OllirExprResult var_ref = visit(jmmNode.getJmmChild(0));
        computation.append(var_ref.getComputation());
        computation.append(temp + SPACE + ASSIGN + intOllirType
                + " arraylength(" + var_ref.getCode()
                + ")" + intOllirType + END_STMT);

        return new OllirExprResult(temp, computation);
    }
    private OllirExprResult visitArrayNew(JmmNode jmmNode, Void unused){
        StringBuilder computation = new StringBuilder();
        Type type = jmmNode.getObject("type", Type.class);
        Type sizetype= jmmNode.getJmmChild(0).getObject("type", Type.class);
        String intOllirType = OptUtils.toOllirType(sizetype);
        String arrayOllirType= OptUtils.toOllirType(type);

        OllirExprResult size = visit(jmmNode.getJmmChild(0));

        String sizeTemp = OptUtils.getTemp() + intOllirType;
        computation.append(size.getComputation());
        computation.append(sizeTemp + SPACE + ASSIGN + intOllirType + SPACE + size.getCode() + END_STMT);

        String arrayTemp = OptUtils.getTemp() + arrayOllirType;
        computation.append(arrayTemp + SPACE + ASSIGN + arrayOllirType +  " new(array, " + sizeTemp + ")" + arrayOllirType + END_STMT);

        return new OllirExprResult(arrayTemp, computation);

    }
    private OllirExprResult visitParens(JmmNode jmmNode, Void unused) {
        return visit(jmmNode.getJmmChild(0), unused);
    }
    private OllirExprResult visitLogicalExpr(JmmNode jmmNode, Void unused){
        StringBuilder computation = new StringBuilder();

        OllirExprResult childVisit = visit(jmmNode.getJmmChild(0));

        computation.append(childVisit.getComputation());

        String temp = OptUtils.getTemp() + ".bool";

        computation.append(temp + " " + ASSIGN + ".bool" + " ");
        computation.append("!.bool " + childVisit.getCode() + END_STMT);

        return new OllirExprResult(temp , computation);
    }
    private OllirExprResult visitNewObj(JmmNode jmmNode, Void unused) {
        Type objType = jmmNode.getObject("type", Type.class);
        String resOllirType = OptUtils.toOllirType(objType);

        String code = OptUtils.getTemp() + resOllirType;
        StringBuilder computation = new StringBuilder();

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType)
                .append(SPACE).append("new(").append(objType.getName()).append(')')
                .append(resOllirType).append(END_STMT);
        computation.append("invokespecial(").append(code).append(",\"<init>\").V").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitThis(JmmNode jmmNode, Void unused) {
        return new OllirExprResult("this");
    }

    private OllirExprResult visitMethodCall(JmmNode jmmNode, Void unused) {
        Type retType = jmmNode.getObject("type", Type.class);
        Type objType = jmmNode.getJmmChild(0).getObject("type", Type.class);
        String code;
        boolean tempRoot = false;

        StringBuilder computation = new StringBuilder();
        StringBuilder abbreviatedComputation = new StringBuilder();
        List<String> codes = new ArrayList<>();

        boolean isStatic = objType.getOptionalObject("isStatic").isPresent();
        List<JmmNode> children = jmmNode.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (isStatic && i == 0) continue;
            OllirExprResult res = visit(children.get(i), unused);
            codes.add(res.getCode());
            computation.append(res.getComputation());
        }


        String ollirRetType = OptUtils.toOllirType(retType);

        if (retType.getName().equals("void")) {
            code = "";
        }
        else {
            String resOllirType = OptUtils.toOllirType(retType);
            if (rootOfAssign && jmmNode.getParent().isInstance(ASSIGN_STMT) && rootCondition(jmmNode)) {
                tempRoot = true;
                code = "";
                rootOfAssign = false;
            }
            else {
                code = OptUtils.getTemp() + resOllirType;
                computation.append(code).append(SPACE).append(ASSIGN).append(ollirRetType)
                        .append(SPACE);
            }
        }


        int i = 0;
        if (isStatic) {
            abbreviatedComputation.append("invokestatic").append('(').append(objType.getName())
                    .append(", \"").append(jmmNode.get("name")).append("\"");
        }
        else {
            abbreviatedComputation.append("invokevirtual").append('(').append(codes.get(i))
                    .append(", \"").append(jmmNode.get("name")).append("\"");
            i++;
        }

        for (; i < codes.size(); i++) {
            abbreviatedComputation.append(", ").append(codes.get(i));
        }
        abbreviatedComputation.append(')').append(ollirRetType);

        if (tempRoot) {
            code = abbreviatedComputation.toString();
        }
        else {
            computation.append(abbreviatedComputation).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRefLiteral(JmmNode node, Void unused) {
        String varName= node.get("name");
        Type type = node.getObject("type", Type.class);

        String ollirVarType = OptUtils.toOllirType(type);
        String code;
        String computation="";
        if( type.getObject("level", Integer.class) == 0 ){
            if(type.isArray()){code = "getfield(this," + varName+ ".array"  + ollirVarType+ ')'+ollirVarType;}
            else {
                var temp = OptUtils.getTemp()+ ollirVarType;
                computation = temp+SPACE+ASSIGN+ollirVarType+SPACE+"getfield(this," + varName + ollirVarType+ ')'+ollirVarType+END_STMT;
                code = temp;
            }
            return new OllirExprResult(code,computation);
        }
        code = varName + ollirVarType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBoolTypeName(), false);
        String ollirboolType = OptUtils.toOllirType(boolType);
        StringBuilder code= new StringBuilder();
        if(node.get("value").equals("true")){code.append("1"+ollirboolType);}
        if(node.get("value").equals("false")){code.append("0"+ollirboolType);}

        return new OllirExprResult(code.toString());
    }
    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitAndExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();

        OllirExprResult lhs = visit(node.getJmmChild(0));
        OllirExprResult rhs = visit(node.getJmmChild(1));

        Type type = node.getObject("type", Type.class);
        String nodetype = OptUtils.toOllirType(type);

        computation.append(lhs.getComputation()).append(rhs.getComputation());

        String temp = OptUtils.getTemp() + nodetype;
        String if_ = OptUtils.getif();
        String endif = OptUtils.getendif();

        computation.append("if (" + lhs.getCode() + ") goto " + if_ + END_STMT);
        computation.append(temp + SPACE + ASSIGN + nodetype + SPACE + "0" + nodetype + END_STMT);
        computation.append("goto " + endif + END_STMT);

        computation.append(if_ + ":\n");
        computation.append(temp + SPACE + ASSIGN + nodetype+ SPACE + rhs.getCode() + END_STMT);

        computation.append(endif + ":\n");
        return new OllirExprResult(temp, computation);

    }
    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        if(node.get("op").equals("&&")){return visitAndExpr(node,unused);}
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        StringBuilder abbreviatedComputation = new StringBuilder();
        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code;

        Type type = TypeUtils.getExprType(node, table);
        abbreviatedComputation.append(lhs.getCode()).append(SPACE).append(node.get("op"))
                .append(OptUtils.toOllirType(type)).append(SPACE).append(rhs.getCode());

        if (rootOfAssign  && node.getParent().isInstance(ASSIGN_STMT) && rootCondition(node)) {
            code = abbreviatedComputation.toString();
            rootOfAssign = false;
        }
        else {
            code = OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE);
            computation.append(abbreviatedComputation).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}

