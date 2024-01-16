// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/**
 * State for Constraints Analysis. Stores path constraints and information about their
 * satisfiability. This class is immutable.
 */
public final class ConstraintsState extends ForwardingSet<Constraint>
    implements AbstractState, Graphable {

  /** The constraints of this state */
  private final ImmutableSet<Constraint> constraints;

  /**
   * The last constraint added to this state. This does not have to be the last constraint in {@link
   * #constraints}.
   */
  // It does not have to be the last constraint contained in 'constraints' because we only
  // add a constraint to 'constraints' if it's not yet in this list.
  private final Optional<Constraint> lastAddedConstraint;

  private final ImmutableList<ValueAssignment> lastModelAsAssignment;
  private final ImmutableList<ValueAssignment> definiteAssignment;

  /** Creates a new, initial <code>ConstraintsState</code> object. */
  public ConstraintsState() {
    this(ImmutableSet.of());
  }

  public ConstraintsState(final Set<Constraint> pConstraints) {
    constraints = ImmutableSet.copyOf(pConstraints);
    lastModelAsAssignment = ImmutableList.of();
    definiteAssignment = ImmutableList.of();
    lastAddedConstraint = Optional.empty();
  }

  private ConstraintsState(
      final ImmutableSet<Constraint> pConstraints,
      final Optional<Constraint> pLastAddedConstraint,
      final ImmutableList<ValueAssignment> lastSatisfyingModel,
      final ImmutableList<ValueAssignment> knownDefiniteAssignments) {
    constraints = pConstraints;
    lastAddedConstraint = pLastAddedConstraint;
    lastModelAsAssignment = lastSatisfyingModel;
    definiteAssignment = knownDefiniteAssignments;
  }

  @Override
  protected ImmutableSet<Constraint> delegate() {
    return constraints;
  }

  /**
   * Creates a copy of this ConstraintsState with the given {@link Constraint} added to this state.
   *
   * @param pConstraint the <code>Constraint</code> to add to the new copy of the state
   * @return the copy of this ConstraintsState with the added constraint
   */
  public ConstraintsState copyWithNew(Constraint pConstraint) {
    return copyWithNew(ImmutableList.of(pConstraint));
  }

  /**
   * Creates a copy of this ConstraintsState with the given {@link Constraint constraints} added to
   * this state.
   *
   * @param pConstraints the <code>Constraint</code>s to add to the new copy of the state
   * @return the copy of this ConstraintsState with the added constraints
   */
  public ConstraintsState copyWithNew(List<Constraint> pConstraints) {
    checkNotNull(pConstraints);

    Optional<Constraint> addedConstraint = Optional.of(pConstraints.get(pConstraints.size() - 1));
    ImmutableSet<Constraint> newConstraints =
        ImmutableSet.<Constraint>builder().addAll(constraints).addAll(pConstraints).build();
    return new ConstraintsState(
        newConstraints, addedConstraint, lastModelAsAssignment, definiteAssignment);
  }

  public Optional<Constraint> getLastAddedConstraint() {
    return lastAddedConstraint;
  }

  /**
   * Returns the known unambiguous assignment of variables so this state's {@link Constraint}s are
   * fulfilled. Variables that can have more than one valid assignment are not included in the
   * returned assignments.
   *
   * @return the known assignment of variables that have no other fulfilling assignment
   */
  public ImmutableCollection<ValueAssignment> getDefiniteAssignment() {
    return definiteAssignment;
  }

  /**
   * Creates a copy of this ConstraintsState with the given {@link Constraint constraints} added to
   * this state.
   *
   * @param pAssignment the definite assignment to store in the new copy of the state
   * @return the copy of this ConstraintsState with the definite assignment
   */
  public ConstraintsState copyWithDefiniteAssignment(
      ImmutableCollection<ValueAssignment> pAssignment) {
    checkNotNull(pAssignment);

    return new ConstraintsState(
        constraints, lastAddedConstraint, lastModelAsAssignment, ImmutableList.copyOf(pAssignment));
  }

  /** Returns the last model computed for this constraints state. */
  public ImmutableList<ValueAssignment> getModel() {
    return lastModelAsAssignment;
  }

  /**
   * Creates a copy of this ConstraintsState with the given {@link Constraint constraints} added to
   * this state.
   *
   * @param pModel the satisfying model for the constraints, to store in the new copy of the state
   * @return the copy of this ConstraintsState with the satisfying model
   */
  public ConstraintsState copyWithSatisfyingModel(List<ValueAssignment> pModel) {
    checkNotNull(pModel);

    return new ConstraintsState(
        constraints, lastAddedConstraint, ImmutableList.copyOf(pModel), definiteAssignment);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstraintsState that = (ConstraintsState) o;

    return constraints.equals(that.constraints)
        && definiteAssignment.equals(that.definiteAssignment);
  }

  @Override
  public int hashCode() {
    int result = constraints.hashCode();
    result = 31 * result + definiteAssignment.hashCode();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");

    for (Constraint currConstraint : constraints) {
      sb.append(" <");
      sb.append(currConstraint);
      sb.append(">\n");
    }

    return sb.append("] size->  ").append(constraints.size()).toString();
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("[");
    Joiner.on(", ").appendTo(sb, constraints);
    sb.append("]");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
