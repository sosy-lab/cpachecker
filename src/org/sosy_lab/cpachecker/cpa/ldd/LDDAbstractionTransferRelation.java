/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ldd;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegion;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class LDDAbstractionTransferRelation extends SingleEdgeTransferRelation {

  private final LDDRegionManager regionManager;

  private final Map<String, Integer> variables;

  private final Set<String> usedVars = new HashSet<>();

  public LDDAbstractionTransferRelation(LDDRegionManager regionManager, Map<String, Integer> variables) {
    this.regionManager = regionManager;
    this.variables = variables;
  }

  @Override
  public Collection<? extends LDDAbstractState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision precision, CFAEdge edge)
          throws CPATransferException, InterruptedException {
    if (!(element instanceof LDDAbstractState)) { return Collections.emptyList(); }
    LDDAbstractState analysisElement = (LDDAbstractState) element;
    LDDRegion region = toRegion(edge, analysisElement.getRegion());
    // If the LDD is null or false, no successor state is reachable.
    if (region == null || region.isFalse()) { return Collections.emptyList(); }
    return Collections.singleton(new LDDAbstractState(region));
  }

  /**
   * Tries to create an LDD as the successor state of the given state over the given control flow edge.
   *
   * @param edge the control flow edge.
   * @param currentRegion the current state.
   * @return the successor state of the current state over the control flow edge.
   */
  private LDDRegion toRegion(CFAEdge edge, LDDRegion currentRegion) {
    LDDRegion edgePartialRegion = null;
    if (edge instanceof CAssumeEdge) {
      CAssumeEdge assumeEdge = (CAssumeEdge) edge;
      edgePartialRegion = assumeToRegion(assumeEdge.getExpression());
      if (edgePartialRegion != null && !assumeEdge.getTruthAssumption()) {
        edgePartialRegion = this.regionManager.makeNot(edgePartialRegion);
      }
    } else if (edge instanceof CDeclarationEdge) {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
      return toRegion(declarationEdge.getDeclaration(), currentRegion);
    } else if (edge instanceof CStatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) edge;
      CStatement statement = statementEdge.getStatement();
      return toRegion(statement, currentRegion);
    } else if (edge instanceof CFunctionCallEdge) {
      // CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) edge;
      // TODO currently not supported
    } else if (edge instanceof CFunctionReturnEdge) {
      // CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) edge;
      // TODO currently not supported
    }
    if (edgePartialRegion == null) { return currentRegion; }
    return this.regionManager.makeAnd(currentRegion, edgePartialRegion);
  }

  /**
   * Tries to extract information about the program state from the given statement to compute
   * the successor state by combining it with the given previous state. Only expression assignment
   * states are supported at the moment.
   *
   * @param pStatement the assignment statement.
   * @param previousRegion the previous state.
   * @return the new state.
   */
  private LDDRegion toRegion(CStatement pStatement, LDDRegion previousRegion) {
    if (pStatement instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement eas = (CExpressionAssignmentStatement) pStatement;
      CExpression leftHandSide = eas.getLeftHandSide();
      if (leftHandSide instanceof CIdExpression) {
        CExpression rightHandSide = eas.getRightHandSide();
        CIdExpression lhsId = (CIdExpression) leftHandSide;
        return assign(lhsId.getName(), rightHandSide, previousRegion);
      }
    }
    // As long as function calls are not supported, it might be dangerous to default
    // to the previous state, because error locations that were previously not reachable might
    // become reachable by the new program state, therefore true is returned.
    if (pStatement instanceof CFunctionCallAssignmentStatement
        || pStatement instanceof CFunctionCallStatement) {
      return this.regionManager.makeTrue();
 }
    return previousRegion;
  }

  /**
   * Tries to create an LDD for the given assume condition. If the expression is not supported,
   * <code>null</code> is returned.
   *
   * @param pExpression the assume condition.
   * @return the LDD representing the condition or <code>null</code> if the expression is not supported.
   */
  private LDDRegion assumeToRegion(CExpression pExpression) {
    if (pExpression instanceof CIntegerLiteralExpression) {
      Integer constant = reduceToConstant(pExpression);
      if (constant == null || constant == 0) { return this.regionManager.makeFalse(); }
      return this.regionManager.makeTrue();
    }
    if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) pExpression;
      CExpression left = binaryExpression.getOperand1();
      CExpression right = binaryExpression.getOperand2();
      BinaryOperator operator = binaryExpression.getOperator();
      switch (operator) {
      case LESS_EQUAL:
      case LESS_THAN:
        break;
      case GREATER_EQUAL: // Revert logic
      case GREATER_THAN:
        CExpression temp = right;
        right = left;
        left = temp;
        operator = operator == BinaryOperator.GREATER_EQUAL ? BinaryOperator.LESS_EQUAL : BinaryOperator.LESS_THAN;
        break;
      case BINARY_AND:
        return this.regionManager.makeAnd(assumeToRegion(left), assumeToRegion(right));
      case BINARY_OR:
        return this.regionManager.makeOr(assumeToRegion(left), assumeToRegion(right));
      case BINARY_XOR:
        return this.regionManager.makeXor(assumeToRegion(left), assumeToRegion(right));
      case EQUALS:
        Map<String, Integer> leftTerm = reduceToTerm(left);
        if (leftTerm == null) { return null; }
        Map<String, Integer> rightTerm = reduceToTerm(right);
        if (rightTerm == null) {
          return null;
        }
        return this.regionManager.makeAnd(makeNode(leftTerm, rightTerm, true), makeNode(rightTerm, leftTerm, true));
      case NOT_EQUALS:
        leftTerm = reduceToTerm(left);
        if (leftTerm == null) { return null; }
        rightTerm = reduceToTerm(right);
        if (rightTerm == null) {
          return null;
        }
        return this.regionManager.makeNot(this.regionManager.makeAnd(makeNode(leftTerm, rightTerm, true), makeNode(rightTerm, leftTerm, true)));
      default:
        return null;
      }
      Map<String, Integer> leftTerm = reduceToTerm(left);
      boolean leq = operator == BinaryOperator.LESS_EQUAL;
      if (leftTerm == null) { return null; }
      Map<String, Integer> rightTerm = reduceToTerm(right);
      if (rightTerm == null) {
        return null;
      }
      return makeNode(leftTerm, rightTerm, leq);
    }
    return null;
  }

  private LDDRegion makeNode(Map<String, Integer> leftTerm, Map<String, Integer> rightTerm, boolean leq) {
    Integer leftConst = removeConstant(leftTerm);
    Integer rightConst = removeConstant(rightTerm);
    int constant = rightConst - leftConst;
    Map<String, Integer> term = new HashMap<>();
    // Subtract right side variables (from left side, if given)
    for (Map.Entry<String, Integer> rightCoeff : rightTerm.entrySet()) {
      Integer leftCoeff = removeCoefficient(leftTerm, rightCoeff.getKey());
      int value = leftCoeff - rightCoeff.getValue();
      term.put(rightCoeff.getKey(), value);
    }
    // Add variables that were only on left side
    for (Map.Entry<String, Integer> leftCoeff : leftTerm.entrySet()) {
      if (!term.containsKey(leftCoeff.getKey()) && leftCoeff.getValue() != null) {
        term.put(leftCoeff.getKey(), leftCoeff.getValue());
      }
    }
    return toNode(term, leq, constant);
  }

  /**
   * Tries to create an LDD for an assignment of the given expression to the variable with the given name.
   * If the expression is not supported, the previous LDD is returned. If the expression is supported,
   * the conjunction of the previous LDD with the LDD obtained from the assignment is returned.
   *
   * @param variable the name of the variable.
   * @param expression the assignment expression.
   * @param previousRegion the previous LDD.
   * @return the successor state.
   */
  private LDDRegion assign(String variable, CExpression expression, LDDRegion previousRegion) {
    Integer reduced = reduceToConstant(expression);
    if (reduced != null) {
      this.usedVars.add(variable);
      return this.regionManager.makeAnd(previousRegion, toConstantAssignmentRegion(Collections.singleton(variable),
          reduced));
    }
    Map<String, Integer> term = reduceToTerm(expression);
    if (term != null) {
      Integer constant = removeConstant(term);
      term = negate(term);
      // Check if the variable is present on the right hand side of the assignment, like in x = x + 1
      Integer coefficient = removeCoefficient(term, variable);
      // Those assignments are not supported yet
      if (coefficient != 0) { return null; }
      final LDDRegion region;
      if (this.usedVars.contains(variable)) {
        region = substituteByTerm(variable, term, constant, previousRegion);
      } else {
        term.put(variable, 1);
        for (String var : term.keySet()) {
          this.usedVars.add(var);
        }
        region = this.regionManager.makeAnd(previousRegion, toConstantAssignmentRegion(term, constant));
      }
      return region;
    }
    return previousRegion;
  }

  /**
   * Substitutes the given variable in the given LDD by the given term.
   * @param pVariable the name of the variable.
   * @param pTerm the variables part of the linear term.
   * @param constant the constant part of the term.
   * @param region the previous LDD.
   * @return the resulting LDD.
   */
  private LDDRegion substituteByTerm(String pVariable, Map<String, Integer> pTerm, int constant, LDDRegion region) {
    return this.regionManager.replace(this.variables.get(pVariable), toIndexCoefficients(pTerm), this.variables.size(),
        constant, region);
  }

  /**
   * Removes the integer constant from the given integer linear term.
   *
   * @param coefficients the integer linear term to remove the constant from.
   * @return the integer constant of the given integer linear term.
   */
  private Integer removeConstant(Map<String, Integer> coefficients) {
    return removeCoefficient(coefficients, "const");
  }

  /**
   * Removes the integer coefficient for the given variable from the given integer linear term and returns it.
   * If the coefficient does not exist, it is treated as zero.
   *
   * @param coefficients the integer linear term.
   * @param variable the name of the variable to get the coefficient for.
   * @return the coefficient of the given variable in the given integer linear term.
   */
  private Integer removeCoefficient(Map<String, Integer> coefficients, String variable) {
    Integer coeff = coefficients.remove(variable);
    if (coeff == null) {
      coeff = 0;
    }
    return coeff;
  }

  /**
   * Removes the rational coefficient for the given variable from the given rational linear term and returns it.
   * If the coefficient does not exist, 0/1 is returned, so the coefficient is treated as zero.
   *
   * @param coefficients the rational linear term.
   * @param variable the name of the variable to get the coefficient for.
   * @return the coefficient of the given variable in the given rational linear term.
   */
  private Pair<Integer, Integer> removeRationalCoefficient(Map<String, Pair<Integer, Integer>> coefficients,
      String variable) {
    Pair<Integer, Integer> rational = coefficients.remove(variable);
    if (rational == null) { return Pair.of(0, 1); }
    return rational;
  }

  /**
   * Negates the given integer linear term by negating all coefficients.
   *
   * @param toNegate the integer linear term to negate.
   * @return the negated term.
   */
  private Map<String, Integer> negate(Map<String, Integer> toNegate) {
    Map<String, Integer> result = new HashMap<>();
    for (Map.Entry<String, Integer> coeff : toNegate.entrySet()) {
      result.put(coeff.getKey(), -coeff.getValue());
    }
    return result;
  }

  /**
   * Tries to reduce the expression to an integer linear term. If this is not
   * possible, <code>null</code> is returned.
   *
   * @param expression the expression to convert.
   * @return the integer linear term the expression was converted to or <code>null</code>
   * if the conversion failed.
   */
  private Map<String, Integer> reduceToTerm(CExpression expression) {
    Map<String, Integer> variableCoeffs = new HashMap<>();
    Map<String, Pair<Integer, Integer>> rationalTerm = reduceToRationalTerm(expression);
    if (rationalTerm == null) { return null; }
    for (Map.Entry<String, Pair<Integer, Integer>> coeff : rationalTerm.entrySet()) {
      int num = coeff.getValue().getFirst();
      int denom = coeff.getValue().getSecond();
      // Only integer coefficients are supported
      if (denom == 0 || num % denom != 0) { return null; }
      int value = num / denom;
      variableCoeffs.put(coeff.getKey(), value);
    }
    return variableCoeffs;
  }

  /**
   * This is actually not a real rational normalization, but trying to get a
   * denominator value of 1 should be good enough for the computations.
   *
   * @param num the numerator.
   * @param denom the denominator.
   * @return a pair representing the "normalized" rational.
   */
  private Pair<Integer, Integer> normalizeRational(int num, int denom) {
    if (num % denom == 0) { return Pair.of(num / denom, 1); }
    return Pair.of(num, denom);
  }

  /**
   * Tries to reduce the expression to a rational linear term. If this is not
   * possible, <code>null</code> is returned.
   *
   * @param expression the expression to convert.
   * @return the rational linear term the expression was converted to or <code>null</code>
   * if the conversion failed.
   */
  private Map<String, Pair<Integer, Integer>> reduceToRationalTerm(CExpression expression) {
    expression.toASTString();
    Map<String, Pair<Integer, Integer>> variableCoeffs = new HashMap<>();
    if (expression instanceof CIntegerLiteralExpression) {
      CIntegerLiteralExpression literal = (CIntegerLiteralExpression) expression;
      return Collections.singletonMap("const", Pair.of(literal.getValue().intValue(), 1));
    }
    if (expression instanceof CIdExpression) {
      CIdExpression id = (CIdExpression) expression;
      return Collections.singletonMap(id.getName(), Pair.of(1, 1));
    }
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;
      CExpression op1 = binaryExpression.getOperand1();
      CExpression op2 = binaryExpression.getOperand2();
      BinaryOperator operator = binaryExpression.getOperator();
      Map<String, Pair<Integer, Integer>> firstAsTerm = reduceToRationalTerm(op1);
      Map<String, Pair<Integer, Integer>> secondAsTerm = reduceToRationalTerm(op2);
      Integer firstAsConstant = reduceToConstant(op1);
      Integer secondAsConstant = reduceToConstant(op2);
      if (firstAsConstant != null && secondAsConstant != null) {
        // Not a variable, but reducible to a constant
        Integer constant = reduceToConstant(expression);
        if (constant == null) { return null; }
        return Collections.singletonMap("const", Pair.of(constant, 1));
      }
      // If both are terms, addition and subtraction are supported
      if (firstAsTerm != null && secondAsTerm != null) {
        final int multiplier;
        switch (operator) {
        case PLUS:
          multiplier = 1;
          break;
        case MINUS:
          multiplier = -1;
          break;
        default:
          return null;
        }
        for (Map.Entry<String, Pair<Integer, Integer>> coeff : firstAsTerm.entrySet()) {
          int num1 = coeff.getValue().getFirst();
          int denom1 = coeff.getValue().getSecond();

          Pair<Integer, Integer> second = secondAsTerm.get(coeff.getKey());
          int num2;
          int denom2;
          // If first does not exist, just use 0/1
          if (second == null) {
            num2 = 0;
            denom2 = 1;
          } else {
            num2 = second.getFirst();
            denom2 = second.getSecond();
          }
          int denom = denom1 * denom2;
          num1 = num1 * denom2;
          num2 = num2 * denom1;
          int num = 0;
          num = num1 + num2 * multiplier;
          variableCoeffs.put(coeff.getKey(), normalizeRational(num, denom));
        }
        // Add all from second that were not in second
        for (Map.Entry<String, Pair<Integer, Integer>> entry : secondAsTerm.entrySet()) {
          if (!variableCoeffs.containsKey(entry.getKey())) {
            int num = entry.getValue().getFirst() * multiplier;
            int denom = entry.getValue().getSecond();
            variableCoeffs.put(entry.getKey(), Pair.of(num, denom));
          }
        }
        return variableCoeffs;
      }
      // Constant divided by term is not supported
      if (firstAsTerm != null && secondAsTerm != null && operator == BinaryOperator.DIVIDE) { return null; }
      // Multiplication of term and constant as well as division of term by constant is supported
      // Adding constants to or subtracting them from terms is also supported by treating
      // the constant as a coefficient of a "const" variable
      int constant = firstAsConstant != null ? firstAsConstant : secondAsConstant;
      Map<String, Pair<Integer, Integer>> term = firstAsTerm != null ? firstAsTerm : secondAsTerm;
      switch (operator) {
      case MULTIPLY:
        for (Map.Entry<String, Pair<Integer, Integer>> coeff : term.entrySet()) {
          int num = coeff.getValue().getFirst() * constant;
          int denom = coeff.getValue().getSecond();
          variableCoeffs.put(coeff.getKey(), normalizeRational(num, denom));
        }
        break;
      case DIVIDE:
        for (Map.Entry<String, Pair<Integer, Integer>> coeff : term.entrySet()) {
          int num = coeff.getValue().getFirst();
          if (num % constant == 0) {
            num = num / constant;
            constant = 1;
          }
          int denom = coeff.getValue().getSecond() * constant;
          variableCoeffs.put(coeff.getKey(), normalizeRational(num, denom));
        }
        break;
      case MINUS:
        // for minus, it is relevant if const - term or other way round
        if (term == firstAsTerm) { // term - const
          constant = -constant; // fall through with term - const transformed to -const + term
        } else { // const - term
          // Negate the so that const - term is transformed to -term + const
          for (Map.Entry<String, Pair<Integer, Integer>> coeff : term.entrySet()) {
            int num = -coeff.getValue().getFirst();
            int denom = coeff.getValue().getSecond();
            variableCoeffs.put(coeff.getKey(), normalizeRational(num, denom));
          }
        }
        term = variableCoeffs;
        //$FALL-THROUGH$
      case PLUS:
        variableCoeffs = term;
        Pair<Integer, Integer> previousConstVar = removeRationalCoefficient(variableCoeffs, "const");
        int prevNum = previousConstVar.getFirst();
        int prevDenom = previousConstVar.getSecond();
        int num = prevNum + constant * prevDenom;
        int denom = prevDenom;
        variableCoeffs.put("const", normalizeRational(num, denom));
        break;
      default:
        return null;
      }
      return variableCoeffs;
    }
    if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) expression;
      CExpression operand = unaryExpression.getOperand();
      switch (unaryExpression.getOperator()) {
      case MINUS:
        Map<String, Pair<Integer, Integer>> innerTerm = reduceToRationalTerm(operand);
        if (innerTerm != null) {
          return negateRational(innerTerm);
        } else {
          return null;
        }
      default:
        return null;
      }

    }
    return null;
  }

  /**
   * Negates the given rational linear term by negating each numerator of the coefficients.
   * @param toNegate the rational linear term to negate.
   * @return the negated rational linear term.
   */
  private Map<String, Pair<Integer, Integer>> negateRational(Map<String, Pair<Integer, Integer>> toNegate) {
    Map<String, Pair<Integer, Integer>> result = new HashMap<>();
    for (Map.Entry<String, Pair<Integer, Integer>> coeff : toNegate.entrySet()) {
      int num = -coeff.getValue().getFirst();
      int denom = coeff.getValue().getSecond();
      result.put(coeff.getKey(), Pair.of(num, denom));
    }
    return result;
  }

  /**
   * Tries to interpret the given expression as an integer constant.
   * This does not only cover integer literals but also and kind of addition,
   * subtraction, multiplication, division or modulo operation on expressions
   * interpretable as integer constants.
   *
   * @param expression the expression to convert into an integer.
   * @return the integer constant represented by the expression or <code>null</code> if the expression
   * did not represent an integer constant.
   */
  private Integer reduceToConstant(CExpression expression) {
    // If the expression is an integer literal, return its value
    if (expression instanceof CIntegerLiteralExpression) {
      CIntegerLiteralExpression literal = (CIntegerLiteralExpression) expression;
      return literal.getValue().intValue();
    }
    // If the expression is a binary expression and its operands are reducible
    // to constants and the operator is supported on constants, the operator is
    // invoked in the constants and the result is returned
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;
      CExpression op1 = binaryExpression.getOperand1();
      CExpression op2 = binaryExpression.getOperand2();
      Integer reduced1 = reduceToConstant(op1);
      Integer reduced2 = reduceToConstant(op2);
      if (reduced1 == null || reduced2 == null) { return null; }
      switch (binaryExpression.getOperator()) {
      case PLUS:
        return reduced1 + reduced2;
      case MINUS:
        return reduced1 - reduced2;
      case MULTIPLY:
        return reduced1 * reduced2;
      case DIVIDE:
        return reduced1 / reduced2;
      case MODULO:
        return reduced1 % reduced2;
      default:
        return null;
      }
    }
    // If the expression is an unary expression and its operand is reducible to
    // a constant and the operator is supported on integer constants, the
    // operator is applied to the constant and the result is returned
    if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) expression;
      Integer reducedInner = reduceToConstant(unaryExpression.getOperand());
      if (reducedInner == null) { return null; }
      switch (unaryExpression.getOperator()) {
      case MINUS:
        return -reducedInner;
      case TILDE:
        return ~reducedInner;
      default:
        return null;
      }
    }
    // If the expression is not supported, null is returned
    return null;
  }

  /**
   * Creates an LDDRegion from the given linear term and the given constant,
   * using a "less or equal" operator if the corresponding flag is true and a
   * "less than" operator otherwise.
   *
   * @param coeffs the integer linear term.
   * @param leq the flag indicating whether to use a "less or equal" or a "less than" operator.
   * @param constant the integer constant.
   * @return the LDDRegion created for the integer linear term and the constraint.
   */
  private LDDRegion toNode(Map<String, Integer> coeffs, boolean leq, int constant) {
    return this.regionManager.makeNode(toIndexCoefficients(coeffs), this.variables.size(), leq, constant);
  }

  /**
   * Creates an LDDRegion for an assignment transformed to the given integer linear term and constant-
   *
   * @param variables the integer linear term containing all the variable coefficients of the assignment expression.
   * @param constant the constant part of the assignment expression.
   * @return the LDDRegion created for the assignment.
   */
  private LDDRegion toConstantAssignmentRegion(Collection<String> variables, int constant) {
    return this.regionManager.makeConstantAssignment(toIndexCoefficients(variables), this.variables.size(),
        constant);
  }

  /**
   * Creates an LDDRegion for an assignment transformed to the given integer linear term and constant-
   *
   * @param coeffs the integer linear term containing all the variable coefficients of the assignment expression.
   * @param constant the constant part of the assignment expression.
   * @return the LDDRegion created for the assignment.
   */
  private LDDRegion toConstantAssignmentRegion(Map<String, Integer> coeffs, int constant) {
    return this.regionManager.makeConstantAssignment(toIndexCoefficients(coeffs), this.variables.size(),
        constant);
  }

  /**
   * Converts the given integer linear term into a collection of pairs of variable indices
   * and their coefficients.
   *
   * @param pCoeffs the integer linear term to convert.
   * @return a collection of variable indices and their corresponding integer coefficients.
   */
  private Collection<Pair<Integer, Integer>> toIndexCoefficients(Map<String, Integer> pCoeffs) {
    Collection<Pair<Integer, Integer>> indexCoeffs = new LinkedList<>();
    for (Map.Entry<String, Integer> coeff : pCoeffs.entrySet()) {
      indexCoeffs.add(Pair.of(this.variables.get(coeff.getKey()), coeff.getValue()));
    }
    return indexCoeffs;
  }

  /**
   * Converts the given collection of variables into a collection of pairs of variable indices
   * and their coefficients, all of which are treated as 1.
   *
   * @param variables the collection of variable names.
   * @return a collection of pairs of variable indices and their coefficients,
   * all of which are treated as 1.
   */
  private Collection<Pair<Integer, Integer>> toIndexCoefficients(Collection<String> variables) {
    Collection<Pair<Integer, Integer>> indexCoeffs = new LinkedList<>();
    for (String variable : variables) {
      indexCoeffs.add(Pair.of(this.variables.get(variable), 1));
    }
    return indexCoeffs;
  }

  /**
   * Converts the given declaration into an LDDRegion.
   *
   * @param declaration the declaration to convert.
   * @return the converted LDDRegion.
   */
  private LDDRegion toRegion(CDeclaration declaration, LDDRegion previousRegion) {
    String variable = declaration.getName();
    if (this.variables.containsKey(variable)) {
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration varDecl = (CVariableDeclaration) declaration;
        CInitializer init = varDecl.getInitializer();
        if (init instanceof CInitializerExpression) {
          return assign(variable, ((CInitializerExpression) init).getExpression(), previousRegion);
        } else if (init instanceof CInitializerList) {
          // TODO currently not supported
        }
      }

    }
    return this.regionManager.makeTrue();
  }
}
