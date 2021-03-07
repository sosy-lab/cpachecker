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
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ArithmeticStrategy implements StrategyInterface {

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

  public ArithmeticStrategy() {}

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
    CType calculationType = ((CBinaryExpression) expression).getCalculationType();
    CType expressionType = ((CBinaryExpression) expression).getExpressionType();

    if (!((operand1 instanceof CIdExpression
            && (operand2 instanceof CIdExpression || operand2 instanceof CIntegerLiteralExpression))
        || (operand1 instanceof CIntegerLiteralExpression && operand2 instanceof CIdExpression))) {
      return Optional.empty();
    }

    switch (operator) {
      case "<":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand2,
                operand1,
                BinaryOperator.MINUS));

      case ">":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand1,
                operand2,
                BinaryOperator.MINUS));
      case "<=":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand2,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    expressionType,
                    calculationType,
                    operand1,
                    CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
                    BinaryOperator.MINUS),
                BinaryOperator.MINUS));
      case ">=":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand1,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    expressionType,
                    calculationType,
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

  private CAssumeEdge overwriteStartEndStateEdge(
      CAssumeEdge edge, boolean truthAssignment, CFANode startNode, CFANode endNode) {
    return new CAssumeEdge(
        edge.getDescription(),
        FileLocation.DUMMY,
        startNode,
        endNode,
        edge.getExpression(),
        truthAssignment);
  }

  private CStatementEdge overwriteStartEndStateEdge(
      CStatementEdge edge, CFANode startNode, CFANode endNode) {
    return new CStatementEdge(
        edge.getRawStatement(), edge.getStatement(), FileLocation.DUMMY, startNode, endNode);
  }

  private CFANode unrollLoopOnce(CFANode loopStartNode, CFANode endNodeGhostCFA, CFANode startNodeGhostCFA) {
    CFANode currentNode = loopStartNode;
    boolean initial = true;
    CFANode currentUnrollingNode = CFANode.newDummyCFANode("LSU");
    CFANode oldUnrollingNode = startNodeGhostCFA;
    while (currentNode != loopStartNode || initial) {
      CFAEdge currentLoopEdge = currentNode.getLeavingEdge(0);
      if (initial) {
        assert currentLoopEdge instanceof CAssumeEdge;
        CFAEdge tmpLoopEdgeFalse =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, false, oldUnrollingNode, endNodeGhostCFA);
        oldUnrollingNode.addLeavingEdge(tmpLoopEdgeFalse);
        endNodeGhostCFA.addEnteringEdge(tmpLoopEdgeFalse);
        CFAEdge tmpLoopEdgeTrue =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, true, oldUnrollingNode, currentUnrollingNode);
        oldUnrollingNode.addLeavingEdge(tmpLoopEdgeTrue);
        currentUnrollingNode.addEnteringEdge(tmpLoopEdgeTrue);
        currentNode = currentLoopEdge.getSuccessor();
        initial = false;
      } else {
        CFAEdge tmpLoopEdge;
        if (currentLoopEdge instanceof CStatementEdge) {
          tmpLoopEdge =
              overwriteStartEndStateEdge(
                  (CStatementEdge) currentLoopEdge, oldUnrollingNode, currentUnrollingNode);
        } else {
          assert currentLoopEdge instanceof BlankEdge;
          tmpLoopEdge =
              new BlankEdge(
                  currentLoopEdge.getRawStatement(),
                  FileLocation.DUMMY,
                  oldUnrollingNode,
                  currentUnrollingNode,
                  currentLoopEdge.getDescription());
        }
        oldUnrollingNode.addLeavingEdge(tmpLoopEdge);
        currentUnrollingNode.addEnteringEdge(tmpLoopEdge);
        currentNode = currentLoopEdge.getSuccessor();
      }
      oldUnrollingNode = currentUnrollingNode;
      currentUnrollingNode = CFANode.newDummyCFANode("LSU");
    }

    return oldUnrollingNode;
  }

  private StartStopNodesGhostCFA summaryCFA(
      final AbstractState pState,
      final Map<String, Integer> loopVariableDelta,
      final CExpression loopBound,
      final int boundDelta,
      final Integer loopBranchIndex) {
    int CFANodeCounter = 1;
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("LS1");
    CFANode currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS2");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("LSENDGHHOST");
    CFANode loopStartNode = AbstractStates.extractLocation(pState);
    CFAEdge loopIngoingConditionEdge = loopStartNode.getLeavingEdge(loopBranchIndex);
    CFAEdge loopIngoingConditionDummyEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge, true, startNodeGhostCFA, currentEndNodeGhostCFA);
    CFAEdge loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge, false, startNodeGhostCFA, endNodeGhostCFA);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeTrue);
    currentEndNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeTrue);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);
    CFANode currentStartNodeGhostCFA = currentEndNodeGhostCFA;
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS3");

    CType calculationType = ((CBinaryExpression) loopBound).getCalculationType();
    CType expressionType = ((CBinaryExpression) loopBound).getExpressionType();
    // Check for Overflows by unrolling the Loop once before doing the Summary and once after doing
    // the summary
    CBinaryExpression loopBoundtwiceUnrollingExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            expressionType,
            calculationType,
            CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                loopBound,
                CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT),
                BinaryOperator.MINUS),
            BinaryOperator.LESS_THAN);
    CFAEdge twiceLoopUnrollingConditionEdgeTrue =
        new CAssumeEdge(
            ((CBinaryExpression) loopBound).toString() + "- 2 > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            currentEndNodeGhostCFA,
            loopBoundtwiceUnrollingExpression,
            true);
    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingConditionEdgeTrue);
    currentEndNodeGhostCFA.addEnteringEdge(twiceLoopUnrollingConditionEdgeTrue);
    CFANode loopUnrollingCurrentNode = CFANode.newDummyCFANode("LS5");
    CFAEdge twiceLoopUnrollingConditionEdgeFalse =
        new CAssumeEdge(
            ((CBinaryExpression) loopBound).toString() + "- 2 > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            loopUnrollingCurrentNode,
            loopBoundtwiceUnrollingExpression,
            false);

    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingConditionEdgeFalse);
    loopUnrollingCurrentNode.addEnteringEdge(twiceLoopUnrollingConditionEdgeFalse);
    loopUnrollingCurrentNode =
        unrollLoopOnce(loopStartNode, loopUnrollingCurrentNode, endNodeGhostCFA);
    loopUnrollingCurrentNode =
        unrollLoopOnce(loopStartNode, loopUnrollingCurrentNode, endNodeGhostCFA);
    loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge,
            false,
            loopUnrollingCurrentNode,
            endNodeGhostCFA);
    loopUnrollingCurrentNode.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);

    currentStartNodeGhostCFA =
        unrollLoopOnce(loopStartNode, currentEndNodeGhostCFA, endNodeGhostCFA);
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS6");

    // Make Summary
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
              expressionType,
              calculationType,
              new CIdExpression(FileLocation.DUMMY, pc),
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  expressionType,
                  calculationType,
                  CIntegerLiteralExpression.createDummyLiteral(set.getValue(), CNumericTypes.INT),
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      expressionType,
                      calculationType,
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
              currentStartNodeGhostCFA,
              currentEndNodeGhostCFA);
      currentStartNodeGhostCFA.addLeavingEdge(dummyEdge);
      currentEndNodeGhostCFA.addEnteringEdge(dummyEdge);
      currentStartNodeGhostCFA = currentEndNodeGhostCFA;
      currentEndNodeGhostCFA = CFANode.newDummyCFANode("LSI" + CFANodeCounter);
      CFANodeCounter += 1;
    }

    currentStartNodeGhostCFA =
        unrollLoopOnce(loopStartNode, currentStartNodeGhostCFA, endNodeGhostCFA);

    loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge,
            false,
            currentStartNodeGhostCFA,
            endNodeGhostCFA);
    currentStartNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);

    return new StartStopNodesGhostCFA(startNodeGhostCFA, endNodeGhostCFA);
  }

  private boolean linearArithemticExpression(final CExpression expression) {
    if (expression instanceof CIdExpression) {
      return true;
    } else if (expression instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) expression).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
      switch (operator) {
        case "+":
        case "-":
          return ((operand1 instanceof CIdExpression)
                  && (operand2 instanceof CIntegerLiteralExpression))
              || ((operand2 instanceof CIdExpression)
                  && (operand1 instanceof CIntegerLiteralExpression));
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  private boolean linearArithmeticExpressionEdge(final CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return true;
    }

    if (edge instanceof CAssumeEdge) {
      return true;
    }

    if (!(edge instanceof CStatementEdge)) {
      return false;
    }

    CStatement statement = ((CStatementEdge) edge).getStatement();
    if (!(statement instanceof CExpressionAssignmentStatement)) {
      return false;
    }

    CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
    CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();

    if (!(leftSide instanceof CIdExpression)) {
      return false;
    }
    if (!linearArithemticExpression(rigthSide)) {
      return false;
    }

    if (rigthSide instanceof CBinaryExpression) {
      if (((CBinaryExpression)rigthSide).getOperand1() instanceof CIdExpression) {
        return ((CIdExpression) ((CBinaryExpression) rigthSide).getOperand1()).getName()
            == ((CIdExpression) leftSide).getName();
      } else {
        return ((CIdExpression) ((CBinaryExpression) rigthSide).getOperand2()).getName()
            == ((CIdExpression) leftSide).getName();
      }
    }

    if (rigthSide instanceof CIdExpression) {
      return ((CIdExpression) rigthSide).getName() == ((CIdExpression) leftSide).getName();
    }
    return false;
  }


  private boolean linearArithmeticExpressionsLoop(final CFANode pLoopStartNode, int branchIndex) {
    CFANode nextNode0 = pLoopStartNode.getLeavingEdge(branchIndex).getSuccessor();
    boolean nextNode0Valid = true;
    while (nextNode0 != pLoopStartNode && nextNode0Valid) {
      if (nextNode0Valid
          && nextNode0.getNumLeavingEdges() == 1
          && linearArithmeticExpressionEdge(nextNode0.getLeavingEdge(0))) {
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode0 == pLoopStartNode) {
        return true;
      }
    }
    return false;
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

    if (!linearArithmeticExpressionsLoop(AbstractStates.extractLocation(pState), loopBranchIndex)) {
      return Optional.empty();
    }

    Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(pState, loopBranchIndex);

    int boundDelta = boundDelta(loopVariableDelta, loopBound);
    if (boundDelta >= 0) { // TODO How do you treat non Termination?
      return Optional.empty();
    }

    StartStopNodesGhostCFA startStopCFANodesGhostCFA =
        summaryCFA(pState, loopVariableDelta, loopBound, boundDelta, loopBranchIndex);

    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState newLocationState =
        new LocationState(
            startStopCFANodesGhostCFA.getStartNode(), oldLocationState.getFollowFunctionCalls());
    AbstractState dummyStateStart = overwriteLocationState(pState, newLocationState);
    @SuppressWarnings("unchecked")
    ArrayList<AbstractState> dummyStatesEndCollection =
        new ArrayList<>(
            pTransferRelation.getAbstractSuccessors(
                dummyStateStart,
                pPrecision)); // TODO Can you write Collection<AbstractState> insetad of
                              // Collection<?
                         // extends AbstractState>
    Collection<AbstractState> realStatesEndCollection = new ArrayList<>();
    LocationState afterLoopLocationState =
        new LocationState(
            startStopCFANodesGhostCFA.getStopNode(), oldLocationState.getFollowFunctionCalls());
    // Iterate till the end of the ghost CFA
    while (!dummyStatesEndCollection.isEmpty()) {
      ArrayList<AbstractState> newStatesNotFinished = new ArrayList<>();
      Iterator<? extends AbstractState> iterator = dummyStatesEndCollection.iterator();
      while (iterator.hasNext()) {
          AbstractState stateGhostCFA = iterator.next();
        if (AbstractStates.extractLocation(stateGhostCFA)
            == startStopCFANodesGhostCFA.getStopNode()) {
          realStatesEndCollection.add(
              overwriteLocationState(stateGhostCFA, afterLoopLocationState));
        } else {
          newStatesNotFinished.addAll(
              pTransferRelation.getAbstractSuccessors(stateGhostCFA, pPrecision));
        }
      }
      dummyStatesEndCollection = newStatesNotFinished;
    }
    return Optional.of(realStatesEndCollection);
  }


}
