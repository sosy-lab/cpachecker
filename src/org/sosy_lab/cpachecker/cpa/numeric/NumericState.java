// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
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

  private final ImmutableList<Frame> variableFrames;

  NumericState(
      Manager pManager, Value pValue, ImmutableList<Frame> pVariableFrames, LogManager logManager) {
    value = pValue;
    manager = pManager;
    logger = logManager;
    variableFrames = pVariableFrames;
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
    variableFrames = ImmutableList.of(new Frame(ImmutableSet.of(), ImmutableSet.of()));
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
    for (Frame frame : variableFrames) {
      builder.append(">");
      builder.append(frame.toString());
    }
    builder.append(value.toIntervalString(true));
    return builder.toString();
  }

  @Override
  public NumericState join(NumericState pState2) {
    Optional<Value> val = this.value.join(pState2.value);
    if (val.isPresent()) {
      return new NumericState(manager, val.get(), variableFrames, logger);
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
    return val.map(pValue -> new NumericState(manager, pValue, variableFrames, logger));
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
        return Optional.of(new NumericState(manager, newAbs.get(), variableFrames, logger));
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
    ImmutableList<Frame> frames = addVariablesToFrame(Set.of(copy), Set.of());

    if (expandedValue.isPresent()) {
      return new NumericState(manager, expandedValue.get(), frames, logger);
    } else {
      throw new IllegalStateException(
          "Could not add a copy of the variable " + pVariable + " to the value." + value);
    }
  }

  /**
   * Adds a new frame with the variables in it.
   *
   * @param intVariables int variables in the new frame
   * @param realVariables real variables in the new frame
   * @param newVariableValue value of the new variables
   * @return state with the frame added to it
   */
  NumericState addFrame(
      Collection<Variable> intVariables,
      Collection<Variable> realVariables,
      NewVariableValue newVariableValue) {
    ImmutableList.Builder<Frame> newVariableFramesBuilder = new ImmutableList.Builder<>();
    Frame frame = new Frame(intVariables, realVariables);
    newVariableFramesBuilder.add(frame);
    newVariableFramesBuilder.addAll(variableFrames);
    Environment newEnvironment =
        value
            .getEnvironment()
            .add(intVariables.toArray(new Variable[] {}), realVariables.toArray(new Variable[] {}));
    return new NumericState(
        manager,
        changeEnvironment(newEnvironment, newVariableValue),
        newVariableFramesBuilder.build(),
        logger);
  }

  /**
   * Adds the variables to the uppermost frame.
   *
   * @param intVariables int variables which will be added to the frame
   * @param realVariables real variables which will be added to the frame
   * @return state with the variables added to the uppermost frame
   */
  public NumericState addToFrame(
      Collection<Variable> intVariables,
      Collection<Variable> realVariables,
      NewVariableValue initialState) {
    ImmutableList<Frame> frames = addVariablesToFrame(intVariables, realVariables);

    Environment newEnvironment =
        value
            .getEnvironment()
            .add(intVariables.toArray(new Variable[0]), realVariables.toArray(new Variable[0]));
    return new NumericState(
        manager, changeEnvironment(newEnvironment, initialState), frames, logger);
  }

  private ImmutableList<Frame> addVariablesToFrame(
      Collection<Variable> intVariables, Collection<Variable> realVariables) {
    if (variableFrames.size() == 0) {
      throw new IllegalStateException("Tried to add variable to frame, but no frame exists.");
    }

    Frame newFirstFrame = variableFrames.get(0).addVariablesToFrame(intVariables, realVariables);
    ImmutableList.Builder<Frame> newFramesBuilder = new ImmutableList.Builder<>();
    newFramesBuilder.add(newFirstFrame);
    if (variableFrames.size() > 1) {
      newFramesBuilder.addAll(variableFrames.subList(1, variableFrames.size()));
    }

    return newFramesBuilder.build();
  }

  /**
   * Removes the uppermost frame and removes the variables from the environment.
   *
   * @return state without the uppermost frame
   */
  NumericState dropFrame() {
    if (variableFrames.size() == 0) {
      throw new IllegalStateException("Tried to drop frame, but no frame exists.");
    }

    ImmutableList<Frame> newVariableFrames;
    if (variableFrames.size() > 1) {
      newVariableFrames = variableFrames.subList(1, variableFrames.size());
    } else {
      newVariableFrames = ImmutableList.of();
    }
    Environment newEnvironment =
        value
            .getEnvironment()
            .remove(variableFrames.get(0).getVariables().toArray(new Variable[] {}));
    return new NumericState(
        manager,
        changeEnvironment(newEnvironment, NewVariableValue.UNCONSTRAINED),
        newVariableFrames,
        logger);
  }

  /**
   * Removes the variables from the first frame of the state.
   *
   * @param pVariables variables that should no longer be tracked
   * @return state with the variables removed
   */
  public NumericState removeFromFrame(Collection<Variable> pVariables) {
    Frame newFirstFrame = variableFrames.get(0).removeVariablesFromFrame(pVariables);
    ImmutableList.Builder<Frame> newFramesBuilder = new ImmutableList.Builder<>();
    newFramesBuilder.add(newFirstFrame);
    newFramesBuilder.addAll(variableFrames.subList(1, variableFrames.size()));
    Optional<Value> updatedValue = value.removeVariables(pVariables);

    if (updatedValue.isEmpty()) {
      throw new IllegalStateException(
          "Could not remove variables "
              + Arrays.toString(pVariables.toArray(Variable[]::new))
              + " from value "
              + value.toString());
    }

    return new NumericState(manager, updatedValue.get(), newFramesBuilder.build(), logger);
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
      return new NumericState(manager, newAbstractValue.get(), getVariableFrames(), logger);
    } else {
      throw new IllegalStateException("Could not forget value of variable.");
    }
  }

  private Value changeEnvironment(Environment newEnvironment, NewVariableValue variableHandling) {
    Optional<Value> newAbs = value.changeEnvironment(newEnvironment, variableHandling);
    if (newAbs.isEmpty()) {
      throw new IllegalStateException("Could not change environment of abstract value.");
    } else {
      return newAbs.get();
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

  /** Returns the list of frames in the state. */
  ImmutableList<Frame> getVariableFrames() {
    return variableFrames;
  }

  /**
   * A frame is a set of variables which are contained in the environment of the state.
   *
   * <p>Frames are useful to keep track of local variables in a function call.
   */
  private static class Frame {
    private final Set<Variable> variables;

    public Frame(Collection<Variable> pVariables) {
      variables = ImmutableSet.copyOf(pVariables);
    }

    private Frame(Collection<Variable> pVariables1, Collection<Variable> pVariables2) {
      ImmutableSet.Builder<Variable> newVariables = new ImmutableSet.Builder<>();
      newVariables.addAll(pVariables1);
      newVariables.addAll(pVariables2);
      variables = newVariables.build();
    }

    private Frame addVariablesToFrame(
        Collection<Variable> pVariables1, Collection<Variable> pVariables2) {
      ImmutableSet.Builder<Variable> newVariables = new ImmutableSet.Builder<>();
      newVariables.addAll(variables);
      newVariables.addAll(pVariables1);
      newVariables.addAll(pVariables2);
      return new Frame(newVariables.build());
    }

    private Frame removeVariablesFromFrame(Collection<Variable> pVariables) {
      ImmutableSet<Variable> newVariables =
          Sets.difference(variables, ImmutableSet.copyOf(pVariables)).immutableCopy();
      return new Frame(newVariables);
    }

    private Set<Variable> getVariables() {
      return variables;
    }

    @Override
    public String toString() {
      return Arrays.toString(variables.toArray());
    }
  }
}
