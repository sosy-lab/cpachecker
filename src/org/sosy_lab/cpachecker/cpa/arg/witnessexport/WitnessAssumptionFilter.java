// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.exceptions.NoException;

class WitnessAssumptionFilter {

  /**
   * Filter the assumptions of an edge for relevant assumptions, and then return a new edge based on
   * the filtered assumptions.
   */
  static CFAEdgeWithAssumptions filterRelevantAssumptions(
      CFAEdgeWithAssumptions pEdgeWithAssumptions) {
    int originalSize = pEdgeWithAssumptions.getExpStmts().size();
    ImmutableList.Builder<AExpressionStatement> expressionStatementsBuilder =
        ImmutableList.builderWithExpectedSize(originalSize);
    for (AExpressionStatement expressionStatement : pEdgeWithAssumptions.getExpStmts()) {
      if (isRelevantExpression(expressionStatement.getExpression())) {
        expressionStatementsBuilder.add(expressionStatement);
      }
    }

    ImmutableList<AExpressionStatement> expressionStatements = expressionStatementsBuilder.build();
    if (expressionStatements.size() == originalSize) {
      return pEdgeWithAssumptions;
    }
    return new CFAEdgeWithAssumptions(
        pEdgeWithAssumptions.getCFAEdge(), expressionStatements, pEdgeWithAssumptions.getComment());
  }

  /**
   * Check whether an expresion is relevant for the witness export, e.g., we assume that assignments
   * of constants to pointers are not relevant.
   */
  private static boolean isRelevantExpression(final AExpression assumption) {
    if (!(assumption instanceof CBinaryExpression)) {
      return true;

    } else {
      CBinaryExpression binExpAssumption = (CBinaryExpression) assumption;
      CExpression leftSide = binExpAssumption.getOperand1();
      CExpression rightSide = binExpAssumption.getOperand2();

      final CType leftType = leftSide.getExpressionType().getCanonicalType();
      final CType rightType = rightSide.getExpressionType().getCanonicalType();

      if (!(leftType instanceof CVoidType) || !(rightType instanceof CVoidType)) {

        boolean equalTypes = leftType.equals(rightType);
        boolean leftIsAccepted = equalTypes || leftType instanceof CSimpleType;
        boolean rightIsAccepted = equalTypes || rightType instanceof CSimpleType;

        if (leftIsAccepted && rightIsAccepted) {
          boolean leftIsConstant = isConstant(leftSide);
          boolean leftIsPointer = !leftIsConstant && isEffectivelyPointer(leftSide);
          boolean rightIsConstant = isConstant(rightSide);
          boolean rightIsPointer = !rightIsConstant && isEffectivelyPointer(rightSide);
          if (!(leftIsPointer && rightIsConstant) && !(leftIsConstant && rightIsPointer)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean isConstant(CExpression pLeftSide) {
    return pLeftSide.accept(IsConstantExpressionVisitor.INSTANCE);
  }

  private static boolean isEffectivelyPointer(CExpression pLeftSide) {
    return pLeftSide.accept(
        new DefaultCExpressionVisitor<Boolean, NoException>() {

          @Override
          public Boolean visit(CComplexCastExpression pComplexCastExpression) {
            return pComplexCastExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CBinaryExpression pIastBinaryExpression) {
            return pIastBinaryExpression.getOperand1().accept(this)
                || pIastBinaryExpression.getOperand2().accept(this);
          }

          @Override
          public Boolean visit(CCastExpression pIastCastExpression) {
            return pIastCastExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CUnaryExpression pIastUnaryExpression) {
            switch (pIastUnaryExpression.getOperator()) {
              case MINUS:
              case TILDE:
                return pIastUnaryExpression.getOperand().accept(this);
              case AMPER:
                return true;
              default:
                return visitDefault(pIastUnaryExpression);
            }
          }

          @Override
          protected Boolean visitDefault(CExpression pExp) {
            CType type = pExp.getExpressionType().getCanonicalType();
            return type instanceof CPointerType || type instanceof CFunctionType;
          }
        });
  }
}
