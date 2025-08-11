// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

grammar K3;

// import SMTLIBv2;

// Lexer Rules
LPAREN: '(' ;
RPAREN: ')' ;

IDENT: [a-zA-Z_] [a-zA-Z_0-9]* ;

SYM : '+'
    | '='
    | '/'
    | '*'
    | '%'
    | '?'
    | '!'
    | '$'
    | '-'
    | '_'
    | '~'
    | '&'
    | '^'
    | '<'
    | '>'
    | '@'
    | '.'
    ;

WS: [ \t\r\n]+ -> skip ;

Semicolon: ';' ;

Comment
    : Semicolon ~[\r\n]* -> skip
    ;

Nondigit
    : 'a' ..'z'
    | 'A' .. 'Z'
    ;

Integer
    : NonNegativeInteger
    | NegativeInteger
    ;

NonNegativeInteger
    : NonZeroDigit (Digit)*
    | [0]
    ;

NegativeInteger
    : '-' NonZeroDigit (Digit)*
    ;

fragment Digit
    : [0-9]
    ;

fragment NonZeroDigit
    : [1-9]
    ;

variable: IDENT;

sort: IDENT;

tagName: IDENT;

procedureName: IDENT;

label: IDENT;

term
    : LPAREN (SYM | IDENT) term* RPAREN                         # ApplicationTerm
    | variable                                                  # VariableTerm
    ;

statement
    : LPAREN 'assume' term RPAREN                               # AssumeStatement
    | LPAREN 'assign' LPAREN (LPAREN IDENT term RPAREN)+ RPAREN RPAREN        # AssignStatement
    | LPAREN 'sequence' statement+ RPAREN                       # SequenceStatement
    | LPAREN '!' statement attribute+ RPAREN                    # AnnotatedStatement
    | LPAREN 'call'
            procedureName
            LPAREN term* RPAREN
            LPAREN variable* RPAREN
        RPAREN                                                  # CallStatement
    | LPAREN 'return' RPAREN                                    # ReturnStatement
    | LPAREN 'label' label RPAREN                               # LabelStatement
    | LPAREN 'goto' label RPAREN                                # GotoStatement
    | LPAREN 'if' term statement statement? RPAREN              # IfStatement
    | LPAREN 'while' term statement RPAREN                      # WhileStatement
    | LPAREN 'break' RPAREN                                     # BreakStatement
    | LPAREN 'continue' RPAREN                                  # ContinueStatement
    | LPAREN 'havoc' LPAREN variable+ RPAREN RPAREN              # HavocStatement
    | LPAREN 'choice' LPAREN statement+ RPAREN RPAREN           # ChoiceStatement
    ;

trace
    :   (LPAREN 'global' variable value  RPAREN)*
        (LPAREN 'call' variable (value)* RPAREN)
        (step)*
        (LPAREN 'incorrect-tag' tagName attribute+ RPAREN)
    ;

step
    : LPAREN 'local' LPAREN (value)* RPAREN RPAREN              # ChooseLocalVariableValue
    | LPAREN 'havoc' LPAREN (value)* RPAREN RPAREN              # ChooseHavocVariableValue
    | LPAREN 'choice' NonNegativeInteger RPAREN                 # ChooseChoiceStatement
    ;

value
    : Integer
    ;

attribute
    : ':tag' tagName                                            # TagAttribute
    | property                                                  # TagProperty
    ;

// TODO: This currently does not support the LTL tag
property
    : ':assert' relationalTerm                                  # AssertProperty
    | ':live'                                                   # LiveProperty
    | ':not-live'                                               # NotLiveProperty
    | ':ghost' LPAREN variable+ RPAREN                          # GhostProperty
    | ':requires' term                                          # RequiresTerm
    | ':ensures' relationalTerm                                 # EnsuresTerm
    | ':invariant' relationalTerm                               # InvariantTerm
    | ':decreases' term                                         # DecreasesTerm
    | ':decreases' LPAREN term+ RPAREN                          # DecreasesProperty
    | ':modifies' LPAREN variable+ RPAREN                       # ModifiesProperty
    ;

relationalTerm
    : term
    | LPAREN 'old' variable RPAREN
    ;

procDeclarationArguments
    : (LPAREN variable sort RPAREN)*
    ;

command
    : LPAREN 'declare-var' variable sort RPAREN                 # DeclareVar
    | LPAREN
          'define-proc' procedureName
            LPAREN procDeclarationArguments RPAREN
            LPAREN procDeclarationArguments RPAREN
            LPAREN procDeclarationArguments RPAREN
                statement
      RPAREN                                                    # DefineProc
    | LPAREN 'annotate-tag' tagName attribute+ RPAREN           # AnnotateTag
    | LPAREN 'select-trace' trace RPAREN                        # SelectTrace
    | LPAREN 'verify-call' procedureName
            LPAREN term* RPAREN
      RPAREN                                                    # VerifyCall
    | LPAREN 'get-proof' RPAREN                                 # GetProof
    | LPAREN 'get-counterexample' RPAREN                        # GetCounterexample
    | Comment                                                   # Comment
    ;


script
    : command+
    ;