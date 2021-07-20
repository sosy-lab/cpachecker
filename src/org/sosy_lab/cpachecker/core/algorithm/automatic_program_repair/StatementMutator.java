// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.base.Optional;
import java.util.ArrayList;
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

  public static <T extends CStatement> ArrayList<? extends CStatement> calcMutationsFor(
      T statement, CFA cfa) {
    if (statement instanceof CExpressionStatement) {
      return calcMutationsFor((CExpressionStatement) statement, cfa);
    } else if (statement instanceof CFunctionCallStatement) {
      return calcMutationsFor((CFunctionCallStatement) statement, cfa);
    } else if (statement instanceof CAssignment) {
      return calcMutationsFor((CAssignment) statement, cfa);
    } else {
      return new ArrayList<>();
    }
  }

  public static ArrayList<CAssignment> calcMutationsFor(CAssignment assignment, CFA cfa) {
    if (assignment instanceof CExpressionAssignmentStatement) {
      return calcMutationsFor((CExpressionAssignmentStatement) assignment, cfa);
    } else if (assignment instanceof CFunctionCallAssignmentStatement) {
      return calcMutationsFor((CFunctionCallAssignmentStatement) assignment, cfa);
    } else {
      return new ArrayList<>();
    }
  }

  private static ArrayList<CStatement> calcMutationsFor(
      CExpressionStatement originalExpressionStatement, CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CExpression originalExpression = originalExpressionStatement.getExpression();
    final ArrayList<CExpression> expressions =
        ExpressionMutator.calcMutationsFor(originalExpression, cfa);

    for (CExpression expression : expressions) {
      alternativeStatements.add(replaceExpression(originalExpressionStatement, expression));
    }

    return alternativeStatements;
  }

  private static ArrayList<CStatement> calcMutationsFor(
      CFunctionCallStatement originalFunctionCallStatement, CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallStatement.getFunctionCallExpression();
    ArrayList<CFunctionCallExpression> alternativeFunctionCalls =
        ExpressionMutator.calcMutationsFor(originalFunctionCallExpression, cfa);

    for (CFunctionCallExpression alternativeFunctionCall : alternativeFunctionCalls) {
      alternativeStatements.add(
          replaceFunctionCall(originalFunctionCallStatement, alternativeFunctionCall));
    }

    return alternativeStatements;
  }

  private static ArrayList<CAssignment> calcMutationsFor(
      CExpressionAssignmentStatement originalExpressionAssignmentStatement, CFA cfa) {

    ArrayList<CAssignment> alternativeStatements = new ArrayList<>();
    CExpression originalExpression = originalExpressionAssignmentStatement.getRightHandSide();
    final ArrayList<CExpression> expressions =
        ExpressionMutator.calcMutationsFor(originalExpression, cfa);

    for (CExpression expression : expressions) {
      alternativeStatements.add(
          replaceExpression(originalExpressionAssignmentStatement, expression));
    }
    return alternativeStatements;
  }

  private static ArrayList<CAssignment> calcMutationsFor(
      CFunctionCallAssignmentStatement originalFunctionCallAssignmentStatement, CFA cfa) {
    ArrayList<CAssignment> alternativeStatements = new ArrayList<>();
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallAssignmentStatement.getRightHandSide();
    ArrayList<CFunctionCallExpression> alternativeFunctionCalls =
        ExpressionMutator.calcMutationsFor(originalFunctionCallExpression, cfa);

    for (CFunctionCallExpression alternativeFunctionCall : alternativeFunctionCalls) {
      alternativeStatements.add(
          replaceRightHandSide(originalFunctionCallAssignmentStatement, alternativeFunctionCall));
    }

    return alternativeStatements;
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

  public static CReturnStatement replaceAssignment(
      CReturnStatement originalReturnStatement, CAssignment assignment) {
    return new CReturnStatement(
        originalReturnStatement.getFileLocation(),
        Optional.of((CExpression) assignment.getRightHandSide()),
        Optional.of(assignment));
  }
}
