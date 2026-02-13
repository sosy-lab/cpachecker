// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.SimpleTargetInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * Tracks already seen states at loop-head locations and within the same call-stack. In the
 * following documentation, a loop-head is given by the location and call-stack.
 */
public class TerminationToReachState implements Graphable, AbstractQueryableState, Targetable {
  private static final ImmutableSet<TargetInformation> TERMINATION_PROPERTY =
      SimpleTargetInformation.singleton("termination");
  private boolean isTarget;
  private boolean isTerminating;

  /**
   * The following map keeps track of all the variables as type of @Formula, so that they can be
   * directly used in the further formulas by precision-adjustment operator. For every loop-head
   * (i.e. given by location and call-stack), it keeps track of which variables (with SSA indices)
   * where the most recent ones after how many iteration. In other words, it maps location and
   * call-stack states of loop-heads to a map with information about which variables were seen after
   * which unrolling of the loop.
   */
  private ImmutableMap<
          Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
      storedValues;

  /**
   * For every loop-head (given by location and call-stack), we track how many times have we passed
   * it in the abstract graph until reaching this state.
   */
  private ImmutableMap<Pair<LocationState, CallstackState>, Integer> numberOfIterations;

  /**
   * For every loop-head (given by location and call-stack), we track the path formula until
   * reaching this abstract state. This is the part inside the loop, i.e. the loop iterations.
   */
  private ImmutableMap<Pair<LocationState, CallstackState>, PathFormula> pathFormulaForIteration;

  /**
   * For every loop-head (given by location and call-stack), we track the path formula until
   * reaching this abstract state. This is the part before reaching the loop.
   */
  private Optional<PathFormula> pathFormulaForPrefix;

  private Optional<PathFormula> pathFormulaFull;

  public TerminationToReachState(
      ImmutableMap<
              Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
          pStoredValues,
      ImmutableMap<Pair<LocationState, CallstackState>, Integer> pNumberOfIterations,
      ImmutableMap<Pair<LocationState, CallstackState>, PathFormula> pPathFormulaForIteration,
      Optional<PathFormula> pPathFormulaForPrefix,
      Optional<PathFormula> pPathFormulaFull) {

    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
    pathFormulaForIteration = pPathFormulaForIteration;
    pathFormulaForPrefix = pPathFormulaForPrefix;
    pathFormulaFull = pPathFormulaFull;
    isTarget = false;
    isTerminating = false;
  }

  public int getNumberOfIterationsAtLoopHead(Pair<LocationState, CallstackState> pKeyPair) {
    if (numberOfIterations.containsKey(pKeyPair)) {
      return numberOfIterations.get(pKeyPair);
    }
    return 0;
  }

  public ImmutableMap<Pair<LocationState, CallstackState>, Integer> getNumberOfIterationsMap() {
    return numberOfIterations;
  }

  public ImmutableMap<
          Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
      getStoredValues() {
    return storedValues;
  }

  public ImmutableMap<Pair<LocationState, CallstackState>, PathFormula>
      getPathFormulasForIteration() {
    return pathFormulaForIteration;
  }

  public Optional<PathFormula> getPathFormulasForPrefix() {
    return pathFormulaForPrefix;
  }

  public Optional<PathFormula> getPathFormulaFull() {
    return pathFormulaFull;
  }

  public void makeTarget() {
    isTarget = true;
  }

  public void setTerminating() {
    isTerminating = true;
  }

  public boolean isTerminating() {
    return isTerminating;
  }

  @Override
  public int hashCode() {
    return Objects.hash(storedValues, numberOfIterations, isTarget);
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    checkState(isTarget);
    return TERMINATION_PROPERTY;
  }

  @Override
  public String toString() {
    return "TerminationState{storedValues=[" + getReadableStoredValues() + "]" + '}';
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TerminationToReachState other
        && storedValues.equals(other.getStoredValues());
  }

  private String getReadableStoredValues() {
    StringBuilder sb = new StringBuilder();
    for (Entry<Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
        entry : getStoredValues().entrySet()) {
      sb.append(entry);
    }
    return sb.toString();
  }

  @Override
  public String toDOTLabel() {
    return "Stored Values:\n" + getReadableStoredValues().replace(", ", "\n");
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String getCPAName() {
    return "TerminationToReachCPA";
  }
}
