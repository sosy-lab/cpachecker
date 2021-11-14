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
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
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
        AbstractStateWithLocation,
        LatticeAbstractState<ModificationsPropState>,
        Graphable {

  /** Bad is meant in the sense that this may not be covered by a condition. */
  private final boolean isBad;

  /** The location in the given, modified CFA. */
  private final CFANode locationInGivenCfa;

  /** The location in the original, unmodified CFA. */
  private final CFANode locationInOriginalCfa;

  /** Variables that might be modified between the programs here. */
  private final ImmutableSet<String> changedVariables;

  public ModificationsPropState(
      final CFANode pLocationInGivenCfa,
      final CFANode pLocationInOriginalCfa,
      final ImmutableSet<String> pChangedVars,
      final ModificationsPropHelper pHelper) {
    CFANode nodeInOriginal = pHelper.skipUntrackedOperations(pLocationInOriginalCfa);
    // CFANode nodeInGiven = pLocationInGivenCfa;
    ImmutableSet<String> changedVars = pChangedVars;

    // skip untracked parts in original
    nodeInOriginal = pHelper.skipUntrackedOperations(nodeInOriginal);

    // rule out case 2
    if (!pHelper.inReachabilityProperty(nodeInOriginal)
        // case 3
        && pHelper.inReachabilityProperty(pLocationInGivenCfa)) {
      CFANode nodeInOrigNew = nodeInOriginal;
      do {
        nodeInOriginal = nodeInOrigNew;
        nodeInOrigNew = pHelper.skipUntrackedOperations(nodeInOriginal);
        ImmutableTuple<CFANode, ImmutableSet<String>> tup =
            pHelper.skipAssignment(nodeInOrigNew, changedVars);
        nodeInOrigNew = tup.getFirst();
        changedVars = tup.getSecond();
      } while (nodeInOrigNew != nodeInOriginal);
      locationInOriginalCfa = nodeInOriginal;
      changedVariables = changedVars;
      if (pHelper.inReachabilityProperty(nodeInOrigNew)) {
        // Some path covers the bad location here.
        pHelper.logCase("Taking case 3a.");
        isBad = false;
      } else {
        pHelper.logCase("Taking case 3b.");
        isBad = true;
      }
    } else {
      locationInOriginalCfa = nodeInOriginal;
      changedVariables = changedVars;
      isBad = false;
    }
    locationInGivenCfa = pLocationInGivenCfa;
  }

  public ModificationsPropState(
      final CFANode pLocationInGivenCfa,
      final CFANode pLocationInOriginalCfa,
      final ImmutableSet<String> pChangedVars,
      final boolean pIsBad) {
    locationInGivenCfa = pLocationInGivenCfa;
    locationInOriginalCfa = pLocationInOriginalCfa;
    changedVariables = pChangedVars;
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

    // If locations differ, we should never call join. The merge operator used guarantees that.
    assert locationInGivenCfa.equals(pOther.locationInGivenCfa);
    assert locationInOriginalCfa.equals(pOther.locationInOriginalCfa);

    // The first (with pBad || pOther.bad) and last case would semantically be sufficient. However,
    // we want to reuse as many state objects as possible for efficiency.
    if (isBad) {
      // bad location should have empty variable set, introduced by transfer relation
      assert changedVariables.isEmpty();
      return this;
    } else if (pOther.isBad() || pOther.getChangedVariables().containsAll(changedVariables)) {
      assert !pOther.isBad || pOther.getChangedVariables().isEmpty();
      return pOther;
    } else if (changedVariables.containsAll(pOther.getChangedVariables())) {
      return this;
    } else {
      // only create new object if really necessary
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
        && (pOther.isBad || !isBad)
        // variables modifier must be a subset, else join is necessary
        // can stop if location pair is already known to be bad
        && (pOther.changedVariables.containsAll(changedVariables) || pOther.isBad);
  }

  @Override
  public CFANode getLocationNode() {
    return getLocationInGivenCfa();
  }
}
