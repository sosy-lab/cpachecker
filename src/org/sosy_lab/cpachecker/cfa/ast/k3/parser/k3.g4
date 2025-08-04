// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

grammar k3;

import SMTLIBv2;

script 
    : command+
    ;

command
    : '(' 'declare-var' symbol sort ')'             # DeclareVar
    | '(' 
          'define-proc' symbol 
            '(' (symbol sort)* ')' 
            '(' (symbol sort)* ')' 
            '(' (symbol sort)* ')' 
                statement 
      ')'                                           # DefineProc
    | '(' 'annotate-tag' symbol attribute+ ')'      # AnnotateTag 
    | '(' 'select-trace' trace ')'                  # SelectTrace
    | '(' 'verify-call' symbol '(' term* ')' ')'    # VerifyCall
    | '(' 'get-proof' ')'                           # GetProof             
    | '(' 'get-counterexample' ')'                  # GetCounterexample 
    ;

statement
    : '(' 'assume' term ')'                         # AssumeStatement
    | '(' 'assign' '(' (symbol term)+ ')' ')'       # AssignStatement
    | '(' 'sequence' statement+ ')'                 # SequenceStatement
    | '(' '!' statement attribute+ ')'              # AnnotatedStatement          
    | '(' 'call' 
            symbol 
            '(' term* ')' 
            '(' symbol* ')' 
        ')'                                         # CallStatement
    | '(' 'return' ')'                              # ReturnStatement               
    | '(' 'label' symbol ')'                        # LabelStatement  
    | '(' 'goto' symbol ')'                         # GotoStatement
    | '(' 'if' term statement statement? ')'        # IfStatement
    | '(' 'while' term statement ')'                # WhileStatement
    | '(' 'break' ')'                               # BreakStatement
    | '(' 'continue' ')'                            # ContinueStatement 
    | '(' 'havoc' '(' symbol+ ')' ')'               # HavocStatement
    | '(' 'choice' '(' statement+ ')' ')'           # ChoiceStatement
    ;

trace
    :   ('(' 'global' symbol value  ')')*
        ('(' 'call' symbol (value)* ')')
        (step)*
        ('(' 'incorrect-tag' symbol attribute+ ')')
    ;

step
    : '(' 'local' '(' (value)* ')' ')'              # ChooseLocalVariableValue
    | '(' 'havoc' '(' (value)* ')' ')'              # ChooseHavocVariableValue
    | '(' 'choice' NonNegativeInteger ')'           # ChooseChoiceStatement
    ;

identifier
    : Nondigit (Nondigit | Digit | '-')*
    ;

symbol
    : identifier
    ;

value
    : Integer
    ;

attribute
    : ':tag' identifier
    | property
    ;

// TODO: This currently does not support the LTL tag
property
    : ':assert' relationalTerm
    | ':live'
    | ':not-live'
    | ':ghost' '(' symbol+ ')'
    | ':requires' term
    | ':ensures' relationalTerm
    | ':invariant' relationalTerm
    | ':decreases' term
    | ':decreases' '(' term+ ')'
    | ':modifies' '(' symbol+ ')'
    ;

relationalTerm
    : term 
    | '(' 'old' symbol ')'
    ;

Nondigit
    :  [a-zA-Z_]
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

Digit
    : [0-9]
    ;

NonZeroDigit
    : [1-9]
    ;