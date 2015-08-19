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
package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * ConstraintsPrecision that uses the code constraints represent.
 *
 * <p>Example:
 * A ConstraintBasedConstraintsPrecision contains for location <code>N12</code> the precision
 * <code>'a > 5'</code>. The ConstraintsState to adjust at this location is
 * <code>'s1(a) > 5, s3(b) > 5'</code>.
 * After precision adjustment, the ConstraintsState only consists of <code>'s1(a) > 5'</code>.
 * </p>
 */
class ConstraintBasedConstraintsPrecision
    implements ConstraintsPrecision {

  private final static ConstraintBasedConstraintsPrecision
      EMPTY = new ConstraintBasedConstraintsPrecision();

  private Multimap<CFANode, Constraint> trackedLocally;
  private Multimap<String, Constraint> trackedInFunction;
  private Set<Constraint> trackedGlobally;

  public static ConstraintsPrecision getEmptyPrecision() {
    return EMPTY;
  }

  /**
   * Creates a new <code>ConstraintBasedConstraintsPrecision</code> with the given constraints as precision.
   */
  private ConstraintBasedConstraintsPrecision(
      final ConstraintBasedConstraintsPrecision pPrecision
  ) {
    checkNotNull(pPrecision);

    trackedLocally = pPrecision.trackedLocally;
    trackedInFunction = pPrecision.trackedInFunction;
    trackedGlobally = pPrecision.trackedGlobally;
  }

  private ConstraintBasedConstraintsPrecision(
      final Multimap<CFANode, Constraint> pTrackedLocally,
      final Multimap<String, Constraint> pTrackedInFunction,
      final Set<Constraint> pTrackedGlobally
  ) {
    trackedLocally = pTrackedLocally;
    trackedInFunction = pTrackedInFunction;
    trackedGlobally = pTrackedGlobally;
  }

  private ConstraintBasedConstraintsPrecision() {
    trackedLocally = HashMultimap.create();
    trackedInFunction = HashMultimap.create();
    trackedGlobally = new HashSet<>();
  }

  /**
   * Returns whether the given <code>Constraint</code> is tracked by this precision.
   */
  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    for (Constraint c : trackedLocally.get(pLocation)) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    for (Constraint c : trackedInFunction.get(pLocation.getFunctionName())) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    for (Constraint c : trackedGlobally) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
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
  @Override
  public ConstraintBasedConstraintsPrecision join(final ConstraintsPrecision pOther) {
    assert pOther instanceof ConstraintBasedConstraintsPrecision;

    ConstraintBasedConstraintsPrecision other = (ConstraintBasedConstraintsPrecision) pOther;
    Multimap<CFANode, Constraint> joinedLocal = HashMultimap.create(trackedLocally);
    Multimap<String, Constraint> joinedFunctionwise = HashMultimap.create(trackedInFunction);
    Set<Constraint> joinedGlobal = new HashSet<>(trackedGlobally);

    addNewLocalConstraints(joinedLocal, other.trackedLocally);
    addNewFunctionConstraints(joinedFunctionwise, other.trackedInFunction);
    addNewGlobalConstraints(joinedGlobal, other.trackedGlobally);

    return new ConstraintBasedConstraintsPrecision(joinedLocal, joinedFunctionwise, joinedGlobal);
  }

  private void addNewLocalConstraints(
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

  private void addNewFunctionConstraints(
      Multimap<String, Constraint> pMapToAddTo,
      Multimap<String, Constraint> pNewConstraints
  ) {
    for (Entry<String, Constraint> entry : pNewConstraints.entries()) {
      String function = entry.getKey();
      Constraint constraint = entry.getValue();

      if (!constraintWithSameMeaningExists(function, constraint, pMapToAddTo)) {
        pMapToAddTo.put(function, constraint);
      }
    }
  }

  private void addNewGlobalConstraints(
      Set<Constraint> pSetToAddTo,
      Set<Constraint> pNewConstraints
  ) {
    for (Constraint c : pNewConstraints) {
      if (!constraintWithSameMeaningExists(c, pNewConstraints)) {
        pSetToAddTo.add(c);
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
        if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean constraintWithSameMeaningExists(
      final String pFunctionName,
      final Constraint pConstraint,
      final Multimap<String, Constraint> pTrackedConstraints
  ) {

    if (pTrackedConstraints.containsKey(pFunctionName)) {
      final Collection<Constraint> constraintsOnLocation = pTrackedConstraints.get(pFunctionName);

      for (Constraint c : constraintsOnLocation) {
        if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean constraintWithSameMeaningExists(
      final Constraint pConstraint,
      final Set<Constraint> pTrackedConstraints
  ) {

    for (Constraint c : pTrackedConstraints) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public ConstraintBasedConstraintsPrecision withIncrement(final Increment pIncrement) {
    ConstraintBasedConstraintsPrecision newPrecision = new ConstraintBasedConstraintsPrecision(this);

    newPrecision.trackedGlobally.addAll(pIncrement.getTrackedGlobally());
    newPrecision.trackedInFunction.putAll(pIncrement.getTrackedInFunction());
    newPrecision.trackedLocally.putAll(pIncrement.getTrackedLocally());

    return newPrecision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConstraintBasedConstraintsPrecision that = (ConstraintBasedConstraintsPrecision) o;

    return Objects.equals(trackedLocally, that.trackedLocally);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackedLocally);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ConstraintBasedConstraintsPrecision[");

    sb.append("\nLocally tracked: {");
    if (!trackedLocally.keySet().isEmpty()) {
      List<CFANode> nodes = new ArrayList<>(trackedLocally.keySet());
      Collections.sort(nodes); // we always want the same node order

      sb.append("\n");
      for (CFANode n : nodes) {
        sb.append("\t").append(n).append(" -> ");

        // unfortunately, constraints aren't comparable, so we won't have a deterministic order.
        for (Constraint c : trackedLocally.get(n)) {
          sb.append(c.getRepresentation())
            .append(", ");
        }

        sb.append("\n");
      }
    }
    sb.append("} -> size: ").append(trackedLocally.size());

    sb.append("\nFunctionwise tracked: {");
    if (!trackedInFunction.keySet().isEmpty()) {
      List<String> functions = new ArrayList<>(trackedInFunction.keySet());
      Collections.sort(functions); // we always want the same function order

      sb.append("\n");
      for (String f : functions) {
        sb.append("\t").append(f).append(" -> ");

        for (Constraint c : trackedInFunction.get(f)) {
          sb.append(c.getRepresentation())
            .append(", ");
        }

        sb.append("\n");
      }
    }
    sb.append("} -> size: ").append(trackedInFunction.size());

    sb.append("\nGlobally tracked: {");
    for (Constraint c : trackedGlobally) {
      sb.append(c).append(", ");
    }
    sb.append("} -> size: ").append(trackedGlobally.size());

    return sb.toString();
  }

}
