parser grammar CTAGrammarParser;

options {
  tokenVocab = CTALexer;
  language = Java;
}

// PARSER
specification
	: moduleSpecification+
	;

moduleSpecification
	: MODULE ROOT? IDENTIFIER LBRACKET variableDeclarationGroup* initialConfigDefinition? moduleInstantiation* automatonDefinition? RBRACKET
	;

variableDeclarationGroup
	: variableVisibilityQualifier variableDeclaration+
	;

variableVisibilityQualifier
	: LOCAL
	| INPUT
	| OUTPUT
	| MULTREST
	;

variableDeclaration
	: name=IDENTIFIER COLON type=variableType SEMICOLON
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
	: var=IDENTIFIER op=operator constant=NUMBER # BinaryVariableExpression
	;

operator
	: EQUAL
	| LESS
	| GREATER
	| GREATEREQUAL
	| LESSEQUAL
	;

moduleInstantiation
	: INST IDENTIFIER FROM IDENTIFIER WITH LBRACKET variableInstantiation* RBRACKET
	;

variableInstantiation
	: IDENTIFIER AS IDENTIFIER SEMICOLON
	;

automatonDefinition
	: AUTOMATON IDENTIFIER LBRACKET stateDefinition* RBRACKET
	;

stateDefinition
	: STATE name=IDENTIFIER LBRACKET invariantDefinition? derivationDefinition? transitionDefinition* RBRACKET
	;

invariantDefinition
	: INV variableCondition SEMICOLON
	; 
	
derivationDefinition
	: DERIV derivationCondition (AND derivationCondition)* SEMICOLON
	; 

derivationCondition
	: DER LPAREN IDENTIFIER RPAREN operator NUMBER
	;
	
transitionDefinition
	: TRANS LBRACKET  (guardDefinition SEMICOLON)? (syncDefinition SEMICOLON)? (resetDefinition SEMICOLON)? (gotoDefinition SEMICOLON)? RBRACKET
	;

guardDefinition
	: GUARD variableCondition
	;

syncDefinition
	: SYNC variableVisibilityPrefix IDENTIFIER
	;

variableVisibilityPrefix
	: QUESTIONMARK
	| EXCLAMATIONMARK
	| HASH
	;

resetDefinition
	: RESET vars+=IDENTIFIER (COMMA IDENTIFIER)*
	;

gotoDefinition
	: GOTO state=IDENTIFIER
	;