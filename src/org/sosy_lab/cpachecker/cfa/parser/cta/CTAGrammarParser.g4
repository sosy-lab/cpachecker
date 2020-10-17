// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

parser grammar CTAGrammarParser;

options {
  tokenVocab = CTALexer;
  language = Java;
}

// PARSER
specification
	: modules+=moduleSpecification+
	;

moduleSpecification
	: MODULE ROOT? name=IDENTIFIER LBRACKET variables+=variableDeclarationGroup* instantiations+=moduleInstantiation* initialCondition=initialConfigDefinition? automaton=automatonDefinition? RBRACKET
	;

variableDeclarationGroup
	: visibility=variableVisibilityQualifier declarations+=variableDeclaration+
	;

variableVisibilityQualifier
	: LOCAL
	| INPUT
	| OUTPUT
	| MULTREST
	;

variableDeclaration
	: name=IDENTIFIER (EQUAL initialization=NUMBER)? COLON type=variableType SEMICOLON
	;

variableType
	: SYNC 
	| ANALOG
	| STOPWATCH
	| CLOCK
	| DISCRETE
	| CONST
	;

initialConfigDefinition
	: INITIAL stateNames+=IDENTIFIER (COMMA stateNames+=IDENTIFIER)* SEMICOLON
	;

stateCondition
	: STATE LPAREN IDENTIFIER RPAREN EQUAL IDENTIFIER
	;

variableCondition
	: expressions+=variableExpression (AND expressions+=variableExpression)*
	;

variableExpression
	: var=IDENTIFIER op=operator constant=NUMBER #NumericVariableExpression
  | var=IDENTIFIER op=operator constant=IDENTIFIER #ParametricVariableExpression
	;

operator
	: EQUAL
	| LESS
	| GREATER
	| GREATEREQUAL
	| LESSEQUAL
	;

moduleInstantiation
	: INST instanceName=IDENTIFIER FROM specificationName=IDENTIFIER WITH LBRACKET variableInstantiations+=variableInstantiation* RBRACKET
	;

variableInstantiation
	: specName=IDENTIFIER AS instanceName=IDENTIFIER SEMICOLON
	;

automatonDefinition
	: AUTOMATON name=IDENTIFIER LBRACKET states+=stateDefinition* RBRACKET
	;

stateDefinition
	: STATE name=IDENTIFIER (LPAREN TARGET RPAREN)? LBRACKET invariant=invariantDefinition? transitions+=transitionDefinition* RBRACKET
	;

invariantDefinition
	: INV condition=variableCondition SEMICOLON
	; 
	
derivationDefinition
	: DERIV derivationCondition (AND derivationCondition)* SEMICOLON
	; 

derivationCondition
	: DER LPAREN IDENTIFIER RPAREN operator NUMBER
	;
	
transitionDefinition
	: TRANS LBRACKET  (guard=guardDefinition SEMICOLON)? (SYNC syncMark=IDENTIFIER SEMICOLON)? (resetDefinition SEMICOLON)? gotoDefinition SEMICOLON RBRACKET
	;

guardDefinition
	: GUARD variableCondition
	;

resetDefinition
	: RESET vars+=IDENTIFIER (COMMA vars+=IDENTIFIER)*
	;

gotoDefinition
	: GOTO state=IDENTIFIER
	;