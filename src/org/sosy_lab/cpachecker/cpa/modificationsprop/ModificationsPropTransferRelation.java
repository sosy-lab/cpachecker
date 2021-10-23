// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.modificationsrcd.VariableIdentifierVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Transfer relation for abstract states used in variable-dependent difference verification. */
public class ModificationsPropTransferRelation extends SingleEdgeTransferRelation {

  private final ModificationsPropHelper helper;

  public ModificationsPropTransferRelation(final ModificationsPropHelper pHelper) {
    helper = pHelper;
  }

  @Override
  public Collection<ModificationsPropState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    return getAbstractSuccessorsForEdge(pState, pPrecision, pCfaEdge, null);
  }

  // Cases mentioned here relate to the respective master thesis
  /**
   * @param pState current abstract state
   * @param pPrecision precision for abstract state
   * @param pCfaEdge the edge for which the successors should be computed
   * @param emergencyStop recursion anchor for recursive calls to disallow infinite loops
   * @return collection of all successors of the current state (may be empty)
   * @throws CPATransferException exceptions in transfer relation
   * @throws InterruptedException interruption for timeout
   */
  public Collection<ModificationsPropState> getAbstractSuccessorsForEdge(
      final AbstractState pState,
      final Precision pPrecision,
      final CFAEdge pCfaEdge,
      @Nullable final CFANode emergencyStop)
      throws CPATransferException, InterruptedException {

    final ModificationsPropState locations = (ModificationsPropState) pState;
    final CFANode nodeInGiven = locations.getLocationInGivenCfa();
    final CFANode nodeInOriginal = locations.getLocationInOriginalCfa();
    final ImmutableSet<String> changedVars = locations.getChangedVariables();

    if (!locations.isBad()) {

      // rule out case 2
      if (!helper.inReachabilityProperty(nodeInOriginal)) {

        // case 3 outsourced to abstract state creation

        if (CFAUtils.leavingEdges(nodeInGiven).contains(pCfaEdge)) {

          // prepare further cases by skipping ignored operations
          if (helper.isUntracked(pCfaEdge)) {
            helper.logCase("Skipping ignored CFA edge for given program.");
            return ImmutableSet.of(
                new ModificationsPropState(
                    pCfaEdge.getSuccessor(), nodeInOriginal, changedVars, helper));
          }

          // case 4
          for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
            if (helper.edgesMatch(pCfaEdge, edgeInOriginal)) {
              final ImmutableSet<String> changedVarsInSuccessor =
                  helper.modifySetForAssignment(edgeInOriginal, changedVars);
              if (helper.variablesAreUsedInEdge(pCfaEdge, changedVars)) {
                helper.logCase("Taking case 4a.");
                return ImmutableSet.of(helper.makeBad(locations));
              } else {
                helper.logCase("Taking case 4b.");
                return ImmutableSet.of(
                    new ModificationsPropState(
                        pCfaEdge.getSuccessor(),
                        edgeInOriginal.getSuccessor(),
                        changedVarsInSuccessor,
                        helper));
              }
            }
          }

          // TODO: track function calls with modified vars?

          // look for assignments to same variable (cases 5/6)
          // This could be left out, but we expect to find more related statements this way.
          ImmutableTuple<CFANode, ImmutableSet<String>>
              givenTup = helper.skipAssignment(nodeInGiven, changedVars),
              originalTup = helper.skipAssignment(nodeInGiven, changedVars);
          if (!(givenTup.getFirst().equals(nodeInGiven)
              || originalTup.getFirst().equals(nodeInGiven))) {
            if (CFAEdgeUtils.getLeftHandVariable(nodeInGiven.getLeavingEdge(0))
                .equals(CFAEdgeUtils.getLeftHandVariable(nodeInOriginal.getLeavingEdge(0)))) {
              helper.logCase("Combining cases 5 and 6 for same variable.");
              // modified variable sets do not differ
              return ImmutableSet.of(
                  new ModificationsPropState(
                      givenTup.getFirst(), originalTup.getFirst(), givenTup.getSecond(), helper));
            }
          }

          // case 5
          ImmutableTuple<CFANode, ImmutableSet<String>> tup =
              helper.skipAssignment(nodeInGiven, changedVars);
          if (!tup.getFirst().equals(nodeInGiven)) {
            helper.logCase("Taking case 5.");
            return ImmutableSet.of(
                new ModificationsPropState(
                    tup.getFirst(), nodeInOriginal, tup.getSecond(), helper));
          }

          // case 6
          // assuming there is no infinite sequence of assignments only in orginal program, else
          // performing emergency stop
          tup = helper.skipAssignment(nodeInOriginal, changedVars);
          if (!tup.getFirst().equals(nodeInOriginal)) {
            helper.logCase("Taking case 6.");
            if (emergencyStop != null && emergencyStop.equals(tup.getFirst())) {
              helper.logProblem("Found infinite sequence of assignments in original program.");
              return ImmutableSet.of(helper.makeBad(locations));
            } else {
              return getAbstractSuccessorsForEdge(
                  new ModificationsPropState(nodeInGiven, tup.getFirst(), tup.getSecond(), helper),
                  pPrecision,
                  pCfaEdge);
            }
          }

          // case 7
          if (helper.getImplicationCheck()) {
            if (pCfaEdge instanceof CAssumeEdge) {
              CAssumeEdge assGiven = (CAssumeEdge) pCfaEdge;
              for (CFAEdge ce : CFAUtils.leavingEdges(nodeInOriginal)) {
                if (ce instanceof CAssumeEdge) {
                  final CAssumeEdge assOrig = (CAssumeEdge) ce;
                  helper.logCase("Checking for case 7 compliance.");
                  if (helper.implies(assGiven, assOrig)) {
                    VariableIdentifierVisitor visitor = new VariableIdentifierVisitor();
                    final Set<String> varsInAssGiven = assGiven.getExpression().accept(visitor);
                    final Set<String> varsInAssOrig = assOrig.getExpression().accept(visitor);
                    ImmutableSet<String> varsUsedInBoth =
                        new ImmutableSet.Builder<String>()
                            .addAll(varsInAssGiven)
                            .addAll(varsInAssOrig)
                            .build();
                    if (Collections.disjoint(changedVars, varsUsedInBoth)) {
                      helper.logCase("Taking case 7a.");
                      return ImmutableSet.of(
                          new ModificationsPropState(
                              assGiven.getSuccessor(),
                              assOrig.getSuccessor(),
                              changedVars,
                              helper));
                    } else {
                      helper.logCase("Taking case 7b.");
                      return ImmutableSet.of(helper.makeBad(locations));
                    }
                  }
                  helper.logCase("No implication. Continuing.");
                }
              }
            }
          }

          // case 8
          ImmutableSet<CFANode> assumptionSuccessors =
              helper.skipAssumption(nodeInGiven, changedVars);
          if (!assumptionSuccessors.isEmpty()) {
            helper.logCase("Taking case 8.");
            final ImmutableSet<String> cv = changedVars;
            final CFANode nodeOrig = nodeInOriginal;
            return assumptionSuccessors.stream()
                .map(nodeGiven -> new ModificationsPropState(nodeGiven, nodeOrig, cv, helper))
                .collect(Collectors.toUnmodifiableSet());
          } else {
            // case 9
            helper.logCase("Taking case 9.");
            return ImmutableSet.of(helper.makeBad(locations));
          }
        }
      }
      helper.logCase("Taking case 2.");
    }

    // if current location pair is bad or no outgoing edge
    return ImmutableSet.of();
  }
}
