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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.numericdomains.Manager;
import org.sosy_lab.numericdomains.Value;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.Value.ValueType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericState implements AbstractState, LatticeAbstractState<NumericState> {
  private final Manager manager;
  private final Value value;
  private final LogManager logger;

  NumericState(Manager pManager, Value pValue, LogManager logManager) {
    value = pValue;
    manager = pManager;
    logger = logManager;
  }

  /**
   * Creates a default NumericState using the numeric manager.
   *
   * <p>The default state will be a top element of the domain.
   *
   * @param numericManager manager of the chosen numeric domain
   */
  NumericState(Manager numericManager, LogManager logManager) {
    manager = numericManager;
    Environment environment = new Environment(new Variable[] {}, new Variable[] {});
    logger = logManager;
    value = new Value(manager, environment, ValueType.TOP);
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
    StringBuilder builder = new StringBuilder();
    builder.append("NumericState: ");
    builder.append(value.toPrettyString(false));
    return builder.toString();
  }

  @Override
  public NumericState join(NumericState pState2) {
    Optional<Value> val = this.value.join(pState2.value);
    if (val.isPresent()) {
      return new NumericState(manager, val.get(), logger);
    } else {
      return null;
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
    return val.map(pValue -> new NumericState(manager, pValue, logger));
  }

  /**
   * Computes the meet between the state and the constraints.
   *
   * @param pConstraints constraints with which the meet is computed
   * @return state containing the meet as value, empty if the resulting value is the bottom value
   */
  public Optional<NumericState> meet(Collection<TreeConstraint> pConstraints) {
    Optional<Value> newAbs = value.meet(pConstraints.toArray(TreeConstraint[]::new));

    if (newAbs.isPresent()) {
      if (!newAbs.get().isBottom()) {
        logger.log(
            Level.FINEST,
            value.toString(),
            "meet",
            Arrays.toString(pConstraints.toArray(TreeConstraint[]::new)),
            "=>",
            newAbs.toString());
        return Optional.of(new NumericState(manager, newAbs.get(), logger));
      } else {
        logger.log(
            Level.FINEST,
            "meet of",
            newAbs.get(),
            "and",
            Arrays.toString(pConstraints.toArray(TreeConstraint[]::new)),
            " was empty");
        // If the new Value is the bottom value it can be ignored
        newAbs.get().dispose();
      }
    } else {
      logger.log(
          Level.FINEST,
          "meet of",
          newAbs.get(),
          "and",
          Arrays.toString(pConstraints.toArray(TreeConstraint[]::new)),
          " could not be computed.");
    }
    return Optional.empty();
  }

  @Override
  public boolean isLessOrEqual(NumericState pState2) {
    return value.isLessOrEqual(pState2.value);
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
      return new NumericState(manager, expandedValue.get(), logger);
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
      return new NumericState(manager, newValue.get(), logger);
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
      return new NumericState(manager, newValue.get(), logger);
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
      return new NumericState(manager, newAbstractValue.get(), logger);
    } else {
      throw new IllegalStateException(
          "Could not forget value of variables " + variables + " in value " + value);
    }
  }

  /** Returns the value representing the state. */
  public Value getValue() {
    return value;
  }

  /** Returns the manager used with the value. */
  public Manager getManager() {
    return manager;
  }
}
