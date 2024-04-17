package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
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
        setDefaultVisit(this::defaultVisit);
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

        StringBuilder computation = new StringBuilder();
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
            code = OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirRetType)
                    .append(SPACE);
        }


        int i = 0;
        if (isStatic) {
            computation.append("invokestatic").append('(').append(objType.getName())
                    .append(", \"").append(jmmNode.get("name")).append("\"");
        }
        else {
            computation.append("invokevirtual").append('(').append(codes.get(i))
                    .append(", \"").append(jmmNode.get("name")).append("\"");
            i++;
        }

        for (; i < codes.size(); i++) {
            computation.append(", ").append(codes.get(i));
        }
        computation.append(')').append(ollirRetType).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRefLiteral(JmmNode node, Void unused) {
        String varName= node.get("name");
        Type type = node.getObject("type", Type.class);

        String ollirVarType = OptUtils.toOllirType(type);
        String code="" ;
        String computation="";
        Symbol a_symbol= new Symbol(type, varName);
        if( table.getFields().contains(a_symbol) ){
            if(type.isArray()){code = "getfield(this," + varName+ ".array"  + ollirVarType+ ')'+ollirVarType;}
            else {
                var temp = OptUtils.getTemp()+ ollirVarType;
                computation = temp+SPACE+ASSIGN+ollirVarType+SPACE+"getfield(this," + varName + ollirVarType+ ')'+ollirVarType+END_STMT;
                code = temp;
            }
            return new OllirExprResult(code,computation);
        }
        if(type.isArray()){
            code = varName + ".array" + ollirVarType;
        }
        else{ code = varName + ollirVarType;}

        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBoolTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(boolType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }
    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        String code="";
        Symbol a_symbol= new Symbol(type, id);
        /*
        if( table.getFields().contains(a_symbol)){

            if(type.isArray()){code = "getfield(this," + id+ ".array"  + ollirType+ ')'+ollirType;}
            else {
                code = "getfield(this," + id + ollirType+ ')'+ollirType;
            }
            return new OllirExprResult(code);
        }

         */
        if(type.isArray()){
            code = id + ".array" + ollirType;
        }
        else{ code = id + ollirType;}

        return new OllirExprResult(code);
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

