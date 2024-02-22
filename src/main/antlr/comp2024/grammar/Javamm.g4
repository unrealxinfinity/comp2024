grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

LENGTH: 'length';
NEW: 'new';
EQUALS: '=';
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
CLASS: 'class';
INT: 'int';
BOOLEAN:'bool';

PUBLIC : 'public';
STATIC: 'static';
VOID: 'void';
MAIN : 'main';
RETURN : 'return' ;
TRUE : 'true';
FALSE: 'false';
THIS : 'this';
AND: '&&';
LT: '<';
INTEGER : [0-9]+ ;
ID : [a-zA-Z]+INTEGER?[a-zA-Z]*INTEGER? ;
STRING : [a-zA-Z]*;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDecl EOF
    ;

importDeclaration : 'import' ID ( '.' ID )* ';' ;

classDecl
    : 'class' ID ('extends' ID)? '{' (varDecl)* (methodDecl)* '}'
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name=INT '['']'
    | name=INT '...'
    | name= INT
    | name = BOOLEAN
    | name= STRING LRECT RRECT
    | name= ID
    ;

methodDecl locals[boolean isPublic=false]
    : ('public' {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    | (PUBLIC)? STATIC VOID MAIN LPAREN 'String' LRECT RRECT ID RPAREN LCURLY ( varDecl)* ( stmt )* RCURLY

    ;

param
    : (type name=ID(',')?)*
    ;

stmt
    : LCURLY (stmt)* RCURLY
    |'if' LPAREN expr RPAREN (stmt)* 'else' (stmt)*
    | 'while' LPAREN expr RPAREN (stmt)*
    | expr ';'
    | ID '=' expr ';'
    | ID LRECT expr RRECT '=' expr ';'
    //| RETURN expr SEMI
    ;
expr
    : '(' expr ')' #ParensExpr
    | expr '[' expr ']' #IndexedExpr
    | expr '.' LENGTH #LengthExpr
    | expr '.' ID '(' (expr ( ',' expr )*)? ')' #Custom3Expr
    | expr (op= MUL | op=DIV)  expr #BinaryExpr //
    | expr (op= ADD | op=SUB) expr #BinaryExpr //
    | NOT expr #LogicalExpr
    | expr (op=LT) expr #LogicalExpr
    | expr (op=AND) expr #LogicalExpr
    //| expr OR expr #LogicalExpr
    | NEW INT '[' expr ']' #CustomExpr
    | NEW ID LPAREN RPAREN #Custom2Expr
    | '[' expr '(' ',' expr')''*' ')''?' ']' #Custom3Expr
    | value=INTEGER #IntegerLiteral //
    | value=TRUE #BooleanLiteral
    | value=FALSE #BooleanLiteral
    | name=ID #VarRefExpr //
    | 'this' #This
    ;



