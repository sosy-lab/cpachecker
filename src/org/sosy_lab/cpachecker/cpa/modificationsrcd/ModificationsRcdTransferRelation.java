// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsrcd;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
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

            if (variablesAreUsedInEdge(edgeInOriginal, changedVarsInGiven)
                || variablesAreUsedInEdge(pCfaEdge, changedVarsInGiven)) {
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
            : "List of successors should never be empty if previous state represents no"
                + " modification";
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

      // edges describe the same operation
      if (edgesMatch(pEdgeInGiven, originalEdge)) {
        ImmutableSet<String> changedVarsInSuccessor =
            removeVariableFromSetIfAssignedInEdge(originalEdge, pChangedVarsInGiven);
        return Optional.of(
            new ModificationsRcdState(
                pEdgeInGiven.getSuccessor(), originalEdge.getSuccessor(), changedVarsInSuccessor));
      }

      // a variable assignment was added to the CFA: the edge in the given CFA is a new variable
      // assignment and the edge in the original CFA matches an outgoing edge of the successor node
      // of the given CFA
      if (pEdgeInGiven.getPredecessor().getNumLeavingEdges() == 1) {
        Optional<String> added = checkEdgeAddedAssignment(pEdgeInGiven, originalEdge);
        if (added.isPresent()) {
          ImmutableSet<String> changedVarsInSuccessor =
              new ImmutableSet.Builder<String>()
                  .addAll(pChangedVarsInGiven)
                  .add(added.orElseThrow())
                  .build();
          return Optional.of(
              new ModificationsRcdState(
                  pEdgeInGiven.getSuccessor(),
                  originalEdge.getPredecessor(), // do not move on in the original CFA!
                  changedVarsInSuccessor));
        }
      }

      // a variable assignment was deleted from the CFA: the edge in the original CFA is a variable
      // assignment and the edge in the given CFA matches an outgoing edge of the successor node of
      // the original CFA
      if (originalEdge.getPredecessor().getNumLeavingEdges() == 1) {
        Optional<String> deleted = checkEdgeDeletedAssignment(pEdgeInGiven, originalEdge);
        if (deleted.isPresent()) {
          ImmutableSet<String> changedVarsInSuccessor =
              new ImmutableSet.Builder<String>()
                  .addAll(pChangedVarsInGiven)
                  .add(deleted.orElseThrow())
                  .build();
          // if the variable which was changed in the deleted assignment is used in pEdgeInGiven, we
          // have the use of a changed variable and therefore return without a found matching
          // successor
          if (variablesAreUsedInEdge(pEdgeInGiven, ImmutableSet.of(deleted.orElseThrow()))) {
            return Optional.empty();
          }
          // move on one by one node in the given, and by two nodes in the original CPA;
          // find out which edge is matching
          for (CFAEdge edgeLeavingOrigSuccessor :
              CFAUtils.leavingEdges(originalEdge.getSuccessor())) {
            if (edgesMatch(pEdgeInGiven, edgeLeavingOrigSuccessor)) {
              changedVarsInSuccessor =
                  removeVariableFromSetIfAssignedInEdge(pEdgeInGiven, changedVarsInSuccessor);
              return Optional.of(
                  new ModificationsRcdState(
                      pEdgeInGiven.getSuccessor(),
                      edgeLeavingOrigSuccessor.getSuccessor(),
                      changedVarsInSuccessor));
            }
          }
        }
      }

      // edges represent different assignments of the same variable (assignment was replaced)
      if (originalEdge.getPredecessor().getNumLeavingEdges() == 1
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

  private boolean declarationNameAlreadyExistsInOtherCFA(
      final boolean isOtherOrigCFA, final CDeclarationEdge pDeclEdge) {
    if (!pDeclEdge.getDeclaration().isGlobal()) {
      if (containsDeclaration(
          isOtherOrigCFA
              ? funToVarsOrig.get(pDeclEdge.getSuccessor().getFunctionName())
              : funToVarsGiven.get(pDeclEdge.getSuccessor().getFunctionName()),
          pDeclEdge.getDeclaration().getOrigName())) {
        return true;
      }
    }

    return containsDeclaration(
        isOtherOrigCFA ? funToVarsOrig.get("") : funToVarsGiven.get(""),
        pDeclEdge.getDeclaration().getOrigName());
  }

  private boolean containsDeclaration(@Nullable final Set<String> varNames, final String varName) {
    return varNames != null && varNames.contains(varName);
  }

  // check whether edges describe the same operation
  private boolean edgesMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    String firstAst = pEdgeInGiven.getRawStatement();
    String sndAst = pEdgeInOriginal.getRawStatement();

    return firstAst.equals(sndAst)
        && pEdgeInGiven.getEdgeType() == pEdgeInOriginal.getEdgeType()
        && successorsMatch(pEdgeInGiven, pEdgeInOriginal);
  }

  // Check whether edges represent assignments of the same variable and return an Optional of the
  // variable if that is the case.
  // If the assignments differ, the returned variable describes which variable was changed.
  private Optional<String> checkEdgeChangedAssignment(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    if (pEdgeInOriginal instanceof CStatementEdge) {
      String lhsInOriginal = CFAEdgeUtils.getLeftHandVariable(pEdgeInOriginal);
      String lhsInGiven = CFAEdgeUtils.getLeftHandVariable(pEdgeInGiven);
      if (lhsInOriginal != null && lhsInGiven != null && lhsInOriginal.equals(lhsInGiven)) {
        return Optional.of(lhsInOriginal);
      }
    }
    return Optional.empty();
  }

  // Check whether a variable assignment was deleted from the CFA and return an Optional of the
  // changed variable if that is the case.
  // Check whether edges are unequal with !edgesMatch(pEdgeInGiven, pEdgeInOriginal) before calling!
  // (Method assumes that there is only one outgoing edge if an outgoing edge is an assignment.)
  private Optional<String> checkEdgeDeletedAssignment(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {

    // check if pEdgeInOriginal is a variable assignment
    if (pEdgeInOriginal instanceof CStatementEdge) {
      String lhsInOriginal = CFAEdgeUtils.getLeftHandVariable(pEdgeInOriginal);
      if (lhsInOriginal != null) {

        // check if pEdgeInOriginal has successor edge equal to pEdgeInGiven
        for (CFAEdge edgeLeavingOrigSuccessor :
            CFAUtils.leavingEdges(pEdgeInOriginal.getSuccessor())) {
          if (edgesMatch(pEdgeInGiven, edgeLeavingOrigSuccessor)) {
            return Optional.of(lhsInOriginal);
          }
        }
      }
    }

    return Optional.empty();
  }

  // Check whether a variable assignment was added to the CFA and return an Optional of the changed
  // variable if that is the case.
  // Check whether edges are unequal with !edgesMatch(pEdgeInGiven, pEdgeInOriginal) before calling!
  // (Method assumes that there is only one outgoing edge if an outgoing edge is an assignment.)
  private Optional<String> checkEdgeAddedAssignment(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {

    // check if pEdgeInGiven is a variable assignment
    if (pEdgeInGiven instanceof CStatementEdge) {
      String lhsInGiven = CFAEdgeUtils.getLeftHandVariable(pEdgeInGiven);
      if (lhsInGiven != null) {

        // check if pEdgeInGiven has successor edge equal to pEdgeInOriginal
        for (CFAEdge edgeLeavingGivenSuccessor :
            CFAUtils.leavingEdges(pEdgeInGiven.getSuccessor())) {
          if (edgesMatch(edgeLeavingGivenSuccessor, pEdgeInOriginal)) {
            return Optional.of(lhsInGiven);
          }
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

  // Check whether the edge is an assignment to one of the given variables and return an Optional of
  // that variable.
  private ImmutableSet<String> removeVariableFromSetIfAssignedInEdge(
      final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    if (pEdge instanceof CStatementEdge) {
      String lhs = CFAEdgeUtils.getLeftHandVariable(pEdge);
      if (lhs != null && pVars.contains(lhs)) {
        return FluentIterable.from(pVars).filter(Predicates.not(Predicates.equalTo(lhs))).toSet();
      }
    }

    return pVars;
  }

  // Check whether one of the given variables is used in the edge. If the edge is an assignment that
  // consists only of a variable on the left-hand side, this is not considered a use of the
  // variable.
  private boolean variablesAreUsedInEdge(final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    // visitor and its return value
    VariableIdentifierVisitor visitor = new VariableIdentifierVisitor();
    Set<String> usedVars = new HashSet<>();

    // EdgeType is..
    if (pEdge instanceof CDeclarationEdge) { // DeclarationEdge
      CDeclaration decl = ((CDeclarationEdge) pEdge).getDeclaration();

      if (decl instanceof CFunctionDeclaration || decl instanceof CTypeDeclaration) {
        return false;
      } else if (decl instanceof CVariableDeclaration) {
        CInitializer initl = ((CVariableDeclaration) decl).getInitializer();
        if (initl instanceof CInitializerExpression) {
          usedVars = ((CInitializerExpression) initl).getExpression().accept(visitor);
        } else {
          return !pVars.isEmpty(); // not implemented for this initializer types, fallback
        }
      }

    } else if (pEdge instanceof CReturnStatementEdge) { // ReturnStatementEdge
      CExpression exp = ((CReturnStatementEdge) pEdge).getExpression().orElse(null);
      if (exp != null) {
        usedVars = exp.accept(visitor);
      } else {
        return !pVars.isEmpty(); // fallback, shouldn't happen
      }
    } else if (pEdge instanceof CAssumeEdge) { // AssumeEdge
      usedVars = ((CAssumeEdge) pEdge).getExpression().accept(visitor);
    } else if (pEdge instanceof CStatementEdge) { // StatementEdge
      CStatement stmt = ((CStatementEdge) pEdge).getStatement();

      if (stmt instanceof CExpressionStatement) {
        usedVars = ((CExpressionStatement) stmt).getExpression().accept(visitor);
      } else {
        if (stmt instanceof CAssignment) {
          CLeftHandSide lhs = ((CAssignment) stmt).getLeftHandSide();
          if (!(lhs instanceof CIdExpression)) {
            usedVars.addAll(lhs.accept(visitor));
          }
          if (stmt instanceof CExpressionAssignmentStatement) {
            usedVars.addAll(
                ((CExpressionAssignmentStatement) stmt).getRightHandSide().accept(visitor));
          }
        }
        if (stmt instanceof CFunctionCall) {
          CFunctionCallExpression funCall = ((CFunctionCall) stmt).getFunctionCallExpression();
          usedVars.addAll(funCall.getFunctionNameExpression().accept(visitor));
          for (CExpression exp : funCall.getParameterExpressions()) {
            usedVars.addAll(exp.accept(visitor));
          }
        }
      }

    } else if (pEdge instanceof BlankEdge) { // BlankEdge
      return false;
    } else if (pEdge instanceof CFunctionCallEdge) { // FunctionCallEdge
      for (CExpression exp : ((CFunctionCallEdge) pEdge).getArguments()) {
        usedVars.addAll(exp.accept(visitor));
      }
    } else if (pEdge instanceof CFunctionSummaryEdge
        || pEdge instanceof CFunctionReturnEdge) { // CallToReturnEdge, FunctionReturnEdge
      return false;
    } else {
      return !pVars.isEmpty();
    }

    return !Collections.disjoint(usedVars, pVars);
  }
}
