// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
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
import org.sosy_lab.cpachecker.util.Pair;
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
  private final CFANode locationInModCfa;

  /** The location in the original, unmodified CFA. */
  private final CFANode locationInOriginalCfa;

  /** Variables that might be modified between the programs here. */
  private final ImmutableSet<String> changedVariables;

  /** Stack to track function return edges to take in original program. */
  private final Deque<CFANode> originalStack;

  public ModificationsPropState(
      final CFANode pLocationInGivenCfa,
      final CFANode pLocationInOriginalCfa,
      final ImmutableSet<String> pChangedVars,
      final Deque<CFANode> pStack,
      final ModificationsPropHelper pHelper) {
    CFANode nodeInOriginal = pHelper.skipUntrackedOperations(pLocationInOriginalCfa);

    ImmutableSet<String> changedVars = pChangedVars;

    // rule out case 2
    if (!pHelper.isErrorLocation(nodeInOriginal)
        // case 3
        && pHelper.isErrorLocation(pLocationInGivenCfa)) {
      CFANode nodeInOrigNew = nodeInOriginal;
      do {
        nodeInOriginal = nodeInOrigNew;
        nodeInOrigNew = pHelper.skipUntrackedOperations(nodeInOriginal);
        Pair<CFANode, ImmutableSet<String>> tup =
            pHelper.skipAssignment(nodeInOrigNew, changedVars);
        nodeInOrigNew = tup.getFirst();
        changedVars = tup.getSecond();
      } while (nodeInOrigNew != nodeInOriginal);
      locationInOriginalCfa = nodeInOriginal;
      changedVariables = changedVars;
      if (pHelper.isErrorLocation(locationInOriginalCfa)) {
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
    locationInModCfa = pLocationInGivenCfa;
    originalStack = pStack;
  }

  public ModificationsPropState(
      final CFANode pLocationInGivenCfa,
      final CFANode pLocationInOriginalCfa,
      final ImmutableSet<String> pChangedVars,
      final Deque<CFANode> pStack,
      final boolean pIsBad) {
    locationInModCfa = pLocationInGivenCfa;
    locationInOriginalCfa = pLocationInOriginalCfa;
    changedVariables = pChangedVars;
    isBad = pIsBad;
    originalStack = pStack;
  }

  /**
   * Converts a given state to a (therefore more abstract) bad state at that location.
   *
   * @return the bad state
   */
  ModificationsPropState makeBad() {
    return new ModificationsPropState(
        getLocationInModCfa(),
        getLocationInOriginalCfa(),
        ImmutableSet.of(),
        new ArrayDeque<CFANode>(),
        true);
  }

  public CFANode getLocationInOriginalCfa() {
    return locationInOriginalCfa;
  }

  public CFANode getLocationInModCfa() {
    return locationInModCfa;
  }

  public ImmutableSet<String> getChangedVariables() {
    return changedVariables;
  }

  public Deque<CFANode> getOriginalStack() {
    return originalStack;
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
        && Objects.equals(locationInModCfa, that.locationInModCfa)
        && Objects.equals(changedVariables, that.changedVariables)
        && Arrays.equals(originalStack.toArray(), that.originalStack.toArray())
        && (isBad == that.isBad);
  }

  @Override
  public int hashCode() {

    return Objects.hash(locationInOriginalCfa, locationInModCfa, changedVariables, isBad);
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
      FluentIterable<CFAEdge> edgesInMod = CFAUtils.enteringEdges(locationInModCfa);
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
  public ModificationsPropState join(final ModificationsPropState pOther)
      throws CPAException, InterruptedException {

    // If locations differ, we should never call join. The merge operator used guarantees that.
    assert locationInModCfa.equals(pOther.locationInModCfa);
    assert locationInOriginalCfa.equals(pOther.locationInOriginalCfa);
    assert Arrays.equals(originalStack.toArray(), pOther.originalStack.toArray());

    // The first (with pBad || pOther.bad) and last case would semantically be sufficient. However,
    // we want to reuse as many state objects as possible for efficiency.
    if (pOther.isBad() || (!isBad && pOther.getChangedVariables().containsAll(changedVariables))) {
      assert !pOther.isBad || pOther.getChangedVariables().isEmpty();
      return pOther;
    } else if (isBad || changedVariables.containsAll(pOther.getChangedVariables())) {
      // bad location should have empty variable set, introduced by transfer relation
      assert !isBad || changedVariables.isEmpty();
      return this;
    } else {
      // only create new object if really necessary
      return new ModificationsPropState(
          locationInModCfa,
          locationInOriginalCfa,
          // join modified variables for more abstract states, could be omitted
          ImmutableSet.<String>builder()
              .addAll(changedVariables)
              .addAll(pOther.changedVariables)
              .build(),
          originalStack,
          false);
    }
  }

  @Override
  public boolean isLessOrEqual(final ModificationsPropState pOther)
      throws CPAException, InterruptedException {
    // location pair must be identical
    return Objects.equals(locationInOriginalCfa, pOther.locationInOriginalCfa)
        && Objects.equals(locationInModCfa, pOther.locationInModCfa)
        && Arrays.equals(originalStack.toArray(), pOther.originalStack.toArray())
        // if this state is bad, the other one must be bad as well
        && (pOther.isBad || !isBad)
        // variables modifier must be a subset, else join is necessary
        // can stop if location pair is already known to be bad
        && (pOther.changedVariables.containsAll(changedVariables) || pOther.isBad);
  }

  @Override
  public CFANode getLocationNode() {
    return getLocationInModCfa();
  }
}
