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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class arithmeticStrategy implements strategyInterface {

  // private final LocationStateFactory locationStateFactory;
  // TODO is this needed, because then we get initalization problems
  /*
  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;

  public arithmeticStrategy(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }
  */

  static final class StartStopNodesGhostCFA {
    private final CFANode startNode;
    private final CFANode stopNode;

    public StartStopNodesGhostCFA(CFANode startNode, CFANode stopNode) {
      this.startNode = startNode;
      this.stopNode = stopNode;
    }

    public CFANode getStartNode() {
      return startNode;
    }

    public CFANode getStopNode() {
      return stopNode;
    }
  }

  public arithmeticStrategy() {}

  // Returns the bound in the form 0 < x where x is the CExpression returned
  private Optional<CExpression> bound(final CFANode pLoopStartNode) {
    CFAEdge edge = pLoopStartNode.getLeavingEdge(0);
    if (!(edge instanceof CAssumeEdge)) {
      return Optional.empty();
    }
    CExpression expression = ((CAssumeEdge) edge).getExpression();
    if (!(expression instanceof CBinaryExpression)) {
      return Optional.empty();
    }

    String operator = ((CBinaryExpression) expression).getOperator().getOperator();
    CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression) expression).getOperand2();

    if (!((operand1 instanceof CIdExpression
            && (operand2 instanceof CIdExpression || operand2 instanceof CIntegerLiteralExpression))
        || (operand1 instanceof CIntegerLiteralExpression && operand2 instanceof CIdExpression))) {
      return Optional.empty();
    }

    switch (operator) {
      case "<":
        return Optional.of(
            new CBinaryExpression(
                expression.getFileLocation(),
                null,
                null,
                operand2,
                operand1,
                BinaryOperator.MINUS));

      case ">":
        return Optional.of(
            new CBinaryExpression(
                expression.getFileLocation(),
                null,
                null,
                operand1,
                operand2,
                BinaryOperator.MINUS));
      case "<=":
        return Optional.of(
            new CBinaryExpression(
                expression.getFileLocation(),
                null,
                null,
                operand2,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    null,
                    null,
                    operand1,
                    CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
                    BinaryOperator.MINUS),
                BinaryOperator.MINUS));
      case ">=":
        return Optional.of(
            new CBinaryExpression(
                expression.getFileLocation(),
                null,
                null,
                operand1,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    null,
                    null,
                    operand2,
                    CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
                    BinaryOperator.MINUS),
                BinaryOperator.MINUS));
      default:
        return Optional.empty();
    }
  }

  private Optional<Integer> getLoopBranchIndex(CFANode loopStartNode) {
    if (loopStartNode.getNumLeavingEdges() != 2) {
      return Optional.empty();
    }
    CFANode nextNode0 = loopStartNode.getLeavingEdge(0).getSuccessor();
    CFANode nextNode1 = loopStartNode.getLeavingEdge(1).getSuccessor();
    boolean nextNode0Valid = true;
    boolean nextNode1Valid = true;
    while (nextNode0 != loopStartNode
        && nextNode1 != loopStartNode
        && (nextNode0Valid || nextNode1Valid)) {
      if (nextNode0Valid && nextNode0.getNumLeavingEdges() == 1) {
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode1Valid && nextNode1.getNumLeavingEdges() == 1) {
        nextNode1 = nextNode1.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode1Valid = false;
      }
      if (nextNode0 == loopStartNode) {
        return Optional.of(0);
      } else if (nextNode1 == loopStartNode) {
        return Optional.of(1);
      }
    }
    return Optional.empty();
  }

  private Map<String, Integer> getLoopVariableDeltas(
      final AbstractState pState, final Integer loopBranchIndex) {
    Map<String, Integer> loopVariableDelta = new HashMap<>();
    CFANode loopStartNode =
        AbstractStates.extractLocation(pState).getLeavingEdge(loopBranchIndex).getSuccessor();
    // Calculate deltas in one Loop Iteration
    CFANode currentNode = loopStartNode;
    boolean initial = true;
    while (currentNode != loopStartNode || initial) {
      initial = false;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge instanceof CStatementEdge) {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
        CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();
        if (leftSide instanceof CIdExpression && rigthSide instanceof CBinaryExpression) {
            Integer value = 0;
            CExpression operand1 = ((CBinaryExpression) rigthSide).getOperand1();
            CExpression operand2 = ((CBinaryExpression) rigthSide).getOperand2();
            if (operand1 instanceof CIntegerLiteralExpression) {
              value = ((CIntegerLiteralExpression) operand1).getValue().intValue();
            } else if (operand2 instanceof CIntegerLiteralExpression) {
              value = ((CIntegerLiteralExpression) operand2).getValue().intValue();
            }
            switch (((CBinaryExpression) rigthSide).getOperator().getOperator()) {
              case "+":
              if (loopVariableDelta.containsKey(((CIdExpression) leftSide).getName())) {
                loopVariableDelta.put(
                    ((CIdExpression) leftSide).getName(),
                    value + loopVariableDelta.get(((CIdExpression) leftSide).getName()));
                } else {
                loopVariableDelta.put(((CIdExpression) leftSide).getName(), value);
                }
                break;
              case "-":
              if (loopVariableDelta.containsKey(((CIdExpression) leftSide).getName())) {
                loopVariableDelta.put(
                    ((CIdExpression) leftSide).getName(),
                    -value + loopVariableDelta.get(((CIdExpression) leftSide).getName()));
              } else {
                loopVariableDelta.put(((CIdExpression) leftSide).getName(), value);
              }
                break;
              default:
                break;
            }
        }
      }
      currentNode = edge.getSuccessor();
    }
    return loopVariableDelta;
  }

  private int boundDelta(
      final Map<String, Integer> loopVariableDelta, final CExpression loopBound) {
    if (!(loopBound instanceof CBinaryExpression)) {
      if (loopBound instanceof CIdExpression) {
        return loopVariableDelta.get(((CIdExpression) loopBound).getName());
      } else {
        return 0;
      }
    } else {
      switch (((CBinaryExpression) loopBound).getOperator().getOperator()) {
        case "+":
          return boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand1())
              + boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand2());
        case "-":
          return boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand1())
              - boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand2());
        default:
          return 0;
      }
    }
  }

  public AbstractState overwriteLocationState(AbstractState pState, LocationState locState) {
    List<AbstractState> allWrappedStatesByCompositeState = new ArrayList<>();
    for (AbstractState a :
        ((CompositeState) ((ARGState) pState).getWrappedState()).getWrappedStates()) {
      if (a instanceof LocationState) {
        allWrappedStatesByCompositeState.add(locState);
      } else {
        allWrappedStatesByCompositeState.add(a);
      }
    }
    AbstractState wrappedCompositeState = new CompositeState(allWrappedStatesByCompositeState);
    return new ARGState(wrappedCompositeState, null);
  }

  private StartStopNodesGhostCFA summaryCFA(
      final AbstractState pState,
      final Map<String, Integer> loopVariableDelta,
      final CExpression loopBound,
      final int boundDelta,
      final Integer loopBranchIndex) {
    int CFANodeCounter = 4;
    CFANode startNode = CFANode.newDummyCFANode("LS1");
    CFANode currentEndNode = CFANode.newDummyCFANode("LS2");
    CFAEdge loopIngoingConditionEdge =
        AbstractStates.extractLocation(pState).getLeavingEdge(loopBranchIndex);
    CFAEdge loopIngoingConditionDummyEdge =
        new CAssumeEdge(
            loopIngoingConditionEdge.getDescription(),
            FileLocation.DUMMY,
            startNode,
            currentEndNode,
            ((CAssumeEdge) loopIngoingConditionEdge).getExpression(),
            ((CAssumeEdge) loopIngoingConditionEdge).getTruthAssumption());
    startNode.addLeavingEdge(loopIngoingConditionDummyEdge);
    currentEndNode.addEnteringEdge(loopIngoingConditionDummyEdge);
    CFANode currentStartNode = currentEndNode;
    currentEndNode = CFANode.newDummyCFANode("LS3");
    for (Map.Entry<String, Integer> set : loopVariableDelta.entrySet()) {
      CVariableDeclaration pc =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.EXTERN,
              CNumericTypes.INT,
              set.getKey(),
              set.getKey(),
              set.getKey(),
              null);
      CExpression rightHandSide =
          new CBinaryExpression(
              FileLocation.DUMMY,
              null,
              null,
              new CIdExpression(FileLocation.DUMMY, pc),
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  null,
                  null,
                  CIntegerLiteralExpression.createDummyLiteral(set.getValue(), CNumericTypes.INT),
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      null,
                      null,
                      loopBound,
                      CIntegerLiteralExpression.createDummyLiteral(boundDelta, CNumericTypes.INT),
                      BinaryOperator.DIVIDE),
                  BinaryOperator.MULTIPLY),
              BinaryOperator.PLUS);
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, pc);
      CExpressionAssignmentStatement cStatementEdge =
          new CExpressionAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      CFAEdge dummyEdge =
          new CStatementEdge(
              set.getKey() + " = " + set.getValue(),
              cStatementEdge,
              FileLocation.DUMMY,
              currentStartNode,
              currentEndNode);
      currentStartNode.addLeavingEdge(dummyEdge);
      currentEndNode.addEnteringEdge(dummyEdge);
      currentStartNode = currentEndNode;
      currentEndNode = CFANode.newDummyCFANode("LS" + CFANodeCounter);
      CFANodeCounter += 1;
    }

    CFANode endNode = currentEndNode;
    CFAEdge loopOutgoingConditionEdge =
        AbstractStates.extractLocation(pState)
            .getLeavingEdge(
                1 - loopBranchIndex); // loopBranchIndex is either 0 or 1, so we negate it here to
    // get the other Edge
    CFAEdge loopOutgoingConditionDummyEdgeStart =
        new CAssumeEdge(
            loopIngoingConditionEdge.getDescription(),
            FileLocation.DUMMY,
            startNode,
            endNode,
            ((CAssumeEdge) loopIngoingConditionEdge).getExpression(),
            ((CAssumeEdge) loopIngoingConditionEdge).getTruthAssumption());
    startNode.addLeavingEdge(loopOutgoingConditionDummyEdgeStart);
    endNode.addEnteringEdge(loopOutgoingConditionDummyEdgeStart);
    CFAEdge loopOutgoingConditionDummyEdgeEndSummary =
        new CAssumeEdge(
            loopOutgoingConditionEdge.getDescription(),
            FileLocation.DUMMY,
            currentStartNode,
            endNode,
            ((CAssumeEdge) loopOutgoingConditionEdge).getExpression(),
            ((CAssumeEdge) loopOutgoingConditionEdge).getTruthAssumption());
    currentStartNode.addLeavingEdge(loopOutgoingConditionDummyEdgeEndSummary);
    endNode.addEnteringEdge(loopOutgoingConditionDummyEdgeEndSummary);
    return new StartStopNodesGhostCFA(startNode, endNode);
  }



  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      final AbstractState pState, final Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {

    Optional<Integer> loopBranchIndexOptional =
        getLoopBranchIndex(AbstractStates.extractLocation(pState));
    Integer loopBranchIndex;
    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.get();
    }

    Optional<CExpression> loopBoundOptional = bound(AbstractStates.extractLocation(pState));
    CExpression loopBound;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBound = loopBoundOptional.get();
    }

    Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(pState, loopBranchIndex);

    int boundDelta = boundDelta(loopVariableDelta, loopBound);
    if (boundDelta >= 0) {
      return Optional.empty();
    }

    StartStopNodesGhostCFA startStopCFANodesGhostCFA =
        summaryCFA(pState, loopVariableDelta, loopBound, loopBranchIndex, loopBranchIndex);

    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState newLocationState =
        new LocationState(
            startStopCFANodesGhostCFA.getStartNode(), oldLocationState.getFollowFunctionCalls());
    AbstractState dummyStateStart = overwriteLocationState(pState, newLocationState);
    Collection<? extends AbstractState> dummyStatesEndCollection =
        pTransferRelation.getAbstractSuccessors(dummyStateStart, pPrecision);
    Collection<AbstractState> realStatesEndCollection = new ArrayList<>();
    LocationState afterLoopLocationState =
        new LocationState(
            startStopCFANodesGhostCFA.getStopNode(), oldLocationState.getFollowFunctionCalls());
    for (AbstractState a : dummyStatesEndCollection) {
      realStatesEndCollection.add(overwriteLocationState(a, afterLoopLocationState));
    }
    return Optional.of(realStatesEndCollection);
  }


}
