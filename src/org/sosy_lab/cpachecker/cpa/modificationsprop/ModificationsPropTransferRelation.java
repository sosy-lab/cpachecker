// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
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
      if (!helper.inReachabilityProperty(nodeInOriginal)
          && helper.goalReachable(nodeInGiven, false)) {

        if (!helper.goalReachable(nodeInOriginal, true)) {
          helper.logCase(
              "Original program cannot reach an error location anymore. Making state bad.");
          return ImmutableSet.of(helper.makeBad(locations));
        }

        // case 3 outsourced to abstract state creation

        if (CFAUtils.leavingEdges(nodeInGiven).contains(pCfaEdge)) {
          // helper.logCase(pCfaEdge.getCode());
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
              final ImmutableSet<String> changedVarsInSuccessor;
              try {
                changedVarsInSuccessor = helper.modifySetForAssignment(edgeInOriginal, changedVars);
              } catch (PointerAccessException e) {
                helper.logProblem("Caution: Pointer or similar detected.");
                return ImmutableSet.of(helper.makeBad(locations));
              }
              if (helper.variablesAreUsedInEdge(pCfaEdge, changedVars)) {
                if (!(pCfaEdge instanceof CDeclarationEdge
                    || pCfaEdge instanceof CFunctionCallEdge
                    || pCfaEdge instanceof CReturnStatementEdge
                    || pCfaEdge instanceof CFunctionReturnEdge)) {
                  // otherwise we will handle it later
                  helper.logCase("Taking case 4a.");
                  return ImmutableSet.of(helper.makeBad(locations));
                }
              } else {
                // For function return edges we have to be careful, as modified return values modify
                // variables above.
                if (!(pCfaEdge instanceof CFunctionReturnEdge)) {
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
                if (helper.sameFunction(retMo.getSuccessor(), retOr.getSuccessor())) {
                  helper.logCase(
                      "Taking case 4 for function returns with modified variables or different statements.");
                  final ImmutableSet<String> returnChangedVars;
                  try {
                    if (retMo.getReturnStatement() == null
                        || retOr.getReturnStatement() == null
                        || (retMo.getReturnStatement().equals(retOr.getReturnStatement())
                            && Collections.disjoint(
                                retOr
                                    .getReturnStatement()
                                    .asAssignment()
                                    .orElseThrow()
                                    .getRightHandSide()
                                    .accept(helper.getVisitor()),
                                changedVars))) {
                      final ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
                      final Set<String> leftVars =
                          retMo
                              .getReturnStatement()
                              .asAssignment()
                              .orElseThrow()
                              .getLeftHandSide()
                              .accept(helper.getVisitor());
                      // remove return lhs, because return value is equal \o/
                      for (String var : changedVars) {
                        if (!leftVars.contains(var)) {
                          builder.add(var);
                        }
                      }
                      returnChangedVars = builder.build();
                    } else {
                      returnChangedVars =
                          new ImmutableSet.Builder<String>()
                              .addAll(changedVars)
                              .addAll(
                                  retMo
                                      .getReturnStatement()
                                      .asAssignment()
                                      .orElseThrow()
                                      .getLeftHandSide()
                                      .accept(helper.getVisitor()))
                              .build();
                    }
                    return ImmutableSet.of(
                        new ModificationsPropState(
                            pCfaEdge.getSuccessor(),
                            edgeInOriginal.getSuccessor(),
                            returnChangedVars,
                            helper));

                  } catch (PointerAccessException e) {
                    helper.logProblem("Caution: Pointer or similar detected.");
                    return ImmutableSet.of(helper.makeBad(locations));
                  }
                }
              }
            }
          }
          if (pCfaEdge instanceof CFunctionReturnEdge) {
            for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
              edgeInOriginal =
                  CFAUtils.leavingEdges(nodeInOriginal)
                      .get(
                          CFAUtils.leavingEdges(nodeInGiven)
                              .toList()
                              .indexOf(pCfaEdge)); // TODO: very much of a hack!!! If no better way,
              // delete for
              if (edgeInOriginal instanceof CFunctionReturnEdge) {
                final CFunctionReturnEdge retOr = (CFunctionReturnEdge) edgeInOriginal,
                    retMo = (CFunctionReturnEdge) pCfaEdge;
                if (helper.sameFunction(retMo.getSuccessor(), retOr.getSuccessor())) {
                  helper.logCase(
                      "Taking case 4 for function return statement with modified variables or different statements.");

                  final CFunctionCall summaryOr = retOr.getSummaryEdge().getExpression(),
                      summaryMo = retOr.getSummaryEdge().getExpression();
                  if (summaryOr instanceof CFunctionCallAssignmentStatement
                      && summaryMo instanceof CFunctionCallAssignmentStatement) {
                    CFunctionCallAssignmentStatement
                        summaryOrAss = (CFunctionCallAssignmentStatement) summaryOr,
                        summaryMoAss = (CFunctionCallAssignmentStatement) summaryMo;
                    try {
                      // add variables as modified if LHS or RHS not equal or return value modified
                      if (!summaryOrAss.getLeftHandSide().equals(summaryMoAss.getLeftHandSide())
                          // TODO: this is currently more of a hack for __retval__.
                          || changedVars.contains(
                              retMo.getPredecessor().getFunctionName() + "::__retval__")
                          || !summaryOrAss
                              .getRightHandSide()
                              .equals(summaryMoAss.getRightHandSide())) {
                        return ImmutableSet.of(
                            new ModificationsPropState(
                                pCfaEdge.getSuccessor(),
                                edgeInOriginal.getSuccessor(),
                                new ImmutableSet.Builder<String>()
                                    .addAll(changedVars)
                                    .addAll(
                                        summaryMoAss.getLeftHandSide().accept(helper.getVisitor()))
                                    .addAll(
                                        summaryOrAss.getLeftHandSide().accept(helper.getVisitor()))
                                    .build(),
                                helper));
                      }
                    } catch (PointerAccessException e) {
                      helper.logProblem("Caution: Pointer or similar detected.");
                      return ImmutableSet.of(helper.makeBad(locations));
                    }
                  }
                  if (summaryOr instanceof CFunctionCallAssignmentStatement
                      ^ summaryMo instanceof CFunctionCallAssignmentStatement) {
                    // Exactly one is an assignment. This can happen, but we do not expect to find
                    // a similar structure anymore.
                    helper.logCase(
                        "Assignment and non-assignment with call of same function. Stopping here.");
                    return ImmutableSet.of(helper.makeBad(locations));
                  }
                  return ImmutableSet.of(
                      new ModificationsPropState(
                          pCfaEdge.getSuccessor(),
                          edgeInOriginal.getSuccessor(),
                          changedVars,
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
                        transformedImmutableListCopy(
                            entryNodeOr.getFunctionParameters(), param -> param.getQualifiedName()),
                    paramsMo =
                        transformedImmutableListCopy(
                            entryNodeMo.getFunctionParameters(), param -> param.getQualifiedName());
                // we require that all old parameters must be contained in new parameters
                if (paramsMo.containsAll(paramsOr)
                    && helper.sameFunction(entryNodeMo, entryNodeOr)) {
                  helper.logCase("Taking case 4 for function calls with modified variables.");
                  Set<String> modifiedAfterFunctionCall = new HashSet<>(changedVars);
                  for (String param : paramsOr) {
                    // if not equal expressions for parameter or variable in expression modified
                    try {
                      if (!callMo
                              .getArguments()
                              .get(paramsMo.indexOf(param))
                              .equals(callOr.getArguments().get(paramsOr.indexOf(param)))
                          || !Collections.disjoint(
                              callMo
                                  .getArguments()
                                  .get(paramsMo.indexOf(param))
                                  .accept(helper.getVisitor()),
                              changedVars)) {
                        modifiedAfterFunctionCall.add(param);
                      } else if (changedVars.contains(param)) {
                        modifiedAfterFunctionCall.remove(param);
                      }
                    } catch (PointerAccessException e) {
                      helper.logProblem("Caution: Pointer or similar detected.");
                      return ImmutableSet.of(helper.makeBad(locations));
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
              originalTup = helper.skipAssignment(nodeInOriginal, changedVars);
          if (!(givenTup.getFirst().equals(nodeInGiven)
              || originalTup.getFirst().equals(nodeInOriginal))) {
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
                    final Set<String> varsInAssGiven, varsInAssOrig;
                    try {
                      varsInAssGiven = assGiven.getExpression().accept(helper.getVisitor());
                      varsInAssOrig = assOrig.getExpression().accept(helper.getVisitor());
                    } catch (PointerAccessException e) {
                      helper.logProblem("Caution: Pointer or similar detected.");
                      return ImmutableSet.of(helper.makeBad(locations));
                    }

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
          if (pCfaEdge instanceof CAssumeEdge) {
            helper.logCase("Taking case 8.");
            return ImmutableSet.of(
                new ModificationsPropState(
                    pCfaEdge.getSuccessor(), nodeInOriginal, changedVars, helper));
          } else {
            // case 9
            helper.logCase("Taking case 9.");
            return ImmutableSet.of(helper.makeBad(locations));
          }
        }
      }
      if (helper.inReachabilityProperty(nodeInOriginal)) {
        helper.logCase("Taking case 2.");
      } else {
        helper.logCase("No error location reachable. Stopping here.");
      }
    }

    // if current location pair is bad or no outgoing edge
    return ImmutableSet.of();
  }
}
