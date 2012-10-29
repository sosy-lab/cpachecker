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

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.VariableDeclarationException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Abstract state that gets represented by a binary decision diagram.
 */
public class FsmState implements AbstractState {

  private static Map<String, BDDDomain> declaredVariables = new HashMap<String, BDDDomain>();
  private static ExpressionCache expressionCache = new ExpressionCache();

  /**
   * Reference to the instance of the BDD library.
   */
  private final BDDFactory bddFactory;

  /**
   * The BDDs that represent the state.
   */
  private BDD stateBdd;

  private CExpression conditionBlock;

  /**
   * Constructor.
   */
  public FsmState(BDDFactory pBddFactory) {
    this.bddFactory = pBddFactory;

    // Initially, the state is TRUE;
    // this means that every possible error state would be reachable.
    this.stateBdd = bddFactory.one();
    this.conditionBlock = null;
  }

  /**
   * Disjunct the BDD of this state
   * with the BDD of another state.
   * (= JOIN)
   *
   * @param pOtherState
   */
  public void disjunctWithState(FsmState pOtherState) {
    this.stateBdd = this.stateBdd.or(pOtherState.getStateBdd());
  }

  /**
   * Return the BDD that represents the state.
   */
  public BDD getStateBdd() {
    return stateBdd;
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
    stateBdd = stateBdd.exist(varDomain.set());
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

  public void addToConditionBlock(CExpression conjunctWith, boolean conjunction) {
    if (conditionBlock == null) {
      conditionBlock = conjunctWith;
    } else {
      CExpression left = conjunctWith;
      CExpression right = conditionBlock;
      BinaryOperator op = conjunction ? BinaryOperator.LOGICAL_AND : BinaryOperator.LOGICAL_OR;

      conditionBlock = expressionCache.fetchCachedBinExpression(left, op, right);
    }
  }

  public CExpression getConditionBlock() {
    return conditionBlock;
  }

  public void resetConditionBlock() {
    this.conditionBlock = null;
  }

  /**
   * Modify the state by conjuncting (AND) the
   * BDD of the state with the given BDD.
   */
  public void conjunctStateWith(BDD bdd) {
    stateBdd = stateBdd.and(bdd);
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

    stateBdd = stateBdd.exist(variableDomain.set()).and(variableDomain.ithVar(literalIndex));
  }

  /**
   * Modify the state: Assign a new value to the given variable based on the value of another variable.
   * After an existential quantification of the old value, we conjunct
   * the BDD of the state with the equality between the variables.
   */
  public void assignVariableToVariable(String pSourceVariable, String pTargetVariable) throws VariableDeclarationException {
    BDDDomain targetDomain = getGlobalVariableDomain(pTargetVariable);
    BDDDomain sourceDomain = getGlobalVariableDomain(pSourceVariable);

    stateBdd = stateBdd.exist(targetDomain.set()).and(sourceDomain.buildEquals(targetDomain));
  }

   /**
   * Create a copy (new instance) of the state.
   */
  public FsmState cloneState() {
    FsmState result = new FsmState(bddFactory);
    result.stateBdd = this.stateBdd;
    result.conditionBlock = this.conditionBlock;

    return result;
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
      stateBddText = String.format("BDD with %d nodes.", bddNodes);
    } else {
      stateBddText = stateBdd.toStringWithDomains();
    }
    result.append(String.format(" | %15s : %s\n", "State", stateBddText));

    return result.toString();
  }


}
