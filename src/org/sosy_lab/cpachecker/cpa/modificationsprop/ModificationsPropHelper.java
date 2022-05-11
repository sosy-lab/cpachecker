// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/** Helper functions used that are outsourced to be accessible for transfer relation and state. */
public class ModificationsPropHelper {

  /** Nodes that are part of the property. */
  private final ImmutableSet<CFANode> errorLocationsInOrigOrMod;
  /** Setting for ignoring of declaration statements. */
  private final boolean ignoreDeclarations;
  /** Setting for switching on/off the SMT implication check. */
  private final boolean implicationCheck;
  /** Set of nodes from which an error location is reachable in original program. */
  private final Set<CFANode> mayReachErrorLocOrig;
  /** Set of nodes from which an error location is reachable in modified program. */
  private final Set<CFANode> mayReachErrorLocMod;

  private final Solver solver;
  private final CtoFormulaConverter converter;
  /** Logging manager to log with different levels of severity. */
  private final LogManager logger;
  /** A visitor to get used variables as string set for an expression. */
  private final VariableIdentifierVisitor visitor;

  /**
   * Creates a helper for ModificationsPropCPA.
   *
   * @param pErrorLocations CFA nodes that are part of the property in original or modified program
   *     (disjoint union).
   * @param pIgnoreDeclarations Setting for ignoring of declaration statements.
   * @param pImplicationCheck Setting for switching on/off the SMT implication check.
   * @param pStopOnPointers Safely stop analysis on pointer accesses and similar.
   * @param pReachElocOrig Set of nodes from which an error location is reachable in original
   *     program.
   * @param pReachElocMod Set of nodes from which an error location is reachable in modified
   *     program.
   * @param pSolver A solver for implication checks on assumptions.
   * @param pConverter A converter for forumla formats.
   * @param pLogger Logging manager to log with different levels of severity.
   */
  public ModificationsPropHelper(
      final ImmutableSet<CFANode> pErrorLocations,
      final boolean pIgnoreDeclarations,
      final boolean pImplicationCheck,
      final boolean pStopOnPointers,
      final Set<CFANode> pReachElocOrig,
      final Set<CFANode> pReachElocMod,
      final Solver pSolver,
      final CtoFormulaConverter pConverter,
      final LogManager pLogger) {
    errorLocationsInOrigOrMod = pErrorLocations;
    ignoreDeclarations = pIgnoreDeclarations;
    implicationCheck = pImplicationCheck;
    mayReachErrorLocOrig = pReachElocOrig;
    mayReachErrorLocMod = pReachElocMod;
    solver = pSolver;
    converter = pConverter;
    logger = pLogger;
    visitor = new VariableIdentifierVisitor(pStopOnPointers);
  }

  /**
   * Checks whether an CFA edge represents an untracked operation.
   *
   * @param pEdge the edge to be checked
   * @return the analysis result
   */
  boolean isUntracked(final CFAEdge pEdge) {
    return (pEdge instanceof BlankEdge)
        || (ignoreDeclarations && (pEdge instanceof CDeclarationEdge));
  }

  /**
   * Skips an arbitrary number of operations that are irrelevant for us. Stops on operations in the
   * reachability property. Caution: this only terminates if there is no infinite sequence of such
   * operations.
   *
   * @param pNode the node to start in
   * @return the node we reached
   */
  CFANode skipUntrackedOperations(final CFANode pNode) {
    CFANode currentNode = pNode;
    while (currentNode.getNumLeavingEdges() == 1 && !isErrorLocation(currentNode)) {
      final CFAEdge currentEdge = currentNode.getLeavingEdge(0);
      if (isUntracked(currentEdge)
          // can omit check for summary edges here, as these are not untracked anway
          && inSameFunction(currentNode, currentEdge.getSuccessor())) {
        currentNode = currentEdge.getSuccessor();
        if (currentNode == pNode) {
          logProblem("Found infinite sequence of untracked operations.");
          return pNode;
        }
      } else {
        break;
      }
    }
    return currentNode;
  }

  /**
   * Skips an assignment or declaration edge and gives information about the changes.
   *
   * @param pNode the node to start in
   * @param pVars the variables that may be different in the two programs before
   * @return a tuple of the node reached and an updated set of modified variables
   */
  Pair<CFANode, ImmutableSet<String>> skipAssignment(
      final CFANode pNode, final ImmutableSet<String> pVars) {
    if (pNode.getNumLeavingEdges() == 1
        && (pNode.getLeavingEdge(0) instanceof CStatementEdge
            || pNode.getLeavingEdge(0) instanceof CDeclarationEdge)
        && !isErrorLocation(pNode)) {
      CFAEdge edge = pNode.getLeavingEdge(0);
      String written = CFAEdgeUtils.getLeftHandVariable(pNode.getLeavingEdge(0));
      if (written != null) { // TODO might be unsound for pointers?
        if (pVars.contains(written)) {
          return Pair.of(edge.getSuccessor(), pVars);
        } else {
          return Pair.of(
              edge.getSuccessor(),
              new ImmutableSet.Builder<String>().addAll(pVars).add(written).build());
        }
      }
    }
    return Pair.of(pNode, pVars);
  }

  /**
   * Check whether edges describe the same operation.
   *
   * @param pEdgeInGiven the edge in the given CFA
   * @param pEdgeInOriginal the edge in the original CFA
   * @return whether these are similar enough for us
   */
  boolean edgesMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    final String firstAst = pEdgeInGiven.getRawStatement();
    final String sndAst = pEdgeInOriginal.getRawStatement();

    return firstAst.equals(sndAst)
        && pEdgeInGiven.getEdgeType() == pEdgeInOriginal.getEdgeType()
        && successorsMatch(pEdgeInGiven, pEdgeInOriginal);
  }

  /**
   * Checks whether successors of two similar edges are similar as well.
   *
   * @param pEdgeInGiven the edge from the given CFA
   * @param pEdgeInOriginal the edge form the original CFA
   * @return whether the next node functions and summary edges match
   */
  private boolean successorsMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    final CFANode givenSuccessor = pEdgeInGiven.getSuccessor(),
        originalSuccessor = pEdgeInOriginal.getSuccessor();
    // TODO: check whether needed in our case -> sufficient because we use stack?
    /*if (pEdgeInGiven.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      for (CFAEdge enterBeforeCall :
          CFAUtils.enteringEdges(
              ((FunctionReturnEdge) pEdgeInGiven).getSummaryEdge().getPredecessor())) {
        for (CFAEdge enterOriginalBeforeCAll :
            CFAUtils.enteringEdges(
                ((FunctionReturnEdge) pEdgeInOriginal).getSummaryEdge().getPredecessor())) {
          if (edgesMatch(enterBeforeCall, enterOriginalBeforeCAll)) {
            break;
          }
        }
        return false;
      }
    }*/

    return inSameFunction(givenSuccessor, originalSuccessor);
  }

  /**
   * Updates the set of variables if the statement is an assignment to delete / add the variable.
   *
   * @param pEdge the edge under consideration
   * @param pVars the variables modified yet
   * @return the updated set of modified variables
   * @throws PointerAccessException exception if pointer is contained in right hand side
   */
  ImmutableSet<String> modifySetForAssignment(final CFAEdge pEdge, final ImmutableSet<String> pVars)
      throws PointerAccessException {

    ImmutableSet<String> vars = pVars;

    if (pEdge instanceof CStatementEdge) {
      String lhs = CFAEdgeUtils.getLeftHandVariable(pEdge);
      Set<String> rhs = new HashSet<>();

      CStatement stmt = ((CStatementEdge) pEdge).getStatement();
      if (stmt instanceof CExpressionStatement) {
        rhs = ((CExpressionStatement) stmt).getExpression().accept(visitor);
      } else {
        if (stmt instanceof CAssignment) {
          CLeftHandSide clhs = ((CAssignment) stmt).getLeftHandSide();
          if (!(clhs instanceof CIdExpression)) {
            rhs.addAll(clhs.accept(visitor));
          }
          if (stmt instanceof CExpressionAssignmentStatement) {
            rhs.addAll(((CExpressionAssignmentStatement) stmt).getRightHandSide().accept(visitor));
          }
        }
        /* Function Calls are handled differently now.
         *
         * if (stmt instanceof CFunctionCall) {
         * CFunctionCallExpression funCall = ((CFunctionCall) stmt).getFunctionCallExpression();
         * rhs.addAll(funCall.getFunctionNameExpression().accept(visitor));
         * for (CExpression exp : funCall.getParameterExpressions()) {
         *   rhs.addAll(exp.accept(visitor));
         * }
         * }
         */
      }

      if (lhs != null && pVars.contains(lhs)) {
        vars = FluentIterable.from(pVars).filter(Predicates.not(Predicates.equalTo(lhs))).toSet();
        if (!Collections.disjoint(pVars, rhs)) {
          // add lhs variable because written expression includes modified variable
          return new ImmutableSet.Builder<String>().addAll(vars).add(lhs).build();
        }
      }
    }
    return vars;
  }

  /**
   * Checks whether one of the given variables is used in the edge. If the edge is an assignment we
   * ignore variable usages, however.
   *
   * @param pEdge the edge under consideration
   * @param pVars the variables modified yet
   * @return the updated set of modified variables
   */
  boolean areVariablesUsedInEdge(final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    Set<String> usedVars = new HashSet<>();

    // EdgeType is..
    if (pEdge instanceof BlankEdge // BlankEdge, CallToReturnEdge, FunctionReturnEdge
        || pEdge instanceof CFunctionReturnEdge
        || pEdge instanceof CFunctionSummaryEdge) {
      // no operation (BlankEdge, CFunctionReturnEdge),
      // CallToReturnEdge handled by CFunctionCallEdge
      return false;
    } else if (pEdge instanceof CStatementEdge) { // StatementEdge
      return false; // ignored as handled later
    } else {
      try {
        if (pEdge instanceof CDeclarationEdge) { // DeclarationEdge
          final CDeclaration decl = ((CDeclarationEdge) pEdge).getDeclaration();

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
        } else if (pEdge instanceof CFunctionCallEdge) { // FunctionCallEdge
          for (CExpression exp : ((CFunctionCallEdge) pEdge).getArguments()) {
            usedVars.addAll(exp.accept(visitor));
          }
        } else {
          return !pVars.isEmpty();
        }
      } catch (PointerAccessException e) {
        return true;
      }
    }

    return !Collections.disjoint(usedVars, pVars);
  }

  /**
   * Getter for the property whether the implication check is activated
   *
   * @return implicationCheck property value
   */
  boolean useImplicationCheck() {
    return implicationCheck;
  }

  /**
   * Checks whether the condition of one CAssumeEdge implies the one of another.
   *
   * @param pAntecedent the implying assumption edge
   * @param pConsequence the implied assumption edge
   * @return whether the implication holds
   */
  boolean implies(final CAssumeEdge pAntecedent, final CAssumeEdge pConsequence) {
    final BooleanFormula fAntecedent;
    final BooleanFormula fConsequence;
    final SSAMapBuilder ssaMap = SSAMap.emptySSAMap().builder();
    logger.log(
        Level.FINEST,
        "Checking whether",
        MoreStrings.lazyString(() -> pAntecedent.getCode()),
        "implies",
        MoreStrings.lazyString(() -> pConsequence.getCode()),
        ".");
    try {
      fAntecedent =
          converter.makePredicate(
              pAntecedent.getExpression(),
              pAntecedent,
              pAntecedent.getPredecessor().getFunctionName(),
              ssaMap,
              pAntecedent.getTruthAssumption());
      fConsequence =
          converter.makePredicate(
              pConsequence.getExpression(),
              pConsequence,
              pConsequence.getPredecessor().getFunctionName(),
              ssaMap,
              pConsequence.getTruthAssumption());
    } catch (IllegalArgumentException | UnrecognizedCodeException | InterruptedException e) {
      logger.log(Level.WARNING, "Converting to predicate failed.");
      return false;
    }
    try {
      return solver.implies(fAntecedent, fConsequence);
    } catch (SolverException | InterruptedException e) {
      logger.log(Level.WARNING, "Implication check failed.");
      return false;
    }
  }

  /**
   * Checks whether a location is in the reachability property.
   *
   * @param node the CFA node to be checked
   * @return whether the node is in the reachability property
   */
  boolean isErrorLocation(final CFANode node) {
    return errorLocationsInOrigOrMod.contains(node);
  }

  /**
   * Compares functions of two nodes to be identical.
   *
   * @param pNodeInGiven the node in the given CFA
   * @param pNodeInOriginal the node in the original CFA
   * @return whether class and function of the nodes are equivalent
   */
  boolean inSameFunction(final CFANode pNodeInGiven, final CFANode pNodeInOriginal) {
    return pNodeInGiven.getFunctionName().equals(pNodeInOriginal.getFunctionName());
  }

  /**
   * Checks whether an error location is reachable from the given state.
   *
   * @param pNode the CFA node to be considered
   * @param pOriginal consider original instead of modified program
   * @return the computation result, true if analysis switched off.
   */
  boolean mayReachErrorLocation(CFANode pNode, boolean pOriginal) {
    if (pOriginal) {
      return mayReachErrorLocOrig.contains(pNode);
    } else {
      return mayReachErrorLocMod.contains(pNode);
    }
  }

  /**
   * Getter for the variable identifier visitor.
   *
   * @return the visitor
   */
  VariableIdentifierVisitor getVisitor() {
    return visitor;
  }

  /**
   * Logs the case we took in finest log level.
   *
   * @param pMsg the text to print
   */
  void logCase(final String pMsg) {
    logger.log(Level.FINEST, pMsg);
  }

  /**
   * Logs problems that occured.
   *
   * @param pMsg the text to print
   */
  void logProblem(final String pMsg) {
    logger.log(Level.WARNING, pMsg);
  }
}
