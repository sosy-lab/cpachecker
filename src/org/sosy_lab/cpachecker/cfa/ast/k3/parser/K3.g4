// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

grammar K3;

import SMTLIBv2;

// Parser Rules

script
    : commandk3+
    ;

commandk3
    : ParOpen 'declare-var' symbol sort ParClose                      # DeclareVar
    | ParOpen
          'define-proc' symbol
            ParOpen procDeclarationArguments ParClose
            ParOpen procDeclarationArguments ParClose
            ParOpen procDeclarationArguments ParClose
                statement
      ParClose                                                        # DefineProc
    | ParOpen 'annotate-tag' symbol attribute+ ParClose               # AnnotateTag
    | ParOpen 'select-trace' trace ParClose                           # SelectTrace
    | ParOpen 'verify-call' symbol
            ParOpen term* ParClose
      ParClose                                                        # VerifyCall
    | ParOpen 'get-witness' ParClose                                  # GetWitness
    | command                                                         # SMTLIBv2Command
    ;

statement
    : ParOpen 'assume' term ParClose                                  # AssumeStatement
    | ParOpen 'assign'
            (ParOpen symbol term ParClose)+
      ParClose                                                        # AssignStatement
    | ParOpen 'sequence' statement* ParClose                          # SequenceStatement
    | ParOpen GRW_Exclamation statement attribute+ ParClose           # AnnotatedStatement
    | ParOpen 'call'
            symbol
            ParOpen term* ParClose
            ParOpen symbol* ParClose
        ParClose                                                      # CallStatement
    | ParOpen 'return' ParClose                                       # ReturnStatement
    | ParOpen 'label' symbol ParClose                                 # LabelStatement
    | ParOpen 'goto' symbol ParClose                                  # GotoStatement
    | ParOpen 'if' term statement statement? ParClose                 # IfStatement
    | ParOpen 'while' term statement ParClose                         # WhileStatement
    | ParOpen 'break' ParClose                                        # BreakStatement
    | ParOpen 'continue' ParClose                                     # ContinueStatement
    | ParOpen 'havoc' ParOpen symbol+ ParClose ParClose               # HavocStatement
    | ParOpen 'choice' ParOpen statement+ ParClose ParClose           # ChoiceStatement
    ;

attribute
    : ':tag' symbol                                                   # TagAttribute
    | property                                                        # TagProperty
    ;

// TODO: This currently does not support the LTL tag
property
    : ':assert' relationalTerm                                        # AssertProperty
    | ':live'                                                         # LiveProperty
    | ':not-live'                                                     # NotLiveProperty
    | ':ghost' ParOpen symbol+ ParClose                               # GhostProperty
    | ':requires' term                                                # RequiresProperty
    | ':ensures' relationalTerm                                       # EnsuresProperty
    | ':invariant' relationalTerm                                     # InvariantProperty
    | ':decreases' term                                               # DecreasesProperty
    | ':decreases' ParOpen term+ ParClose                             # DecreasesProperty
    | ':modifies' ParOpen symbol+ ParClose                            # ModifiesProperty
    ;


trace
    :   (ParOpen 'global' symbol term  ParClose)*
        (ParOpen 'call' symbol (term)* ParClose)
        (step)*
        (ParOpen 'incorrect-tag' symbol attribute+ ParClose)
    ;

step
    : ParOpen 'local' ParOpen (term)* ParClose ParClose               # ChooseLocalVariableValue
    | ParOpen 'havoc' ParOpen (term)* ParClose ParClose               # ChooseHavocVariableValue
    | ParOpen 'choice' Numeral ParClose                               # ChooseChoiceStatement
    ;

relationalTerm
    : term                                                            # NormalRelationalTerm
    | ParOpen 'old' term ParClose                                     # OldRelationalTerm
    | ParOpen qual_identifer term+ ParClose                           # ApplicationRelationalTerm
    // TODO: We need to handle the other constructors of terms here as well
    ;

procDeclarationArguments
    : (ParOpen symbol sort ParClose)*
    ;