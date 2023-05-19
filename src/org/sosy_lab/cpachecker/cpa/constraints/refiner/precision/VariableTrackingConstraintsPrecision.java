// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.UnaryConstraint;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicIdentifierLocator;

public class VariableTrackingConstraintsPrecision implements ConstraintsPrecision {
  final ImmutableMultimap<String, String> trackedFunctions;
  final ImmutableMultimap<CFANode, String> trackedLocations;
  final ImmutableSet<String> trackedGlobal;
  final ConstraintsPrecision constraintsPrecision;

  final boolean mustTrackAll;

  public VariableTrackingConstraintsPrecision(
      final Multimap<String, String> pTrackedFunctions,
      final Multimap<CFANode, String> pTrackedLocations,
      final Set<String> pTrackedGlobal,
      final ConstraintsPrecision pConstraintsPrecision,
      final boolean pMustTrackAll) {

    trackedFunctions = ImmutableMultimap.copyOf(pTrackedFunctions);
    trackedLocations = ImmutableMultimap.copyOf(pTrackedLocations);
    trackedGlobal = ImmutableSet.copyOf(pTrackedGlobal);
    constraintsPrecision = pConstraintsPrecision;
    mustTrackAll = pMustTrackAll;
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    // check if constraint is already tracked in constraintsPrecision
    if (!constraintsPrecision.isTracked(pConstraint, pLocation)) {
      // verify if pConstraint is Unary or BinaryConstraint
      if (pConstraint instanceof UnaryConstraint) {
        for (SymbolicIdentifier symId :
            ((UnaryConstraint) pConstraint)
                .getOperand()
                .accept(SymbolicIdentifierLocator.getInstance())) {
          if (isVariableTracked(symId, pLocation)) {
            if (!mustTrackAll) {
              return true;
            }
          } else {
            if (mustTrackAll) {
              return false;
            }
          }
        }
      } else if (pConstraint instanceof BinaryConstraint) {
        for (SymbolicIdentifier symId :
            ((BinaryConstraint) pConstraint)
                .getOperand1()
                .accept(SymbolicIdentifierLocator.getInstance())) {
          if (isVariableTracked(symId, pLocation)) {
            if (!mustTrackAll) {
              return true;
            }
          } else {
            if (mustTrackAll) {
              return false;
            }
          }
        }

        for (SymbolicIdentifier symId :
            ((BinaryConstraint) pConstraint)
                .getOperand2()
                .accept(SymbolicIdentifierLocator.getInstance())) {
          if (isVariableTracked(symId, pLocation)) {
            if (!mustTrackAll) {
              return true;
            }
          } else {
            if (mustTrackAll) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  private boolean isVariableTracked(final SymbolicIdentifier pSymId, final CFANode pLocation) {
    String var = pSymId.getRepresentation();
    return trackedGlobal.contains(var)
        || trackedFunctions.containsEntry(pLocation.getFunctionName(), var)
        || trackedLocations.containsEntry(pLocation, var);
  }

  @Override
  public ConstraintsPrecision join(final ConstraintsPrecision pOther) {
    VariableTrackingConstraintsPrecision other = (VariableTrackingConstraintsPrecision) pOther;
    return new VariableTrackingConstraintsPrecision(
        trackedFunctions,
        trackedLocations,
        trackedGlobal,
        constraintsPrecision.join(other.getConstraintsPrecision()),
        mustTrackAll);
  }

  public ConstraintsPrecision getConstraintsPrecision() {
    return constraintsPrecision;
  }

  @Override
  public ConstraintsPrecision withIncrement(final Increment pIncrement) {
    return new VariableTrackingConstraintsPrecision(
        trackedFunctions,
        trackedLocations,
        trackedGlobal,
        constraintsPrecision.withIncrement(pIncrement),
        mustTrackAll);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("VariableTrackingConstraintsPrecision [ \n");
    sb.append(constraintsPrecision.toString());
    sb.append("\nwith tracked variables: ");
    if (!trackedLocations.isEmpty()) {
      sb.append("\n\tLocations: ");
      sb.append(trackedLocations.toString());
    }
    if (!trackedFunctions.isEmpty()) {
      sb.append("\n\tFunctions: ");
      sb.append(trackedFunctions.toString());
    }
    if (!trackedGlobal.isEmpty()) {
      sb.append("\n\tGlobal: ");
      sb.append(trackedGlobal);
    }
    return sb.append("]").toString();
  }
}
