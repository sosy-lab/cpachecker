// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class ModificationsPropState
    implements AvoidanceReportingState,
        AbstractQueryableState,
        LatticeAbstractState<ModificationsPropState>,
        Graphable {

  /** Bad is meant in the sense that this may not be covered by a condition. */
  private boolean isBad;

  private CFANode locationInGivenCfa;
  private CFANode locationInOriginalCfa;
  private ImmutableSet<String> changedVariables;

  public ModificationsPropState(
      CFANode pLocationInGivenCfa,
      CFANode pLocationInOriginalCfa,
      ImmutableSet<String> pChangedVarsInGivenCfa) {
    this(pLocationInGivenCfa, pLocationInOriginalCfa, pChangedVarsInGivenCfa, false);
  }

  public ModificationsPropState(
      CFANode pLocationInGivenCfa,
      CFANode pLocationInOriginalCfa,
      ImmutableSet<String> pChangedVarsInGivenCfa,
      boolean pIsBad) {
    locationInGivenCfa = pLocationInGivenCfa;
    locationInOriginalCfa = pLocationInOriginalCfa;
    changedVariables = pChangedVarsInGivenCfa;
    isBad = pIsBad;
  }

  public CFANode getLocationInOriginalCfa() {
    return locationInOriginalCfa;
  }

  public CFANode getLocationInGivenCfa() {
    return locationInGivenCfa;
  }

  public ImmutableSet<String> getChangedVariables() {
    return changedVariables;
  }

  public boolean isBad() {
    return isBad;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ModificationsPropState that = (ModificationsPropState) pO;
    return Objects.equals(locationInOriginalCfa, that.locationInOriginalCfa)
        && Objects.equals(locationInGivenCfa, that.locationInGivenCfa)
        && Objects.equals(changedVariables, that.changedVariables)
        && (isBad == that.isBad);
  }

  @Override
  public int hashCode() {

    return Objects.hash(locationInOriginalCfa, locationInGivenCfa, changedVariables, isBad);
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return isBad;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
    if (mustDumpAssumptionForAvoidance()) {
      return pMgr.getBooleanFormulaManager().makeFalse();
    }
    return pMgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public String getCPAName() {
    return ModificationsPropCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case "is_bad":
        return isBad;
      default:
        throw new InvalidQueryException(
            "Unknown query to " + getClass().getSimpleName() + ": " + pProperty);
    }
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (isBad) {
      sb.append("Uncovered edge");
      if (!changedVariables.isEmpty()) {
        sb.append(" for modified variables {");
        for (String v : changedVariables) {
          sb.append(v);
          sb.append(", ");
        }
        sb.setLength(sb.length() - 2); // remove last ", "
        sb.append("}");
      }
      sb.append(":");
      FluentIterable<CFAEdge> edgesInMod = CFAUtils.enteringEdges(locationInGivenCfa);
      for (CFAEdge e : edgesInMod) {
        sb.append(e);
        sb.append(", ");
      }
      sb.setLength(sb.length() - 2); // remove last ", "
      sb.append("}");
    } else if (!changedVariables.isEmpty()) {
      sb.append("Modified: ");
      sb.append("{");
      for (String v : changedVariables) {
        sb.append(v);
        sb.append(", ");
      }
      sb.setLength(sb.length() - 2); // remove last ", "
      sb.append("}");
    } else {
      sb.append("No modification");
    }
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return isBad;
  }

  @Override
  public ModificationsPropState join(ModificationsPropState pOther)
      throws CPAException, InterruptedException {
    // TODO: maybe only join if locations identical
    if (isBad || pOther.isBad) {
      return new ModificationsPropState(
          locationInGivenCfa,
          locationInOriginalCfa,
          // modified variables not relevant for bad location pairs
          ImmutableSet.of(),
          true);
    } else {
      return new ModificationsPropState(
          locationInGivenCfa,
          locationInOriginalCfa,
          // join modified variables for more abstract states, could be omitted
          ImmutableSet.<String>builder()
              .addAll(changedVariables)
              .addAll(pOther.changedVariables)
              .build(),
          false);
    }
  }

  @Override
  public boolean isLessOrEqual(ModificationsPropState pOther)
      throws CPAException, InterruptedException {
    // location pair must be identical
    return Objects.equals(locationInOriginalCfa, pOther.locationInOriginalCfa)
        && Objects.equals(locationInGivenCfa, pOther.locationInGivenCfa)
        // if this state is bad, the other one must be bad as well
        // TODO: think over when transition is implemented
        && (pOther.isBad || !isBad)
        // variables modifier must be a subset, else join is necessary
        // can stop if location pair is already known to be bad
        && (pOther.changedVariables.containsAll(changedVariables) || pOther.isBad);
  }
}
