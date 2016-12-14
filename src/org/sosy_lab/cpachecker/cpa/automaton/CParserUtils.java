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

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

class CParserUtils {

  static CStatement parseSingleStatement(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException {
    return parse(addFunctionDeclaration(pSource), parser, scope);
  }

  static List<CStatement> parseListOfStatements(String pSource, CParser parser, Scope scope)
      throws InvalidAutomatonException, CParserException {
    return parseBlockOfStatements(addFunctionDeclaration(pSource), parser, scope);
  }

  static List<AExpression> convertStatementsToAssumptions(
      Iterable<CStatement> assumptions, MachineModel machineModel, LogManager logger) {
    ImmutableList.Builder<AExpression> result = ImmutableList.builder();
    CBinaryExpressionBuilder expressionBuilder = new CBinaryExpressionBuilder(machineModel, logger);
    for (CStatement statement : assumptions) {

      if (statement instanceof CAssignment) {
        CAssignment assignment = (CAssignment) statement;

        if (assignment.getRightHandSide() instanceof CExpression) {

          CExpression expression = (CExpression) assignment.getRightHandSide();
          CBinaryExpression assumeExp =
              expressionBuilder.buildBinaryExpressionUnchecked(
                  assignment.getLeftHandSide(),
                  expression,
                  CBinaryExpression.BinaryOperator.EQUALS);

          result.add(assumeExp);
        } else if (assignment.getRightHandSide() instanceof CFunctionCall) {
          // TODO FunctionCalls, ExpressionStatements etc
        }
      }

      if (statement instanceof CExpressionStatement) {
        if (((CExpressionStatement) statement).getExpression().getExpressionType()
                instanceof CSimpleType
            && ((CSimpleType)
                    (((CExpressionStatement) statement).getExpression().getExpressionType()))
                .getType()
                .isIntegerType()) {
          result.add(((CExpressionStatement) statement).getExpression());
        }
      }
    }
    return result.build();
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
   * Parse the content of a file into an AST with the Eclipse CDT parser. If an error occurs, the
   * program is halted.
   *
   * @param code The C code to parse.
   * @param parser The parser to use
   * @param scope the scope to use
   * @return The AST.
   */
  private static CStatement parse(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException {
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
   * Parse the assumption of a automaton, which are C assignments, return statements or function
   * calls, into a list of CStatements with the Eclipse CDT parser. If an error occurs, an empty
   * list will be returned, and the error will be logged.
   *
   * @param code The C code to parse.
   * @return The AST.
   */
  private static List<CStatement> parseBlockOfStatements(String code, CParser parser, Scope scope)
      throws InvalidAutomatonException, CParserException {
    List<CAstNode> statements = parser.parseStatements(code, scope);

    for (CAstNode statement : statements) {
      if (!(statement instanceof CStatement)) {
        throw new InvalidAutomatonException(
            "Code in assumption: <" + statement.toASTString() + "> is not a valid assumption.");
      }
    }

    return transformedImmutableListCopy(statements, statement -> (CStatement) statement);
  }
}
