/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.util.List;

class CParserUtils {

  static CStatement parseSingleStatement(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException, InvalidConfigurationException {
    return parse(addFunctionDeclaration(pSource), parser, scope);
  }

  static List<CStatement> parseListOfStatements(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException, InvalidConfigurationException, CParserException {
    return parseBlockOfStatements(addFunctionDeclaration(pSource), parser, scope);
  }

  /**
   * Surrounds the argument with a function declaration.
   * This is necessary so the string can be parsed by the CDT parser.
   * @param pBody the body of the function
   * @return "void test() { " + body + ";}";
   */
  private static String addFunctionDeclaration(String pBody) {
    if (pBody.trim().endsWith(";")) {
      return "void test() { " + pBody + "}";
    } else {
      return "void test() { " + pBody + ";}";
    }
  }

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   *
   * @param code The C code to parse.
   * @param parser The parser to use
   * @param scope the scope to use
   * @return The AST.
   */
  private static CStatement parse(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException, InvalidConfigurationException {
    try {
      CAstNode statement = parser.parseSingleStatement(code, scope);
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException("Not a valid statement: " + statement.toASTString());
      }
      return (CStatement) statement;
    } catch (ParserException e) {
      throw new InvalidAutomatonException(
          "Error during parsing C code \"" + code + "\": " + e.getMessage());
    }
  }

  /**
   * Parse the assumption of a automaton, which are C assignments,
   * return statements or function calls, into a list of
   * CStatements with the Eclipse CDT parser. If an error occurs,
   * an empty list will be returned, and the error will be logged.
   *
   *
   * @param code The C code to parse.
   * @return The AST.
   */
  private static List<CStatement> parseBlockOfStatements(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException, InvalidConfigurationException, CParserException {
    List<CAstNode> statements = parser.parseStatements(code, scope);

    for (CAstNode statement : statements) {
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException(
            "Code in assumption: <" + statement.toASTString() + "> is not a valid assumption.");
      }
    }

    return ImmutableList.copyOf(Lists.transform(statements, statement -> (CStatement) statement));
  }
}
