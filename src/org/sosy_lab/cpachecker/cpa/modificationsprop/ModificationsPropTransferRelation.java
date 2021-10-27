// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
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
                if (!(pCfaEdge instanceof CDeclarationEdge
                    || pCfaEdge instanceof CFunctionCallEdge
                    || pCfaEdge instanceof CReturnStatementEdge)) {
                  // otherwise we will handle it later
                  helper.logCase("Taking case 4a.");
                  return ImmutableSet.of(helper.makeBad(locations));
                }
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
          if (pCfaEdge instanceof CDeclarationEdge) {
            for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
              if (edgeInOriginal instanceof CDeclarationEdge) {
                final CDeclaration declOr = ((CDeclarationEdge) edgeInOriginal).getDeclaration(),
                    declMo = ((CDeclarationEdge) pCfaEdge).getDeclaration();
                if (declOr.getOrigName().equals(declMo.getOrigName())) {
                  helper.logCase("Taking case 4 for different declarations or modified variables.");
                  return ImmutableSet.of(
                      new ModificationsPropState(
                          pCfaEdge.getSuccessor(),
                          edgeInOriginal.getSuccessor(),
                          new ImmutableSet.Builder<String>()
                              .addAll(changedVars)
                              .add(declOr.getOrigName())
                              .build(),
                          helper));
                }
              }
            }
          }
          if (pCfaEdge instanceof CReturnStatementEdge) {
            for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
              if (edgeInOriginal instanceof CReturnStatementEdge) {
                final CReturnStatementEdge retOr = (CReturnStatementEdge) edgeInOriginal,
                    retMo = (CReturnStatementEdge) pCfaEdge;
                if (helper.sameClassAndFunction(retMo.getSuccessor(), retOr.getSuccessor())) {
                  helper.logCase(
                      "Taking case 4 for function returns with modified variables or different statements.");
                  final ImmutableSet<String> returnChangedVars;
                  if (retMo.getReturnStatement() == null
                      || retOr.getReturnStatement() == null
                      || retMo.getReturnStatement().equals(retOr.getReturnStatement())
                          && Collections.disjoint(
                              retOr
                                  .getReturnStatement()
                                  .asAssignment()
                                  .orElseThrow()
                                  .getRightHandSide()
                                  .accept(new RHSVisitor()),
                              changedVars)) {
                    returnChangedVars = changedVars;
                  } else {
                    returnChangedVars =
                        new ImmutableSet.Builder<String>().addAll(changedVars).build();
                  }
                  return ImmutableSet.of(
                      new ModificationsPropState(
                          pCfaEdge.getSuccessor(),
                          edgeInOriginal.getSuccessor(),
                          returnChangedVars,
                          helper));
                }
              }
            }
          }
          if (pCfaEdge instanceof CFunctionCallEdge) {
            for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
              if (edgeInOriginal instanceof CFunctionCallEdge) {
                final CFunctionCallEdge callOr = (CFunctionCallEdge) edgeInOriginal,
                    callMo = (CFunctionCallEdge) pCfaEdge;
                final CFunctionEntryNode entryNodeOr = callOr.getSuccessor(),
                    entryNodeMo = callMo.getSuccessor();
                final List<String>
                    paramsOr =
                        entryNodeOr.getFunctionParameters().stream()
                            .map(param -> param.getQualifiedName())
                            .collect(ImmutableList.toImmutableList()),
                    paramsMo =
                        entryNodeMo.getFunctionParameters().stream()
                            .map(param -> param.getQualifiedName())
                            .collect(ImmutableList.toImmutableList());
                // we require that all old parameters must be contained in new parameters
                if (paramsMo.containsAll(paramsOr) && helper.sameClassAndFunction(entryNodeMo, entryNodeOr)) {
                  helper.logCase("Taking case 4 for function calls with modified variables.");
                  HashSet<String> modifiedAfterFunctionCall = new HashSet<>(changedVars);
                  for (String param : paramsOr) {
                    // if not equal expressions for parameter or variable in expression modified
                    if (!callMo
                            .getArguments()
                            .get(paramsMo.indexOf(param))
                            .equals(callOr.getArguments().get(paramsOr.indexOf(param)))
                        || !Collections.disjoint(
                            callMo
                                .getArguments()
                                .get(paramsMo.indexOf(param))
                                .accept(new VariableIdentifierVisitor()),
                            changedVars)) {
                      modifiedAfterFunctionCall.add(param);
                    } else {
                      if (changedVars.contains(param)) {
                        modifiedAfterFunctionCall.remove(param);
                      }
                    }
                  }
                  return ImmutableSet.of(
                      new ModificationsPropState(
                          pCfaEdge.getSuccessor(),
                          edgeInOriginal.getSuccessor(),
                          new ImmutableSet.Builder<String>()
                              .addAll(modifiedAfterFunctionCall)
                              .build(),
                          helper));
                }
              }
            }
          }

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
            return transformedImmutableSetCopy(
                assumptionSuccessors,
                nodeGiven -> new ModificationsPropState(nodeGiven, nodeOrig, cv, helper));
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
