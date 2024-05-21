package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */


public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(SIMPLE_STATEMENT, this::visitSimpleStatement);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(IF_STATEMENT,this::visitIfStatement);
        addVisit(WHILE_STATEMENT, this::visitWhileStatement);
        addVisit(ENCVALOSE_STATEMENT, this::visitEncvaloseStatement);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitEncvaloseStatement(JmmNode node, Void unused) {
        StringBuilder codeBuilder = new StringBuilder();

        for (JmmNode stmt : node.getChildren())
            codeBuilder.append(visit(stmt));

        return codeBuilder.toString();
    }
    private String visitWhileStatement(JmmNode node, Void unused){
        StringBuilder codeBuilder = new StringBuilder();

        OllirExprResult condition = exprVisitor.visit(node.getJmmChild(0));
        String loopbranch= OptUtils.getWhileLoop();
        String conditionbranch= OptUtils.getWhileCond();
        String whileendbranch= OptUtils.getWhileEnd();
        String loopbody = visit(node.getJmmChild(1));

        codeBuilder.append(conditionbranch).append(":").append(NL);

        codeBuilder.append(condition.getComputation()).append(NL);

        codeBuilder.append("if ").append("(").append(condition.getCode()).append(")").append(SPACE).append("goto ").append(loopbranch).append(END_STMT);
        codeBuilder.append("goto ").append(SPACE).append(whileendbranch).append(END_STMT);
        codeBuilder.append(loopbranch).append(":").append(NL);
        codeBuilder.append(loopbody).append(NL);
        codeBuilder.append("goto ").append(SPACE).append(conditionbranch).append(END_STMT);
        codeBuilder.append(whileendbranch).append(":").append(NL);

        return codeBuilder.toString();
    }
    private String visitIfStatement(JmmNode node, Void unused){
        StringBuilder codeBuilder = new StringBuilder();


        OllirExprResult condition = exprVisitor.visit(node.getJmmChild(0));
        codeBuilder.append(condition.getComputation()).append(NL);
        String if_= OptUtils.getif();
        String endif_ = OptUtils.getendif();

        codeBuilder.append("if (").append(condition.getCode()).append(")").append(SPACE).append("goto ").append(if_).append(END_STMT);

        String then_ = visit(node.getJmmChild(1));
        String else_ = visit(node.getJmmChild(2));

        codeBuilder.append(else_).append(NL);
        codeBuilder.append("goto ").append(endif_).append(END_STMT);

        codeBuilder.append(if_).append(":").append(NL);
        codeBuilder.append(then_);
        codeBuilder.append(endif_).append(":").append(NL);
        return codeBuilder.toString();
    }
    private String visitImportDecl(JmmNode node, Void unused) {
        StringBuilder codeBuilder = new StringBuilder();

        // Append each child's name with a dot if there are multiple children
        codeBuilder.append("import ");
        String importname = node.get("name");
        codeBuilder.append(((importname.substring(1, importname.length()-1)).replace(',','.').trim()).replaceAll("\\s+", ""));
        codeBuilder.append(';');
        codeBuilder.append(NL); // Append newline character

        return codeBuilder.toString();
    }


    private String visitSimpleStatement(JmmNode node, Void unused){
        //StringBuilder codeBuilder = new StringBuilder();
        return exprVisitor.visit(node.getJmmChild(0)).getComputation();
    }
    private String visitVarDecl(JmmNode node, Void unused) {
        //System.out.println("Entered Visit VarDecl");
        StringBuilder codeBuilder = new StringBuilder();
        //if (METHOD_DECL.check(node.getParent()) ){return "";}
        if (CLASS_DECL.check(node.getParent())) {
            codeBuilder.append(" public ");
        }
        if (METHOD_DECL.check(node.getParent())){
            return codeBuilder.toString();
        }
        String varName = node.get("name");
        boolean is_array= node.getJmmChild(0).getObject("isArray", Boolean.class);
        String ollirType = OptUtils.toOllirType(node.getJmmChild(0));
        codeBuilder.append(varName).append(ollirType).append(END_STMT);

        return codeBuilder.toString();
    }
    private String visitAssignStmt(JmmNode node, Void unused) {

        boolean indexed_expr = false;
        for(JmmNode node_  : node.getJmmChild(1).getDescendants()){
            if (node_.getKind().equals("IndexedExpr")){
                indexed_expr = true;
                break;
            }
        }
        //boolean skip= false ideia de skip a ser analisada para saltar o pedacço de código que pede o level;
        if(!node.getJmmChild(0).getKind().equals("IndexedExpr") && !indexed_expr && node.getJmmChild(1).getKind().equals("BinaryExpr") ) {
           if(node.getJmmChild(1).getJmmChild(1).getKind().equals("IntegerLiteral")){
            if(node.getJmmChild(0).get("name").equals(node.getJmmChild(1).getJmmChild(0).get("name")))
            {
                Type type = node.getJmmChild(0).getObject("type", Type.class);
                String ollirVarType = OptUtils.toOllirType(type);
                StringBuilder code = new StringBuilder();

                code.append(node.getJmmChild(0).get("name"));
                code.append(ollirVarType);
                code.append(SPACE);
                code.append(ASSIGN);
                code.append(SPACE);
                code.append(ollirVarType);
                code.append(SPACE);
                code.append(node.getJmmChild(1).getJmmChild(0).get("name")).append(ollirVarType);
                code.append(SPACE);
                code.append(node.getJmmChild(1).get("op"));
                code.append(SPACE);
                code.append(ollirVarType);
                code.append(SPACE);
                code.append(node.getJmmChild(1).getJmmChild(1).get("value"));
                code.append(ollirVarType);
                code.append(END_STMT);

                return code.toString();
            }
            }
            if(node.getJmmChild(1).getJmmChild(0).getKind().equals("IntegerLiteral")){
                if(node.getJmmChild(0).get("name").equals(node.getJmmChild(1).getJmmChild(1).get("name")))
                {
                    Type type = node.getJmmChild(0).getObject("type", Type.class);
                    String ollirVarType = OptUtils.toOllirType(type);
                    StringBuilder code = new StringBuilder();

                    code.append(node.getJmmChild(0).get("name"));
                    code.append(ollirVarType);
                    code.append(SPACE);
                    code.append(ASSIGN);
                    code.append(SPACE);
                    code.append(ollirVarType);
                    code.append(SPACE);
                    code.append(node.getJmmChild(1).getJmmChild(1).get("name")).append(ollirVarType);
                    code.append(SPACE);
                    code.append(node.getJmmChild(1).get("op"));
                    code.append(SPACE);
                    code.append(ollirVarType);
                    code.append(SPACE);
                    code.append(node.getJmmChild(1).getJmmChild(0).get("value"));
                    code.append(ollirVarType);
                    code.append(END_STMT);

                    return code.toString();
                }
            }
            }



        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();
        Type type = node.getJmmChild(0).getObject("type", Type.class);
        String ollirVarType = OptUtils.toOllirType(type);

        // code to compute the children
        //code.append(lhs.getComputation());
        code.append(rhs.getComputation());
        if(node.getJmmChild(0).getKind().equals("IndexedExpr")){

            String input = lhs.getIndexedCode();
            // Print the result
            code.append(lhs.getIndexedComputation());
            code.append(input);
            //code.append(ollirVarType+ node.getJmmChild(0).getJmmChild(0).get("name")+"["+)
            code.append(SPACE);
            code.append(ASSIGN);
            code.append(ollirVarType);
            code.append(SPACE);
            code.append(rhs.getCode());
            code.append(END_STMT);
            //skip    = true;
            return code.toString();
        }

        if (node.getJmmChild(0).getObject("type", Type.class).getObject("level", Integer.class) == 0) {

            code.append("putfield(this, " + node.getJmmChild(0).get("name") + ollirVarType + "," + rhs.getCode() + ").V;");
            code.append(NL);
            return code.toString();
        }

        // code to compute self
        // statement has type of lhs
        Type thisType = node.getJmmChild(0).getObject("type",Type.class);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        System.out.println("Entered Visit Return");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }
        if (retType.getName().equals("void")){
            code.append("ret.V;");
        }
        else {
            code.append(expr.getComputation());
            code.append("ret");
            code.append(OptUtils.toOllirType(retType));
            code.append(SPACE);

            code.append(expr.getCode());

            code.append(END_STMT);
        }
        return code.toString();
    }



    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");
        String code="";

        code = id + typeCode;

        return code;
    }



    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean hasVarargs = node.hasAttribute("hasVarargs");
        boolean hasreturn = false;
        if (isPublic) {
            code.append("public ");
        }
        if (hasVarargs) {
            code.append("varargs ");
        }
        if (node.getObject("isStatic", Boolean.class)) {
            code.append("static ");
        }
        var childCode="";
        // Method name
        var name = node.get("name");
        code.append(name);

        // Parameters
        StringBuilder paramCode = new StringBuilder();
        var paramNodes = node.getChildren("Param");
        for (int i = 0; i < paramNodes.size(); i++) {
            var paramNode = paramNodes.get(i);
            var paramCodeStr = visit(paramNode);
            paramCode.append(paramCodeStr);
            if (i < paramNodes.size() - 1) {
                paramCode.append(",");
            }
        }

        // Return type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append("(").append(paramCode).append(")").append(retType).append(L_BRACKET);
        int i=0;
        // Method body
        for (var child: node.getChildren()) {
            if (PARAM.check(child)){continue;}
            //if (VAR_DECL.check(child)){continue;}
            System.out.println(child.getKind());
            if (Objects.equals(child.getKind(), "ReturnStmt")){
                hasreturn=true;
            }
            childCode = visit(child);
            code.append(childCode);
            i++;
        }
        if(!hasreturn){code.append("ret.V;");}
        code.append(R_BRACKET).append(NL);

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        /*
        for (String importedName : table.getImports()) {

            //importedName = importedName.substring(1, importedName.length() - 1);
            code.append("import ").append(importedName).append(";");
            code.append(NL);
        }

         */
        code.append(table.getClassName());
        if(node.hasAttribute("superclass")){
            code.append(" extends " + node.getObject("superclass"));
        }
        code.append(L_BRACKET);

        code.append(NL);


        for (var child : node.getChildren()) {
            var result = visit(child);
            var needNl = true;
            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }
            if (VAR_DECL.check(child) && needNl){
                code.append(".field ");


            }
            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
