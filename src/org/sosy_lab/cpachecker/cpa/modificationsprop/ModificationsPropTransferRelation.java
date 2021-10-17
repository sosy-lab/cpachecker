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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
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
import org.sosy_lab.cpachecker.cpa.modificationsrcd.VariableIdentifierVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

class ImmutableTuple<A, B> {
  private final A first;
  private final B second;

  public ImmutableTuple(A pFirst, B pSecond) {
    first = pFirst;
    second = pSecond;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }
}

public class ModificationsPropTransferRelation extends SingleEdgeTransferRelation {

  private final boolean ignoreDeclarations;
  private final boolean implicationCheck;

  @SuppressWarnings("unused") // will be used in the future
  private final Map<String, Set<String>> funToVarsOrig;

  @SuppressWarnings("unused") // will be used in the future
  private final Map<String, Set<String>> funToVarsGiven;

  private final Solver solver;
  private final CtoFormulaConverter converter;
  private final LogManager logger;

  public ModificationsPropTransferRelation(
      final boolean pIgnoreDeclarations,
      final boolean pImplicationCheck,
      final Map<String, Set<String>> pFunToVarsOrig,
      final Map<String, Set<String>> pFunToVarsGiven,
      final Solver pSolver,
      CtoFormulaConverter pConverter,
      LogManager pLogger) {
    ignoreDeclarations = pIgnoreDeclarations;
    implicationCheck = pImplicationCheck;
    funToVarsOrig = pFunToVarsOrig;
    funToVarsGiven = pFunToVarsGiven;
    solver = pSolver;
    converter = pConverter;
    logger = pLogger;
  }

  public ModificationsPropTransferRelation(
      boolean pImplicationCheck,
      Solver pSolver,
      CtoFormulaConverter pConverter,
      LogManager pLogger) {
    this(
        false,
        pImplicationCheck,
        ImmutableMap.of(),
        ImmutableMap.of(),
        pSolver,
        pConverter,
        pLogger);
  }

  // Cases mentioned here relate to the respective master thesis
  @Override
  public Collection<ModificationsPropState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    ModificationsPropState locations = (ModificationsPropState) pState;
    CFANode nodeInGiven = locations.getLocationInGivenCfa();
    CFANode nodeInOriginal = locations.getLocationInOriginalCfa();
    ImmutableSet<String> changedVars = locations.getChangedVariables();

    if (!locations.isBad()) {

      // skip untracked parts in original
      nodeInOriginal = skipUntrackedOperations(nodeInOriginal);

      // rule out case 2
      if (!inReachabilityProperty(nodeInOriginal)) {

        // case 3
        if (inReachabilityProperty(nodeInGiven)) {
          CFANode nodeInGivenNew = nodeInGiven;
          do {
            nodeInGiven = nodeInGivenNew;
            nodeInGivenNew = skipUntrackedOperations(nodeInGiven);
            ImmutableTuple<CFANode, ImmutableSet<String>> tup =
                skipAssignment(nodeInGivenNew, changedVars);
            nodeInGivenNew = tup.getFirst();
            changedVars = tup.getSecond();
          } while (nodeInGivenNew == nodeInGiven);
          if (inReachabilityProperty(nodeInGivenNew)) {
            // Some path covers the bad location here.
            logger.log(Level.FINEST, "Taking case 3a.");
            return ImmutableSet.of();
          } else {
            logger.log(Level.FINEST, "Taking case 3b.");
            return ImmutableSet.of(makeBad(locations));
          }
        }

        if (CFAUtils.leavingEdges(nodeInGiven).contains(pCfaEdge)) {

          // prepare further cases by skipping ignored operations
          if (isUntracked(pCfaEdge)) {
            logger.log(Level.FINEST, "Skipping ignored CFA edge for given program.");
            return ImmutableSet.of(
                new ModificationsPropState(
                    pCfaEdge.getSuccessor(), nodeInOriginal, changedVars, false));
          }

          // case 4
          for (CFAEdge edgeInOriginal : CFAUtils.leavingEdges(nodeInOriginal)) {
            if (edgesMatch(pCfaEdge, edgeInOriginal)) {
              ImmutableSet<String> changedVarsInSuccessor =
                  removeVariableFromSetIfAssignedInEdge(edgeInOriginal, changedVars);
              if (variablesAreUsedInEdge(pCfaEdge, changedVars)) {
                logger.log(Level.FINEST, "Taking case 4a.");
                return ImmutableSet.of(makeBad(locations));
              } else {
                logger.log(Level.FINEST, "Taking case 4b.");
                return ImmutableSet.of(
                    new ModificationsPropState(
                        pCfaEdge.getSuccessor(),
                        edgeInOriginal.getSuccessor(),
                        changedVarsInSuccessor,
                        false));
              }
            }
          }

          // TODO: look for assignments to same variable?

          // case 5
          ImmutableTuple<CFANode, ImmutableSet<String>> tup =
              skipAssignment(nodeInGiven, changedVars);
          if (!tup.getFirst().equals(nodeInGiven)) {
            logger.log(Level.FINEST, "Taking case 5.");
            return ImmutableSet.of(
                new ModificationsPropState(tup.getFirst(), nodeInOriginal, tup.getSecond(), false));
          }

          // case 6
          // assuming there is no infinite sequence of assignments only in orginal program
          tup = skipAssignment(nodeInOriginal, changedVars);
          if (!tup.getFirst().equals(nodeInGiven)) {
            logger.log(Level.FINEST, "Taking case 6.");
            return getAbstractSuccessorsForEdge(
                new ModificationsPropState(nodeInGiven, tup.getFirst(), tup.getSecond(), false),
                pPrecision,
                pCfaEdge);
          }

          // case 7
          if (implicationCheck) {
            if (pCfaEdge instanceof CAssumeEdge) {
              CAssumeEdge assGiven = (CAssumeEdge) pCfaEdge;
              for (CFAEdge ce : CFAUtils.leavingEdges(nodeInOriginal)) {
                if (ce instanceof CAssumeEdge) {
                  CAssumeEdge assOrig = (CAssumeEdge) ce;
                  logger.log(Level.FINEST, "Checking for case 7 compliance.");
                  if (implies(assGiven, assOrig)) {
                    VariableIdentifierVisitor visitor = new VariableIdentifierVisitor();
                    Set<String> varsInAssGiven = assGiven.getExpression().accept(visitor);
                    Set<String> varsInAssOrig = assOrig.getExpression().accept(visitor);
                    ImmutableSet<String> varsUsedInBoth =
                        new ImmutableSet.Builder<String>()
                            .addAll(varsInAssGiven)
                            .addAll(varsInAssOrig)
                            .build();
                    if (Collections.disjoint(changedVars, varsUsedInBoth)) {
                      logger.log(Level.FINEST, "Taking case 7a.");
                      return ImmutableSet.of(
                          new ModificationsPropState(
                              assGiven.getSuccessor(), assOrig.getSuccessor(), changedVars, false));
                    } else {
                      logger.log(Level.FINEST, "Taking case 7b.");
                      return ImmutableSet.of(makeBad(locations));
                    }
                  }
                  logger.log(Level.FINEST, "No implication. Continuing.");
                }
              }
            }
          }

          // case 8
          ImmutableSet<CFANode> assumptionSuccessors = skipAssumption(nodeInGiven, changedVars);
          if (!assumptionSuccessors.isEmpty()) {
            logger.log(Level.FINEST, "Taking case 8.");
            final ImmutableSet<String> cv = changedVars;
            final CFANode nodeOrig = nodeInOriginal;
            return assumptionSuccessors.stream()
                .map(nodeGiven -> new ModificationsPropState(nodeGiven, nodeOrig, cv, false))
                .collect(Collectors.toUnmodifiableSet());
          } else {
            // case 9
            logger.log(Level.FINEST, "Taking case 9.");
            return ImmutableSet.of(makeBad(locations));
          }
        }
      }
      logger.log(Level.FINEST, "Taking case 2.");
    }

    // if current location pair is bad or no outgoing edge
    return ImmutableSet.of();
  }

  private ModificationsPropState makeBad(final ModificationsPropState pState) {
    return new ModificationsPropState(
        pState.getLocationInGivenCfa(), pState.getLocationInOriginalCfa(), ImmutableSet.of(), true);
  }

  // TODO: check successor, add blank and so on
  private CFANode skipUntrackedOperations(final CFANode pNode) {
    CFANode currentNode = pNode;
    while (currentNode.getNumLeavingEdges() == 1 && !inReachabilityProperty(currentNode)) {
      CFAEdge currentEdge = currentNode.getLeavingEdge(0);
      if (isUntracked(currentNode.getLeavingEdge(0))) {
        // if (!declarationNameAlreadyExistsInOtherCFA(true, (CDeclarationEdge) currentEdge)) {
        // TODO: overthink handling of declarations
        currentNode = currentEdge.getSuccessor();
        // }
      }
    }
    return currentNode;
  }

  private boolean isUntracked(final CFAEdge pEdge) {
    return ignoreDeclarations && (pEdge instanceof CDeclarationEdge);
  }

  private ImmutableTuple<CFANode, ImmutableSet<String>> skipAssignment(
      final CFANode pNode, final ImmutableSet<String> pVars) {
    if (pNode.getNumLeavingEdges() == 1
        && pNode.getLeavingEdge(0) instanceof CStatementEdge
        && !inReachabilityProperty(pNode)) {
      CFAEdge edge = pNode.getLeavingEdge(0);
      String written = CFAEdgeUtils.getLeftHandVariable(pNode.getLeavingEdge(0));
      if (pVars.contains(written)) {
        return new ImmutableTuple<>(edge.getSuccessor(), pVars);
      } else {
        return new ImmutableTuple<>(
            edge.getSuccessor(),
            new ImmutableSet.Builder<String>().addAll(pVars).add(written).build());
      }
    }
    return new ImmutableTuple<>(pNode, pVars);
  }

  private ImmutableSet<CFANode> skipAssumption(
      final CFANode pNode, final ImmutableSet<String> pVars) {
    Set<CFANode> reached = new HashSet<>();
    if (!inReachabilityProperty(pNode)) {
      for (CFAEdge ce : CFAUtils.leavingEdges(pNode)) {
        if (ce instanceof CAssumeEdge) {
          VariableIdentifierVisitor visitor = new VariableIdentifierVisitor();
          Set<String> usedVars = ((CAssumeEdge) ce).getExpression().accept(visitor);
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

  // edges represent different assignments of the same variable (assignment was replaced)
  /*if (originalEdge.getPredecessor().getNumLeavingEdges() == 1
      && pEdgeInGiven.getPredecessor().getNumLeavingEdges() == 1) {
    Optional<String> changed = checkEdgeChangedAssignment(pEdgeInGiven, originalEdge);
    if (changed.isPresent()) {
      ImmutableSet<String> changedVarsInSuccessor =
          new ImmutableSet.Builder<String>()
              .addAll(pChangedVarsInGiven)
              .add(changed.orElseThrow())
              .build();

      return Optional.of(
          new ModificationsPropState(
              pEdgeInGiven.getSuccessor(),
              originalEdge.getSuccessor(),
              changedVarsInSuccessor));
    }
  }*/

  // assume that variables are not renamed
  // only new declarations are added and existing declarations are deleted
  /*if (ignoreDeclarations) {

        if (pEdgeInGiven instanceof CDeclarationEdge) {
          if (!declarationNameAlreadyExistsInOtherCFA(true, (CDeclarationEdge) pEdgeInGiven)) {
            return Optional.of(
                new ModificationsPropState(
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
  }*/

  /*private boolean declarationNameAlreadyExistsInOtherCFA(
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
  }*/

  /*private boolean containsDeclaration(@Nullable final Set<String> varNames, final String varName) {
    return varNames != null && varNames.contains(varName);
  }*/

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
  /*private Optional<String> checkEdgeChangedAssignment(
      final CFAEdge pEdgeInGiven, final CFAEdge pEdgeInOriginal) {
    if (pEdgeInOriginal instanceof CStatementEdge) {
      String lhsInOriginal = CFAEdgeUtils.getLeftHandVariable(pEdgeInOriginal);
      String lhsInGiven = CFAEdgeUtils.getLeftHandVariable(pEdgeInGiven);
      if (lhsInOriginal != null && lhsInGiven != null && lhsInOriginal.equals(lhsInGiven)) {
        return Optional.of(lhsInOriginal);
      }
    }
    return Optional.empty();
  }*/

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

  // Update the set of variables if the statement is an assignment to delete / add the variable.
  private ImmutableSet<String> removeVariableFromSetIfAssignedInEdge(
      final CFAEdge pEdge, final ImmutableSet<String> pVars) {

    if (pEdge instanceof CStatementEdge) {
      String lhs = CFAEdgeUtils.getLeftHandVariable(pEdge);
      Set<String> rhs = new HashSet<>();

      VariableIdentifierVisitor visitor = new VariableIdentifierVisitor();
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
        return FluentIterable.from(pVars).filter(Predicates.not(Predicates.equalTo(lhs))).toSet();
      }
    }

    return pVars;
  }

  // Check whether one of the given variables is used in the edge. If the edge is an assignment we
  // ignore variable usages, however.
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
      return false;
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

  /**
   * Checks whether the condition of one CAssumeEdge implies the one of another.
   *
   * @param pA the implying assumption edge
   * @param pB the implied assumption edge
   * @return whether the implication holds
   */
  private boolean implies(final CAssumeEdge pA, final CAssumeEdge pB) {
    final BooleanFormula a;
    final BooleanFormula b;
    // empty SSA overapproximates in worst case
    SSAMapBuilder ssaMap = SSAMap.emptySSAMap().builder();
    try {
      a =
          converter.makePredicate(
              pA.getExpression(), pA, pA.getPredecessor().getFunctionName(), ssaMap);
      b =
          converter.makePredicate(
              pB.getExpression(), pB, pB.getPredecessor().getFunctionName(), ssaMap);
    } catch (UnrecognizedCodeException | InterruptedException e1) {
      logger.log(Level.FINER, "Converting to predicate failed.");
      return false;
    }
    try {
      return solver.implies(a, b);
    } catch (SolverException | InterruptedException e) {
      logger.log(Level.FINER, "Implication check failed.");
      return false;
    }
  }

  /**
   * Checks whether a location is in the reachability property.
   *
   * @param node the CFA node to be checked
   * @return whether the node is in the reachability property
   */
  private boolean inReachabilityProperty(final CFANode node) {
    // TODO: probably not what we want to check yet
    return node instanceof CFATerminationNode;
  }
}
