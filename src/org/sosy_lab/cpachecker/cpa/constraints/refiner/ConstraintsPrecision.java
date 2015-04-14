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
package org.sosy_lab.cpachecker.cpa.constraints.refiner;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.domain.AliasedSubsetLessOrEqualOperator;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Precision for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 */
public class ConstraintsPrecision implements Precision {

  public final static ConstraintsPrecision EMPTY = new ConstraintsPrecision();

  private Multimap<CFANode, Constraint> trackedConstraints;

  private final AliasedSubsetLessOrEqualOperator leqOperator =
      AliasedSubsetLessOrEqualOperator.getInstance();

  /**
   * Creates a new <code>ConstraintsPrecision</code> with the given constraints as precision.
   */
  public ConstraintsPrecision(final Multimap<CFANode, Constraint> pConstraints) {
    checkNotNull(pConstraints);
    trackedConstraints = pConstraints;
  }

  private ConstraintsPrecision() {
    trackedConstraints = HashMultimap.create();
  }

  /**
   * Returns whether the given <code>Constraint</code> is tracked by this precision.
   */
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    for (Constraint c : trackedConstraints.get(pLocation)) {
      if (leqOperator.haveEqualMeaning(c, pConstraint)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Joins this precision with the given one.
   * The join of two precisions is their union.
   *
   * @param pOther the precision to join with this precision
   * @return the join of both precisions
   */
  public ConstraintsPrecision join(final ConstraintsPrecision pOther) {
    Multimap<CFANode, Constraint> joinedSet = HashMultimap.create(trackedConstraints);

    addNewConstraints(joinedSet, pOther.trackedConstraints);

    return new ConstraintsPrecision(joinedSet);
  }

  private void addNewConstraints(
      Multimap<CFANode, Constraint> pMapToAddTo,
      Multimap<CFANode, Constraint> pNewConstraints
  ) {
    for (Entry<CFANode, Constraint> entry : pNewConstraints.entries()) {
      CFANode loc = entry.getKey();
      Constraint constraint = entry.getValue();

      if (!constraintWithSameMeaningExists(loc, constraint, pMapToAddTo)) {
        pMapToAddTo.put(loc, constraint);
      }
    }
  }

  private boolean constraintWithSameMeaningExists(
      final CFANode pLoc,
      final Constraint pConstraint,
      final Multimap<CFANode, Constraint> pTrackedConstraints
  ) {

    if (pTrackedConstraints.containsKey(pLoc)) {
      final Collection<Constraint> constraintsOnLocation = pTrackedConstraints.get(pLoc);

      for (Constraint c : constraintsOnLocation) {
        if (leqOperator.haveEqualMeaning(c, pConstraint)) {
          return true;
        }
      }
    }

    return false;
  }

  public ConstraintsPrecision withIncrement(final ConstraintsPrecisionIncrement pIncrement) {
    if (trackedConstraints.entries().containsAll(pIncrement.entries())) {
      return this;

    } else {
      HashMultimap<CFANode, Constraint> newPrecision = HashMultimap.create(trackedConstraints);
      addNewConstraints(newPrecision, pIncrement);

      return new ConstraintsPrecision(newPrecision);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConstraintsPrecision that = (ConstraintsPrecision) o;

    return Objects.equals(trackedConstraints, that.trackedConstraints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackedConstraints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ConstraintsPrecision[\n");
    List<CFANode> nodes = new ArrayList<>(trackedConstraints.keySet());
    Collections.sort(nodes); // we always want the same node order

    for (CFANode n : nodes) {
      sb.append("\t").append(n).append(" -> ");

      // unfortunately, constraints aren't comparable, so we won't have a deterministic order.
      for (Constraint c : trackedConstraints.get(n)) {
        sb.append(c.getRepresentation() + ", ");
      }

      sb.append("\n");
    }

    sb.append("]");
    return sb.toString();
  }
}
