// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public abstract class AbstractStrategy implements StrategyInterface {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final int strategyNumber;

  protected AbstractStrategy(
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final int strategyNumber) {
    this.shutdownNotifier = pShutdownNotifier;
    this.logger = pLogger;
    this.strategyNumber = strategyNumber;
  }

  protected Optional<Integer> getLoopBranchIndex(CFANode loopStartNode) {
    if (loopStartNode.getNumLeavingEdges() != 2) {
      return Optional.empty();
    }
    List<CFANode> reachedNodes = new ArrayList<>();
    List<CFANode> reachableNodesIndex0 = new ArrayList<>();
    reachableNodesIndex0.add(loopStartNode.getLeavingEdge(0).getSuccessor());
    List<CFANode> reachableNodesIndex1 = new ArrayList<>();
    reachableNodesIndex1.add(loopStartNode.getLeavingEdge(1).getSuccessor());
    reachedNodes.add(loopStartNode.getLeavingEdge(1).getSuccessor());
    reachedNodes.add(loopStartNode.getLeavingEdge(0).getSuccessor());
    Integer loopBranchIndex = -1;
    while (loopBranchIndex == -1) {
      if (reachableNodesIndex1.isEmpty() && reachableNodesIndex0.isEmpty()) {
        return Optional.empty();
      }
      List<CFANode> newReachableNodesIndex0 = new ArrayList<>();
      for (CFANode s : reachableNodesIndex0) {
        if (s == loopStartNode) {
          loopBranchIndex = 0;
          break;
        } else {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (!reachedNodes.contains(s.getLeavingEdge(i).getSuccessor())) {
              reachedNodes.add(s.getLeavingEdge(i).getSuccessor());
              newReachableNodesIndex0.add(s.getLeavingEdge(i).getSuccessor());
            }
          }
        }
      }
      reachableNodesIndex0 = newReachableNodesIndex0;
      List<CFANode> newReachableNodesIndex1 = new ArrayList<>();
      for (CFANode s : reachableNodesIndex1) {
        if (s == loopStartNode) {
          loopBranchIndex = 1;
          break;
        } else {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (!reachedNodes.contains(s.getLeavingEdge(i).getSuccessor())) {
              reachedNodes.add(s.getLeavingEdge(i).getSuccessor());
              newReachableNodesIndex1.add(s.getLeavingEdge(i).getSuccessor());
            }
          }
        }
      }
      reachableNodesIndex1 = newReachableNodesIndex1;
    }

    return Optional.of(loopBranchIndex);
  }

  public static AbstractState overwriteLocationState(
      AbstractState pState, LocationState locState, AbstractState originalState) {
    List<AbstractState> allWrappedStatesByCompositeState = new ArrayList<>();
    if (pState instanceof ARGState) {
      AbstractState wrappedState = ((ARGState) pState).getWrappedState();
      if (wrappedState instanceof CompositeState) {
        for (AbstractState a : ((CompositeState) wrappedState).getWrappedStates()) {
          allWrappedStatesByCompositeState.add(overwriteLocationState(a, locState, originalState));
        }
        AbstractState wrappedCompositeState = new CompositeState(allWrappedStatesByCompositeState);
        return new ARGState(wrappedCompositeState, null);
      }
      return new ARGState(overwriteLocationState(wrappedState, locState, originalState), null);
    } else {
      if (pState instanceof LocationState) {
        return locState;
      } else if (pState instanceof PredicateAbstractState) {
        PersistentMap<CFANode, Integer> pAbstractionLocations = ((PredicateAbstractState)pState).getAbstractionLocationsOnPath();
        if (pAbstractionLocations.get(AbstractStates.extractLocation(originalState)) != null) {
          pAbstractionLocations =
              pAbstractionLocations.putAndCopy(
                  locState.getLocationNode(),
                  pAbstractionLocations.get(AbstractStates.extractLocation(originalState)));
        } else {
          pAbstractionLocations = pAbstractionLocations.putAndCopy(locState.getLocationNode(), 1);
          // This is a quickfix for a Null Pointer exception generate by
          // scripts/cpa.sh -heap 10000M -config
          // config/loop-summary/predicateAnalysis-loopsummary.properties -setprop
          // counterexample.export.enabled=false -setprop cpa.arg.export=false -setprop
          // report.export=false -timelimit 900s -stats -spec
          // test/programs/benchmarks/properties/no-overflow.prp -32
          // test/programs/benchmarks/loop-zilu/benchmark16_conjunctive.i
          // TODO see if this is correct like this and fix it if some problems arise from this in
          // some way
        }
        return PredicateAbstractState.mkAbstractionState(
            ((PredicateAbstractState) pState).getPathFormula(),
            ((PredicateAbstractState) pState).getAbstractionFormula(),
            pAbstractionLocations);
      } else {
        return pState;
      }
    }
  }

  protected CAssumeEdge overwriteStartEndStateEdge(
      CAssumeEdge edge, boolean truthAssignment, CFANode startNode, CFANode endNode) {
    return new CAssumeEdge(
        edge.getDescription(),
        FileLocation.DUMMY,
        startNode,
        endNode,
        edge.getExpression(),
        truthAssignment);
  }

  protected CStatementEdge overwriteStartEndStateEdge(
      CStatementEdge edge, CFANode startNode, CFANode endNode) {
    return new CStatementEdge(
        edge.getRawStatement(), edge.getStatement(), FileLocation.DUMMY, startNode, endNode);
  }

  protected Collection<AbstractState> transverseGhostCFA(
      GhostCFA ghostCFA,
      final AbstractState pState,
      @SuppressWarnings("unused") final Precision pPrecision,
      @SuppressWarnings("unused") TransferRelation pTransferRelation,
      int loopBranchIndex) {

    CFAEdge dummyTrueEdgeStart =
        new CAssumeEdge(
            "true GHOST CFA Strategy " + this.strategyNumber,
            FileLocation.DUMMY,
            AbstractStates.extractLocation(pState),
            ghostCFA.getStartNode(),
            CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
            true);
    AbstractStates.extractLocation(pState).addLeavingEdge(dummyTrueEdgeStart);
    ghostCFA.getStartNode().addEnteringEdge(dummyTrueEdgeStart);

    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState ghostStartLocationState =
        new LocationState(ghostCFA.getStartNode(), oldLocationState.getFollowFunctionCalls());
    AbstractState dummyStateStart = overwriteLocationState(pState, ghostStartLocationState, pState);
    Collection<AbstractState> realStatesEndCollection = new ArrayList<>();
    CFAEdge dummyTrueEdgeEnd =
        new CAssumeEdge(
            "true GHOST CFA",
            FileLocation.DUMMY,
            ghostCFA.getStopNode(),
            AbstractStates.extractLocation(pState)
                .getLeavingEdge(1 - loopBranchIndex)
                .getSuccessor(),
            CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
            true);
    ghostCFA.getStopNode().addLeavingEdge(dummyTrueEdgeEnd);
    AbstractStates.extractLocation(pState)
        .getLeavingEdge(1 - loopBranchIndex)
        .getSuccessor()
        .addEnteringEdge(dummyTrueEdgeEnd);

    ((ARGState) dummyStateStart).addParent((ARGState) pState);
    realStatesEndCollection.add(dummyStateStart);
    return realStatesEndCollection;
  }

  protected Optional<CFANode> unrollLoopOnce(
      CFANode loopStartNode,
      Integer loopBranchIndex,
      CFANode endNodeGhostCFA,
      CFANode startNodeGhostCFA) {
    // TODO Loops inside the loop to be unrolled, are unrolled completely, meaning it is possible
    // that this function does not terminate. How do we handle this?
    boolean initial = true;
    List<CFANode> reachedNodes = new ArrayList<>();
    CFANode endLoopUnrollingNode = CFANode.newDummyCFANode("LSU");
    // First entry is the ghostCFA Node, the second entry is the real CFA Node
    List<Pair<CFANode, CFANode>> currentVisitedNodes = new ArrayList<>();
    reachedNodes.add(loopStartNode);
    while (!currentVisitedNodes.isEmpty() || initial) {
      if (initial) {
        CFANode currentUnrollingNode = CFANode.newDummyCFANode("LSU");
        CFAEdge currentLoopEdge = loopStartNode.getLeavingEdge(loopBranchIndex);
        assert currentLoopEdge instanceof CAssumeEdge;
        CFAEdge tmpLoopEdgeFalse =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, false, startNodeGhostCFA, endNodeGhostCFA);
        startNodeGhostCFA.addLeavingEdge(tmpLoopEdgeFalse);
        endNodeGhostCFA.addEnteringEdge(tmpLoopEdgeFalse);
        CFAEdge tmpLoopEdgeTrue =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, true, startNodeGhostCFA, currentUnrollingNode);
        startNodeGhostCFA.addLeavingEdge(tmpLoopEdgeTrue);
        currentUnrollingNode.addEnteringEdge(tmpLoopEdgeTrue);
        reachedNodes.add(currentLoopEdge.getSuccessor());
        currentVisitedNodes.add(Pair.of(currentUnrollingNode, currentLoopEdge.getSuccessor()));
        initial = false;
      } else {
        List<Pair<CFANode, CFANode>> newVisitedNodes = new ArrayList<>();
        for (Pair<CFANode, CFANode> p : currentVisitedNodes) {
          for (int i = 0; i < p.getSecond().getNumLeavingEdges(); i++) {
            CFANode nextGhostCFANode = CFANode.newDummyCFANode("LSU");
            CFAEdge currentLoopEdge = p.getSecond().getLeavingEdge(i);
            CFAEdge tmpLoopEdge;
            if (currentLoopEdge.getSuccessor() == loopStartNode) {
              nextGhostCFANode = endLoopUnrollingNode;
            }
            if (currentLoopEdge instanceof CStatementEdge) {
              tmpLoopEdge =
                  overwriteStartEndStateEdge(
                      (CStatementEdge) currentLoopEdge, p.getFirst(), nextGhostCFANode);
            } else if (currentLoopEdge instanceof BlankEdge) {
              tmpLoopEdge =
                  new BlankEdge(
                      currentLoopEdge.getRawStatement(),
                      FileLocation.DUMMY,
                      p.getFirst(),
                      nextGhostCFANode,
                      currentLoopEdge.getDescription());
            } else if (currentLoopEdge instanceof CDeclarationEdge) {
              tmpLoopEdge =
                  new CDeclarationEdge(
                      ((CDeclarationEdge) currentLoopEdge).getRawStatement(),
                      FileLocation.DUMMY,
                      p.getFirst(),
                      nextGhostCFANode,
                      ((CDeclarationEdge) currentLoopEdge).getDeclaration());
            } else if (currentLoopEdge instanceof CFunctionCallEdge) {
              /*tmpLoopEdge =
              new CFunctionCallEdge(
                  currentLoopEdge.getRawStatement(),
                  FileLocation.DUMMY,
                  p.getFirst(),
                  ((CFunctionCallEdge) currentLoopEdge).getSuccessor(),
                  (CFunctionCall) currentLoopEdge.getRawAST().get(),
                  ((CFunctionCallEdge) currentLoopEdge).getSummaryEdge());*/
              // Does not work since the out node is not the dummy node
              // TODO Improve this
              return Optional.empty();
            } else if (currentLoopEdge instanceof CAssumeEdge) {
              tmpLoopEdge =
                  new CAssumeEdge(
                      currentLoopEdge.getRawStatement(),
                      FileLocation.DUMMY,
                      p.getFirst(),
                      nextGhostCFANode,
                      ((CAssumeEdge) currentLoopEdge).getExpression(),
                      ((CAssumeEdge) currentLoopEdge).getTruthAssumption());
            } else if (currentLoopEdge instanceof CReturnStatementEdge) {
              /*tmpLoopEdge =
              new CReturnStatementEdge(
                  currentLoopEdge.getRawStatement(),
                  currentLoopEdge.getRawAST().get(),
                  FileLocation.DUMMY,
                  p.getFirst(),
                  nextGhostCFANode);*/
              // Does not work since we would need to go out of the ghost cfa in order to follow the
              // return edge, which may not be optimal
              // TODO Improve this
              return Optional.empty();
            } else {
              logger.log(
                  Level.INFO,
                  "Following edge class was detected when performing loop unrolling, which is not already been checked in a case: "
                      + currentLoopEdge.getClass()
                      + "   "
                      + currentLoopEdge);
              return Optional.empty();
            }
            p.getFirst().addLeavingEdge(tmpLoopEdge);
            nextGhostCFANode.addEnteringEdge(tmpLoopEdge);
            if (currentLoopEdge.getSuccessor() != loopStartNode) {
              if (reachedNodes.contains(currentLoopEdge.getSuccessor())) {
                return Optional.empty();
              }
              reachedNodes.add(currentLoopEdge.getSuccessor());
              newVisitedNodes.add(Pair.of(nextGhostCFANode, currentLoopEdge.getSuccessor()));
            }
          }
        }
        currentVisitedNodes = newVisitedNodes;
      }
    }

    return Optional.of(endLoopUnrollingNode);
  }

  protected Optional<Set<String>> getModifiedVariables(
      CFANode loopStartNode, Integer loopBranchIndex) {
    Set<String> modifiedVariables = new HashSet<>();
    List<CFANode> reachedNodes = new ArrayList<>();
    reachedNodes.add(loopStartNode.getLeavingEdge(loopBranchIndex).getSuccessor());
    Collection<CFANode> seenNodes = new HashSet<>();
    while (!reachedNodes.isEmpty()) {
      List<CFANode> newReachableNodes = new ArrayList<>();
      for (CFANode s : reachedNodes) {
        seenNodes.add(s);
        if (s != loopStartNode) {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (s != loopStartNode) {
              CFAEdge edge = s.getLeavingEdge(i);
              if (edge instanceof CStatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                CExpression leftSide;
                if (statement instanceof CFunctionCallAssignmentStatement) {
                  leftSide = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();
                } else if (statement instanceof CExpressionAssignmentStatement) {
                  leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
                } else if (statement instanceof CExpressionStatement
                    || statement instanceof CFunctionCallStatement) {
                  continue;
                } else {
                  logger.log(
                      Level.INFO,
                      "Unknown Statement of type: "
                          + statement.getClass()
                          + " \n Statetement has form: "
                          + statement);
                  return Optional.empty();
                }
                if (leftSide instanceof CIdExpression) { // TODO Generalize
                  modifiedVariables.add(((CIdExpression) leftSide).getName());
                }
              }
              if (!seenNodes.contains(edge.getSuccessor())) {
                newReachableNodes.add(edge.getSuccessor());
              }
            }
          }
        }
      }
      reachedNodes = newReachableNodes;
    }
    return Optional.of(modifiedVariables);
  }

  @Override
  public Optional<GhostCFA> getGhostCFA(final AbstractState pState) {
    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return false;
  }
}
