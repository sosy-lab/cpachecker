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
package org.sosy_lab.cpachecker.cpa.value.symbolic.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Alias creator that returns all possible aliases for two sets of symbolic values.
 */
public class RealAliasCreator implements AliasCreator {

  @Override
  public Set<Environment> getPossibleAliases(
      final Collection<? extends SymbolicValue> pFirstValues,
      final Collection<? extends SymbolicValue> pSecondValues
  ) {

    Set<Environment> possibleEnvironments = new HashSet<>();
    possibleEnvironments.add(new Environment()); // initial environment that allows everything

    for (SymbolicValue v : pSecondValues) {
      possibleEnvironments = getPossibleAliases(v, pFirstValues, possibleEnvironments);

      if (possibleEnvironments.isEmpty()) {
        return possibleEnvironments;
      }
    }

    return possibleEnvironments;
  }

  private Set<Environment> getPossibleAliases(
      final SymbolicValue pExpressionToGetAliasFor,
      final Collection<? extends SymbolicValue> pPoolOfExpressionsToGetAliasesOf,
      final Set<Environment> pCurrentPossibleAliases
  ) {

    Set<Environment> newEnvironments = new HashSet<>();

    for (Environment e : pCurrentPossibleAliases) {
      for (SymbolicValue v : pPoolOfExpressionsToGetAliasesOf) {

        Environment newEnv = hasSameMeaning((SymbolicExpression) pExpressionToGetAliasFor,
            (SymbolicExpression) v,
            e);

        if (newEnv == null) {
          continue;

        } else {
          newEnv.addCounterpart(pExpressionToGetAliasFor, v);
          newEnvironments.add(newEnv);
        }
      }
    }

    return newEnvironments;
  }

  /**
   * Checks whether two symbolic expressions represent the same expression, just using different
   * symbolic identifiers.
   *
   * @param pExpressionOfFirstState the first expression
   * @param pExpressionOfSecondState the second expression that could be an alias of the first one
   * @param pEnvironment the environment to use as basis for the check
   * @return <code>null</code> if the two expressions can't be aliases in the given environment,
   *    a new environment in which the two expressions are aliases, otherwise
   */
  // we use these strange parameter names for the symbolic expressions so we don't confuse
  // the order of symbolic expressions in the algorithm above
  private Environment hasSameMeaning(
      final SymbolicExpression pExpressionOfFirstState,
      final SymbolicExpression pExpressionOfSecondState,
      final Environment pEnvironment
  ) {

    // Expressions can't be aliases of each other if they don't have the same return type
    if (!pExpressionOfFirstState.getType().equals(pExpressionOfSecondState.getType())) {
      return null;
    }

    // Expressions can't be aliases of each other if they don't represent the same type of
    // expression
    if (!pExpressionOfFirstState.getClass().equals(pExpressionOfSecondState.getClass())) {
      return null;
    }

    if (pEnvironment.hasCounterpart(pExpressionOfFirstState)) {
      if (SymbolicValues.representSameSymbolicMeaning(
          pEnvironment.getCounterpart(pExpressionOfFirstState), pExpressionOfSecondState)) {

        // If the given second expression already is the counterpart of the first one in the given
        // environment, we don't have to do anything else
        return pEnvironment;
      } else {
        return null;
      }
    }

    // if the second expression already is the counterpart of another expression, it can't be a
    // counterpart for the first expression (since the relation has to be bijective)
    if (pEnvironment.isCounterpart(pExpressionOfSecondState)) {
      return null;
    }

    // at this point we already made sure that both expressions are of the same type, so we only
    // have to check their operands now
    if (pExpressionOfFirstState instanceof UnarySymbolicExpression) {
      assert pExpressionOfSecondState instanceof UnarySymbolicExpression;

      SymbolicExpression e1Op = ((UnarySymbolicExpression) pExpressionOfFirstState).getOperand();
      SymbolicExpression e2Op = ((UnarySymbolicExpression) pExpressionOfSecondState).getOperand();

      return hasSameMeaning(e1Op, e2Op, pEnvironment);

    } else if (pExpressionOfFirstState instanceof BinarySymbolicExpression) {
      assert pExpressionOfSecondState instanceof BinarySymbolicExpression;

      BinarySymbolicExpression e1AsBin = (BinarySymbolicExpression) pExpressionOfFirstState;
      BinarySymbolicExpression e2AsBin = (BinarySymbolicExpression) pExpressionOfSecondState;

      SymbolicExpression e1Op1 = e1AsBin.getOperand1();
      SymbolicExpression e1Op2 = e1AsBin.getOperand2();

      SymbolicExpression e2Op1 = e2AsBin.getOperand1();
      SymbolicExpression e2Op2 = e2AsBin.getOperand2();

      // check whether the first operands of the first and second expression are aliases
      Environment resultOfFirstOperands = hasSameMeaning(e1Op1, e2Op1, pEnvironment);

      if (resultOfFirstOperands == null) {
        return null;
      }

      // check whether the second operands of the first and second expression are aliases
      Environment resultOfSndOperands = hasSameMeaning(e1Op2, e2Op2, pEnvironment);

      if (resultOfSndOperands == null) {
        return null;
      }

      // combine the information of both independent checks
      return resultOfFirstOperands.join(resultOfSndOperands);

    } else {
      assert pExpressionOfFirstState instanceof ConstantSymbolicExpression;
      assert pExpressionOfSecondState instanceof ConstantSymbolicExpression;

      Value e1Val = ((ConstantSymbolicExpression) pExpressionOfFirstState).getValue();
      Value e2Val = ((ConstantSymbolicExpression) pExpressionOfSecondState).getValue();

      // Compare the values
      if (e1Val instanceof SymbolicIdentifier && e2Val instanceof SymbolicIdentifier) {
        final SymbolicIdentifier id1 = (SymbolicIdentifier) e1Val;
        final SymbolicIdentifier id2 = (SymbolicIdentifier) e2Val;

        if (pEnvironment.hasAlias(id1)) {
          if (SymbolicValues.representSameSymbolicMeaning(pEnvironment.getAlias(id1), id2)) {
            return pEnvironment;
          } else {
            return null;
          }

        } else {
          Environment newEnv = new Environment(pEnvironment);
          newEnv.addAlias(id1, id2);

          return newEnv;
        }

      } else if (e1Val.equals(e2Val)) {
        return pEnvironment;

      } else {
        return null;
      }
    }
  }
}
