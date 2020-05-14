/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.modifications;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ModificationsTransferRelation extends SingleEdgeTransferRelation {

  private final boolean ignoreDeclarations;
  private final Map<String, Set<String>> funToVarsOrig;
  private final Map<String, Set<String>> funToVarsGiven;

  public ModificationsTransferRelation(
      final boolean pIgnoreDeclarations,
      final Map<String, Set<String>> pFunToVarsOrig,
      final Map<String, Set<String>> pFunToVarsGiven) {
    ignoreDeclarations = pIgnoreDeclarations;
    funToVarsOrig = pFunToVarsOrig;
    funToVarsGiven = pFunToVarsGiven;
  }

  public ModificationsTransferRelation() {
    this(false, ImmutableMap.of(), ImmutableMap.of());
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ModificationsState locations = (ModificationsState) pState;

    if (!locations.hasModification()) {
      CFANode nodeInGiven = locations.getLocationInGivenCfa();
      CFANode nodeInOriginal = locations.getLocationInOriginalCfa();

      if (CFAUtils.leavingEdges(nodeInGiven).contains(pCfaEdge)) {
        // possible successor adheres to control-flow
        CFANode succInGiven = pCfaEdge.getSuccessor();
        Collection<ModificationsState> successors = new HashSet<>();

        Optional<ModificationsState> potSucc;
        for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
          potSucc = findMatchingSuccessor(pCfaEdge, edgeInOriginal);
          if (potSucc.isPresent()) {
            // We assume that the edges leaving a node are disjunct.
            // Otherwise, we'll have to collect the set of differential states here
            // and return all possibilities
            successors.add(potSucc.orElseThrow());
            break;
          }
        }

        // If no outgoing edge matched, add all outgoing edges to list of modified edges
        if (successors.isEmpty()) {
          for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
            successors.add(
                new ModificationsState(succInGiven, edgeInOriginal.getSuccessor(), true));
          }
        }

        assert !successors.isEmpty()
            : "List of successors should never be empty if previous state represents no modification";
        return successors;
      }
    }

    // if current location doesn't have edge as outgoing edge, or
    // if previous state already depicts modification
    return ImmutableSet.of();
  }

  private Optional<ModificationsState> findMatchingSuccessor(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    CFAEdge originalEdge = pEdgeInOriginal;

    boolean stuttered;
    do {

      stuttered = false;

      if (edgesMatch(pEdgeInGiven, originalEdge)) {
        return Optional.of(
            new ModificationsState(pEdgeInGiven.getSuccessor(), originalEdge.getSuccessor()));
      }

      // assume that variables are not renamed
      // only new declarations are added and existing declarations are deleted
      if (ignoreDeclarations) {

        if (pEdgeInGiven instanceof CDeclarationEdge) {
          if (!declarationNameAlreadyExistsInOtherCFA(true, (CDeclarationEdge) pEdgeInGiven)) {
            return Optional.of(
                new ModificationsState(pEdgeInGiven.getSuccessor(), originalEdge.getPredecessor()));
          }
        }

        if (originalEdge instanceof CDeclarationEdge) {
          if (!declarationNameAlreadyExistsInOtherCFA(false, (CDeclarationEdge) originalEdge)) {
            if (originalEdge.getSuccessor().getNumLeavingEdges() == 1) {
              originalEdge = originalEdge.getSuccessor().getLeavingEdge(0);
              stuttered = true;
            }
          }
        }
      }
    } while (stuttered);

    return Optional.empty();
  }

  private boolean declarationNameAlreadyExistsInOtherCFA(final boolean isOtherOrigCFA, final CDeclarationEdge pDeclEdge) {
    if(!pDeclEdge.getDeclaration().isGlobal()) {
      if (containsDeclaration(
          isOtherOrigCFA
              ? funToVarsOrig.get(pDeclEdge.getSuccessor().getFunctionName())
              : funToVarsGiven.get(pDeclEdge.getSuccessor().getFunctionName()),
          pDeclEdge.getDeclaration().getOrigName())) {
        return true;
      }
    }

    return containsDeclaration(
        isOtherOrigCFA ? funToVarsOrig.get("") : funToVarsGiven.get(""), pDeclEdge.getDeclaration().getOrigName());
  }

  private boolean containsDeclaration(@Nullable final Set<String> varNames, final String varName) {
    return varNames != null && varNames.contains(varName);
  }

  private boolean edgesMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    String firstAst = pEdgeInGiven.getRawStatement();
    String sndAst = pEdgeInOriginal.getRawStatement();

    return firstAst.equals(sndAst)
        && pEdgeInGiven.getEdgeType() == pEdgeInOriginal.getEdgeType()
        && successorsMatch(pEdgeInGiven, pEdgeInOriginal);
  }

  private boolean successorsMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    CFANode givenSuccessor = pEdgeInGiven.getSuccessor(),
        originalSuccessor = pEdgeInOriginal.getSuccessor();
    if (pEdgeInGiven.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      nextEdge:
      for (CFAEdge enterBeforeCall :
          CFAUtils.enteringEdges(
              ((FunctionReturnEdge) pEdgeInGiven).getSummaryEdge().getPredecessor())) {
        for (CFAEdge enterOriginalBeforeCAll :
            CFAUtils.enteringEdges(
                ((FunctionReturnEdge) pEdgeInOriginal).getSummaryEdge().getPredecessor())) {
          if (edgesMatch(enterBeforeCall, enterOriginalBeforeCAll)) {
            continue nextEdge;
          }
        }
        return false;
      }
    }

    return givenSuccessor.getClass() == originalSuccessor.getClass()
        && givenSuccessor.getFunctionName().equals(originalSuccessor.getFunctionName());
  }
}
