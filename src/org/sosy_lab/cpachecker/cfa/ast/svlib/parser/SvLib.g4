// This file is part of SV-LIB: A Standard Exchange Format for Software-Verification Tasks
// https://gitlab.com/sosy-lab/benchmarking/sv-lib
//
// SPDX-FileCopyrightText: 2025 The SV-LIB Maintainers
//
// SPDX-License-Identifier: Apache-2.0

grammar SvLib;

import SMTLIBv2;

// Parser Rules

script
    : commandSvLib+
    ;

commandSvLib
    : ParOpen 'declare-var' symbol sort ParClose                      # DeclareVar
    | ParOpen
          'define-proc' symbol
            ParOpen procDeclarationArguments ParClose
            ParOpen procDeclarationArguments ParClose
            ParOpen procDeclarationArguments ParClose
                statement
      ParClose                                                        # DefineProc
    | ParOpen 'annotate-tag' symbol attributeSvLib+ ParClose               # AnnotateTag
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
    | ParOpen GRW_Exclamation statement attributeSvLib+ ParClose           # AnnotatedStatement
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
    | ParOpen 'havoc' symbol+ ParClose                                # HavocStatement
    | ParOpen 'choice' ParOpen statement+ ParClose ParClose           # ChoiceStatement
    ;

attributeSvLib
    : ':tag' symbol                                                   # TagAttribute
    | property                                                        # TagProperty
    ;

// TODO: This currently does not support the LTL tag
property
    : ':check-true' relationalTerm                                    # CheckTrueProperty
    | ':recurring'                                                    # RecurringProperty
    | ':not-recurring'                                                # NotRecurringProperty
    | ':requires' term                                                # RequiresProperty
    | ':ensures' relationalTerm                                       # EnsuresProperty
    | ':invariant' relationalTerm                                     # InvariantProperty
    | ':decreases' term                                               # DecreasesProperty
    | ':decreases-lex' ParOpen term+ ParClose                         # DecreasesLexProperty
    ;


trace
    :   ParOpen 'model' model_response* ParClose
        ParOpen 'init-global-vars' (ParOpen symbol term ParClose)*  ParClose
        (ParOpen 'entry-proc' symbol ParClose)
        ParOpen 'steps' (step)* ParClose
        violatedProperty
        (ParOpen 'using-annotation' symbol attributeSvLib+ ParClose)*
    ;

violatedProperty
    : ParOpen 'incorrect-annotation' symbol attributeSvLib+ ParClose
    | ParOpen 'invalid-step' step ParClose
    ;


step
    : ParOpen 'init-proc-vars' symbol
        (ParOpen symbol spec_constant ParClose)* ParClose             # ChooseLocalVariableValue
    | ParOpen 'havoc'
        (ParOpen symbol spec_constant ParClose)* ParClose             # ChooseHavocVariableValue
    | ParOpen 'choice' Numeral ParClose                               # ChooseChoiceStatement
    | ParOpen 'leap' symbol
        (ParOpen symbol spec_constant ParClose)* ParClose             # LeapStep
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