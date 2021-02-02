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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
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

  public arithmeticStrategy() {}

  @Override
  public boolean canBeSummarized(final CFANode node) {
    if (node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0).getSuccessor().isLoopStart()) {
      CFANode loopStartNode = node.getLeavingEdge(0).getSuccessor();
      // TODO Check Bound of loop
      if (bound(loopStartNode).isEmpty()) {
        return false;
      }
      if (loopStartNode.getNumLeavingEdges() != 2) {
        return false;
      }
      if (loopContainsBranching(loopStartNode)) {
        return false;
      }
      if (!linearArithmeticExpressionsLoop(loopStartNode)) {
        return false;
      }
      if (loopTerminates(loopStartNode).isEmpty()) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }

  private boolean loopContainsBranching(final CFANode loopStartNode) {
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
      if (nextNode0 == loopStartNode || nextNode1 == loopStartNode) {
        return false;
      }
    }
    return true;
  }

  private Optional<Integer> loopTerminates(final CFANode ploopStartNode) {
    // TODO It is assumed that the start Value is 0
    CFAEdge edge = ploopStartNode.getLeavingEdge(0);
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
    CIdExpression variable;
    CIntegerLiteralExpression bound;

    switch (operator) {
      case "<":
      case "<=":
        if (operand1 instanceof CIdExpression && operand2 instanceof CIntegerLiteralExpression) {
          variable = (CIdExpression) operand1;
          bound = (CIntegerLiteralExpression) operand2;
          Integer oneLoopIterationDelta = oneLoopValue(variable.getName(), ploopStartNode);
          if (oneLoopIterationDelta > 0) {
            return Optional.of(bound.getValue().intValue() / oneLoopIterationDelta);
          } else {
            return Optional.empty();
          }
        } else if (operand2 instanceof CIdExpression && operand1 instanceof CIntegerLiteralExpression) {
          variable = (CIdExpression) operand2;
          bound = (CIntegerLiteralExpression) operand1;
          Integer oneLoopIterationDelta = oneLoopValue(variable.getName(), ploopStartNode);
          if (oneLoopIterationDelta < 0) {
            return Optional.of(bound.getValue().intValue() / oneLoopIterationDelta);
          } else {
            return Optional.empty();
          }
        }
        break;
      case ">":
      case ">=":
        if (operand1 instanceof CIdExpression && operand2 instanceof CIntegerLiteralExpression) {
          variable = (CIdExpression) operand1;
          bound = (CIntegerLiteralExpression) operand2;
          Integer oneLoopIterationDelta = oneLoopValue(variable.getName(), ploopStartNode);
          if (oneLoopIterationDelta < 0) {
            return Optional.of(bound.getValue().intValue() / oneLoopIterationDelta);
          } else {
            return Optional.empty();
          }
        } else if (operand2 instanceof CIdExpression
            && operand1 instanceof CIntegerLiteralExpression) {
          variable = (CIdExpression) operand2;
          bound = (CIntegerLiteralExpression) operand1;
          Integer oneLoopIterationDelta = oneLoopValue(variable.getName(), ploopStartNode);
          if (oneLoopIterationDelta > 0) {
            return Optional.of(bound.getValue().intValue() / oneLoopIterationDelta);
          } else {
            return Optional.empty();
          }
        }
        break;
      default:
        return Optional.empty();
    }
    return Optional.empty();
  }

  private Integer oneLoopValue(String pName, final CFANode ploopStartNode) {
    Integer branchIndex = getLoopBranchIndex(ploopStartNode);
    CFANode currentNode = ploopStartNode.getLeavingEdge(branchIndex).getSuccessor();
    Integer deltaOfVariable = 0;
    while (currentNode != ploopStartNode) {
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge instanceof CStatementEdge) {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
        CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();
        if (leftSide instanceof CIdExpression && rigthSide instanceof CBinaryExpression) {
          if (((CIdExpression) leftSide).getName() == pName) {
            Integer value = 0;
            CExpression operand1 = ((CBinaryExpression) rigthSide).getOperand1();
            CExpression operand2 = ((CBinaryExpression) rigthSide).getOperand2();
            if (operand1 instanceof CIntegerLiteralExpression) {
              value = ((CIntegerLiteralExpression) operand1).getValue().intValue();
            } else if (operand2 instanceof CIntegerLiteralExpression) {
              value =
                  ((CIntegerLiteralExpression) operand2)
                      .getValue()
                      .intValue();
            }
            switch (((CBinaryExpression) rigthSide).getOperator().getOperator()) {
              case "+":
                deltaOfVariable += value;
                break;
              case "-":
                deltaOfVariable -= value;
                break;
              default:
                break;
            }
          }
        }
      }
      currentNode = edge.getSuccessor();
    }
    return deltaOfVariable;
  }

  private boolean linearArithemticExpression(final CExpression expression) {
    if (expression instanceof CIdExpression) {
      return true;
    } else if (expression instanceof CIntegerLiteralExpression) {
      return true;
    } else if (expression instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) expression).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
      switch (operator) {
        case "+":
        case "-":
          // return linearArithemticExpression(operand1) && linearArithemticExpression(operand2);
          return (operand1 instanceof CIdExpression)
              && (operand2 instanceof CIntegerLiteralExpression);
        case "*":
          /*
          // TODO This does not work in the general case, so it is ignored
          if (operand1 instanceof CIntegerLiteralExpression) {
            return linearArithemticExpression(operand2);
          } else if (operand2 instanceof CIntegerLiteralExpression) {
            return linearArithemticExpression(operand1);
          }
          // $FALL-THROUGH$
          */
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

    return true;
  }

  private boolean linearArithmeticExpressionsLoop(final CFANode pLoopStartNode) {
    CFANode nextNode0 = pLoopStartNode.getLeavingEdge(0).getSuccessor();
    CFANode nextNode1 = pLoopStartNode.getLeavingEdge(1).getSuccessor();
    boolean nextNode0Valid = true;
    boolean nextNode1Valid = true;
    while (nextNode0 != pLoopStartNode
        && nextNode1 != pLoopStartNode
        && (nextNode0Valid || nextNode1Valid)) {
      if (nextNode0Valid
          && nextNode0.getNumLeavingEdges() == 1
          && linearArithmeticExpressionEdge(nextNode0.getLeavingEdge(0))) {
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode1Valid
          && nextNode1.getNumLeavingEdges() == 1
          && linearArithmeticExpressionEdge(nextNode1.getLeavingEdge(0))) {
        nextNode1 = nextNode1.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode1Valid = false;
      }
      if (nextNode0 == pLoopStartNode || nextNode1 == pLoopStartNode) {
        return true;
      }
    }
    return false;
  }

  // Returns the bound in the form 0 < x where x is the CExpression
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

    CExpression standardLoopBoundForm;
    switch (operator) {
      case "<":
        return Optional.of(
            CBinaryExpression(
                expression.getFileLocation(),
                ((CBinaryExpression) expression).getExpressionType(),
                ((CBinaryExpression) expression).getCalculationType(),
                operand2,
                operand1,
                BinaryOperator.MINUS)); // TODO Why is this wrong

        /*
        * CBinaryExpression(final FileLocation pFileLocation,
                             final CType pExpressionType,
                             final CType pCalculationType,
                             final CExpression pOperand1,
                             final CExpression pOperand2,
                             final BinaryOperator pOperator)
        */

      case ">":
      case "<=":
      case ">=":
        break;
      default:
        return Optional.empty();
    }

    return Optional.empty();
  }

  public CFANode getAfterLoopCFANode(final CFANode node) {
    CFANode loopStartNode = node.getLeavingEdge(0).getSuccessor();
    CFANode nextNode0 = loopStartNode.getLeavingEdge(0).getSuccessor();
    CFANode nextNode1 = loopStartNode.getLeavingEdge(1).getSuccessor();
    boolean nextNode0Valid = true;
    boolean nextNode1Valid = true;
    while (nextNode0 != loopStartNode
        && nextNode1 != loopStartNode
        && (nextNode0Valid || nextNode1Valid)) {
      if (nextNode0Valid && nextNode0.getNumLeavingEdges() == 1) {
        CFAEdge edge = nextNode0.getLeavingEdge(0);
        nextNode0 = edge.getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode1Valid && nextNode1.getNumLeavingEdges() == 1) {
        nextNode1 = nextNode1.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode1Valid = false;
      }
    }
    if (nextNode0 == loopStartNode) {
      return loopStartNode.getLeavingEdge(1).getSuccessor();
    } else {
      return loopStartNode.getLeavingEdge(0).getSuccessor();
    }
  }

  public AbstractState overwriteLocationState(
      AbstractState pState, LocationState locState) {
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

  private Integer getLoopBound(CFANode loopStartNode) {
    // TODO Improve this using matrix multiplication. For this use JBLAS
    // General Bound using variables must be checked how CPA works with
    // Integer division
    return loopTerminates(loopStartNode).get();
  }

  private Integer getLoopBranchIndex(CFANode loopStartNode) {
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
        return 0;
      } else if (nextNode1 == loopStartNode) {
        return 1;
      }
    }
    return -1;
  }

  private Map<String, Integer> summarizeExpressions(final AbstractState pState) {
    Map<String, Integer> summarizedExpressions = new HashMap<>();
    CFANode loopStartNode = AbstractStates.extractLocation(pState).getLeavingEdge(0).getSuccessor();
    Integer loopIterations = getLoopBound(loopStartNode);
    // Calculate deltas in one Loop Iteration
    Integer branchIndex = getLoopBranchIndex(loopStartNode);
    CFANode currentNode = loopStartNode.getLeavingEdge(branchIndex).getSuccessor();
    while (currentNode != loopStartNode) {
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
                if (summarizedExpressions.containsKey(((CIdExpression) leftSide).getName())) {
                summarizedExpressions.put(
                    ((CIdExpression) leftSide).getName(),
                    value + summarizedExpressions.get(((CIdExpression) leftSide).getName()));
                } else {
                summarizedExpressions.put(((CIdExpression) leftSide).getName(), value);
                }
                break;
              case "-":
              if (summarizedExpressions.containsKey(((CIdExpression) leftSide).getName())) {
                summarizedExpressions.put(
                    ((CIdExpression) leftSide).getName(),
                    -value + summarizedExpressions.get(((CIdExpression) leftSide).getName()));
              } else {
                summarizedExpressions.put(((CIdExpression) leftSide).getName(), value);
              }
                break;
              default:
                break;
            }
        }
      }
      currentNode = edge.getSuccessor();
    }
    Map<String, Integer> finalSummarizedExpressions = new HashMap<>();
    for (Map.Entry<String, Integer> set : summarizedExpressions.entrySet()) {
      finalSummarizedExpressions.put(set.getKey(), set.getValue() * loopIterations);
    }

    return finalSummarizedExpressions;
  }

  private AbstractState summaryCFA(final AbstractState pState) {
    int CFANodeCounter = 3;
    CFANode dummyNodeStart = CFANode.newDummyCFANode("LS1");
    CFANode currentStartNode = dummyNodeStart;
    CFANode currentEndNode = CFANode.newDummyCFANode("LS2");
    Map<String, Integer> summarizedExpression = summarizeExpressions(pState);
    for (Map.Entry<String, Integer> set : summarizedExpression.entrySet()) {
      CExpression rightHandSide =
          CIntegerLiteralExpression.createDummyLiteral(set.getValue(), CNumericTypes.INT);
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

    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState newLocationState =
        new LocationState(dummyNodeStart, oldLocationState.getFollowFunctionCalls());
    AbstractState dummyStateStart = overwriteLocationState(pState, newLocationState);
    return dummyStateStart;
  }

  @Override
  public Collection<? extends AbstractState> summarizeLoopState(
      final AbstractState pState, final Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    AbstractState dummyStateStart = summaryCFA(pState);
    Collection<? extends AbstractState> dummyStatesEndCollection =
        pTransferRelation.getAbstractSuccessors(dummyStateStart, pPrecision);
    Collection<AbstractState> realStatesEndCollection = new ArrayList<>();
    CFANode afterLoopCFANode = getAfterLoopCFANode(AbstractStates.extractLocation(pState));
    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState afterLoopLocationState =
        new LocationState(afterLoopCFANode, oldLocationState.getFollowFunctionCalls());
    for (AbstractState a : dummyStatesEndCollection) {
      realStatesEndCollection.add(overwriteLocationState(a, afterLoopLocationState));
    }
    return realStatesEndCollection;
  }


}
