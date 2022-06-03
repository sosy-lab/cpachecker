// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsrcd;

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

public final class ModificationsRcdState
    implements AvoidanceReportingState,
        AbstractQueryableState,
        LatticeAbstractState<ModificationsRcdState>,
        Graphable {

  private boolean hasRelevantModification;
  private CFANode locationInGivenCfa;
  private CFANode locationInOriginalCfa;
  private ImmutableSet<String> changedVarsInGivenCfa;

  public ModificationsRcdState(
      CFANode pLocationInGivenCfa,
      CFANode pLocationInOriginalCfa,
      ImmutableSet<String> pChangedVarsInGivenCfa) {
    this(pLocationInGivenCfa, pLocationInOriginalCfa, pChangedVarsInGivenCfa, false);
  }

  public ModificationsRcdState(
      CFANode pLocationInGivenCfa,
      CFANode pLocationInOriginalCfa,
      ImmutableSet<String> pChangedVarsInGivenCfa,
      boolean pHasRelevantModification) {
    locationInGivenCfa = pLocationInGivenCfa;
    locationInOriginalCfa = pLocationInOriginalCfa;
    changedVarsInGivenCfa = pChangedVarsInGivenCfa;
    hasRelevantModification = pHasRelevantModification;
  }

  public CFANode getLocationInOriginalCfa() {
    return locationInOriginalCfa;
  }

  public CFANode getLocationInGivenCfa() {
    return locationInGivenCfa;
  }

  public ImmutableSet<String> getChangedVarsInGivenCfa() {
    return changedVarsInGivenCfa;
  }

  public boolean hasRelevantModification() {
    return hasRelevantModification;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ModificationsRcdState that = (ModificationsRcdState) pO;
    return Objects.equals(locationInOriginalCfa, that.locationInOriginalCfa)
        && Objects.equals(locationInGivenCfa, that.locationInGivenCfa)
        && Objects.equals(changedVarsInGivenCfa, that.changedVarsInGivenCfa)
        && (hasRelevantModification == that.hasRelevantModification);
  }

  @Override
  public int hashCode() {

    return Objects.hash(
        locationInOriginalCfa, locationInGivenCfa, changedVarsInGivenCfa, hasRelevantModification);
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return hasRelevantModification;
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
    return ModificationsRcdCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case "is_modified":
        return hasRelevantModification;
      default:
        throw new InvalidQueryException(
            "Unknown query to " + getClass().getSimpleName() + ": " + pProperty);
    }
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (hasRelevantModification) {
      sb.append("Misfit: ");
      FluentIterable<CFAEdge> edgesInOrig = CFAUtils.enteringEdges(locationInOriginalCfa);
      sb.append("{");
      for (CFAEdge e : edgesInOrig) {
        sb.append(e);
        sb.append(", ");
      }
      sb.append("}");
    } else if (!changedVarsInGivenCfa.isEmpty()) {
      sb.append("Reaching changed definitions: ");
      sb.append("{");
      for (String v : changedVarsInGivenCfa) {
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
    return hasRelevantModification;
  }

  @Override
  public ModificationsRcdState join(ModificationsRcdState pOther)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLessOrEqual(ModificationsRcdState pOther)
      throws CPAException, InterruptedException {
    return Objects.equals(locationInOriginalCfa, pOther.locationInOriginalCfa)
        && Objects.equals(locationInGivenCfa, pOther.locationInGivenCfa)
        && hasRelevantModification == pOther.hasRelevantModification
        && pOther.changedVarsInGivenCfa.containsAll(changedVarsInGivenCfa);
  }
}
