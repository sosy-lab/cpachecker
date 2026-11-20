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
    : commandSvLib+ EOF
    ;

witnessFile: (witness)* EOF ;

witness: correctnessWitness | violationWitness ;

metadata: (ParOpen cmd_setInfo ParClose)* ;

correctnessWitness
    : ParOpen metadata command* annotateTagCommand* ParClose
    ;

violationWitness
  : ParOpen metadata selectTraceCommand* ParClose
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
    | ParOpen
          'define-procs-rec'
            procDeclaration+
            (ParOpen statement ParClose)+
      ParClose                                                        # DefineProcsRec
    | annotateTagCommand                                              # AnnotateTag
    | selectTraceCommand                                              # SelectTrace
    | ParOpen 'verify-call' symbol
            ParOpen term* ParClose
      ParClose                                                        # VerifyCall
    | ParOpen 'get-witness' ParClose                                  # GetWitness
    | command                                                         # SMTLIBv2Command
    ;

annotateTagCommand
    : ParOpen 'annotate-tag' symbol attributeSvLib+ ParClose
    ;

selectTraceCommand
    : ParOpen 'select-trace' trace ParClose
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
    | ParOpen 'choice' statement+ ParClose                            # ChoiceStatement
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


traceVariableAssignment
  : ParOpen symbol traceValue ParClose
  ;

// a value as SMT-LIB solvers write it in a model, arrays as constant arrays with stores,
// and a symbol for a Boolean, a rounding mode or a constructor of a datatype
traceValue
  : spec_constant
  | symbol
  | ParOpen PS_Minus (numeral | decimal) ParClose
  | realFraction
  | ParOpen PS_Minus realFraction ParClose
  | ParOpen GRW_Underscore symbol numeral ParClose
  | ParOpen GRW_Underscore symbol numeral numeral ParClose
  | ParOpen PS_Fp traceValue traceValue traceValue ParClose
  | ParOpen ParOpen GRW_As PS_Const sort ParClose traceValue ParClose
  | ParOpen PS_Store traceValue traceValue traceValue ParClose
  ;

// a fraction of numerals which are reals, or which to_real converts where numerals are integers
realFraction
  : ParOpen PS_Divide (numeral | ParOpen PS_Minus numeral ParClose) numeral ParClose
  | ParOpen PS_Divide (realNumeral | ParOpen PS_Minus realNumeral ParClose) realNumeral ParClose
  ;

realNumeral
  : ParOpen PS_ToReal numeral ParClose
  ;

globalVariableTraceAssignments
  : ParOpen 'init-global-vars' (traceVariableAssignment)*  ParClose
  ;

modelReponseTrace
  : ParOpen 'model' model_response* ParClose
  ;

entryProcedureTrace
  : ParOpen 'entry-proc' symbol ParClose
  ;

usingAnnotationsTrace
  : ParOpen 'using-annotation' symbol attributeSvLib+ ParClose
  ;

trace
  : modelReponseTrace
    globalVariableTraceAssignments
    entryProcedureTrace
    ParOpen 'steps' (step)* ParClose
    violatedProperty
    (usingAnnotationsTrace)*
  ;

violatedProperty
    : ParOpen 'incorrect-annotation' symbol attributeSvLib+ ParClose
    | ParOpen 'correct-annotation' symbol attributeSvLib+ ParClose
    | ParOpen 'invalid-step' step ParClose
    ;


step
    : ParOpen 'init-proc-vars' symbol
        (traceVariableAssignment)* ParClose             # ChooseLocalVariableValue
    | ParOpen 'havoc'
        (traceVariableAssignment)* ParClose             # ChooseHavocVariableValue
    | ParOpen 'choice' Numeral ParClose                 # ChooseChoiceStatement
    | ParOpen 'leap' symbol
        (traceVariableAssignment)* ParClose             # LeapStep
    ;

relationalTerm
    : term                                                            # NormalRelationalTerm
    | ParOpen 'old' term ParClose                                     # OldRelationalTerm
    | ParOpen qual_identifer term+ ParClose                           # ApplicationRelationalTerm
    // TODO: We need to handle the other constructors of terms here as well
    ;

procDeclaration
    : ParOpen symbol
          ParOpen procDeclarationArguments ParClose
          ParOpen procDeclarationArguments ParClose
          ParOpen procDeclarationArguments ParClose
      ParClose
    ;

procDeclarationArguments
    : (ParOpen symbol sort ParClose)*
    ;