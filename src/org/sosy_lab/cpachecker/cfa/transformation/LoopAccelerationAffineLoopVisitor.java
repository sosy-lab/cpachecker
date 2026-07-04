// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class LoopAccelerationAffineLoopVisitor {

  private ArrayList<String> variables;
  private HashMap<String, BigInteger[]> assignments = new HashMap<>();

  public LoopAccelerationAffineLoopVisitor(ArrayList<String> pVariables) {
    variables = pVariables;
    for (String pVariable : variables) {
      assignments.put(pVariable, new BigInteger[variables.size()+1]);
    }
  }

  public ImmutableMap<String, BigInteger[]> getAssignments() {
    return ImmutableMap.copyOf(assignments);
  }

  public TraversalProcess visit(CExpression[] pExpressions) {
    int i = 0;
    for (String variable : variables) {
      Coefficients coefficients = visit(variable, pExpressions[i]);
      if (!coefficients.isLinear) { return TraversalProcess.ABORT;}
      assignments.put(coefficients.variable, coefficients.coefficients);
      i++;
    }
    return TraversalProcess.CONTINUE;
  }

  /**
   * Visitor which checks if a CRighthandSide is an affine integer assignment.
   * @param pCurrentVar the variable name from the CLeftHandSide
   * @param pExpression the expression
   * @return a Coefficients record which has a BigInteger array with coefficients, the variable name and a flag isLinear
   */
  private Coefficients visit(String pCurrentVar, CExpression pExpression) {
    Coefficients child1;
    Coefficients child2;
    switch (pExpression) {
      case CIntegerLiteralExpression intExpression:
        BigInteger[] coeffsLiteral = new BigInteger[variables.size()+1];
        Arrays.fill(coeffsLiteral, BigInteger.ZERO);
        coeffsLiteral[variables.size()] = intExpression.getValue();
        return new Coefficients(coeffsLiteral, pCurrentVar, true);
      case CIdExpression idExpression:
        if (!variables.contains(idExpression.getName())) return new Coefficients(null, pCurrentVar, false);
        BigInteger[] coeffsVar = new BigInteger[variables.size()+1];
        Arrays.fill(coeffsVar, BigInteger.ZERO);
        coeffsVar[variables.indexOf(idExpression.getName())] = BigInteger.ONE;
        return new Coefficients(coeffsVar, pCurrentVar, true);
      case CUnaryExpression unaryExpression:
        switch (unaryExpression.getOperator()) {
          case MINUS:
            child1 = visit(pCurrentVar, unaryExpression.getOperand());
            if (!child1.isLinear) return new Coefficients(null, pCurrentVar, false);
            return negate(child1);
          case TILDE:
            child1 = visit(pCurrentVar, unaryExpression.getOperand());
            if (child1.isLinear) {
              child1 = negate(child1);
              child1.coefficients[variables.size()] =
                child1.coefficients[variables.size()].subtract(BigInteger.ONE);
              return child1;
            } else {
              return new Coefficients(null, pCurrentVar, false);
            }
          case AMPER:
          case SIZEOF:
          case ALIGNOF:
          default:
            return new Coefficients(null, pCurrentVar, false);
        }
      case CBinaryExpression binaryExpression:
        switch (binaryExpression.getOperator()) {
          case PLUS:
            child1 = visit(pCurrentVar, binaryExpression.getOperand1());
            child2 = visit(pCurrentVar, binaryExpression.getOperand2());
            if (!child1.isLinear || !child2.isLinear)
              return new Coefficients(null, pCurrentVar, false);
            return addCoefficients(child1, child2);
          case MINUS:
            child1 = visit(pCurrentVar, binaryExpression.getOperand1());
            child2 = visit(pCurrentVar, binaryExpression.getOperand2());
            if (!child1.isLinear || !child2.isLinear)
              return new Coefficients(null, pCurrentVar, false);
            return addCoefficients(child1, negate(child2));
          case MULTIPLY:
            child1 = visit(pCurrentVar, binaryExpression.getOperand1());
            child2 = visit(pCurrentVar, binaryExpression.getOperand2());
            if (!child1.isLinear || !child2.isLinear)
              return new Coefficients(null, pCurrentVar, false);
            boolean child1Constant = true;
            boolean child2Constant = true;
            for (int i = 0; i < child1.coefficients.length; i++) {
              if (i != child1.coefficients.length - 1) {
                if (!child1.coefficients[i].equals(BigInteger.ZERO)) {
                  child1Constant = false;
                }
                if (!child2.coefficients[i].equals(BigInteger.ZERO)) {
                  child2Constant = false;
                }
              }
            }
            if (!child1Constant && !child2Constant) return new Coefficients(null, pCurrentVar, false);
            if (child1Constant) {
              return factorMultiply(child2, child1.coefficients[child1.coefficients.length - 1]);
            } else {
              return factorMultiply(child1, child2.coefficients[child2.coefficients.length - 1]);
            }
          default:
            return new Coefficients(null, pCurrentVar, false);
        }
      default:
        return new Coefficients(null, pCurrentVar, false);
    }
  }

  private record Coefficients (
      BigInteger[] coefficients,
      String variable,
      boolean isLinear
  ) {}

  private static Coefficients negate(Coefficients pChild) {
    BigInteger[] coefficients = new BigInteger[pChild.coefficients.length];
    Arrays.fill(coefficients, BigInteger.ZERO);
    for (int i = 0; i < pChild.coefficients.length; i++) {
      coefficients[i] = pChild.coefficients[i].negate();
    }
    return new Coefficients(coefficients, pChild.variable, true);
  }

  private static Coefficients addCoefficients(Coefficients pChild1, Coefficients pChild2) {
    BigInteger[] coefficients = new BigInteger[pChild1.coefficients.length];
    Arrays.fill(coefficients, BigInteger.ZERO);
    for (int i = 0; i < pChild1.coefficients.length; i++) {
      coefficients[i] = pChild1.coefficients[i].add(pChild2.coefficients[i]);
    }
    return new Coefficients(coefficients, pChild1.variable, true);
  }

  private static Coefficients factorMultiply(Coefficients pChild, BigInteger pFactor) {
    BigInteger[] coefficients = new BigInteger[pChild.coefficients.length];
    Arrays.fill(coefficients, BigInteger.ZERO);
    for (int i = 0; i < pChild.coefficients.length; i++) {
      coefficients[i] = pChild.coefficients[i].multiply(pFactor);
    }
    return new Coefficients(coefficients, pChild.variable, true);
  }

}
