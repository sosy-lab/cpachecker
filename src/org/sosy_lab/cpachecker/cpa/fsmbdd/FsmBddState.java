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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.UnrecognizedSyntaxException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.VariableDeclarationException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Preconditions;

/**
 * Abstract state that gets represented by a binary decision diagram.
 */
public class FsmBddState implements AbstractState {

  private static Map<String, BDDDomain> declaredVariables = new HashMap<String, BDDDomain>();
  public static ExpressionCache expressionCache = new ExpressionCache();
  public static FsmBddStatistics statistic;

  /**
   * Reference to the instance of the BDD library.
   */
  private final BDDFactory bddFactory;

  /**
   * The BDDs that represent the state.
   */
  private BDD stateBdd;

  private SortedMap<Integer, CExpression> conditionBlock;
  private int encodedAssumptions;
  private final CFANode cfaNode;
  private FsmBddState mergedInto;

  /**
   * Constructor.
   */
  public FsmBddState(BDDFactory pBddFactory, CFANode pCfaNode) {
    this.bddFactory = pBddFactory;

    // Initially, the state is TRUE;
    // this means that every possible error state would be reachable.
    this.stateBdd = bddFactory.one();
    this.conditionBlock = null;
    this.encodedAssumptions = 0;
    this.cfaNode = pCfaNode;
  }

  /**
   * Disjunct the BDD of this state
   * with the BDD of another state.
   * (= JOIN)
   *
   * @param pOtherState
   */
  public void disjunctStateBddWith(FsmBddState pOtherState) {
    statistic.disjunctStateBddTimer.start();
    this.stateBdd = this.stateBdd.or(pOtherState.getStateBdd());
    statistic.disjunctStateBddTimer.stop();
  }

  /**
   * Return the BDD that represents the state.
   */
  public BDD getStateBdd() {
    return stateBdd;
  }

  public int getEncodedAssumptions() {
    return this.encodedAssumptions;
  }

  /**
   *  Declare the given variable.
   *  The domain of the variable gets initialized.
   */
  public BDDDomain declareGlobal(String pScopedVariableName, int pDomainSize) throws VariableDeclarationException {
    BDDDomain varDomain = declaredVariables.get(pScopedVariableName);
    if (varDomain != null) {
      // A re-declaration of a variable means that
      // we must forget its current value;
      // this is done by existential quantification.
      //
      // This might happen because our input programs might
      // use block-scope but we only support function scope.
      //
      stateBdd = stateBdd.exist(varDomain.set());
    } else {
      // Declare a new domain for the given variable.
      varDomain = bddFactory.extDomain(pDomainSize);
      varDomain.setName(pScopedVariableName);
      declaredVariables.put(pScopedVariableName, varDomain);
    }

    return varDomain;
  }

  /**
   * Undefine (but not undeclare) a given variable.
   * (forget the current value of the variable)
   * The variable gets existential quantified in the BDD of the state.
   */
  public void undefineVariable(String pScopedVariableName) {
    BDDDomain varDomain = declaredVariables.get(pScopedVariableName);
    statistic.undefineVarInBddTimer.start();
    stateBdd = stateBdd.exist(varDomain.set());
    statistic.undefineVarInBddTimer.stop();
  }

  /**
   * Return the domain of a given variable.
   * A domain represents the set of bits that are used to encode
   * a possible value of one variable.
   */
  public BDDDomain getGlobalVariableDomain(String pVariableName) throws VariableDeclarationException {
    BDDDomain varDomain = declaredVariables.get(pVariableName);
    if (varDomain == null) {
      throw new VariableDeclarationException("Variable " + pVariableName + " not declared.");
    } else {
      return varDomain;
    }
  }

  public void conjunctToConditionBlock(CExpression conjunctWith) throws UnrecognizedSyntaxException {
    encodedAssumptions++;
    if (conditionBlock == null) {
      conditionBlock = new TreeMap<Integer, CExpression>();
    }

    Integer conditionLine = conjunctWith.getFileLocation().getStartingLineNumber();
    CExpression lineCondition = conditionBlock.get(conditionLine);

    if (lineCondition == null) {
      lineCondition = expressionCache.lookupCachedExpressionVersion(conjunctWith);
    } else {
      CExpression left = conjunctWith;
      CExpression right = lineCondition;
      BinaryOperator op = BinaryOperator.LOGICAL_AND;

      lineCondition = expressionCache.binaryExpression(op, left, right);
    }

    conditionBlock.put(conditionLine, lineCondition);
  }

  public void disjunctConditionBlocks(SortedMap<Integer, CExpression> disjunctWith) throws UnrecognizedSyntaxException {
    if (disjunctWith == null || disjunctWith.size() == 0) {
      this.resetConditionBlock();
      return;
    }

    encodedAssumptions++;
    if (conditionBlock == null) {
      conditionBlock = new TreeMap<Integer, CExpression>();
    }

    for (Integer conditionLine : disjunctWith.keySet()) {
      CExpression thisLineCondition = conditionBlock.get(conditionLine);
      CExpression otherLineCondition = disjunctWith.get(conditionLine);

      if (thisLineCondition == null) {
        thisLineCondition = expressionCache.lookupCachedExpressionVersion(otherLineCondition);
      } else {
        CExpression left = thisLineCondition;
        CExpression right = otherLineCondition;
        BinaryOperator op = BinaryOperator.LOGICAL_OR;

        thisLineCondition = expressionCache.binaryExpression(op, left, right);
      }

      conditionBlock.put(conditionLine, thisLineCondition);
    }
  }

  public SortedMap<Integer, CExpression> getConditionBlock() {
    return conditionBlock;
  }

  public boolean getConditionBlockIsTrue() {
    return conditionBlock == null || conditionBlock.size() == 0;
  }

  public void resetConditionBlock() {
    this.conditionBlock = null;
    this.encodedAssumptions = 0;
  }

  /**
   * Modify the state by conjuncting (AND) the
   * BDD of the state with the given BDD.
   */
  public void conjunctStateWith(BDD bdd) {
    statistic.conjunctStateBddTimer.start();
    stateBdd = stateBdd.and(bdd);
    statistic.conjunctStateBddTimer.stop();
  }

  /**
   * Modify the state: Assign a new value to the given variable.
   * After an existential quantification of the old value, we conjunct
   * the BDD of the state with the new value.
   *
   * "domainIntervalProvider" is given as argument to keep the state object small.
   *
   */
  public void assingConstantToVariable(String pVariableName, DomainIntervalProvider domainIntervalProvider, CExpression pValue) throws CPATransferException {
    int literalIndex = domainIntervalProvider.mapLiteralToIndex(pValue);
    BDDDomain variableDomain = getGlobalVariableDomain(pVariableName);

    statistic.assignToVarTimer.start();
    stateBdd = stateBdd.exist(variableDomain.set()).and(variableDomain.ithVar(literalIndex));
    statistic.assignToVarTimer.stop();
  }

  /**
   * Modify the state: Assign a new value to the given variable based on the value of another variable.
   * After an existential quantification of the old value, we conjunct
   * the BDD of the state with the equality between the variables.
   */
  public void assignVariableToVariable(String pSourceVariable, String pTargetVariable) throws VariableDeclarationException {
    BDDDomain targetDomain = getGlobalVariableDomain(pTargetVariable);
    BDDDomain sourceDomain = getGlobalVariableDomain(pSourceVariable);

    statistic.assignToVarTimer.start();
    stateBdd = stateBdd.exist(targetDomain.set()).and(sourceDomain.buildEquals(targetDomain));
    statistic.assignToVarTimer.stop();
  }

   /**
   * Create a copy (new instance) of the state.
   */
  public FsmBddState cloneState(CFANode pCfaNode) {
    FsmBddState result = new FsmBddState(bddFactory, pCfaNode);
    result.stateBdd = this.stateBdd;
    result.conditionBlock = this.conditionBlock == null ? null : new TreeMap<Integer, CExpression>(this.conditionBlock);
    result.encodedAssumptions = this.encodedAssumptions;

    return result;
  }

  public CFANode getCfaNode() {
    return this.cfaNode;
  }

  FsmBddState getMergedInto() {
    return mergedInto;
  }

  void setMergedInto(FsmBddState pMergedInto) {
    Preconditions.checkNotNull(pMergedInto);
    mergedInto = pMergedInto;
  }


  /**
   * Create a string-representation of the state.
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();

    String stateBddText;
    int bddNodes = stateBdd.nodeCount();
    if (bddNodes > 25) {
      stateBddText = String.format("BDD %d with %d nodes.", stateBdd.hashCode(), bddNodes);
    } else {
      stateBddText = stateBdd.toStringWithDomains();
    }

    String condText;
    if (encodedAssumptions > 30) {
      condText = String.format("AST with %d assumptions", encodedAssumptions);
    } else if (conditionBlock == null) {
      condText = "[]";
    } else {
      condText = "";
      for (Integer line: conditionBlock.keySet()) {
        CExpression lineCondition = conditionBlock.get(line);
        condText += String.format("(%d) %s\n", line, lineCondition.accept(new ExpressionToString()));
      }
    }

    result.append("\n");
    result.append(String.format(" | %15s : %s\n", "Node", cfaNode.getNodeNumber()));
    result.append(String.format(" | %15s : %s\n", "BDD", stateBddText));
    result.append(String.format(" | %15s : [%d] %s\n", "Condition", encodedAssumptions, condText));

    return result.toString();
  }

  public boolean condBlockEqualToBlockOf(FsmBddState pOther) {
    SortedMap<Integer, CExpression> thisBlock = this.getConditionBlock();
    SortedMap<Integer, CExpression> otherBlock = pOther.getConditionBlock();

    if (thisBlock == null && otherBlock == null) {
      return true;
    }

    if (thisBlock == null || otherBlock == null) {
      return false;
    }

    if (thisBlock.size() == otherBlock.size()) {
      for (Integer key: thisBlock.keySet()) {
        CExpression otherExpr = otherBlock.get(key);
        if (otherExpr == null) {
          return false;
        } else {
          CExpression thisExpr = thisBlock.get(key);
          if (!expressionCache.expressionsEqual(thisExpr, otherExpr)) {
            return false;
          }
        }
      }

      return true;
    }

    return false;
  }

  public boolean conditionsLessOrEqual(FsmBddState pOther) {
    if (pOther.getConditionBlockIsTrue()) {
      return true;
    }

    if (this.getConditionBlockIsTrue()) {
      return false;
    }

    SortedMap<Integer, CExpression> thisBlock = this.getConditionBlock();
    SortedMap<Integer, CExpression> otherBlock = pOther.getConditionBlock();

    if (thisBlock.size() == otherBlock.size()) {
      for (Integer key: thisBlock.keySet()) {
        CExpression otherExpr = otherBlock.get(key);
        CExpression thisExpr = thisBlock.get(key);
        boolean exprIsLessOrEqual = false;

        if (otherExpr == null) {
          return false;
        } else {
          if (thisExpr instanceof CBinaryExpression) {
            CBinaryExpression s1cond = (CBinaryExpression) thisExpr;
            if (s1cond.getOperator() == BinaryOperator.LOGICAL_AND) {
              if (s1cond.getOperand1() == otherExpr
               || s1cond.getOperand2() == otherExpr) {
                exprIsLessOrEqual = true;
              }
            }
          }

          if (!exprIsLessOrEqual) {
            if (otherExpr instanceof CBinaryExpression) {
              CBinaryExpression s2cond = (CBinaryExpression) otherExpr;
              if (s2cond.getOperator() == BinaryOperator.LOGICAL_OR) {
                if (s2cond.getOperand1() == thisExpr
                 || s2cond.getOperand2() == thisExpr) {
                  exprIsLessOrEqual = true;
                }
              }
            }
          }

          if (!exprIsLessOrEqual) {
            if (expressionCache.expressionsEqual(thisExpr, otherExpr)) {
              exprIsLessOrEqual = true;
            }
          }
        }

        if (!exprIsLessOrEqual) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

}
