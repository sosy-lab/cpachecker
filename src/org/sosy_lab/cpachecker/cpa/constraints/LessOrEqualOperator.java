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

import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsOnlyView;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

/**
 * Less-or-equal operator of the semi-lattice of the {@link ConstraintsCPA}.
 * Allows to check whether one {@link ConstraintsState} is less or equal another one
 * and whether two constraints represent the same meaning.
 *
 * <p>Constraints state <code>c</code> is less or equal <code>c'</code> if a bijective mapping
 * <code>d: SymbolicIdentifier -> SymbolicIdentifier</code> exists so that for each constraint
 * <code>e</code> in <code>c</code> a constraint <code>e'</code> in <code>c'</code> exists that
 * equals <code>e</code> after replacing each symbolic identifier <code>s</code> occurring in it
 * with <code>d(s)</code>.
 * </p>
 */
public class LessOrEqualOperator {

  private static final LessOrEqualOperator SINGLETON = new LessOrEqualOperator();

  private LessOrEqualOperator() {
    // DO NOTHING
  }

  public static LessOrEqualOperator getInstance() {
    return SINGLETON;
  }

  /**
   * Returns whether the given constraints are equal in their meaning.
   * This is the case if two constraints are completely equal after replacing symbolic expressions
   * with the program variables they represent.
   *
   * <p>Example: <code>s1 < 5</code> is equal to <code>s2 + 2 < 5</code> in respect to its meaning
   * with <code>s1</code> and <code>s2</code> being symbolic identifiers, if both constraints
   * represent <code>a < 5</code> with <code>a</code> being a program variable.</p>
   *
   * @param pValue1 the first symbolic value
   * @param pValue2 the second symbolic value
   * @return <code>true</code> if both symbolic values represent the same meaning
   */
  public boolean haveEqualMeaning(
      final SymbolicValue pValue1,
      final SymbolicValue pValue2
  ) {

    if (!pValue1.getClass().equals(pValue2.getClass())) {
      return pValue1.getRepresentedLocation().equals(pValue2.getRepresentedLocation());
    }

    final Optional<MemoryLocation> maybeRepLocVal1 = pValue1.getRepresentedLocation();
    final Optional<MemoryLocation> maybeRepLocVal2 = pValue2.getRepresentedLocation();

    if (pValue1 instanceof SymbolicIdentifier || pValue1 instanceof ConstantSymbolicExpression) {
      assert pValue2 instanceof SymbolicIdentifier || pValue2 instanceof ConstantSymbolicExpression;

      return maybeRepLocVal1.equals(maybeRepLocVal2);

    } else if (pValue1 instanceof UnarySymbolicExpression) {
      assert pValue2 instanceof UnarySymbolicExpression;

      final SymbolicValue val1Op = ((UnarySymbolicExpression) pValue1).getOperand();
      final SymbolicValue val2Op = ((UnarySymbolicExpression) pValue2).getOperand();

      return maybeRepLocVal1.equals(maybeRepLocVal2) && haveEqualMeaning(val1Op, val2Op);

    } else if (pValue1 instanceof BinarySymbolicExpression) {
      assert pValue2 instanceof BinarySymbolicExpression;

      final SymbolicValue val1Op1 = ((BinarySymbolicExpression) pValue1).getOperand1();
      final SymbolicValue val1Op2 = ((BinarySymbolicExpression) pValue1).getOperand2();
      final SymbolicValue val2Op1 = ((BinarySymbolicExpression) pValue2).getOperand1();
      final SymbolicValue val2Op2 = ((BinarySymbolicExpression) pValue2).getOperand2();

      return maybeRepLocVal1.equals(maybeRepLocVal2)
          && haveEqualMeaning(val1Op1, val2Op1)
          && haveEqualMeaning(val1Op2, val2Op2);

    } else {
      throw new AssertionError("Unhandled symbolic value type " + pValue1.getClass());
    }
  }

  /**
   * Returns whether the first state is less or equal the second state.
   *
   * <p>Constraints state <code>c</code> is less or equal <code>c'</code> if a bijective mapping
   * <code>d: SymbolicIdentifier -> SymbolicIdentifier</code> exists so that for each constraint
   * <code>e</code> in <code>c</code> a constraint <code>e'</code> in <code>c'</code> exists that
   * equals <code>e</code> after replacing each symbolic identifier <code>s</code> occurring in it
   * with <code>d(s)</code>.
   * </p>
   *
   * @param pLesserState the state that should be less or equal the second state
   * @param pBiggerState the state that should be equal or bigger the first state
   * @return <code>true</code> if the first given state is less or equal the given second state
   */
  public boolean isLessOrEqual(
      final ConstraintsState pLesserState,
      final ConstraintsState pBiggerState
  ) {

    // Get all constraints of the state, including definite assignments of symbolic identifiers.
    // This simplifies comparison between states because we don't have to look at these separately.
    final ConstraintsOnlyView allConstraintsLesserState =
        new ConstraintsOnlyView(pLesserState);
    final ConstraintsOnlyView allConstraintsBiggerState =
        new ConstraintsOnlyView(pBiggerState);

    if (allConstraintsBiggerState.size() > allConstraintsLesserState.size()) {
      return false;
    }

    // we already know that the second state has less constraints or the same amount of constraints
    // as the first state. So if it is empty, the first one has to be empty, too, and they are equal
    if (allConstraintsBiggerState.isEmpty()) {
      return true;
    }

    final Set<Environment> possibleScenarios = getPossibleAliases(allConstraintsLesserState,
        allConstraintsBiggerState);

    return !possibleScenarios.isEmpty();
  }

  public Set<Environment> getPossibleAliases(
      final Collection<? extends SymbolicValue> pValuesOfFirstState,
      final Collection<? extends SymbolicValue> pValuesOfSecondState
  ) {

    Set<Environment> possibleEnvironments = new HashSet<>();
    possibleEnvironments.add(new Environment()); // initial environment that allows everything

    for (SymbolicValue v : pValuesOfSecondState) {
      possibleEnvironments = getPossibleAliases(v, pValuesOfFirstState, possibleEnvironments);

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
      if (pEnvironment.getCounterpart(pExpressionOfFirstState).equals(pExpressionOfSecondState)) {

        // If the given second expression already is the counterpart of the first one in the given
        // environment, we don't have to do anything else
        return pEnvironment;
      } else {
        return null;
      }
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
          if (pEnvironment.getAlias(id1).equals(id2)) {
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

  /**
   * Environment for comparison of sets of symbolic values. An environment contains
   * aliases for {@link SymbolicIdentifier}s and counterparts of {@link SymbolicValue}s.
   */
  public class Environment {
    private Map<SymbolicIdentifier, SymbolicIdentifier> aliases = new HashMap<>();
    private Map<SymbolicValue, SymbolicValue> counterparts = new HashMap<>();

    Environment() { }

    Environment(final Environment pEnvironmentToClone) {
      aliases = new HashMap<>(pEnvironmentToClone.aliases);
      counterparts = new HashMap<>(pEnvironmentToClone.counterparts);
    }

    /**
     * Joins this environment with the given one.
     * The resulting environment will contain all aliases and counterparts of this and the given
     * environment.
     * If two different aliases/counterparts exist for one identifier/expression, the join of the
     * environments is impossible and <code>null</code> is returned.
     *
     * @param pOther the environment to join with this one
     * @return the join of this environment and the given one
     */
    Environment join(final Environment pOther) {
      Environment newEnv = new Environment(this);

      // add the other environment's aliases
      for (Map.Entry<SymbolicIdentifier, SymbolicIdentifier> entry : pOther.aliases.entrySet()) {
        SymbolicIdentifier key = entry.getKey();

        if (newEnv.aliases.containsKey(key) && !newEnv.aliases.get(key).equals(entry.getValue())) {
          return null;
        }

        newEnv.aliases.put(key, entry.getValue());
      }

      // add the other environment's counterparts
      for (Map.Entry<SymbolicValue, SymbolicValue> entry : pOther.counterparts.entrySet()) {
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

    public SymbolicIdentifier getAlias(final SymbolicIdentifier pIdentifier) {
      return aliases.get(pIdentifier);
    }

    /**
     * Returns whether the given symbolic value has a counterpart in this environment.
     * A counterpart can be the equal expression or any expression of the same form, but with
     * different symbolic identifiers (which have to be aliases in this environment).
     *
     * @param pExpressionOfFirstGraph the expression to check for a counterpart
     * @return <code>true</code> if a counterpart exists in this environment
     */
    public boolean hasCounterpart(final SymbolicValue pExpressionOfFirstGraph) {
      return counterparts.containsKey(pExpressionOfFirstGraph);
    }

    public boolean hasAlias(final SymbolicIdentifier pIdentifier) {
      return aliases.containsKey(pIdentifier);
    }

    public void addAlias(final SymbolicIdentifier pIdentifier, final SymbolicIdentifier pAlias) {
      aliases.put(pIdentifier, pAlias);
    }

    public void addCounterpart(final SymbolicValue pValue, final SymbolicValue pCounterpart) {
      counterparts.put(pValue, pCounterpart);
    }
  }
}
