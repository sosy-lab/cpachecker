/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */

package org.sosy_lab.cpachecker.fshell.fql2.parser;

import java_cup.runtime.*;
import java.io.IOException;

import static org.sosy_lab.cpachecker.fshell.fql2.parser.FQLSym.*;

@javax.annotation.Generated("JFlex")
@SuppressWarnings("all")
%%

%class FQLLexer

%unicode
%line
%column

// %public
%final
// %abstract

%cupsym org.sosy_lab.cpachecker.fshell.fql2.parser.FQLSym
%cup
// %cupdebug

%init{
	// TODO: code that goes to constructor
%init}


%{
	private Symbol sym(int type)
	{
		return sym(type, yytext());
	}

	private Symbol sym(int type, Object value)
	{
		//System.out.println(value);

		return new Symbol(type, yyline, yycolumn, value);
	}

	private void error()
	throws IOException
	{
		throw new IOException("illegal text at line = "+yyline+", column = "+yycolumn+", text = '"+yytext()+"'");
	}
%}

/* general */
TOK_L_PARENTHESIS       = "("
TOK_R_PARENTHESIS       = ")"
TOK_COMMA               = ","

/* filter functions */
TOK_IDENTITY            = "ID"
TOK_FILE                = "@FILE"
TOK_LINE                = "@LINE"
TOK_LINE_ABBREV         = "@"[0-9]+
TOK_COLUMN              = "@COLUMN"
TOK_FUNC                = "@FUNC"
TOK_LABEL               = "@LABEL"
TOK_CALL                = "@CALL"
TOK_CALLS               = "@CALLS"
TOK_ENTRY               = ("@ENTRY" | "^")
TOK_EXIT                = ("@EXIT" | "$")
TOK_EXPR                = "@EXPR"
TOK_REGEXP              = "@REGEXP"
TOK_BASICBLOCKENTRY     = "@BASICBLOCKENTRY"
TOK_CONDITIONEDGE       = "@CONDITIONEDGE"
TOK_DECISIONEDGE        = "@DECISIONEDGE"
TOK_CONDITIONGRAPH      = "@CONDITIONGRAPH"
TOK_PREDICATION			= "PRED"
/*TOK_DEF                 = "@DEF"*/
/*TOK_USE                 = "@USE"*/
/*TOK_STMTTYPE            = "@STMTTYPE"*/
/*TOK_STT_IF              = "IF"*/
/*TOK_STT_FOR             = "FOR"*/
/*TOK_STT_WHILE           = "WHILE"*/
/*TOK_STT_SWITCH          = "SWITCH"*/
/*TOK_STT_CONDOP          = "?:"*/
/*TOK_STT_ASSERT          = "ASSERT"*/

/* operations on target graphs */
TOK_COMPLEMENT          = "COMPLEMENT"
TOK_UNION               = "UNION"
TOK_INTERSECT           = "INTERSECT"
TOK_SETMINUS            = "SETMINUS"
TOK_ENCLOSING_SCOPES    = "ENCLOSING_SCOPES"
TOK_COMPOSE             = "COMPOSE"

/* abstraction/predicates */
TOK_L_BRACE             = "{"
TOK_R_BRACE             = "}"
TOK_GREATER_OR_EQ       = ">="
TOK_GREATER             = ">"
TOK_EQ                  = "=="
TOK_LESS_OR_EQ          = "<="
TOK_LESS                = "<"
TOK_NEQ                 = "!="

/* coverage specification */
TOK_NODECOV             = "NODES"
TOK_EDGECOV             = "EDGES"
TOK_PATHCOV             = "PATHS"

/* coverage/path patterns */
TOK_CONCAT              = "."
TOK_ALTERNATIVE         = "+"
TOK_KLEENE              = "*"

/* path patterns */
TOK_QUOTE				= "\""

/* query */
TOK_IN                  = "IN"
TOK_COVER               = "COVER"
TOK_PASSING             = "PASSING"

/* C identifier */
TOK_C_IDENT             = [_a-zA-Z][_a-zA-Z0-9]*
/* a quoted string (no newline); see
 * http://dinosaur.compilertools.net/flex/flex_11.html for a more powerful
 * quoted-string lexer including support for escape sequences */
TOK_QUOTED_STRING       = "'" [^"'"]* "'"

/* a natural number */
TOK_NAT_NUMBER          = [0-9]+

WHITESPACE				= [ \t\n]

%%

{WHITESPACE}			{ /* NOOP */ }

/* general */
{TOK_L_PARENTHESIS}		{ return sym(TOK_L_PARENTHESIS); }
{TOK_R_PARENTHESIS}		{ return sym(TOK_R_PARENTHESIS); }
{TOK_COMMA}				{ return sym(TOK_COMMA); }

/* filter functions */
{TOK_IDENTITY}			{ return sym(TOK_IDENTITY); }
{TOK_FILE}				{ return sym(TOK_FILE); }
{TOK_LINE}				{ return sym(TOK_LINE); }
{TOK_LINE_ABBREV}		{ return sym(TOK_LINE_ABBREV, Integer.valueOf(yytext().substring(1, yylength()))); }
{TOK_COLUMN}			{ return sym(TOK_COLUMN); }
{TOK_FUNC}				{ return sym(TOK_FUNC); }
{TOK_LABEL}				{ return sym(TOK_LABEL); }
{TOK_CALL}				{ return sym(TOK_CALL); }
{TOK_CALLS}				{ return sym(TOK_CALLS); }
{TOK_ENTRY}				{ return sym(TOK_ENTRY); }
{TOK_EXIT}				{ return sym(TOK_EXIT); }
{TOK_EXPR}				{ return sym(TOK_EXPR); }
{TOK_REGEXP}			{ return sym(TOK_REGEXP); }
{TOK_BASICBLOCKENTRY}	{ return sym(TOK_BASICBLOCKENTRY); }
{TOK_CONDITIONEDGE}		{ return sym(TOK_CONDITIONEDGE); }
{TOK_DECISIONEDGE}		{ return sym(TOK_DECISIONEDGE); }
{TOK_CONDITIONGRAPH}	{ return sym(TOK_CONDITIONGRAPH); }
{TOK_PREDICATION}		{ return sym(TOK_PREDICATION); }
/*{TOK_DEF}				{ return sym(TOK_DEF); }
{TOK_USE}				{ return sym(TOK_USE); }
{TOK_STMTTYPE}			{ return sym(TOK_STMTTYPE); }
{TOK_STT_IF}			{ return sym(TOK_STT_IF); }
{TOK_STT_FOR}			{ return sym(TOK_STT_FOR); }
{TOK_STT_WHILE}			{ return sym(TOK_STT_WHILE); }
{TOK_STT_SWITCH}		{ return sym(TOK_STT_SWITCH); }
{TOK_STT_CONDOP}		{ return sym(TOK_STT_CONDOP); }
{TOK_STT_ASSERT}		{ return sym(TOK_STT_ASSERT); }*/

/* operations on target graphs */
{TOK_COMPLEMENT}		{ return sym(TOK_COMPLEMENT); }
{TOK_UNION}				{ return sym(TOK_UNION); }
{TOK_INTERSECT}			{ return sym(TOK_INTERSECT); }
{TOK_SETMINUS}			{ return sym(TOK_SETMINUS); }
{TOK_ENCLOSING_SCOPES}	{ return sym(TOK_ENCLOSING_SCOPES); }
{TOK_COMPOSE}			{ return sym(TOK_COMPOSE); }

/* abstraction/predicates */
{TOK_L_BRACE}			{ return sym(TOK_L_BRACE); }
{TOK_R_BRACE}			{ return sym(TOK_R_BRACE); }
{TOK_GREATER_OR_EQ}		{ return sym(TOK_GREATER_OR_EQ); }
{TOK_GREATER}			{ return sym(TOK_GREATER); }
{TOK_EQ}				{ return sym(TOK_EQ); }
{TOK_LESS_OR_EQ}		{ return sym(TOK_LESS_OR_EQ); }
{TOK_LESS}				{ return sym(TOK_LESS); }
{TOK_NEQ}				{ return sym(TOK_NEQ); }

/* coverage specification */
{TOK_NODECOV}			{ return sym(TOK_NODECOV); }
{TOK_EDGECOV}			{ return sym(TOK_EDGECOV); }
{TOK_PATHCOV}			{ return sym(TOK_PATHCOV); }

/* path monitors */
{TOK_CONCAT}			{ return sym(TOK_CONCAT); }
{TOK_ALTERNATIVE}		{ return sym(TOK_ALTERNATIVE); }
{TOK_KLEENE}			{ return sym(TOK_KLEENE); }

/* path pattern */
{TOK_QUOTE}				{ return sym(TOK_QUOTE); }

/* query */
{TOK_IN}				{ return sym(TOK_IN); }
{TOK_COVER}				{ return sym(TOK_COVER); }
{TOK_PASSING}			{ return sym(TOK_PASSING); }

/* C identifier */
{TOK_C_IDENT}			{ return sym(TOK_C_IDENT, yytext()); }
{TOK_QUOTED_STRING}		{ return sym(TOK_QUOTED_STRING, yytext()); }

/* a natural number */
{TOK_NAT_NUMBER}		{ return sym(TOK_NAT_NUMBER, Integer.valueOf(yytext())); }

