// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.visitors.LinearVariableDependencyVisitor;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LinearVariableDependency;

public class SummaryUtils {

  /**
   * This method obtains the linear dependencies of this Variable, if it is the case.
   *
   * @param e the edge from which to obtain variables
   * @return a Linear Variable Dependency or None
   */
  public static Optional<LinearVariableDependency> obtainLinearVariableDependency(CFAEdge e) {
    if (e instanceof AStatementEdge) {
      AStatementEdge stmtEdge = (AStatementEdge) e;
      if (stmtEdge.getStatement() instanceof AAssignment) {
        AAssignment assign = (AAssignment) stmtEdge.getStatement();
        LinearVariableDependencyVisitor visitor = new LinearVariableDependencyVisitor();
        ALeftHandSide leftHandSide = assign.getLeftHandSide();
        ARightHandSide rightHandSide = assign.getRightHandSide();
        if (assign instanceof AExpressionAssignmentStatement) {
          if (leftHandSide instanceof AIdExpression) {
            LinearVariableDependency linearVariableDependency;
            if (((AIdExpression) leftHandSide).getDeclaration() instanceof AVariableDeclaration) {
              linearVariableDependency =
                  new LinearVariableDependency(
                      (AVariableDeclaration) ((AIdExpression) leftHandSide).getDeclaration());
            } else {
              return Optional.empty();
            }
            Optional<LinearVariableDependency> dependencies = Optional.empty();
            if (rightHandSide instanceof AExpression) {
              dependencies = ((AExpression) rightHandSide).accept_(visitor);
              if (dependencies.isEmpty()) {
                return Optional.empty();
              }
              // TODO: Make this more general to also include Java Expressions
              linearVariableDependency.modifyDependency(
                  dependencies.orElseThrow(), CBinaryExpression.BinaryOperator.PLUS);
              return Optional.of(linearVariableDependency);
            } else {
              return Optional.empty();
            }
          } else {
            return Optional.empty();
          }
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }
}
