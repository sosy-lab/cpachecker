// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopInformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.loopAbstraction.NotKnownLoopEdgeTypeException;

/** This class collects and saves all of the data in one loop */
public class LoopData implements Comparable<LoopData>, StatisticsProvider {

  private CFANode loopStart;
  private CFANode loopEnd;
  private CFANode failedState;
  private CFANode forStart = null;

  private List<CFANode> conditionInFor;
  private List<CFANode> nodesInLoop;
  private List<CFANode> nodesInCondition;
  private List<LoopVariables> output;
  private List<LoopVariables> inputOutput;
  private List<CFANode> endOfCondition;

  private String condition;
  private LoopType loopType;

  private int amountOfPaths;
  private int numberAllOutputs;

  private boolean flagEndless = false;
  private boolean loopInLoop;
  private boolean outerLoop;
  private Loop innerLoop;
  private boolean canBeAccelerated;
  private boolean onlyRandomCondition;

  private Timer timeToAnalyze;
  private TimeSpan analyzeTime;

  private static final int ONLY_EDGE = 0;
  private static final int VALID_STATE = 0;
  private static final int ERROR_STATE = 1;
  private static final int LAST_POSITION_OF_LIST = 1;
  private static final int FIRST_POSITION_OF_LIST = 0;
  private static final int NO_IF_CASE = -1;

  public LoopData(
      CFA cfa,
      List<CFANode> loopNodes,
      Loop loop,
      LogManager pLogger) {
    timeToAnalyze = new Timer();
    timeToAnalyze.start();
    this.endOfCondition = new ArrayList<>();
    getStartAndConditionEnd(loopNodes);
    conditionInFor = new ArrayList<>();
    output = new ArrayList<>();
    onlyRandomCondition = false;
    loopInLoop = isInnerLoop(loop, cfa);
    outerLoop = isOuterLoop(loop, cfa);
    loopType = findLoopType(loopStart);
    nodesInLoop = loopNodes;
    loopEnd = nodesInLoop.get(nodesInLoop.size() - LAST_POSITION_OF_LIST);
    nodesInCondition =
        nodesInCondition(cfa, pLogger, loopStart, loopType, nodesInLoop, endOfCondition, forStart);
    try {
      output = getAllOutputs(cfa, nodesInLoop);
      inputOutput = getAllIO(output, nodesInLoop);
    } catch (NotKnownLoopEdgeTypeException e) {
      pLogger.log(Level.WARNING, e);
    }
    numberAllOutputs = getAllNumberOutputs(output);
    amountOfPaths = getAllPaths(nodesInLoop, loopEnd, nodesInCondition, failedState, output);
    canBeAccelerated =
        canLoopBeAccelerated(
            nodesInCondition,
            loopType,
            amountOfPaths,
            numberAllOutputs,
            flagEndless,
            conditionInFor);
    condition =
        nodesToCondition(
            nodesInCondition, loopType, endOfCondition, flagEndless, onlyRandomCondition);
    timeToAnalyze.stop();
    analyzeTime = timeToAnalyze.getLengthOfLastInterval();
  }

  /**
   * This method looks for the start- and endnode of the condition
   *
   * @param loopNodes Nodes that are contained in the loop
   */
  private void getStartAndConditionEnd(List<CFANode> loopNodes) {
    CFANode loopHead = loopNodes.get(FIRST_POSITION_OF_LIST);
    CFAEdge tempEdge = loopHead.getLeavingEdge(VALID_STATE);
    CFANode tempNode = null;
    if (tempEdge instanceof AssumeEdge || tempEdge.getCode().contains("CPAchecker_TMP")) {
      while (tempEdge instanceof AssumeEdge || tempEdge.getCode().contains("CPAchecker_TMP")) {
        for (int i = 0; i < tempEdge.getSuccessor().getNumLeavingEdges(); i++) {
          if (!(tempEdge.getSuccessor().getLeavingEdge(i) instanceof AssumeEdge)
              || tempEdge.getCode().contains("CPAchecker_TMP")) {
            if (tempEdge.getCode().contains("__VERIFIER_nondet_")) {
              tempNode = tempEdge.getSuccessor().getLeavingEdge(1).getSuccessor();
            } else if (!tempEdge.getCode().contains("CPAchecker_TMP")) {
              tempNode = tempEdge.getSuccessor();
            }
          }
        }
        if (!(tempEdge.getCode().contains("CPAchecker_TMP") && tempEdge.getCode().contains("=="))) {
          tempEdge = tempEdge.getSuccessor().getLeavingEdge(VALID_STATE);
        } else {
          tempEdge =
              tempEdge
                  .getPredecessor()
                  .getLeavingEdge(1)
                  .getSuccessor()
                  .getLeavingEdge(VALID_STATE);
        }
      }
    } else {
      tempNode = loopHead;
    }

    setLoopStart(loopHead);
    addToLoopEnd(tempNode);
  }

  /**
   * looks for the loop-type of a loop
   *
   * @param firstNode while loops typically have the "while" in the entering edge of the first cfa
   *     node of the loop
   * @return returns the type of the loop, possible solutions are "while", "for" at the moment
   */
  private LoopType findLoopType(CFANode firstNode) {
    LoopType tempLoopType = null;

    if (firstNode.getNumEnteringEdges() > 0 && firstNode.getEnteringEdge(0) instanceof BlankEdge) {
      tempLoopType = LoopType.WHILE;
    } else {
      CFANode temp = firstNode.getEnteringEdge(ONLY_EDGE).getPredecessor();
      boolean flag = true;

      while (flag) {
        if (temp.getNumEnteringEdges() > 0
            && temp.getEnteringEdge(ONLY_EDGE) instanceof BlankEdge) {
          tempLoopType = LoopType.FOR;
          setForStart(temp);
          flag = false;
        }
        if (temp.getNumEnteringEdges() > 0) {
          temp = temp.getEnteringEdge(ONLY_EDGE).getPredecessor();
        } else {
          flag = false;
        }
      }
    }
    return tempLoopType;
  }

  /**
   * Checks if this loop is part of another loop
   *
   * @param loop This loop
   * @param cfa used to get a list of all loops to test if there is another loop that's an outer
   *     loop of this one
   * @return true if this is a inner loop or false if this isn't a inner loop
   */
  private boolean isInnerLoop(Loop loop, CFA cfa) {
    boolean tempInnerLoop = false;

    for (Loop tempLoop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (tempLoop.isOuterLoopOf(loop)) {
        tempInnerLoop = true;
      }
    }

    return tempInnerLoop;
  }

  /**
   * this function checks if the loop is a outer loop for some other loop
   *
   * @param loop this loop
   * @param cfa uses the information about all loops that you can get from the cfa
   * @return returns a boolean value, true if it is a outer loop, false if it isn't
   */
  private boolean isOuterLoop(Loop loop, CFA cfa) {
    boolean tempOuterLoop = false;

    for (Loop tempLoop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (loop.isOuterLoopOf(tempLoop)) {
        tempOuterLoop = true;
        innerLoop = tempLoop;
      }
    }

    return tempOuterLoop;
  }

  /**
   * This method looks for all the outputs a loop can have
   *
   * @param cfa uses the cfa to check when a variable get initialized
   * @param loopNodes checks all of the nodes in a loop for output variables
   * @return returns a list with all of the variable names that are outputs in a loop
   * @throws NotKnownLoopEdgeTypeException Exception that gets thrown if the edge-type isn't
   *     implemented in the loop data class
   */
  private List<LoopVariables> getAllOutputs(CFA cfa, List<CFANode> loopNodes)
      throws NotKnownLoopEdgeTypeException {
    List<LoopVariables> tmpO = new ArrayList<>();

    for (CFANode node : loopNodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if ((node.getLeavingEdge(i) instanceof AStatementEdge
                || node.getLeavingEdge(i) instanceof ADeclarationEdge)
            && (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null
                || CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null)) {
          boolean variablenameNotContainsCPAchecker = true;
          if (CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null
              && CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i))
                  .contains("__CPAchecker_")) {
            variablenameNotContainsCPAchecker = false;
          } else if (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null
              && CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i))
                  .toString()
                  .contains("__CPAchecker_")) {
            variablenameNotContainsCPAchecker = false;
          }

          if (variablenameNotContainsCPAchecker) {

            if (node.getLeavingEdge(i) instanceof AStatementEdge) {
              AStatementEdge x = (AStatementEdge) node.getLeavingEdge(i);
              CExpressionAssignmentStatement c = (CExpressionAssignmentStatement) x.getStatement();
              if (c.getLeftHandSide() instanceof CIdExpression
                  || c.getLeftHandSide() instanceof CArraySubscriptExpression) {
                tmpO.add(checkForValues(c.getLeftHandSide(), node));
              } else {
                throw new NotKnownLoopEdgeTypeException();
              }
          }
        }
      }
    }
    }

    for (CFANode n : cfa.getAllNodes()) {
      for (int e = 0; e < n.getNumLeavingEdges(); e++) {
        if (n.getLeavingEdge(e) instanceof ADeclarationEdge) {
          for (LoopVariables o : tmpO) {

            ADeclarationEdge dec = (ADeclarationEdge) n.getLeavingEdge(e);
            CSimpleDeclaration v = (CSimpleDeclaration) dec.getDeclaration();


            if (o.getVariableNameAsString().equals(v.getName())) {

              o.setInitializationLine(
                  ((ADeclarationEdge) n.getLeavingEdge(e))
                      .getDeclaration()
                      .getFileLocation()
                      .getStartingLineInOrigin());
              }
            }
          }
        }
    }
    List<LoopVariables> removeDO = new ArrayList<>();
    for (LoopVariables o : tmpO) {
      boolean flag = true;
      if (removeDO.isEmpty()) {
        removeDO.add(o);
      } else {
        for (LoopVariables tempO : removeDO) {
          if (tempO.getVariableNameAsString().equals(o.getVariableNameAsString())
              && tempO.getInitializationLine().equals(o.getInitializationLine())) {
            flag = false;
          }
        }
        if (flag) {
          removeDO.add(o);
        }
      }
    }
    tmpO = removeDO;
    return tmpO;
  }

  /**
   * Checks if this is a CIdExpression or a CArraySubscriptExpression and makes a new
   * LoopVariableObject of the variable
   *
   * @param expression CExpression that gets checked
   * @param node CFANode which contains the edge that contains the variable
   * @return returns a LoopVariables-Object with the necessary Variable-information
   */
  private LoopVariables checkForValues(CExpression expression, CFANode node) {
    LoopVariables loopVariable = null;
    if (expression instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) expression;
      loopVariable = new LoopVariables(var, node, false, null, null);
    } else if (expression instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression var = (CArraySubscriptExpression) expression;
      CArrayType array = (CArrayType) var.getArrayExpression().getExpressionType();
      loopVariable = new LoopVariables(var, node, true, array.getLengthAsInt().orElseThrow(), null);
    }
    return loopVariable;
  }

  /**
   * Counts the number of outputs
   *
   * @param pOutput List with all the output loopvariables
   * @return returns the number of all output-variables
   */
  private int getAllNumberOutputs(List<LoopVariables> pOutput) {
    int tmpInt = 0;
    for (LoopVariables tmp : pOutput) {
      if (tmp.getIsArray()) {
        tmpInt = tmpInt + tmp.getArrayLength();

      } else {
        tmpInt = tmpInt + 1;
      }
    }
    return tmpInt;
  }

  /**
   * This method compares the input-variable names and output-variable names to see if there are
   * some that are equal since these are inputs and outputs and used for the loopabstraction
   *
   * @param tmpOutput List of all the output variables to compare to the input variables
   * @param loopNodes List of all the nodes in the loop to filter out the input variables with the
   *     getAllInputs method
   * @return returns a list of variables that are inputs and outputs at the same time
   * @throws NotKnownLoopEdgeTypeException Exception that gets thrown if the edge-type isn't
   *     implemented in the loop data class
   */
  private List<LoopVariables> getAllIO(List<LoopVariables> tmpOutput, List<CFANode> loopNodes)
      throws NotKnownLoopEdgeTypeException {
    List<String> inputs = getAllInputs(loopNodes);
    List<LoopVariables> loopO = tmpOutput;
    List<LoopVariables> temp = new ArrayList<>();

    for (LoopVariables o : loopO) {
      for (String i : inputs) {
        if (o.getVariableNameAsString().contentEquals(i)) {
          boolean flagNO = true;
          for (LoopVariables v : temp) {
            if (o.getVariableNameAsString().equals(v.getVariableNameAsString())) {
              flagNO = false;
            }
          }
          if (flagNO) {
            temp.add(o);
          }
        }
      }
    }
    return temp;
  }

  /**
   * Gets the name of the input variable as a String
   *
   * @param pE BinaryExpression that contains the input variable
   * @return String with the name of the input variable
   */
  private String getInputVariableFromBinaryExpression(CBinaryExpression pE) {
    String inputVariable = "";
    CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
    CExpression rVarInBinaryExp = pE.getOperand2();

    if (lVarInBinaryExp instanceof CIdExpression
        || lVarInBinaryExp instanceof CArraySubscriptExpression) {
      if (lVarInBinaryExp instanceof CIdExpression) {
        CIdExpression lVar = (CIdExpression) lVarInBinaryExp;
        inputVariable = lVar.getName();
      } else {
        CArraySubscriptExpression lVar = (CArraySubscriptExpression) lVarInBinaryExp;
        inputVariable = lVar.getArrayExpression().toString();
      }
    }
    if (rVarInBinaryExp instanceof CIdExpression
        || rVarInBinaryExp instanceof CArraySubscriptExpression) {
      if (rVarInBinaryExp instanceof CIdExpression) {
        CIdExpression rVar = (CIdExpression) rVarInBinaryExp;
        inputVariable = rVar.getName();
      } else {
        CArraySubscriptExpression rVar = (CArraySubscriptExpression) rVarInBinaryExp;
        inputVariable = rVar.getArrayExpression().toString();
      }
    }
    return inputVariable;
  }

  /**
   * This method looks for all of the input-variables in the loop
   *
   * @param loopNodes List of all the nodes in the loop to check if either of them has a edge that
   *     uses a variable that qualifies as an input
   * @return returns a list with the names of all the input variables
   * @throws NotKnownLoopEdgeTypeException Exception that gets thrown if the edge-type isn't
   *     implemented in the loop data class
   */
  private List<String> getAllInputs(List<CFANode> loopNodes) throws NotKnownLoopEdgeTypeException {

    List<String> temp = new ArrayList<>();
    for (CFANode node : loopNodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        switch(node.getLeavingEdge(i).getEdgeType()) {
          case StatementEdge:
          AStatementEdge x = (AStatementEdge) node.getLeavingEdge(i);
          if (node.getLeavingEdge(i) instanceof CExpressionAssignmentStatement) {
            CExpressionAssignmentStatement c = (CExpressionAssignmentStatement) x.getStatement();
          if (c.getRightHandSide() instanceof CBinaryExpression) {
            CBinaryExpression pE = (CBinaryExpression) c.getRightHandSide();
                temp.add(getInputVariableFromBinaryExpression(pE));
          } else if (c.getRightHandSide() instanceof CIdExpression
              || c.getRightHandSide() instanceof CArraySubscriptExpression) {
            if (c.getRightHandSide() instanceof CIdExpression) {
              CIdExpression var = (CIdExpression) c.getRightHandSide();
              temp.add(var.getName());
            } else {
              CArraySubscriptExpression var = (CArraySubscriptExpression) c.getRightHandSide();
              temp.add(var.getArrayExpression().toString());
            }
            } else if (node.getLeavingEdge(i) instanceof CFunctionCallStatement) {
              CFunctionCallStatement cf = (CFunctionCallStatement) x.getStatement();
              for (CExpression ce : cf.getFunctionCallExpression().getParameterExpressions()) {
                temp.add(ce.toString());
              }
            }
          } else {
              throw new NotKnownLoopEdgeTypeException();
          }
          break;
          case AssumeEdge:
            AssumeEdge assume = (AssumeEdge) node.getLeavingEdge(i);
            CExpression c = (CExpression) assume.getExpression();
          if (c instanceof CBinaryExpression) {
            CBinaryExpression pE = (CBinaryExpression) c;
              temp.add(getInputVariableFromBinaryExpression(pE));
          } else if (c instanceof CIdExpression || c instanceof CArraySubscriptExpression) {
            if (c instanceof CIdExpression) {
              CIdExpression var = (CIdExpression) c;
              temp.add(var.getName());
            } else {
              CArraySubscriptExpression var = (CArraySubscriptExpression) c;
              temp.add(var.getArrayExpression().toString());
            }
          } else {
              throw new NotKnownLoopEdgeTypeException();
          }
          break;
          case FunctionCallEdge:
            FunctionCallEdge function = (FunctionCallEdge) node.getLeavingEdge(i);

            for (AExpression var : function.getArguments()) {
            temp.add(var.toString());
          }
          break;
          case ReturnStatementEdge:
            AReturnStatement returnStatement = (AReturnStatement) node.getLeavingEdge(i);
            if (returnStatement.getReturnValue().isPresent()) {
              temp.add(returnStatement.getReturnValue().toString());
          }
        break;
          default:
            break;
        }
      }
    }
    return temp;
    }

  /**
   * This method looks for all the possible path that the loop can go in one iteration
   *
   * @param loopNodes List of all loop-nodes to check how many paths are going from this node to the
   *     next node
   * @param endNode last node of this loop
   * @param conditionNodes Nodes that are part of the condition of this loop
   * @param failed the CFANode that shows when the loop reaches the failed state, since this node
   *     may be part of the loop but it's leaving edges don't count towards the number of paths
   * @param outputs List of the outputs of the loop since size of Array counts towards the number of
   *     all paths if an array is part of the loop
   * @return number of possible paths in one iteration
   */
  private int getAllPaths(
      List<CFANode> loopNodes,
      CFANode endNode,
      List<CFANode> conditionNodes,
      CFANode failed,
      List<LoopVariables> outputs) {
    // there is always one path in a loop that can be travelled
    int paths = 1;
    for (CFANode node : loopNodes) {
      if (!node.equals(endNode) && !conditionNodes.contains(node) && !node.equals(failed)) {
        // we subtract 1 from the amount of leaving edges to indicate that this is the one path we
        // already
        // account for, we only add numbers to the paths that if there are more possibilities like
        // in the case of an if-construct, etc. that adds multiple paths
        paths += (node.getNumLeavingEdges() - LAST_POSITION_OF_LIST);
      }
    }
    for (LoopVariables z : outputs) {
      if (z.getIsArray()) {
        // we add the number of array-cells to the number of paths in case there is an array
        // in the loop. this is an over-approximation that could be refined if you know the
        // amount of array values that will be used later on
        paths += z.getArrayLength();
      }
    }
    return paths;
  }

  /**
   * Checks if the nodes are part of the condition
   *
   * @param tempNode node that gets checked
   * @param nodes List of CFANodes that already got checked
   * @param loopNodes List of all CFANodes in the loop
   * @return boolean that tells you, if a node is part of the condition
   */
  private boolean checkIfNodesInCondition(
      CFANode tempNode, List<CFANode> nodes, List<CFANode> loopNodes) {

    if(tempNode.getNumLeavingEdges() > 1) {
      return (tempNode.getLeavingEdge(0) instanceof AssumeEdge
          && loopNodes.contains(tempNode.getLeavingEdge(VALID_STATE).getSuccessor())
          && !nodes.contains(tempNode))
      || (edgeFromNodeContainsString(tempNode, "CPAchecker_TMP")
          && loopNodes.contains(tempNode.getLeavingEdge(ERROR_STATE).getSuccessor())
          && !nodes.contains(tempNode));
    } else {
      return (tempNode.getLeavingEdge(0) instanceof AssumeEdge
          && loopNodes.contains(tempNode.getLeavingEdge(VALID_STATE).getSuccessor())
          && !nodes.contains(tempNode));
    }
  }

  /**
   * Checks if an edge from a node contains a certain string. Only the "true" edge gets checked in
   * assume edges
   *
   * @param node node, which edges will be checked
   * @param string string that needs to be checked if it is contained in the edge
   * @return true if the string is contained, false if it isn't
   */
  private boolean edgeFromNodeContainsString(CFANode node, String string) {
    boolean contain = false;
    if(node.getNumLeavingEdges() == 2) {
      for (int edges = 0; edges < 2; edges++) {
        AssumeEdge ass = (AssumeEdge) node.getLeavingEdge(edges);
        if (ass.getTruthAssumption()) {
          if (node.getLeavingEdge(edges).getCode().contains(string)) {
            contain = true;
          }
        }
      }
    } else {
      if (node.getLeavingEdge(ONLY_EDGE).getCode().contains(string)) {
        contain = true;
      } else {
        contain = false;
      }
    }
    return contain;
  }

  /**
   * This method looks for all of the nodes in the condition and even cuts out the nodes that belong
   * to an if-case
   *
   * @param cfa used to check which nodes are part of the for-condition since there are some that
   *     are before the "start node" used by the loop class
   * @param pLogger logs exceptions
   * @param start start node of the loop
   * @param type loop type, right now supports for and while loop
   * @param loopNodes used to check if the node in question is part of the loop
   * @param conditionEnd nodes that are after the condition, can be more than one in some cases
   * @param startFor start node of a for loop, can differ from the normal loop Start node
   * @return returns a list with all the nodes that are part of the condition
   */
  public List<CFANode> nodesInCondition(
      CFA cfa,
      LogManager pLogger,
      CFANode start,
      LoopType type,
      List<CFANode> loopNodes,
      List<CFANode> conditionEnd,
      CFANode startFor) {

    List<CFANode> nodes = new ArrayList<>();
    List<CFANode> tempNodes = new ArrayList<>();
    CFANode tempNode = start;
    boolean lookingForNodes = true;

    if (type.equals(LoopType.WHILE)) {

      while (lookingForNodes) {

        if (checkIfNodesInCondition(tempNode, nodes, loopNodes)) {
          nodes.add(tempNode);
        }

        for (int i = 1; i < tempNode.getNumLeavingEdges(); i++) {
          if (edgeFromNodeContainsString(tempNode, "CPAchecker_TMP")
              && edgeFromNodeContainsString(tempNode, "==")) {
            tempNodes.add(tempNode.getLeavingEdge(1).getSuccessor());
          } else if ((tempNode.getLeavingEdge(i).getSuccessor().getLeavingEdge(VALID_STATE)
                      instanceof AssumeEdge
                  || edgeFromNodeContainsString(tempNode, "CPAchecker_TMP"))
              && !loopNodes.contains(tempNode.getLeavingEdge(i).getSuccessor())) {

            tempNodes.add(tempNode.getLeavingEdge(i).getSuccessor());
          }
        }
        if (!conditionEnd.contains(tempNode) && loopNodes.contains(tempNode)) {
          tempNode = tempNode.getLeavingEdge(VALID_STATE).getSuccessor();
        } else if (!tempNodes.isEmpty()) {
          tempNode = tempNodes.get(tempNodes.size() - LAST_POSITION_OF_LIST);
          tempNodes.remove(tempNodes.size() - LAST_POSITION_OF_LIST);
        } else {
          lookingForNodes = false;
        }
      }
    } else if (type.equals(LoopType.FOR)) {
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNodeNumber() >= startFor.getNodeNumber()
            && node.getNodeNumber() <= start.getNodeNumber() + 1) {
          nodes.add(node);
        }
      }

      List<CFANode> forNode = new ArrayList<>();
      while (lookingForNodes) {
        for (CFANode x : nodes) {
          for (int i = 0; i < x.getNumLeavingEdges(); i++) {
            if (x.getLeavingEdge(i).getSuccessor().getLeavingEdge(VALID_STATE) instanceof AssumeEdge
                && !nodes.contains(x.getLeavingEdge(i).getSuccessor())) {
              forNode.add(x.getLeavingEdge(i).getSuccessor());
            }
          }
        }
        if (!forNode.isEmpty()) {
          nodes.addAll(forNode);
          forNode.clear();
        } else {
          lookingForNodes = false;
        }
      }
    }
    if (!nodes.isEmpty()) {
      int smallestIf = UtilIfAfterLoopCondition.getSmallestIf(nodes, pLogger);
      if (smallestIf != NO_IF_CASE) {
        List<CFANode> tempNodeList = new ArrayList<>();
        for (CFANode node : nodes) {
          if (node.getLeavingEdge(VALID_STATE).getFileLocation().getStartingLineInOrigin()
              < smallestIf) {
            tempNodeList.add(node);
          } else {
            conditionEnd.add(node);
          }
        }

        nodes = tempNodeList;
      }
    }

    List<CFANode> condWithoutBody = new ArrayList<>();

    for (CFANode cond : nodes) {
      boolean partOfBody = false;
      for (CFANode body : endOfCondition) {
        if (cond.equals(body)) {
          partOfBody = true;
        }
      }
      if (!partOfBody) {
        condWithoutBody.add(cond);
      }
    }

    nodes = condWithoutBody;

    if (nodes.isEmpty()) {
      setEndless(true);
    }

    return nodes;
  }

  private String whileCondition(CFANode node, String booleanOperator) {

    String cond = "";

    if (edgeFromNodeContainsString(node, "nondet")) {
      String tmpType = CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(VALID_STATE)).toString();
      cond = "__VERIFIER_nondet_" + tmpType + "() " + booleanOperator;
    } else {
      cond = node.getLeavingEdge(VALID_STATE).getCode() + " " + booleanOperator + " ";
    }

    return cond;
  }

  /**
   * This method takes all of the nodes in the condition of this loop and returns a readable string
   * that shows the condition of the loop
   *
   * @param conditionNodes all Nodes that are part of the condition
   * @param type type of the loop to determine the way the condition will be put together
   * @param conditionEnd nodes that are not in the condition to make sure that only nodes in the
   *     condition will be part of the condition string
   * @return string that represents the condition of the loop
   */

  /**
   * This method takes all of the nodes in the condition of this loop and returns a readable string
   * that shows the condition of the loop
   *
   * @param conditionNodes all Nodes that are part of the condition
   * @param type type of the loop to determine the way the condition will be put together
   * @param conditionEnd nodes that are not in the condition to make sure that only nodes in the
   *     condition will be part of the condition string
   * @param endless boolean that shows, if the loop is an endless loop or not (condition = 1/true)
   * @param onlyRandomC boolean that shows, if the condition is only a random condition
   * @return string that represents the condition of the loop
   */
  public String nodesToCondition(
      List<CFANode> conditionNodes,
      LoopType type,
      List<CFANode> conditionEnd,
      boolean endless,
      boolean onlyRandomC) {
    String cond = "";
    List<CFANode> temp = new ArrayList<>();
    if (!onlyRandomC) {
      if (type.equals(LoopType.WHILE)) {
        for (CFANode n : conditionNodes) {
          if (!edgeFromNodeContainsString(n, "CPAchecker_TMP")) {
            temp.add(n);
          }
          if (edgeFromNodeContainsString(n, "nondet")) {
            temp.add(n);
          }
        }
      } else if (type.equals(LoopType.FOR)) {
        temp = copyList(conditionNodes);
      }
    }
    CFANode node;

    if (type.equals(LoopType.WHILE)) {
      if (temp.isEmpty() || endless) {
        cond = "1";
      }

      while (!temp.isEmpty()) {
        node = temp.get(FIRST_POSITION_OF_LIST);
        temp.remove(FIRST_POSITION_OF_LIST);

        if (!temp.isEmpty()) {
          boolean notNodeToEndCondition = true;

          for (int i = 0; i < node.getNumLeavingEdges(); i++) {
            if (conditionEnd.contains(node.getLeavingEdge(i).getSuccessor())) {
              notNodeToEndCondition = false;
            }
          }

          if (notNodeToEndCondition) {
            cond = cond + whileCondition(node, "&&");
          } else {
            cond = cond + whileCondition(node, "||");
          }
        } else {
          if (edgeFromNodeContainsString(node, "nondet")) {
            String tmpType =
                CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(VALID_STATE)).toString();
            cond = cond + "__VERIFIER_nondet_" + tmpType + "()";
            setFailedState(
                node.getLeavingEdge(VALID_STATE).getSuccessor().getLeavingEdge(0).getSuccessor());
          } else {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode();
            setFailedState(node.getLeavingEdge(ERROR_STATE).getSuccessor());
          }
        }
      }
    } else if (type.equals(LoopType.FOR)) {

      CFANode start = temp.get(FIRST_POSITION_OF_LIST);
      temp.remove(FIRST_POSITION_OF_LIST);

      List<CFANode> forCondition = new ArrayList<>();
      for (Iterator<CFANode> tempIterator = temp.iterator(); tempIterator.hasNext(); ) {
        CFANode temps = tempIterator.next();

        ImmutableList<String> operator = ImmutableList.of("<", ">", "==", "!=");

        if (operator.contains(temps.getLeavingEdge(VALID_STATE).getCode())
            || temps.getLeavingEdge(VALID_STATE).getCode().isEmpty()) {
          forCondition.add(temps);
          tempIterator.remove();
        }
      }

      CFANode end = temp.get(FIRST_POSITION_OF_LIST);
      temp.remove(FIRST_POSITION_OF_LIST);

      setConditionInFor(forCondition);

      cond += start.getLeavingEdge(VALID_STATE).getDescription();

      while (!forCondition.isEmpty()) {
        node = forCondition.get(FIRST_POSITION_OF_LIST);
        forCondition.remove(FIRST_POSITION_OF_LIST);

        if (!forCondition.isEmpty()) {
          boolean notNodeToEndCondition = true;

          for (int i = 0; i < node.getNumLeavingEdges(); i++) {
            if (conditionEnd.contains(node.getLeavingEdge(i).getSuccessor())) {
              notNodeToEndCondition = false;
            }
          }

          if (notNodeToEndCondition) {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " && ";
          } else {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " || ";
          }
        } else {
          if (node.getLeavingEdge(VALID_STATE).getCode().contentEquals("")) {
            cond = cond + "1";
            setEndless(true);
          } else {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode();
            setFailedState(node.getLeavingEdge(ERROR_STATE).getSuccessor());
          }
        }
      }

      cond += ";";
      cond += end.getLeavingEdge(VALID_STATE).getCode();
    }
    return cond;
  }

  private static AExpression unwrap(AExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    while (expression instanceof CCastExpression) {
      expression = ((CCastExpression) expression).getOperand();
    }

    return expression;
  }

  /**
   * This method looks for hints if it makes any sense to accelerate the loop or if a
   * Bounded-Model-Checker should be able to handle it
   *
   * @param condNodes all Nodes of the condition to check if the
   * @param type loop type to specify the method used to check that
   * @param pathNumber number of possible path in the loop
   * @param outputNumber number of outputs in the loop
   * @param endless check if the loop already has the endless tag, the loop should be abstracted if
   *     it is already endless
   * @param forCondition inner part of the for condition
   * @return true if the loop should be accelerated or false if it doesn't make that much sense to
   *     accelerate it
   */
  private boolean canLoopBeAccelerated(
      List<CFANode> condNodes,
      LoopType type,
      int pathNumber,
      int outputNumber,
      boolean endless,
      List<CFANode> forCondition) {

    boolean tmpEndless = endless;
    boolean onlyRandomOperator = true;
    List<CFANode> conditionNodes = new ArrayList<>();

    for (CFANode n : condNodes) {
      if (n.getLeavingEdge(VALID_STATE) instanceof AssumeEdge) {
        AssumeEdge edge = (AssumeEdge) n.getLeavingEdge(VALID_STATE);
        if (edge.getExpression() instanceof CBinaryExpression) {
        CBinaryExpression bin = (CBinaryExpression) edge.getExpression();
          if (!bin.getOperand1().toString().contains("CPAchecker_TMP")) {
            onlyRandomOperator = false;
            conditionNodes.add(n);
          }
        }
      }
    }

    if (onlyRandomOperator && !condNodes.isEmpty()) {
      setOnlyRandomCondition(true);
      setEndless(false);
      tmpEndless = true;
    }

    boolean canAccelerate = false;

    List<Boolean> temp = new ArrayList<>();
    if (!tmpEndless) {
      if (type.equals(LoopType.WHILE)) {
        for (CFANode node : conditionNodes) {

          AssumeEdge assume = (AssumeEdge) node.getLeavingEdge(VALID_STATE);

          CBinaryExpression pE = (CBinaryExpression) assume.getExpression();

          BinaryOperator binaryOperator = pE.getOperator();
          CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
          CExpression rVarInBinaryExp = pE.getOperand2();

          if (binaryOperator.equals(BinaryOperator.EQUALS)) {
            temp.add(true);
          } else {
            if (lVarInBinaryExp instanceof CIdExpression
                || rVarInBinaryExp instanceof CIdExpression) {
              temp.add(true);
            } else if (lVarInBinaryExp instanceof CLiteralExpression
                || rVarInBinaryExp instanceof CLiteralExpression) {
              if (lVarInBinaryExp instanceof CLiteralExpression) {
                if (lVarInBinaryExp instanceof CIntegerLiteralExpression) {
                  if (((CIntegerLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > pathNumber
                      || ((CIntegerLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                  temp.add(true);
                  }
                } else if (lVarInBinaryExp instanceof CCharLiteralExpression) {
                  if (((CCharLiteralExpression) lVarInBinaryExp).getCharacter() > pathNumber
                      || ((CCharLiteralExpression) lVarInBinaryExp).getCharacter() > outputNumber) {
                    temp.add(true);
                  }
                } else if (lVarInBinaryExp instanceof CFloatLiteralExpression) {
                  if (((CFloatLiteralExpression) lVarInBinaryExp).getValue().intValue() > pathNumber
                      || ((CFloatLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                }
              }
              if (rVarInBinaryExp instanceof CLiteralExpression) {
                if (rVarInBinaryExp instanceof CIntegerLiteralExpression) {
                  if (((CIntegerLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > pathNumber
                      || ((CIntegerLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                  temp.add(true);
                  }
                } else if (rVarInBinaryExp instanceof CCharLiteralExpression) {
                  if (((CCharLiteralExpression) rVarInBinaryExp).getCharacter() > pathNumber
                      || ((CCharLiteralExpression) rVarInBinaryExp).getCharacter() > outputNumber) {
                    temp.add(true);
                  }
                } else if (rVarInBinaryExp instanceof CFloatLiteralExpression) {
                  if (((CFloatLiteralExpression) rVarInBinaryExp).getValue().intValue() > pathNumber
                      || ((CFloatLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                }
              }
            }
          }
        }
      } else if (type.equals(LoopType.FOR)) {
        for (CFANode node : forCondition) {
          AssumeEdge assume = (AssumeEdge) node.getLeavingEdge(VALID_STATE);

          CBinaryExpression pE = (CBinaryExpression) assume.getExpression();

          BinaryOperator binaryOperator = pE.getOperator();
          CExpression lVarInBinaryExp = (CExpression) unwrap(pE.getOperand1());
          CExpression rVarInBinaryExp = pE.getOperand2();

          if (binaryOperator.equals(BinaryOperator.EQUALS)) {
            temp.add(true);
          } else {
            if (lVarInBinaryExp instanceof CIdExpression
                || rVarInBinaryExp instanceof CIdExpression) {
              temp.add(true);
              if (lVarInBinaryExp instanceof CLiteralExpression) {
                if (lVarInBinaryExp instanceof CIntegerLiteralExpression) {
                  if (((CIntegerLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > pathNumber
                      || ((CIntegerLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                } else if (lVarInBinaryExp instanceof CCharLiteralExpression) {
                  if (((CCharLiteralExpression) lVarInBinaryExp).getCharacter() > pathNumber
                      || ((CCharLiteralExpression) lVarInBinaryExp).getCharacter() > outputNumber) {
                    temp.add(true);
                  }
                } else if (lVarInBinaryExp instanceof CFloatLiteralExpression) {
                  if (((CFloatLiteralExpression) lVarInBinaryExp).getValue().intValue() > pathNumber
                      || ((CFloatLiteralExpression) lVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                  }
                }
              if (rVarInBinaryExp instanceof CLiteralExpression) {
                if (rVarInBinaryExp instanceof CIntegerLiteralExpression) {
                  if (((CIntegerLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > pathNumber
                      || ((CIntegerLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                } else if (rVarInBinaryExp instanceof CCharLiteralExpression) {
                  if (((CCharLiteralExpression) rVarInBinaryExp).getCharacter() > pathNumber
                      || ((CCharLiteralExpression) rVarInBinaryExp).getCharacter() > outputNumber) {
                    temp.add(true);
                  }
                } else if (rVarInBinaryExp instanceof CFloatLiteralExpression) {
                  if (((CFloatLiteralExpression) rVarInBinaryExp).getValue().intValue() > pathNumber
                      || ((CFloatLiteralExpression) rVarInBinaryExp).getValue().intValue()
                          > outputNumber) {
                    temp.add(true);
                  }
                  }
                }
            }
          }
        }
      }
      for (Boolean b : temp) {
        if (b) {
          canAccelerate = true;
        }
      }
    } else {
      canAccelerate = true;
    }
    return canAccelerate;
  }

  private List<CFANode> copyList(List<CFANode> pNodesInCondition) {
    List<CFANode> temp = new ArrayList<>();
    Iterables.addAll(temp, pNodesInCondition);
    return temp;
  }

  /**
   * compareTo method, that compares startinglineInOrigin to be able to sort it specifically with
   * that variable in mind
   */
  @Override
  public int compareTo(LoopData otherLoop) {
    return (this.getLoopStart().getLeavingEdge(0).getFileLocation().getStartingLineInOrigin()
            < otherLoop.getLoopStart().getLeavingEdge(0).getFileLocation().getStartingLineInOrigin()
        ? -1
        : (this.getLoopStart().getLeavingEdge(0).getFileLocation().getStartingLineInOrigin()
                == otherLoop
                    .getLoopStart()
                    .getLeavingEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin()
            ? 0
            : 1));
  }

  /** equals method, that compares loopstart, failedState, amountOfPaths and numberOutputs */
  @Override
  public boolean equals(Object otherLoop) {
    if (!(otherLoop instanceof LoopData)) {
      return false;
    }
    if (otherLoop == this) {
      return true;
    }
    LoopData ld = (LoopData) otherLoop;
    if (this.getLoopStart().getNodeNumber() == ld.getLoopStart().getNodeNumber()
        && this.getFaileState().getNodeNumber() == ld.getFaileState().getNodeNumber()
        && this.getAmountOfPaths() == ld.getAmountOfPaths()
        && this.getNumberOutputs() == ld.getNumberOutputs()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * hashcode method that takes loopstart, failedState, amountOfPaths and numberAllOutputs in
   * account
   */
  @Override
  public int hashCode() {
    final int prime = 29;
    return (loopStart.getNodeNumber()
            + failedState.getNodeNumber()
            + amountOfPaths
            + numberAllOutputs)
        * prime;
  }

  private void setLoopStart(CFANode pLoopStart) {
    loopStart = pLoopStart;
  }

  private void addToLoopEnd(CFANode pLoopEnd) {
    endOfCondition.add(pLoopEnd);
  }

  public CFANode getLoopStart() {
    return loopStart;
  }

  public CFANode getLoopEnd() {
    return loopEnd;
  }

  public List<CFANode> getNodesInLoop() {
    return nodesInLoop;
  }

  public String getCondition() {
    return condition;
  }

  public List<LoopVariables> getOutputs() {
    return output;
  }

  public LoopType getLoopType() {
    return loopType;
  }

  public boolean getLoopInLoop() {
    return loopInLoop;
  }

  public int getAmountOfPaths() {
    return amountOfPaths;
  }

  public List<LoopVariables> getInputsOutputs() {
    return inputOutput;
  }

  public boolean getCanBeAccelerated() {
    return canBeAccelerated;
  }

  public CFANode getFaileState() {
    return failedState;
  }

  public List<CFANode> getNodesInCondition() {
    return nodesInCondition;
  }

  public boolean getIsOuterLoop() {
    return outerLoop;
  }

  public Loop getInnerLoop() {
    return innerLoop;
  }

  public int getNumberOutputs() {
    return numberAllOutputs;
  }

  private void setForStart(CFANode fs) {
    forStart = fs;
  }

  private void setFailedState(CFANode failed) {
    failedState = failed;
  }

  private void setEndless(boolean endless) {
    flagEndless = endless;
  }

  public boolean getEndless() {
    return flagEndless;
  }

  private void setConditionInFor(List<CFANode> tempForCondition) {
    conditionInFor.addAll(tempForCondition);
  }

  private void setOnlyRandomCondition(boolean value) {
    onlyRandomCondition = value;
  }

  public TimeSpan getAnalyzeTime() {
    return analyzeTime;
  }

  public boolean getOnlyRandomCondition() {
    return onlyRandomCondition;
  }

  @Override
  public String toString() {
    return "Start "
        + loopStart.toString()
        + "\n Ende "
        + loopEnd.toString()
        + "\n Nodes in Loop "
        + nodesInLoop.toString()
        + "\n Output"
        + output.toString()
        + "\n Condition "
        + condition
        + "\n Loop in Loop "
        + loopInLoop
        + "\n Looptype: "
        + loopType
        + "\n Amount of Paths: "
        + amountOfPaths
        + "\n IO-Variablen: "
        + inputOutput
        + "\n Can be accelerated: "
        + canBeAccelerated
        + "\n Failed State: "
        + failedState;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new LoopStatistics(this));
  }


}
