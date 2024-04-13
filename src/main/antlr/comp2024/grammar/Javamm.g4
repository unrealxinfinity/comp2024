grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LENGTH: 'length';
EQUALS: '=';
DOT: '...';
SEMI : ';' ;
LRECT: '[';
RRECT: ']';
LCURLY: '{';
RCURLY: '}';
LPAREN: '(';
RPAREN: ')';
MUL: '*' ;
ADD: '+' ;
DIV: '/' ;
SUB: '-' ;
NOT: '!';
AND: '&&';
LT: '<';

PUBLIC : 'public';
STATIC: 'static';
CLASS: 'class';
VOID: 'void';
RETURN: 'return';
TRUE: 'true';
FALSE: 'false';
THIS : 'this';
IF: 'if';
ELSE:'else';
WHILE: 'while';
STR: 'String';
INT: 'int';
//STRINGARRAY: STR LRECT RRECT;
//INTVARARG:'int...';
BOOLEAN:'boolean';
NEW: 'new';

SINGLE_COMMENT : '//' .*? '\n' -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;

INTEGER : [0-9] | [1-9][0-9]+ ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]*  ;
STRING : [a-zA-Z]+;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl : 'import' ( name+=ID '.')* name+=ID ';' ;

classDecl
    : 'class' name=ID ('extends' superclass=ID)? '{'
            (varDecl)* (methodDecl)*
        '}'
    ;

varDecl
    : type name=ID SEMI
    ;

type locals[boolean isArray=false, boolean isVarargs=false]
    : name=INT ((LRECT RRECT {$isArray=true;}) | (DOT {$isVarargs=true;}))?
    | name=INT DOT
    | name= INT
    | name = BOOLEAN
    | name= STR LRECT RRECT {$isArray=true;}
    | name= ID
    | name= STR
    | name= VOID
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;})? type name=ID LPAREN (param)? (',' param)* RPAREN
        LCURLY
            varDecl* stmt*
        RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY (stmt)* RCURLY #EncvaloseStatement
    | IF LPAREN expr RPAREN
        stmt
      ELSE
        stmt #IfStatement
    | WHILE LPAREN expr RPAREN stmt #WhileStatement
    | expr SEMI #SimpleStatement
    | expr '=' expr SEMI #AssignmentStatement
    | expr LRECT expr RRECT '=' expr SEMI #ArrayAlterIndexStatement
    | RETURN expr SEMI #ReturnStmt
    ;
expr
    : '(' expr ')' #ParensExpr
    | expr '[' expr ']' #IndexedExpr
    | expr '.' LENGTH #LengthFunctionExpr
    | NEW name=ID LPAREN RPAREN #NewClassExpr
    | expr '.' name=ID LPAREN (expr ( ',' expr )*)? RPAREN #ClassFunctionCallExpr
    | name=ID LPAREN (expr ( ',' expr )*)? RPAREN #SameClassCallExpr
    | expr (op= MUL | op=DIV)  expr #BinaryExpr //
    | expr (op= ADD | op=SUB) expr #BinaryExpr //
    | NOT expr #LogicalExpr
    | expr (op=LT) expr #BinaryExpr
    | expr (op=AND) expr #BinaryExpr
    //| expr OR expr #LogicalExpr
    | NEW INT LRECT expr RRECT #NewArrayExpr
    | LRECT (expr ( ',' expr)* )? RRECT #ArrayExpr
    | value=INTEGER #IntegerLiteral
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefLiteral //
    | THIS #This
    ;



