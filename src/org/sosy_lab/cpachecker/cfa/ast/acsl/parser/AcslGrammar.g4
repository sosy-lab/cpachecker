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
  | '\\at' '(' termTernaryConditionBody ',' label_id ')'        # AtTermTernaryConditionBody
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
    | '\\base_addr' one_label? '(' term ')'             # BaseAddrTerm
    | '\\block_length' one_label? '(' term ')'          # BlockLengthTerm
    | '\\offset' one_label?  '(' term ')'               # OffsetTerm
    | '{' '\\allocation' '}' one_label?   '(' term ')'  # AllocationTerm
// exitbehavior.tex
    | '\\exit_status'                                   # ExitStatusTerm
// at.tex
    | '\\at' '(' term ',' label_id ')'                  # AtTerm
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
    | '\\allocable' one_label? '(' term ')'                         # AllocablePred
    | '\\freeable' one_label? '(' term ')'                          # FreeablePred
    | '\\fresh'   two_labels? '(' term ',' term ')'                 # FreshPred
    | '\\valid'  one_label?  '(' location_address ')'               # ValidPred
    | '\\initialized'  one_label?  '(' location_address ')'         # InitializedPred
    | '\\valid_read'  one_label? '(' location_address ')'           # ValidReadPred
    | '\\separated' '(' location_address ',' location_addresses ')' # SeparatedPred
    ;

ident
    : id
    ;

// binders.tex
binders
    : binder (',' binder)*
    ;

binder
    : typeExpr variable_ident (',' variable_ident)*
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

variable_ident
    : id
    | '*' variable_ident
    | variable_ident '[]'
    | '(' variable_ident ')'
    ;

// fn_behavior.tex
function_contract
    : requires_clause* terminates_clause? decreases_clause? simple_clause* named_behavior* completeness_clause*
    ;

requires_clause
    : 'requires' pred ';'
    ;

terminates_clause
    : 'terminates' pred ';'
    ;

decreases_clause
    : 'decreases' term ('for' id)? ';'
    ;

simple_clause
    : assigns_clause | ensures_clause
    | allocation_clause | abrupt_clause
    ;

assigns_clause
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

ensures_clause
    : 'ensures' pred ';'
    ;

named_behavior
    : 'behavior' id ':' behavior_body
    ;

behavior_body
    : assumes_clause* requires_clause* simple_clause*
    ;

assumes_clause
    : 'assumes' pred ';'
    ;

completeness_clause
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

c_compound_statement
    : '{' declaration* statement* assertion+ '}'
    ;

c_statement
    : assertion c_statement
    ;

assertion
    : '/*@' 'assert' pred ';' '*/'
    | '/*@' 'for' id (',' id)* ':' 'assert' pred ';' '*/'
    ;


// allocation.tex
allocation_clause
    : 'allocates' dyn_allocation_addresses ';' # AllocatesClause
    | 'frees' dyn_allocation_addresses ';'     # FreesClause
    ;

loop_allocation
    : 'loop' 'allocates' dyn_allocation_addresses ';'
    | 'loop' 'frees'  dyn_allocation_addresses ';'
    ;

dyn_allocation_addresses
    : location_addresses
    | '\\nothing'
    ;

// memory.tex
one_label
    : '{' label_id '}'
    ;

two_labels
    : '{' label_id ',' label_id '}'
    ;

location_addresses
    : location_address (',' location_address)*
    ;

location_address
    : tset
    ;

// exitbehaviour.tex
abrupt_clause
    : exits_clause
    ;

exits_clause
    : 'exits' pred ';'
    ;

abrupt_clause_stmt
    : breaks_clause | continues_clause | returns_clause
    ;

breaks_clause
    : 'breaks' pred ';'
    ;

continues_clause
    : 'continues' pred ';'
    ;

returns_clause
    : 'returns' pred ';'
    ;

// at.tex
label_id
    : 'Here' | 'Old' | 'Pre' | 'Post'
    | 'LoopEntry' | 'LoopCurrent' | 'Init'
    | id
    ;

// loops.tex
loop_annot
    : loop_clause* loop_behavior* loop_variant?
    ;

loop_clause
    : loop_invariant | loop_assigns | loop_allocation
    ;

loop_invariant
    : 'loop' 'invariant' pred ';'
    ;

loop_assigns
    : 'loop' 'assigns' locations ';'
    ;

loop_behavior
    : 'for' id (',' id)* ':' loop_clause+
    ;

loop_variant
    : 'loop' 'variant' term ';'
    | 'loop' 'variant' term 'for' id ';'
    ;

// st_contracts.tex
statement_contract
    : ('for' id (',' id)* ':')? requires_clause* simple_clause_stmt* named_behavior_stmt* completeness_clause*
    ;

simple_clause_stmt
    : simple_clause | abrupt_clause_stmt
    ;

named_behavior_stmt
    : 'behavior' id ':' behavior_body_stmt
    ;

behavior_body_stmt
    : assumes_clause* requires_clause* simple_clause_stmt*
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
