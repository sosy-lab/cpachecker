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
import com.google.common.collect.ImmutableSetMultimap;
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
  ConstraintsPrecision constraintsPrecision;

  final boolean mustTrackAll;

  private enum IncrementScope {
    NONE(0),
    LOCAL(1),
    FUNCTION(2),
    GLOBAL(3);

    private final int order;

    IncrementScope(final int pOrder) {
      order = pOrder;
    }

    public IncrementScope lower(final IncrementScope pOther) {
      if (pOther.order < order) {
        return pOther;
      } else {
        return this;
      }
    }
  }

  public VariableTrackingConstraintsPrecision(
      final Multimap<String, String> pTrackedFunctions,
      final Multimap<CFANode, String> pTrackedLocations,
      final Set<String> pTrackedGlobal,
      final ConstraintsPrecision pConstraintsPrecision,
      final boolean pMustTrackAll) {

    trackedFunctions = ImmutableSetMultimap.copyOf(pTrackedFunctions);
    trackedLocations = ImmutableSetMultimap.copyOf(pTrackedLocations);
    trackedGlobal = ImmutableSet.copyOf(pTrackedGlobal);
    constraintsPrecision = pConstraintsPrecision;
    mustTrackAll = pMustTrackAll;
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    // check if constraint is already tracked in constraintsPrecision
    if (!constraintsPrecision.isTracked(pConstraint, pLocation)) {
      IncrementScope constraintScope = IncrementScope.NONE;
      // verify if pConstraint is Unary or BinaryConstraint
      if (pConstraint instanceof UnaryConstraint) {
        constraintScope =
            getScopeForConstraint(
                ((UnaryConstraint) pConstraint)
                    .getOperand()
                    .accept(SymbolicIdentifierLocator.getInstance()),
                pLocation,
                IncrementScope.GLOBAL);
      } else if (pConstraint instanceof BinaryConstraint) {
        constraintScope =
            getScopeForConstraint(
                ((BinaryConstraint) pConstraint)
                    .getOperand1()
                    .accept(SymbolicIdentifierLocator.getInstance()),
                pLocation,
                IncrementScope.GLOBAL);
        if (constraintScope != IncrementScope.NONE) {
          constraintScope =
              getScopeForConstraint(
                  ((BinaryConstraint) pConstraint)
                      .getOperand2()
                      .accept(SymbolicIdentifierLocator.getInstance()),
                  pLocation,
                  constraintScope);
        }
      }

      switch (constraintScope) {
        case LOCAL:
          constraintsPrecision =
              constraintsPrecision.withIncrement(
                  Increment.builder().locallyTracked(pLocation, pConstraint).build());
          break;
        case FUNCTION:
          constraintsPrecision =
              constraintsPrecision.withIncrement(
                  Increment.builder()
                      .functionWiseTracked(pLocation.getFunctionName(), pConstraint)
                      .build());
          break;
        case GLOBAL:
          constraintsPrecision =
              constraintsPrecision.withIncrement(
                  Increment.builder().globallyTracked(pConstraint).build());
          break;
        case NONE:
          break;
      }
      return constraintScope != IncrementScope.NONE;
    }
    return true;
  }

  private IncrementScope getScopeForConstraint(
      final Set<SymbolicIdentifier> symVars, final CFANode pLocation, IncrementScope pInitScope) {
    IncrementScope varScope;
    IncrementScope resScope = pInitScope;
    boolean noVarTracked = true;
    for (SymbolicIdentifier symId : symVars) {
      varScope = getTrackedScopeForVar(symId, pLocation);
      if (varScope == IncrementScope.NONE) {
        if (mustTrackAll) {
          return IncrementScope.NONE;
        }
      } else {
        noVarTracked = false;
        resScope = resScope.lower(varScope);
      }
    }
    if (!mustTrackAll && noVarTracked) {
      return IncrementScope.NONE;
    }
    return resScope;
  }

  private IncrementScope getTrackedScopeForVar(
      final SymbolicIdentifier pSymId, final CFANode pLocation) {
    String var = pSymId.getRepresentation();
    if (trackedGlobal.contains(var)) {
      return IncrementScope.GLOBAL;
    }
    if (trackedFunctions.containsEntry(pLocation.getFunctionName(), var)) {
      return IncrementScope.FUNCTION;
    }
    if (trackedLocations.containsEntry(pLocation, var)) {
      return IncrementScope.LOCAL;
    }
    return IncrementScope.NONE;
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
