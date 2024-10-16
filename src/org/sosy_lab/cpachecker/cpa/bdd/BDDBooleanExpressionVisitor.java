// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;

/**
 * This Visitor implements evaluation of simply typed expressions. This Visitor is specialized for
 * boolean expressions.
 */
public class BDDBooleanExpressionVisitor extends DefaultCExpressionVisitor<Region, NoException> {

  protected static final int BOOLEAN_SIZE = 1;
  protected final PredicateManager predMgr;
  protected final VariableTrackingPrecision precision;
  protected final RegionManager rmgr;
  protected final CFANode location;

  /** This Visitor returns the boolean value for an expression. */
  protected BDDBooleanExpressionVisitor(
      final PredicateManager pPredMgr,
      final RegionManager pRmgr,
      final VariableTrackingPrecision pPrecision,
      final CFANode pLocation) {
    predMgr = pPredMgr;
    rmgr = pRmgr;
    precision = pPrecision;
    location = pLocation;
  }

  @Override
  protected Region visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Region visit(final CBinaryExpression pE) {
    final Region lVal = pE.getOperand1().accept(this);
    final Region rVal = pE.getOperand2().accept(this);
    if (lVal == null || rVal == null) {
      return null;
    }
    return calculateBinaryOperation(lVal, rVal, rmgr, pE);
  }

  public static Region calculateBinaryOperation(
      Region l, Region r, final RegionManager rmgr, final CBinaryExpression binaryExpr) {

    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    return switch (binaryOperator) {
      case BINARY_AND -> rmgr.makeAnd(l, r);
      case BINARY_OR -> rmgr.makeOr(l, r);
      case BINARY_XOR, NOT_EQUALS -> rmgr.makeUnequal(l, r);
      case EQUALS -> rmgr.makeEqual(l, r);
      default -> throw new AssertionError("unhandled binary operator");
    };
  }

  @Override
  public Region visit(CCharLiteralExpression pE) {
    return getNum(pE.getCharacter());
  }

  @Override
  public Region visit(CIntegerLiteralExpression pE) {
    return getNum(pE.getValue());
  }

  @Override
  public Region visit(CImaginaryLiteralExpression pE) {
    return pE.getValue().accept(this);
  }

  @Override
  public Region visit(CIdExpression idExp) {
    if (idExp.getDeclaration() instanceof CEnumerator enumerator) {
      return getNum(enumerator.getValue());
    }

    final Region[] result =
        predMgr.createPredicate(
            idExp.getDeclaration().getQualifiedName(),
            idExp.getExpressionType(),
            location,
            BOOLEAN_SIZE,
            precision);
    if (result == null) {
      return null;
    } else {
      assert result.length == BOOLEAN_SIZE;
      return result[0];
    }
  }

  private Region getNum(long num) {
    if (num == 0) {
      return rmgr.makeFalse();
    } else if (num == 1) {
      return rmgr.makeTrue();
    } else {
      throw new AssertionError("no boolean value: " + num);
    }
  }

  private Region getNum(BigInteger num) {
    try {
      long value = num.longValueExact();
      return getNum(value);
    } catch (ArithmeticException e) {
      // big integer does not fit into long value. But we actually expect a boolean value of 0 or 1,
      // so this should not happen.
      throw new AssertionError("no boolean value: " + num, e);
    }
  }
}
