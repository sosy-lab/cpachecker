// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/** State for Constraints Analysis. Stores constraints and whether they are solvable. */
public final class ConstraintsState implements AbstractState, Graphable, Set<Constraint> {

  /** The constraints of this state */
  private List<Constraint> constraints;

  /**
   * The last constraint added to this state. This does not have to be the last constraint in {@link
   * #constraints}.
   */
  // It does not have to be the last constraint contained in 'constraints' because we only
  // add a constraint to 'constraints' if it's not yet in this list.
  private Optional<Constraint> lastAddedConstraint = Optional.empty();

  private ImmutableList<ValueAssignment> definiteAssignment;
  private ImmutableList<ValueAssignment> lastModelAsAssignment = ImmutableList.of();

  /** Creates a new, initial <code>ConstraintsState</code> object. */
  public ConstraintsState() {
    this(ImmutableSet.of());
  }

  public ConstraintsState(final Set<Constraint> pConstraints) {
    constraints = new ArrayList<>(pConstraints);
    definiteAssignment = ImmutableList.of();
  }

  /**
   * Creates a new <code>ConstraintsState</code> copy of the given <code>ConstraintsState</code>.
   *
   * <p>This constructor should only be used by {@link #copyOf()} and subtypes of this class.
   *
   * @param pState the state to copy
   */
  ConstraintsState(ConstraintsState pState) {
    constraints = new ArrayList<>(pState.constraints);

    lastAddedConstraint = pState.lastAddedConstraint;
    definiteAssignment = ImmutableList.copyOf(pState.definiteAssignment);
    lastModelAsAssignment = pState.lastModelAsAssignment;
  }

  /** Returns a new copy of the given <code>ConstraintsState</code> object. */
  // We use a method here so subtypes can override it, in contrast to a public copy constructor
  public ConstraintsState copyOf() {
    return new ConstraintsState(this);
  }

  /**
   * Adds the given {@link Constraint} to this state.
   *
   * @param pConstraint the <code>Constraint</code> to add
   * @return <code>true</code> if this state did not already contain the given <code>Constraint
   *     </code>, <code>false</code> otherwise
   */
  @Override
  public boolean add(Constraint pConstraint) {
    checkNotNull(pConstraint);

    lastAddedConstraint = Optional.of(pConstraint);
    return !constraints.contains(pConstraint) && constraints.add(pConstraint);
  }

  @Override
  public boolean remove(Object pObject) {
    boolean changed = constraints.remove(pObject);

    if (changed) {
      definiteAssignment = ImmutableList.of();
    }

    return changed;
  }

  Optional<Constraint> getLastAddedConstraint() {
    return lastAddedConstraint;
  }

  @Override
  public boolean containsAll(Collection<?> pCollection) {
    return constraints.containsAll(pCollection);
  }

  @Override
  public boolean addAll(Collection<? extends Constraint> pCollection) {
    boolean changed = false;
    for (Constraint c : pCollection) {
      changed |= add(c);
    }

    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> pCollection) {
    List<Constraint> constraintsCopy = new ArrayList<>(constraints);
    boolean changed = false;

    for (Constraint c : constraintsCopy) {
      if (!pCollection.contains(c)) {
        changed |= remove(c);
      }
    }

    if (changed) {
      definiteAssignment = ImmutableList.of();
    }

    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> pCollection) {
    boolean changed = false;

    for (Object o : pCollection) {
      changed |= remove(o);
    }

    if (changed) {
      definiteAssignment = ImmutableList.of();
    }

    return changed;
  }

  @Override
  public void clear() {
    constraints.clear();
    definiteAssignment = ImmutableList.of();
  }

  @Override
  public int size() {
    return constraints.size();
  }

  @Override
  public boolean isEmpty() {
    return constraints.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return constraints.contains(o);
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

  void setDefiniteAssignment(ImmutableCollection<ValueAssignment> pAssignment) {
    definiteAssignment = pAssignment.asList();
  }

  /** Returns the last model computed for this constraints state. */
  public ImmutableList<ValueAssignment> getModel() {
    return lastModelAsAssignment;
  }

  void setModel(List<ValueAssignment> pModel) {
    lastModelAsAssignment = ImmutableList.copyOf(pModel);
  }

  @Override
  public Iterator<Constraint> iterator() {
    return new ConstraintIterator();
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
  public Object[] toArray() {
    return constraints.toArray();
  }

  @Override
  public <T> T[] toArray(T[] pTs) {
    return constraints.toArray(pTs);
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

  private class ConstraintIterator implements Iterator<Constraint> {

    private int index = -1;

    @Override
    public boolean hasNext() {
      return constraints.size() - 1 > index;
    }

    @Override
    public Constraint next() {
      index++;
      return constraints.get(index);
    }

    @Override
    public void remove() {
      checkState(index >= 0, "Iterator not at valid location");

      constraints.remove(index);
      index--;
    }
  }
}
