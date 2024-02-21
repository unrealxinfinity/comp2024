grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;
DIV : '/' ;
MINUS: '-';
IF : 'if' ;
ELSE: 'else';
WHILE: 'while';


CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

INTEGER : [0-9] ;
ID : [a-zA-Z]+INTEGER*[a-zA-Z]*INTEGER* ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;
importDecl 
    : ‘import’ ID ( ‘.’ ID )* SEMI

classDecl
    : 'class' name=ID ('extends' ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY 
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= 'int' 
    | name= 'int...'
    | name= 'boolean'
    | name= 'int[]'
    | name= ID
    ;

methodDecl locals[boolean isPublic=false]
    : ('public' {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : (type name=ID(',')?)*
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt
    | IF LPAREN expr RPAREN 
        LCURLY 
            stmt 
        RCURLY 
      (ELSE
        LCURLY
            stmt
        RCURLY)? #IfStmt  
    | WHILE LPAREN expr RPAREN
        LCURLY
            stmt
        RCURLY #WhileStmt
    | expr SEMI
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : expr op=(MUL | DIV) expr #BinaryExpr 
    | expr op=(ADD | MINUS) expr #BinaryExpr 
    | value=INTEGER #IntegerLiteral 
    | name=ID #VarRefExpr 
    ;



