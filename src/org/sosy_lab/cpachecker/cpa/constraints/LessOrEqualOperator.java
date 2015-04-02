/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class LessOrEqualOperator {

  private static final LessOrEqualOperator SINGLETON = new LessOrEqualOperator();

  private LessOrEqualOperator() {
    // DO NOTHING
  }

  public static LessOrEqualOperator getInstance() {
    return SINGLETON;
  }

  public boolean isLessOrEqual(
      final ConstraintsState pLesserState,
      final ConstraintsState pBiggerState
  ) {

    // Get all constraints of the state, including definite assignments of symbolic identifiers.
    // This simplifies comparison between states because we don't have to look at these separately.
    final ConstraintsState.ConstraintsOnlyView allConstraintsLesserState =
        pLesserState.getConstraintsOnlyView();
    final ConstraintsState.ConstraintsOnlyView allConstraintsBiggerState =
        pBiggerState.getConstraintsOnlyView();

    if (allConstraintsBiggerState.size() > allConstraintsLesserState.size()) {
      return false;
    }

    if (allConstraintsBiggerState.isEmpty()) {
      return true;
    }

    final Set<Environment> possibleScenarios = getPossibleAliasings(allConstraintsLesserState,
                                                                    allConstraintsBiggerState);

    return !possibleScenarios.isEmpty();
  }

  public Set<Environment> getPossibleAliasings(
      final Collection<? extends SymbolicValue> pValuesOfFirstGraph,
      final Collection<? extends SymbolicValue> pValuesOfSecondGraph
  ) {

    Set<Environment> possibleScenarios = new HashSet<>();

    for (SymbolicValue v : pValuesOfSecondGraph) {
      possibleScenarios = getPossibleAliasings(v, pValuesOfFirstGraph, possibleScenarios);

      if (possibleScenarios.isEmpty()) {
        return possibleScenarios;
      }
    }

    return possibleScenarios;
  }

  private Set<Environment> getPossibleAliasings(
      final SymbolicValue pExpressionOfSecondGraph,
      final Collection<? extends SymbolicValue> pExpressionsOfFirstGraph,
      final Set<Environment> pPossibleScenarios
  ) {

    Set<Environment> oldScenarios = pPossibleScenarios;
    Set<Environment> newPossibleScenarios = new HashSet<>();

    if (pPossibleScenarios.isEmpty()) {
      oldScenarios = new HashSet<>();
      oldScenarios.add(new Environment());
    }

    for (Environment e : oldScenarios) {
      for (SymbolicValue v : pExpressionsOfFirstGraph) {

        Environment newEnv = isAlias((SymbolicExpression) v,
                                     (SymbolicExpression) pExpressionOfSecondGraph,
                                     e);

        if (newEnv == null) {
          continue;

        } else {
          newEnv.counterparts.put(pExpressionOfSecondGraph, v);
          newPossibleScenarios.add(newEnv);
        }
      }
    }

    return newPossibleScenarios;
  }

  /**
   * Checks whether two symbolic expressions represent the same expression, just using different
   * symbolic identifiers.
   *
   * @param pExpressionOfFirstGraph
   * @param pExpressionOfSecondGraph
   * @param pEnvironment
   * @return
   */
  private Environment isAlias(
      final SymbolicExpression pExpressionOfFirstGraph,
      final SymbolicExpression pExpressionOfSecondGraph,
      final Environment pEnvironment
  ) {

    if (!pExpressionOfFirstGraph.getType().equals(pExpressionOfSecondGraph.getType())) {
      return null;
    }

    if (!pExpressionOfFirstGraph.getClass().equals(pExpressionOfSecondGraph.getClass())) {
      return null;
    }

    if (pEnvironment.counterparts.containsKey(pExpressionOfFirstGraph)) {
      if (pEnvironment.counterparts.get(pExpressionOfFirstGraph).equals(pExpressionOfSecondGraph)) {
        return pEnvironment;
      } else {
        return null;
      }
    }

    if (pExpressionOfFirstGraph instanceof UnarySymbolicExpression) {
      assert pExpressionOfSecondGraph instanceof UnarySymbolicExpression;

      SymbolicExpression e1Op = ((UnarySymbolicExpression) pExpressionOfFirstGraph).getOperand();
      SymbolicExpression e2Op = ((UnarySymbolicExpression) pExpressionOfSecondGraph).getOperand();

      return isAlias(e1Op, e2Op, pEnvironment);

    } else if (pExpressionOfFirstGraph instanceof BinarySymbolicExpression) {
      assert pExpressionOfSecondGraph instanceof BinarySymbolicExpression;

      BinarySymbolicExpression e1AsBin = (BinarySymbolicExpression) pExpressionOfFirstGraph;
      BinarySymbolicExpression e2AsBin = (BinarySymbolicExpression) pExpressionOfSecondGraph;

      SymbolicExpression e1Op1 = e1AsBin.getOperand1();
      SymbolicExpression e1Op2 = e1AsBin.getOperand2();

      SymbolicExpression e2Op1 = e2AsBin.getOperand1();
      SymbolicExpression e2Op2 = e2AsBin.getOperand2();

      Environment resultOfFirstOperands = isAlias(e1Op1, e2Op1, pEnvironment);

      if (resultOfFirstOperands == null) {
        return null;
      }

      Environment resultOfSndOperands = isAlias(e1Op2, e2Op2, pEnvironment);

      if (resultOfSndOperands == null) {
        return null;
      }

      return resultOfFirstOperands.join(resultOfSndOperands);

    } else {
      assert pExpressionOfFirstGraph instanceof ConstantSymbolicExpression;
      assert pExpressionOfSecondGraph instanceof ConstantSymbolicExpression;

      Value e1Val = ((ConstantSymbolicExpression) pExpressionOfFirstGraph).getValue();
      Value e2Val = ((ConstantSymbolicExpression) pExpressionOfSecondGraph).getValue();

      if (e1Val instanceof SymbolicIdentifier && e2Val instanceof SymbolicIdentifier) {
        if (pEnvironment.aliasses.containsKey(e1Val)) {
          if (pEnvironment.aliasses.get(e1Val).equals(e2Val)) {
            return pEnvironment;
          } else {
            return null;
          }

        } else {
          Environment newEnv = new Environment(pEnvironment);
          newEnv.aliasses.put((SymbolicIdentifier) e2Val, (SymbolicIdentifier) e1Val);

          return newEnv;
        }

      } else if (e1Val.equals(e2Val)) {
        return pEnvironment;
      }
    }

    return null;
  }

  public class Environment {
    private Map<SymbolicIdentifier, SymbolicIdentifier> aliasses = new HashMap<>();
    private Map<SymbolicValue, SymbolicValue> counterparts = new HashMap<>();

    Environment() { }

    Environment(final Environment pEnvironmentToClone) {
      aliasses = new HashMap<>(pEnvironmentToClone.aliasses);
      counterparts = new HashMap<>(pEnvironmentToClone.counterparts);
    }

    Environment join(final Environment pOther) {
      Environment newEnv = new Environment(this);

      for (Map.Entry<SymbolicIdentifier, SymbolicIdentifier> entry : pOther.aliasses.entrySet()) {
        SymbolicIdentifier key = entry.getKey();

        if (newEnv.aliasses.containsKey(key) && !newEnv.aliasses.get(key).equals(entry.getValue())) {
          return null;
        }

        newEnv.aliasses.put(key, entry.getValue());
      }

      for (Map.Entry<SymbolicValue, SymbolicValue> entry
          : pOther.counterparts.entrySet()) {
        SymbolicValue key = entry.getKey();

        if (newEnv.counterparts.containsKey(key)
            && !newEnv.counterparts.get(key).equals(entry.getValue())) {
          return null;
        }

        newEnv.counterparts.put(key, entry.getValue());
      }

      return newEnv;
    }

    public SymbolicValue getCounterpart(final SymbolicValue pExp) {
      return counterparts.get(pExp);
    }

    public SymbolicIdentifier getCounterpart(final SymbolicIdentifier pIdentifier) {
      return aliasses.get(pIdentifier);
    }
  }
}
