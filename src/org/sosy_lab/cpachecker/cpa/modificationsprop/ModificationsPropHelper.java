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
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/** Helper functions used that are outsourced to be accessible for transfer relation and state. */
public class ModificationsPropHelper {

  /** Nodes that are part of the property. */
  private final ImmutableSet<CFANode> propNodes;
  /** Setting for ignoring of declaration statements. */
  private final boolean ignoreDeclarations;
  /** Setting for switching on/off the SMT implication check. */
  private final boolean implicationCheck;
  /** Safely stop analysis on pointer accesses and similar. */
  private final boolean performPreprocessing;
  /** Set of nodes from which an error location is reachable in original program. */
  private final Set<CFANode> reachElocOrig;
  /** Set of nodes from which an error location is reachable in modified program. */
  private final Set<CFANode> reachElocMod;

  private final Solver solver;
  private final CtoFormulaConverter converter;
  /** Logging manager to log with different levels of severity. */
  private final LogManager logger;
  /** A visitor to get used variables as string set for an expression. */
  private final VariableIdentifierVisitor visitor;

  /**
   * Creates a helper for ModificationsPropCPA.
   *
   * @param pPropNodes Nodes that are part of the property.
   * @param pIgnoreDeclarations Setting for ignoring of declaration statements.
   * @param pImplicationCheck Setting for switching on/off the SMT implication check.
   * @param pStopOnPointers Safely stop analysis on pointer accesses and similar.
   * @param pPerformPreprocessing Setting for switching reachability preprocessing on/off.
   * @param pReachElocOrig Set of nodes from which an error location is reachable in original
   *     program.
   * @param pReachElocMod Set of nodes from which an error location is reachable in modified
   *     program.
   * @param pSolver A solver for implication checks on assumptions.
   * @param pConverter A converter for forumla formats.
   * @param pLogger Logging manager to log with different levels of severity.
   */
  public ModificationsPropHelper(
      final ImmutableSet<CFANode> pPropNodes,
      final boolean pIgnoreDeclarations,
      final boolean pImplicationCheck,
      final boolean pStopOnPointers,
      final boolean pPerformPreprocessing,
      final Set<CFANode> pReachElocOrig,
      final Set<CFANode> pReachElocMod,
      final Solver pSolver,
      final CtoFormulaConverter pConverter,
      final LogManager pLogger) {
    propNodes = pPropNodes;
    ignoreDeclarations = pIgnoreDeclarations;
    implicationCheck = pImplicationCheck;
    performPreprocessing = pPerformPreprocessing;
    reachElocOrig = pReachElocOrig;
    reachElocMod = pReachElocMod;
    solver = pSolver;
    converter = pConverter;
    logger = pLogger;
    visitor = new VariableIdentifierVisitor(pStopOnPointers);
  }

  /**
   * Converts a given state to a (therefore more abstract) bad state at that location.
   *
   * @param pState the former abstract state
   * @return the bad state
   */
  ModificationsPropState makeBad(final ModificationsPropState pState) {
    return new ModificationsPropState(
        pState.getLocationInGivenCfa(), pState.getLocationInOriginalCfa(), ImmutableSet.of(), true);
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
    while (currentNode.getNumLeavingEdges() == 1 && !inReachabilityProperty(currentNode)) {
      CFAEdge currentEdge = currentNode.getLeavingEdge(0);
      if (isUntracked(currentEdge)
          // can omit check for summary edges here, as these are not untracked anway
          && sameFunction(currentNode, currentEdge.getSuccessor())) {
        currentNode = currentEdge.getSuccessor();
        if (currentNode == pNode) {
          logProblem("Found infinite sequence of untracked operations.");
          return pNode;
        }
        // }
      } else {
        break;
      }
    }
    return currentNode;
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
   * Skips an assignment or declaration edge and gives information about the changes.
   *
   * @param pNode the node to start in
   * @param pVars the variables that may be different in the two programs before
   * @return a tuple of the node reached and an updated set of modified variables
   */
  ImmutableTuple<CFANode, ImmutableSet<String>> skipAssignment(
      final CFANode pNode, final ImmutableSet<String> pVars) {
    if (pNode.getNumLeavingEdges() == 1
        && (pNode.getLeavingEdge(0) instanceof CStatementEdge
            || pNode.getLeavingEdge(0) instanceof CDeclarationEdge)
        && !inReachabilityProperty(pNode)) {
      CFAEdge edge = pNode.getLeavingEdge(0);
      String written = CFAEdgeUtils.getLeftHandVariable(pNode.getLeavingEdge(0));
      if (written != null) {
        if (pVars.contains(written)) {
          return new ImmutableTuple<>(edge.getSuccessor(), pVars);
        } else {
          return new ImmutableTuple<>(
              edge.getSuccessor(),
              new ImmutableSet.Builder<String>().addAll(pVars).add(written).build());
        }
      }
    }
    return new ImmutableTuple<>(pNode, pVars);
  }

  /**
   * Skips outgoing assume statements.
   *
   * @param pNode the node to start in
   * @param pVars the variables that may be different in the two programs before
   * @return the nodes reached by outgoing assume statements, empty if any assumption is not covered
   */
  ImmutableSet<CFANode> skipAssumption(final CFANode pNode, final ImmutableSet<String> pVars) {
    Set<CFANode> reached = new HashSet<>();
    if (!inReachabilityProperty(pNode)) {
      for (CFAEdge ce : CFAUtils.leavingEdges(pNode)) {
        if (ce instanceof CAssumeEdge) {
          Set<String> usedVars;
          try {
            usedVars = ((CAssumeEdge) ce).getExpression().accept(visitor);
          } catch (PointerAccessException e) {
            return ImmutableSet.of();
          }
          if (Collections.disjoint(usedVars, pVars)) {
            reached.add(ce.getSuccessor());
          } else {
            return ImmutableSet.of();
          }
        }
      }
    }
    return ImmutableSet.copyOf(reached);
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
        if (stmt instanceof CFunctionCall) {
          CFunctionCallExpression funCall = ((CFunctionCall) stmt).getFunctionCallExpression();
          rhs.addAll(funCall.getFunctionNameExpression().accept(visitor));
          for (CExpression exp : funCall.getParameterExpressions()) {
            rhs.addAll(exp.accept(visitor));
          }
        }
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
  boolean variablesAreUsedInEdge(final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    Set<String> usedVars = new HashSet<>();

    // EdgeType is..
    if (pEdge instanceof CDeclarationEdge) { // DeclarationEdge
      final CDeclaration decl = ((CDeclarationEdge) pEdge).getDeclaration();

      if (decl instanceof CFunctionDeclaration || decl instanceof CTypeDeclaration) {
        return false;
      } else if (decl instanceof CVariableDeclaration) {
        CInitializer initl = ((CVariableDeclaration) decl).getInitializer();
        if (initl instanceof CInitializerExpression) {
          try {
            usedVars = ((CInitializerExpression) initl).getExpression().accept(visitor);
          } catch (PointerAccessException e) {
            return true;
          }
        } else {
          return !pVars.isEmpty(); // not implemented for this initializer types, fallback
        }
      }

    } else if (pEdge instanceof CReturnStatementEdge) { // ReturnStatementEdge
      CExpression exp = ((CReturnStatementEdge) pEdge).getExpression().orElse(null);
      if (exp != null) {
        try {
          usedVars = exp.accept(visitor);
        } catch (PointerAccessException e) {
          return true;
        }
      } else {
        return !pVars.isEmpty(); // fallback, shouldn't happen
      }
    } else if (pEdge instanceof CAssumeEdge) { // AssumeEdge
      try {
        usedVars = ((CAssumeEdge) pEdge).getExpression().accept(visitor);
      } catch (PointerAccessException e) {
        return true;
      }
    } else if (pEdge instanceof CStatementEdge) { // StatementEdge
      return false; // ignored as handled later
    } else if (pEdge instanceof BlankEdge) { // BlankEdge
      return false;
    } else if (pEdge instanceof CFunctionCallEdge) { // FunctionCallEdge
      for (CExpression exp : ((CFunctionCallEdge) pEdge).getArguments()) {
        try {
          usedVars.addAll(exp.accept(visitor));
        } catch (PointerAccessException e) {
          return true;
        }
      }
    } else if (pEdge instanceof CFunctionSummaryEdge
        || pEdge instanceof CFunctionReturnEdge) { // CallToReturnEdge, FunctionReturnEdge
      return false;
    } else {
      return !pVars.isEmpty();
    }

    return !Collections.disjoint(usedVars, pVars);
  }

  /**
   * Checks whether the condition of one CAssumeEdge implies the one of another.
   *
   * @param pA the implying assumption edge
   * @param pB the implied assumption edge
   * @return whether the implication holds
   */
  boolean implies(final CAssumeEdge pA, final CAssumeEdge pB) {
    BooleanFormula a;
    BooleanFormula b;
    // empty SSA overapproximates in worst case
    final SSAMapBuilder ssaMap = SSAMap.emptySSAMap().builder();
    logger.log(
        Level.FINEST,
        "Checking whether",
        MoreStrings.lazyString(() -> pA.getCode()),
        "implies",
        MoreStrings.lazyString(() -> pB.getCode()),
        ".");
    try {
      a =
          converter.makePredicate(
              pA.getExpression(),
              pA,
              pA.getPredecessor().getFunctionName(),
              ssaMap,
              pA.getTruthAssumption());
      b =
          converter.makePredicate(
              pB.getExpression(),
              pB,
              pB.getPredecessor().getFunctionName(),
              ssaMap,
              pB.getTruthAssumption());
    } catch (UnrecognizedCodeException | InterruptedException e1) {
      logger.log(Level.WARNING, "Converting to predicate failed.");
      return false;
    }
    try {
      final boolean result = solver.implies(a, b);
      return result;
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
  boolean inReachabilityProperty(final CFANode node) {
    return propNodes.contains(node);
    /*return node instanceof CFALabelNode
    && ((CFALabelNode) node).getLabel().equalsIgnoreCase("error");*/
  }

  /**
   * Logs the case we took in finest log level.
   *
   * @param pNote the text to print
   */
  void logCase(final String pNote) {
    logger.log(Level.WARNING, pNote);
  }

  /**
   * Logs problems that occured.
   *
   * @param pNote the text to print
   */
  void logProblem(final String pNote) {
    logger.log(Level.WARNING, pNote);
  }

  /**
   * Getter for the property whether the implication check is activated
   *
   * @return implicationCheck property value
   */
  boolean getImplicationCheck() {
    return implicationCheck;
  }

  /**
   * Compares functions of two nodes to be identical.
   *
   * @param pNodeInGiven the node in the given CFA
   * @param pNodeInOriginal the node in the original CFA
   * @return whether class and function of the nodes are equivalent
   */
  boolean sameFunction(final CFANode pNodeInGiven, final CFANode pNodeInOriginal) {
    return pNodeInGiven.getFunctionName().equals(pNodeInOriginal.getFunctionName());
  }

  /**
   * Checks whether an error location is reachable from the given state.
   *
   * @param pNode the CFA node to be considered
   * @param pOriginal consider original instead of modified program
   * @return the computation result, true if analysis switched off.
   */
  boolean goalReachable(CFANode pNode, boolean pOriginal) {
    if (performPreprocessing) {
      if (pOriginal) {
        return reachElocOrig.contains(pNode);
      } else {
        return reachElocMod.contains(pNode);
      }
    } else {
      return true;
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
   * Checks whether successors of two similar edges are similar as well.
   *
   * @param pEdgeInGiven the edge from the given CFA
   * @param pEdgeInOriginal the edge form the original CFA
   * @return whether the next node functions and summary edges match
   */
  private boolean successorsMatch(final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    final CFANode givenSuccessor = pEdgeInGiven.getSuccessor(),
        originalSuccessor = pEdgeInOriginal.getSuccessor();
    // TODO: check whether needed in our case
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

    return sameFunction(givenSuccessor, originalSuccessor);
  }
}
