package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOL_TYPE_NAME= "boolean";
    private static final String INT_VAR_ARG_TYPE_NAME = "int...";
    private static final String INT_ARRAY_TYPE_NAME = "int[]";
    private static final String STRING_ARRAY_TYPE_NAME = "String[]";
    private static final String STRING_TYPE_NAME ="String";
    private static final String VOID_TYPE_NAME = "void";
    public static String getIntTypeName() {return INT_TYPE_NAME;}
    public static String getBoolTypeName(){ return BOOL_TYPE_NAME;}
    public static String getIntVarArgTypeName() {return INT_VAR_ARG_TYPE_NAME;}
    public static String getIntArrayTypeName(){return INT_ARRAY_TYPE_NAME;}
    public static String getStringArrayTypeName(){return  STRING_ARRAY_TYPE_NAME;}
    public static String getStringTypeName(){return STRING_TYPE_NAME;}
    public static String getVoidTypeName(){return  VOID_TYPE_NAME;}

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded ? idk it seems complete

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_LITERAL -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOL_TYPE_NAME,false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*","-","/"-> new Type(INT_TYPE_NAME, false);
            case "<","&&" ->new Type(BOOL_TYPE_NAME,false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        List<Symbol> fields = table.getFields();
        return fields.stream().filter(field -> field.getName().equals(varRefExpr.get("name"))).map(
                field -> field.getType()
        ).toList().get(0);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
