// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import java.util.Optional;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;

public class StatementMutator {
  /**
   * Returns a list of possible mutations for a given statement. The mutation consists in altering
   * an expression contained in the statement.
   */
  public static <T extends CStatement> Stream<? extends CStatement> calcMutationsFor(
      T statement, CFA cfa) {
    if (statement instanceof CExpressionStatement) {
      return calcMutationsFor((CExpressionStatement) statement, cfa);
    } else if (statement instanceof CFunctionCallStatement) {
      return calcMutationsFor((CFunctionCallStatement) statement, cfa);
    } else if (statement instanceof CAssignment) {
      return calcMutationsFor((CAssignment) statement, cfa);
    } else {
      return Stream.empty();
    }
  }
  /**
   * Returns a list of possible mutations for a given expression statement. The mutation changing
   * the right-hand side of the assignment.
   */
  public static Stream<CAssignment> calcMutationsFor(CAssignment assignment, CFA cfa) {
    if (assignment instanceof CExpressionAssignmentStatement) {
      return calcMutationsFor((CExpressionAssignmentStatement) assignment, cfa);
    } else if (assignment instanceof CFunctionCallAssignmentStatement) {
      return calcMutationsFor((CFunctionCallAssignmentStatement) assignment, cfa);
    } else {
      return Stream.empty();
    }
  }

  /**
   * Returns a list of possible mutations for a given expression statement. The mutation consists in
   * changing the expression.
   */
  private static Stream<CStatement> calcMutationsFor(
      CExpressionStatement originalExpressionStatement, CFA cfa) {
    CExpression originalExpression = originalExpressionStatement.getExpression();
    return ExpressionMutator.calcMutationsFor(originalExpression, cfa)
        .map(
            (CExpression expression) -> replaceExpression(originalExpressionStatement, expression));
  }

  /**
   * Returns a list of possible mutations for a given function call statement. The mutation consists
   * in changing one expression of the parameter list.
   */
  private static Stream<CStatement> calcMutationsFor(
      CFunctionCallStatement originalFunctionCallStatement, CFA cfa) {
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallStatement.getFunctionCallExpression();
    return ExpressionMutator.calcMutationsFor(originalFunctionCallExpression, cfa)
        .map(
            (CFunctionCallExpression alternativeFunctionCall) ->
                replaceFunctionCall(originalFunctionCallStatement, alternativeFunctionCall));
  }

  private static Stream<CAssignment> calcMutationsFor(
      CExpressionAssignmentStatement originalExpressionAssignmentStatement, CFA cfa) {
    CExpression originalExpression = originalExpressionAssignmentStatement.getRightHandSide();
    return ExpressionMutator.calcMutationsFor(originalExpression, cfa)
        .map(
            (CExpression expression) ->
                replaceExpression(originalExpressionAssignmentStatement, expression));
  }

  private static Stream<CAssignment> calcMutationsFor(
      CFunctionCallAssignmentStatement originalFunctionCallAssignmentStatement, CFA cfa) {
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallAssignmentStatement.getRightHandSide();
    return ExpressionMutator.calcMutationsFor(originalFunctionCallExpression, cfa)
        .map(
            (CFunctionCallExpression alternativeFunctionCall) ->
                replaceRightHandSide(
                    originalFunctionCallAssignmentStatement, alternativeFunctionCall));
  }

  /* REPLACEMENTS */

  private static CFunctionCallStatement replaceFunctionCall(
      CFunctionCallStatement originalFunctionCallStatement,
      CFunctionCallExpression newFunctionCallExpression) {
    return new CFunctionCallStatement(
        originalFunctionCallStatement.getFileLocation(), newFunctionCallExpression);
  }

  private static CExpressionStatement replaceExpression(
      CExpressionStatement expressionAssignmentStatement, CExpression expression) {
    return new CExpressionStatement(expressionAssignmentStatement.getFileLocation(), expression);
  }

  private static CFunctionCallAssignmentStatement replaceRightHandSide(
      CFunctionCallAssignmentStatement originalFunctionCallAssignmentStatement,
      CFunctionCallExpression newFunctionCallExpression) {
    return new CFunctionCallAssignmentStatement(
        originalFunctionCallAssignmentStatement.getFileLocation(),
        originalFunctionCallAssignmentStatement.getLeftHandSide(),
        newFunctionCallExpression);
  }

  private static CExpressionAssignmentStatement replaceExpression(
      CExpressionAssignmentStatement expressionAssignmentStatement, CExpression expression) {
    return new CExpressionAssignmentStatement(
        expressionAssignmentStatement.getFileLocation(),
        expressionAssignmentStatement.getLeftHandSide(),
        expression);
  }
  /** Returns a copy of the given assignment statement but with the new assignment provided. */
  public static CReturnStatement replaceAssignment(
      CReturnStatement originalReturnStatement, CAssignment assignment) {
    return new CReturnStatement(
        originalReturnStatement.getFileLocation(),
        Optional.of((CExpression) assignment.getRightHandSide()),
        Optional.of(assignment));
  }
}
