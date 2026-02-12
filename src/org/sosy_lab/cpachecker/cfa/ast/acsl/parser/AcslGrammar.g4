// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// It is based on a project by Maxim Menshchikov cf. https://github.com/interpretica-io/acsl-grammar
// which is licensed under Creative Commons Attribution 4.0 International (CC BY 4.0) License
// cf. http://creativecommons.org/licenses/by/4.0/

grammar AcslGrammar;

import C;

// Edit to get rid of dependency upon ANTLR C++ target.
@parser::preinclude {
    #include "CParser.h"
}

// TODO: Missing Grammar for global logic definition, Figure 2.14 ACSL spec
// TODO: Missing support for types and so on Figure 2.4 ACSL spec
// Information:
// - The name of the rules is how the visitor rules will be named

id
    : Identifier
    ;

string
    : StringLiteral+
    ;



// term.tex
literal
    : '\\true'                                          # TrueConstant
    | '\\false'                                         # FalseConstant
    // let's use C classes
    | Constant                                          # CConstant
    | string                                            # StringConstant
    ;

binOp
    : '+' | '-' | '*' | '/' | '%' | '<<' | '>>'
    // The ACSL standard also uses the following operators
    // but these make ANTLR not work due to an ambiguous grammar
    // since it is unclear if the terms or the predicates should be matched
    // | '==' | '!=' | '<=' | '>=' | '>' | '<'
    | '&' | '|' | '-->' | '<-->' | '^'
    ;

unaryOp
    : '+'
    | '-'
    | '*'
    | '&'
    ;

termTernaryConditionBody
  : literal                                                     # LiteralTermTernaryConditionBody
  | ident                                                       # VariableTermTernaryConditionBody
  | unaryOp termTernaryConditionBody                            # UnaryOpTermTernaryConditionBody
  | termTernaryConditionBody binOp termTernaryConditionBody     # BinaryOpTermTernaryConditionBody
  | termTernaryConditionBody '[' termTernaryConditionBody ']'   # ArrayAccessTermTernaryConditionBody
  | termTernaryConditionBody '->' id                            # PointerStructureFieldAccessTermTernaryConditionBody
  | termTernaryConditionBody '.' id                             # StructureFieldAccessTermTernaryConditionBody
  | '(' typeExpr ')' termTernaryConditionBody                   # CastTermTernaryConditionBody
  | '(' termTernaryConditionBody ')'                            # ParenthesesTermTernaryConditionBody
  | '\\result'                                                  # ResultTermTernaryConditionBody
  | '\\old' '(' termTernaryConditionBody ')'                    # OldTermTernaryConditionBody
  | '\\at' '(' termTernaryConditionBody ',' labelId ')'        # AtTermTernaryConditionBody
  | ident '(' termTernaryConditionBody (',' termTernaryConditionBody)* ')'                              # FuncApplicationTermTernaryConditionBody
  ;

termPredTernaryCondition
  :'\\true'                                                             # LogicalTrueTermPredTernaryCondition
  | '\\false'                                                           # LogicalFalseTermPredTernaryCondition
  | termTernaryConditionBody (relOp termTernaryConditionBody)+          # ComparisonTermPredTernaryCondition
  | termPredTernaryCondition binaryPredOp termPredTernaryCondition      # BinaryTermPredTernaryCondition
  | '(' termPredTernaryCondition ')'                                    # ParenthesesTermPredTernaryCondition
  | unaryPredOp termPredTernaryCondition                                # UnaryTermPredTernaryCondition
  | '\\old' '(' termPredTernaryCondition ')'                            # OldTermPredTernaryCondition
  ;

term
    : literal                                           # LiteralTerm
    | ident                                             # VariableTerm
    | unaryOp term                                      # UnaryOpTerm
    | term binOp term                                   # BinaryOpTerm
    | term '[' term ']'                                 # ArrayAccessTerm
    | '{' term '\\with' '[' term ']' '=' term '}'       # ArrayFuncModifierTerm
    | term '.' id                                       # StructureFieldAccessTerm
    | '{' term '\\with' '.' id '=' term '}'             # FieldFuncModifierTerm
    | term '->' id                                      # PointerStructureFieldAccessTerm
    | '(' typeExpr ')' term                            # CastTerm
    | ident '(' term (',' term)* ')'                    # FuncApplicationTerm
    | '(' term ')'                                      # ParenthesesTerm
    // The ACSL standard allow a general predicate to be the condition here,
    // but since this would make the grammmar left recursive between terms and predicates,
    // we decided to use a more restricted grammar, with a lot of different rules,
    // since this is the only way to avoid being left recursive
    | termPredTernaryCondition '?' term ':' term                    # TernaryCondTerm
    | '\\let' id '=' term ';' term                      # LocalBindingTerm
    | 'sizeof' '(' term ')'                             # SizeofTerm
    | 'sizeof' '(' typeName ')'                         # SizeofTypeTerm
    | id ':' term                                       # SyntacticNamingTerm
    | string ':' term                                   # SyntacticNamingTerm
// oldandresult.tex
    | '\\old' '(' term ')'                              # OldTerm
    | '\\result'                                        # ResultTerm
// memory.tex:
    | '\\null'                                          # NullTerm
    | '\\base_addr' oneLabel? '(' term ')'             # BaseAddrTerm
    | '\\block_length' oneLabel? '(' term ')'          # BlockLengthTerm
    | '\\offset' oneLabel?  '(' term ')'               # OffsetTerm
    | '{' '\\allocation' '}' oneLabel?   '(' term ')'  # AllocationTerm
// exitbehavior.tex
    | '\\exit_status'                                   # ExitStatusTerm
// at.tex
    | '\\at' '(' term ',' labelId ')'                  # AtTerm
    ;

// predicate.tex
relOp
    : '==' | '!=' | '<=' | '>=' | '>' | '<'
    ;

binaryPredOp
    : '&&' | '||' | '^^' | '==>' | '<==>'
    ;

unaryPredOp
  : '!'
  ;

// Also called logic expressions in the ACSL standard.
// We will call them expressions in CPAchecker in order to
// match the AExpressions.
pred
    : '\\true'                          # LogicalTruePred
    | '\\false'                         # LogicalFalsePred
    | ident                             # PredicateVariable
    // Not really part of the ACSL spec, but ACSL has an implicit conversion from terms
    // to predicates, which works the same way as in C i.e.
    // 0 is false and everything else is true
    | term                                                          # PredicateTerm
    // We transform these into binary expressions when parsing
    | term (relOp term)+                # ComparisonPred
    | ident '(' term (',' term)* ')'    # PredicateApplicationPred
    | '(' pred ')'                      # ParenthesesPred
    | pred binaryPredOp pred            # BinaryPredicate
    | unaryPredOp pred                          # UnaryPred
    // IMO having a term on the left side is not correct,
    // since in general it must not return a boolean
    // value, but a predicate must do this
    // | term '?' pred ':' pred            # TernaryConditionTermPred
    | pred '?' pred ':' pred            # TernaryConditionPred
    | '\\let' id '=' term ';' pred      # LocalBindingPred
    | '\\let' id '=' pred ';' pred      # LocalBindingPred
    | '\\forall' binders ';' pred       # UniversalQuantificationPred
    | '\\exists' binders ';' pred       # ExistentialQuantificationPred
    | id ':' pred                       # SyntacticNamingPred
    | string ':' pred                   # SyntacticNamingPred
// oldandresult.tex
    | '\\old' '(' pred ')'              # oldPred
// loc.tex
    | '\\subset' '(' tset ',' tset ')'  # SetInclusionPred
    | term '\\in' tset                  # SetMembershipPred
// memory.tex:
    | '\\allocable' oneLabel? '(' term ')'                         # AllocablePred
    | '\\freeable' oneLabel? '(' term ')'                          # FreeablePred
    | '\\fresh'   twoLabels? '(' term ',' term ')'                 # FreshPred
    | '\\valid'  oneLabel?  '(' locationAddress ')'               # ValidPred
    | '\\initialized'  oneLabel?  '(' locationAddress ')'         # InitializedPred
    | '\\valid_read'  oneLabel? '(' locationAddress ')'           # ValidReadPred
    | '\\separated' '(' locationAddress ',' locationAddresses ')' # SeparatedPred
    ;

ident
    : id
    ;

// binders.tex
binders
    : binder (',' binder)*
    ;

binder
    : typeExpr variableIdent (',' variableIdent)*
    ;

typeVar
    : id
    ;

typeExpr
    : logicTypeExpr
    | typeName
    ;

logicTypeExpr
    : builtInLogicType
    | typeVar
    | '<' typeExpr (',' typeExpr)* '>'
    ;

builtInLogicType
    : 'boolean' | 'integer' | 'real'
    ;

variableIdent
    : id
    | '*' variableIdent
    | variableIdent '[]'
    | '(' variableIdent ')'
    ;

// fn_behavior.tex
functionContract
    : requiresClause* terminatesClause? decreasesClause? simpleClause* namedBehavior* completenessClause*
    ;

requiresClause
    : 'requires' pred ';'
    ;

terminatesClause
    : 'terminates' pred ';'
    ;

decreasesClause
    : 'decreases' term ('for' id)? ';'
    ;

simpleClause
    : assignsClause | ensuresClause
    | allocationClause | abruptClause
    ;

assignsClause
    : 'assigns' locations ';'
    ;

strings
    : string (',' string)*
    ;

locations
    : location (',' location)* | '\\nothing'
    ;

location
    : tset
    ;

ensuresClause
    : 'ensures' pred ';'
    ;

namedBehavior
    : 'behavior' id ':' behaviorBody
    ;

behaviorBody
    : assumesClause* requiresClause* simpleClause*
    ;

assumesClause
    : 'assumes' pred ';'
    ;

completenessClause
    : 'complete' 'behaviors' (id ',' (',' id)*)? ';'
    | 'disjoint' 'behaviors' (id ',' (',' id)*)? ';'
    ;

// loc.tex
// This represents a set of memory locations
tset
    : '\\empty'                         # TsetEmpty
    | tset '->' id                      # TsetPointerAccess
    | tset '.' id                       # TsetMemberAccess
    | '*' tset                          # TsetDeref
    | '&' tset                          # TsetAddr
    | tset '[' tset ']'                 # TsetArrayAccess
    | term? '..' term?                  # TsetRange
    | '\\union' ( tset (',' tset)* )    # TsetUnion
    | '\\inter' ( tset (',' tset)* )    # TsetIntersection
    | tset '+' tset                     # TsetPlus
    | '(' tset ')'                      # TsetParen
    | '{' tset '|' binders (':' pred)? '}'  # TsetBinders
    | '{' (tset (',' tset)*)? '}'           # TsetSet
    | term                                  # TsetTerm
    ;

cCompoundStatement
    : '{' declaration* statement* assertion+ '}'
    ;

cStatement
    : assertion cStatement
    ;

assertion
    : 'assert' pred ';'
    | 'for' id (',' id)* ':' 'assert' pred ';'
    ;


// allocation.tex
allocationClause
    : 'allocates' dynAllocationAddresses ';' # AllocatesClause
    | 'frees' dynAllocationAddresses ';'     # FreesClause
    ;

loopAllocation
    : 'loop' 'allocates' dynAllocationAddresses ';'
    | 'loop' 'frees'  dynAllocationAddresses ';'
    ;

dynAllocationAddresses
    : locationAddresses
    | '\\nothing'
    ;

// memory.tex
oneLabel
    : '{' labelId '}'
    ;

twoLabels
    : '{' labelId ',' labelId '}'
    ;

locationAddresses
    : locationAddress (',' locationAddress)*
    ;

locationAddress
    : tset
    ;

// exitbehaviour.tex
abruptClause
    : exitsClause
    ;

exitsClause
    : 'exits' pred ';'
    ;

abruptClauseStmt
    : breaksClause | continuesClause | returnsClause
    ;

breaksClause
    : 'breaks' pred ';'
    ;

continuesClause
    : 'continues' pred ';'
    ;

returnsClause
    : 'returns' pred ';'
    ;

// at.tex
labelId
    : 'Here' | 'Old' | 'Pre' | 'Post'
    | 'LoopEntry' | 'LoopCurrent' | 'Init'
    | id
    ;

// loops.tex
loopAnnot
    : loopClause* loopBehavior* loopVariant?
    ;

loopClause
    : loopInvariant | loopAssigns | loopAllocation
    ;

loopInvariant
    : 'loop' 'invariant' pred ';'
    ;

loopAssigns
    : 'loop' 'assigns' locations ';'
    ;

loopBehavior
    : 'for' id (',' id)* ':' loopClause+
    ;

loopVariant
    : 'loop' 'variant' term ';'
    | 'loop' 'variant' term 'for' id ';'
    ;

// st_contracts.tex
statementContract
    : ('for' id (',' id)* ':')? requiresClause* simpleClauseStmt* namedBehaviorStmt* completenessClause*
    ;

simpleClauseStmt
    : simpleClause | abruptClauseStmt
    ;

namedBehaviorStmt
    : 'behavior' id ':' behaviorBodyStmt
    ;

behaviorBodyStmt
    : assumesClause* requiresClause* simpleClauseStmt*
    ;

// logic.tex

logicDef
    : logicConstDef
    | logicFunctionDef
    | logicPredicateDef
    | lemmaDef
    ;

typeVarBinders
    : '<' typeVar (',' typeVar)* '>'
    ;

polyId
    : id typeVarBinders?  // polymorphic object identifier
    ;

logicConstDef
    : typeExpr polyId '=' term
    ;

logicFunctionDef
    : typeExpr polyId parameters '=' term
    ;

logicPredicateDef
    : polyId parameters? '=' pred
    ;


parameters
    : '(' parameter ( ',' parameter )* ')'
    ;

parameter
    : typeExpr id
    ;

lemmaDef
    : polyId ':' pred
    ;


acslStatement
    : assertion | ensuresClause | assignsClause | requiresClause | loopInvariant
    ;


acslComment
    :  assertion* | loopInvariant* | functionContract
    ;
