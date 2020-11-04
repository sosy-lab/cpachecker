// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.numericdomains.Manager;
import org.sosy_lab.numericdomains.Value;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.Value.ValueType;
import org.sosy_lab.numericdomains.coefficients.Interval;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

/**
 * Defines a state which uses values abstracted by an numeric domain.
 *
 * <p>The type of the domain is specified by the {@link NumericState#manager}
 */
public class NumericState implements AbstractState, LatticeAbstractState<NumericState> {
  private static final String NON_EMPTY_VARIABLE_NAME = "__NON_EMPTY_VARIABLE__";

  private final Manager manager;
  private final Value value;
  private final LogManager logger;
  private final boolean isLoopHead;

  /** Creates a NumericState with the given value. */
  NumericState(Value pValue, LogManager pLogManager) {
    this(pValue, pLogManager, false);
  }

  /** Creates a NumericState with the given value and sets the loop head flag. */
  NumericState(Value pValue, LogManager pLogManager, boolean pIsLoopHead) {
    value = pValue;
    manager = pValue.getManager();
    logger = pLogManager;
    isLoopHead = pIsLoopHead;
  }

  /**
   * Creates a default NumericState using the numeric manager.
   *
   * <p>The default state will be a top element of the domain.
   *
   * @param numericManager manager of the chosen numeric domain
   */
  public NumericState(Manager numericManager, LogManager logManager) {
    manager = numericManager;
    // The variable is needed, because a value with an empty environment is always a bottom element.
    Variable nonEmptyVariable = new Variable(NON_EMPTY_VARIABLE_NAME);
    Environment environment = new Environment(new Variable[] {nonEmptyVariable}, new Variable[] {});
    logger = logManager;
    value = new Value(manager, environment, ValueType.TOP);
    isLoopHead = false;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof NumericState) {
      NumericState otherState = (NumericState) other;
      return manager.equals(otherState.manager) && value.equals(otherState.value);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(manager, value);
  }

  @Override
  public String toString() {
    return "NumericState: " + value.toPrettyString(false);
  }

  @Override
  public NumericState join(NumericState pState2) {
    Optional<Environment> leastCommonEnvironment =
        Environment.createLeastCommonEnvironment(
            value.getEnvironment(), pState2.getValue().getEnvironment());

    if (leastCommonEnvironment.isEmpty()) {
      throw new AssertionError(
          "States do not have a least common environment: "
              + value.getEnvironment()
              + " and "
              + pState2.getValue().getEnvironment());
    }

    // This is an approximation that is not perfect.
    // All variables that are only in one of the states will be unconstrained after the join.
    // There is no simple way of setting variables to unsatisfiable in a value.
    Optional<Value> extendedA =
        value.changeEnvironment(leastCommonEnvironment.get(), NewVariableValue.UNCONSTRAINED);
    Optional<Value> extendedB =
        pState2
            .getValue()
            .changeEnvironment(leastCommonEnvironment.get(), NewVariableValue.UNCONSTRAINED);

    Optional<Value> join;

    if (extendedA.isPresent() && extendedB.isPresent()) {
      join = extendedA.get().join(extendedB.get());
    } else {
      join = Optional.empty();
    }

    extendedA.ifPresent(Value::dispose);
    extendedB.ifPresent(Value::dispose);

    if (join.isPresent()) {
      return new NumericState(join.get(), logger);
    } else {
      throw new IllegalStateException("Could not compute the widening of the two states.");
    }
  }

  /**
   * Computes the widening of the values in the two states.
   *
   * @param pState2 state with which the widening is computed
   * @return State containing the result of the widening as value
   */
  public NumericState widening(NumericState pState2) {
    Optional<Environment> leastCommonEnvironment =
        Environment.createLeastCommonEnvironment(
            value.getEnvironment(), pState2.getValue().getEnvironment());

    if (leastCommonEnvironment.isEmpty()) {
      throw new AssertionError(
          "States do not have a least common environment: "
              + value.getEnvironment()
              + " and "
              + pState2.getValue().getEnvironment());
    }

    // This is an approximation that is not perfect.
    // All variables that are only in one of the states will be unconstrained after the join.
    // There is no simple way of setting variables to unsatisfiable in a value.
    Optional<Value> extendedA =
        value.changeEnvironment(leastCommonEnvironment.get(), NewVariableValue.UNCONSTRAINED);
    Optional<Value> extendedB =
        pState2
            .getValue()
            .changeEnvironment(leastCommonEnvironment.get(), NewVariableValue.UNCONSTRAINED);

    Optional<Value> newValue;

    if (extendedA.isPresent() && extendedB.isPresent()) {
      newValue = extendedA.get().widening(extendedB.get());
    } else {
      newValue = Optional.empty();
    }

    extendedA.ifPresent(Value::dispose);
    extendedB.ifPresent(Value::dispose);

    if (newValue.isPresent()) {
      return new NumericState(newValue.get(), logger);
    } else {
      throw new IllegalStateException("Could not compute the widening of the two states.");
    }
  }

  /**
   * Computes the meet between the state and the given value.
   *
   * @param pMeetValue value with which the meet is computed
   * @return state containing the meet as value
   */
  public Optional<NumericState> meet(Value pMeetValue) {
    Optional<Value> val = value.meet(pMeetValue);
    return val.map(pValue -> new NumericState(pValue, logger));
  }

  /**
   * Computes the meet between the state and the constraints.
   *
   * @param pConstraints constraints with which the meet is computed
   * @return state containing the meet as value, empty if the resulting value is the bottom value
   */
  public Optional<NumericState> meetConstraints(Collection<TreeConstraint> pConstraints) {
    Optional<Value> newAbs = value.meet(pConstraints.toArray(TreeConstraint[]::new));
    if (newAbs.isPresent()) {
      if (!newAbs.get().isBottom()) {
        return Optional.of(new NumericState(newAbs.get(), logger));
      } else {
        // If the new Value is the bottom value it can be ignored
        newAbs.get().dispose();
      }
    }
    return Optional.empty();
  }

  /**
   * This method works like {@see meetConstraints}.
   *
   * <p>This method is necessary because the meet with TreeConstraints is sometimes not correctly
   * computed, if an epsilon value is used.
   */
  public Optional<NumericState> meetEpsilon(Collection<TreeConstraint> pConstraints) {
    // Fix problem where  meet with TreeConstraints is not computed correctly
    // Instead create a value from the constraints and compute the meet between the values
    if (manager instanceof org.sosy_lab.numericdomains.elina.ZonesManager
        || manager instanceof org.sosy_lab.numericdomains.elina.OctagonManager) {
      Value temp =
          new Value(
              value.getManager(),
              value.getEnvironment(),
              pConstraints.toArray(TreeConstraint[]::new));
      Optional<NumericState> newState = meet(temp);
      temp.dispose();
      return newState;
    } else {
      return meetConstraints(pConstraints);
    }
  }

  @Override
  public boolean isLessOrEqual(NumericState pState2) {
    ReducedValues reducedValues = ReducedValues.reduceValues(value, pState2.getValue());
    if (reducedValues.reducedB.isPresent() && reducedValues.reducedA.isPresent()) {
      boolean isLeq = reducedValues.reducedA.get().isLessOrEqual(reducedValues.reducedB.get());
      reducedValues.reducedA.get().dispose();
      reducedValues.reducedB.get().dispose();
      return isLeq;
    } else {
      if (reducedValues.reducedB.isPresent()) {
        reducedValues.reducedB.get().dispose();
      } else {
        reducedValues.reducedA.get().dispose();
      }
      return false;
    }
  }

  /**
   * Adds a copy of the variable with all its constraints.
   *
   * @param pVariable variable that should be copied
   * @param copy variable to which the value is copied
   * @return state with the copy added to the variables
   */
  public NumericState addTemporaryCopyOf(Variable pVariable, Variable copy) {
    Optional<Value> expandedValue = value.expand(pVariable, Set.of(copy));

    if (expandedValue.isPresent()) {
      return new NumericState(expandedValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not add a copy of the variable " + pVariable + " to the value." + value);
    }
  }

  /**
   * Add the variables to the state.
   *
   * @param intVariables variables that will be added as integer variables
   * @param realVariables variables that will be added as real varibles
   * @param initialValue initial value of the new variables
   * @return state with the added variables
   */
  public NumericState addVariables(
      Collection<Variable> intVariables,
      Collection<Variable> realVariables,
      NewVariableValue initialValue) {
    Optional<Value> newValue = value.addVariables(intVariables, realVariables, initialValue);

    if (newValue.isPresent()) {
      return new NumericState(newValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not add the integer variables "
              + Arrays.toString(intVariables.toArray(Variable[]::new))
              + " and the real variables "
              + Arrays.toString(realVariables.toArray(Variable[]::new))
              + " to the value "
              + value);
    }
  }

  /**
   * Removes the variables from the environment.
   *
   * @param pVariables variables that will be removed from the environment
   * @return state with the variables removed
   */
  public NumericState removeVariables(Collection<Variable> pVariables) {
    Optional<Value> newValue = value.removeVariables(pVariables);

    if (newValue.isPresent()) {
      return new NumericState(newValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not remove the variables "
              + Arrays.toString(pVariables.toArray(Variable[]::new))
              + " from value "
              + value.toPrettyString(false));
    }
  }

  /**
   * Forgets the value of the variables.
   *
   * @param variables variables of which the value will be forgotten
   * @param newVariableValue value that is assigned to the variables
   * @return state after setting the variable values
   */
  public NumericState forget(ImmutableSet<Variable> variables, NewVariableValue newVariableValue) {
    Optional<Value> newAbstractValue = value.forgetAll(variables, newVariableValue);
    if (newAbstractValue.isPresent()) {
      return new NumericState(newAbstractValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not forget value of variables " + variables + " in value " + value);
    }
  }

  public NumericState assignTreeExpression(Variable variable, TreeNode expression) {
    Optional<Value> newValue = value.assign(variable, expression);

    if (newValue.isPresent()) {
      return new NumericState(newValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not assign expression to variable. variable"
              + variable
              + " expression:"
              + expression);
    }
  }

  /**
   * Creates a copy of the numeric state.
   *
   * @return a copy of the state containing a copy of the value
   */
  public NumericState deepCopy() {
    return new NumericState(getValue().copy(), logger, isLoopHead);
  }

  /** Returns the value representing the state. */
  public Value getValue() {
    return value;
  }

  /** Returns the manager used with the value. */
  public Manager getManager() {
    return manager;
  }

  /** Checks whether the state contains a bottom value. */
  public boolean isBottom() {
    if (manager instanceof org.sosy_lab.numericdomains.apron.BoxManager) {
      Interval[] intervals = value.toBox();
      for (Interval interval : intervals) {
        if (interval.isEmpty()) {
          return true;
        }
      }
      return false;
    } else {
      return value.isBottom();
    }
  }

  /**
   * Returns a state equal to this state with loop head set to true.
   *
   * <p>This does not copy the value of the state. Disposing the value of the original state will
   * dispose the value in the copy.
   */
  NumericState getAsLoopHead() {
    return new NumericState(value, logger, true);
  }

  /** Checks whether this state is a loop head. */
  public boolean isLoopHead() {
    return isLoopHead;
  }

  private static class ReducedValues {
    private final Optional<Value> reducedA;
    private final Optional<Value> reducedB;

    public ReducedValues(Optional<Value> pNewValueA, Optional<Value> pNewValueB) {
      reducedA = pNewValueA;
      reducedB = pNewValueB;
    }

    private static Set<Variable> getAllVariables(Environment pEnvironment) {
      ImmutableSet.Builder<Variable> variables = new ImmutableSet.Builder<>();
      variables.addAll(Arrays.asList(pEnvironment.getIntVariables()));
      variables.addAll(Arrays.asList(pEnvironment.getRealVariables()));
      return variables.build();
    }

    private static Set<Variable> difference(Set<Variable> pVariablesA, Set<Variable> pVariablesB) {
      Set<Variable> difference = new HashSet<>(pVariablesA);
      difference.removeIf(pVariablesB::contains);
      return ImmutableSet.copyOf(difference);
    }

    private static ReducedValues reduceValues(Value pValueA, Value pValueB) {
      Set<Variable> variablesA = getAllVariables(pValueA.getEnvironment());
      Set<Variable> variablesB = getAllVariables(pValueB.getEnvironment());

      Collection<Variable> removalsA = difference(variablesA, variablesB);
      Collection<Variable> removalsB = difference(variablesB, variablesA);

      Optional<Value> newValueA;
      if (removalsA.isEmpty()) {
        newValueA = Optional.of(pValueA.copy());
      } else {
        newValueA = pValueA.removeVariables(removalsA);
      }
      Optional<Value> newValueB;
      if (removalsB.isEmpty()) {
        newValueB = Optional.of(pValueB.copy());
      } else {
        newValueB = pValueB.removeVariables(removalsB);
      }

      return new ReducedValues(newValueA, newValueB);
    }
  }
}
