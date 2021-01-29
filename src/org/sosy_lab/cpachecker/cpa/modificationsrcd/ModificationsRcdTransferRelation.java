// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsrcd;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ModificationsRcdTransferRelation extends SingleEdgeTransferRelation {

  private final boolean ignoreDeclarations;
  private final Map<String, Set<String>> funToVarsOrig;
  private final Map<String, Set<String>> funToVarsGiven;

  public ModificationsRcdTransferRelation(
      final boolean pIgnoreDeclarations,
      final Map<String, Set<String>> pFunToVarsOrig,
      final Map<String, Set<String>> pFunToVarsGiven) {
    ignoreDeclarations = pIgnoreDeclarations;
    funToVarsOrig = pFunToVarsOrig;
    funToVarsGiven = pFunToVarsGiven;
  }

  public ModificationsRcdTransferRelation() {
    this(false, ImmutableMap.of(), ImmutableMap.of());
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ModificationsRcdState locations = (ModificationsRcdState) pState;

    if (!locations.hasRelevantModification()) {
      CFANode nodeInGiven = locations.getLocationInGivenCfa();
      CFANode nodeInOriginal = locations.getLocationInOriginalCfa();
      ImmutableSet<String> changedVarsInGiven = locations.getChangedVarsInGivenCfa();

      if (CFAUtils.leavingEdges(nodeInGiven).contains(pCfaEdge)) {
        // possible successor adheres to control-flow
        CFANode succInGiven = pCfaEdge.getSuccessor();
        Collection<ModificationsRcdState> successors = new HashSet<>();

        Optional<ModificationsRcdState> potSucc;
        for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
          potSucc = findMatchingSuccessor(pCfaEdge, edgeInOriginal, changedVarsInGiven);
          if (potSucc.isPresent()) {

            // Check that no changed variable is used (TODO)
            if (variablesAreUsedInEdge(edgeInOriginal, changedVarsInGiven)) {
              break;
            }

            // We assume that the edges leaving a node are disjunct.
            // Otherwise, we'll have to collect the set of differential states here
            // and return all possibilities
            successors.add(potSucc.orElseThrow());
            break;
          }
        }

        // If no outgoing edge matched, add all outgoing edges to list of modified edges.
        // Since the modified edges do not have any successors we do not need to worry about the
        // changed variables any more and can simply pass an empty set instead.
        if (successors.isEmpty()) {
          for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
            successors.add(
                new ModificationsRcdState(
                    succInGiven, edgeInOriginal.getSuccessor(), ImmutableSet.of(), true));
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

  private Optional<ModificationsRcdState> findMatchingSuccessor(
      final CFAEdge pEdgeInGiven,
      final CFAEdge pEdgeInOriginal,
      ImmutableSet<String> pChangedVarsInGiven) {
    CFAEdge originalEdge = pEdgeInOriginal;

    boolean stuttered;
    do {

      stuttered = false;

      if (edgesMatch(pEdgeInGiven, originalEdge)) {
        return Optional.of(
            new ModificationsRcdState(
                pEdgeInGiven.getSuccessor(), originalEdge.getSuccessor(), pChangedVarsInGiven));
      }

      if (pEdgeInOriginal.getPredecessor().getNumLeavingEdges() == 1
          && pEdgeInGiven.getPredecessor().getNumLeavingEdges() == 1) {
        Optional<String> changed = checkEdgeChangedAssignment(pEdgeInGiven, originalEdge);
        if (changed.isPresent()) {
          ImmutableSet<String> changedVarsInSuccessor =
              new ImmutableSet.Builder<String>()
                  .addAll(pChangedVarsInGiven)
                  .add(changed.orElseThrow())
                  .build();
          return Optional.of(
              new ModificationsRcdState(
                  pEdgeInGiven.getSuccessor(),
                  originalEdge.getSuccessor(),
                  changedVarsInSuccessor));
        }
      }

      // ==TODO: MORE CASES HERE ==========================================
      // ==================================================================
      // ==================================================================
      // ==================================================================
      // ==================================================================

      // assume that variables are not renamed
      // only new declarations are added and existing declarations are deleted
      if (ignoreDeclarations) {

        if (pEdgeInGiven instanceof CDeclarationEdge) {
          if (!declarationNameAlreadyExistsInOtherCFA(true, (CDeclarationEdge) pEdgeInGiven)) {
            return Optional.of(
                new ModificationsRcdState(
                    pEdgeInGiven.getSuccessor(),
                    originalEdge.getPredecessor(),
                    pChangedVarsInGiven));
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

  private Optional<String> checkEdgeChangedAssignment(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    if ((pEdgeInGiven instanceof CStatementEdge) && (pEdgeInOriginal instanceof CStatementEdge)) {
      CStatement stmtInOriginal = ((CStatementEdge) pEdgeInOriginal).getStatement();
      CStatement stmtInGiven = ((CStatementEdge) pEdgeInGiven).getStatement();

      CLeftHandSide lhsInOriginal;
      CLeftHandSide lhsInGiven;
      if ((stmtInGiven instanceof CExpressionAssignmentStatement)
          && (stmtInOriginal instanceof CExpressionAssignmentStatement)) {
        lhsInOriginal = ((CExpressionAssignmentStatement) stmtInOriginal).getLeftHandSide();
        lhsInGiven = ((CExpressionAssignmentStatement) stmtInGiven).getLeftHandSide();
      } else if ((stmtInGiven instanceof CFunctionCallAssignmentStatement)
          && (stmtInOriginal instanceof CFunctionCallAssignmentStatement)) {
        lhsInOriginal = ((CFunctionCallAssignmentStatement) stmtInOriginal).getLeftHandSide();
        lhsInGiven = ((CFunctionCallAssignmentStatement) stmtInGiven).getLeftHandSide();
      } else {
        return Optional.empty();
      }

      if ((lhsInGiven instanceof AIdExpression) && (lhsInOriginal instanceof AIdExpression)) {
        ASimpleDeclaration declInOriginal = ((AIdExpression) lhsInOriginal).getDeclaration();
        ASimpleDeclaration declInGiven = ((AIdExpression) lhsInGiven).getDeclaration();

        if ((declInOriginal instanceof AVariableDeclaration)
            && (declInGiven instanceof AVariableDeclaration)
            && ((AVariableDeclaration) declInOriginal)
                .getName()
                .equals(((AVariableDeclaration) declInGiven).getName())
            && successorsMatch(pEdgeInGiven, pEdgeInOriginal)) {
          return Optional.of(((AVariableDeclaration) declInOriginal).getName());
        }
      }
    }

    return Optional.empty();
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

  @SuppressWarnings("unused") // TODO: remove annotation when done
  private boolean variablesAreUsedInEdge(final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    CAstNode expressionToCheck;

    ALeftHandSide lhs = CFAEdgeUtils.getLeftHandSide(pEdge); // might be null if not an assignment
    CRightHandSide rhs = CFAEdgeUtils.getRightHandSide(pEdge);
    if (lhs != null
        && lhs instanceof CIdExpression
        && CFAEdgeUtils.getLeftHandVariable(pEdge) != null
        && rhs != null) {
      expressionToCheck = rhs;
    } else if (pEdge.getRawAST().isPresent()) {
      expressionToCheck = (CAstNode) pEdge.getRawAST().get();
    } else {
      return false;
    }

    return true; // TODO, dummy return value
  }
}
