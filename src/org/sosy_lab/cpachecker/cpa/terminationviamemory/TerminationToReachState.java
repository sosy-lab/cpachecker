// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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

  /**
   * The following map keeps track of all the variables as type of @Formula, so that they can be
   * directly used in the further formulas by precision-adjustment operator. For every loop-head
   * (i.e. given by location and call-stack), it keeps track of which variables (with SSA indices)
   * where the most recent ones after how many iteration. In other words, it maps location and
   * call-stack states of loop-heads to a map with information about which variables were seen after
   * which unrolling of the loop.
   */
  private final ImmutableMap<
          Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
      storedValues;

  /**
   * For every loop-head (given by location and call-stack), we track how many times have we passed
   * it in the abstract graph until reaching this state.
   */
  private final ImmutableMap<Pair<LocationState, CallstackState>, Integer> numberOfIterations;

  /**
   * For every loop-head (given by location and call-stack), we track the path formula until
   * reaching this abstract state. This is the part inside the loop, i.e. the loop iterations.
   */
  private final ImmutableMap<Pair<LocationState, CallstackState>, PathFormula>
      pathFormulaForIteration;

  /**
   * For every loop-head (given by location and call-stack), we track the path formula until
   * reaching this abstract state. This is the part before reaching the loop.
   */
  private final Optional<PathFormula> pathFormulaForPrefix;

  /**
   * We collect transition invariants that hold for previous iteration formulas at this abstract
   * state. If the transition invariant does not hold in another branch, we weaken it with another
   * candidate transition invariant. This set represents a conjunction of all possible transition
   * invariants at this location.
   */
  private final ImmutableSet<PartitionedRelationFormula> transitionInvariants;

  /** Available transition predicates to use. */
  private final ImmutableSet<PartitionedRelationFormula> transitionPredicates;

  private final Optional<PathFormula> pathFormulaFull;
  private final ImmutableList<CFANode> pathSequence;

  public TerminationToReachState(
      ImmutableMap<
              Pair<LocationState, CallstackState>, ImmutableMap<Integer, ImmutableSet<Formula>>>
          pStoredValues,
      ImmutableMap<Pair<LocationState, CallstackState>, Integer> pNumberOfIterations,
      ImmutableMap<Pair<LocationState, CallstackState>, PathFormula> pPathFormulaForIteration,
      Optional<PathFormula> pPathFormulaForPrefix,
      Optional<PathFormula> pPathFormulaFull,
      ImmutableList<CFANode> pPathSequence,
      ImmutableSet<PartitionedRelationFormula> pTransitionInvariants,
      ImmutableSet<PartitionedRelationFormula> pAvailableTransitionPredicates) {

    storedValues = pStoredValues;
    numberOfIterations = pNumberOfIterations;
    pathFormulaForIteration = pPathFormulaForIteration;
    pathFormulaForPrefix = pPathFormulaForPrefix;
    pathFormulaFull = pPathFormulaFull;
    pathSequence = pPathSequence;
    isTarget = false;
    transitionInvariants = pTransitionInvariants;
    transitionPredicates = pAvailableTransitionPredicates;
  }

  public int getNumberOfIterationsAtLoopHead(Pair<LocationState, CallstackState> pKeyPair) {
    if (numberOfIterations.containsKey(pKeyPair)) {
      return numberOfIterations.get(pKeyPair);
    }
    return 0;
  }

  public ImmutableMap<Pair<LocationState, CallstackState>, Integer> getNumberOfIterations() {
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

  // TODO: use PersistentStack for pathSequence
  public ImmutableList<CFANode> getPathSequence() {
    return pathSequence;
  }

  public void makeTarget() {
    isTarget = true;
  }

  public ImmutableSet<PartitionedRelationFormula> getTransitionInvariants() {
    return transitionInvariants;
  }

  public ImmutableSet<PartitionedRelationFormula> getTransitionPredicates() {
    return transitionPredicates;
  }

  @Override
  public int hashCode() {
    return Objects.hash(transitionInvariants, isTarget);
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
    return "TerminationState{transitionPredicates=["
        + getReadableTransitionInvariants()
        + "]"
        + '}';
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof TerminationToReachState other
        && transitionInvariants.equals(other.getTransitionInvariants())
        && !transitionInvariants.isEmpty()
        && !other.getTransitionInvariants().isEmpty()
        && isTarget == other.isTarget();
  }

  private String getReadableTransitionInvariants() {
    StringBuilder sb = new StringBuilder();
    for (PartitionedRelationFormula transInv : transitionInvariants) {
      sb.append(transInv.getFormula());
    }
    return sb.toString();
  }

  @Override
  public String toDOTLabel() {
    return "Transition Predicates:\n" + getReadableTransitionInvariants().replace(", ", "\n");
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
