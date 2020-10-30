// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopAcc;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** This class collects and saves all of the data in one loop */
public class LoopData implements Comparable<LoopData> {

  private CFANode loopStart;
  private CFANode loopEnd;
  private CFANode failedState;
  private CFANode forStart = null;

  private List<CFANode> conditionInFor;
  private List<CFANode> nodesInLoop;
  private List<CFANode> nodesInCondition;
  private List<String> output;
  private List<String> inputOutput;
  private List<CFANode> endOfCondition;

  private String condition;
  private String loopType = "";

  private int amountOfPaths;
  private int numberAllOutputs;

  private boolean flagEndless = false;
  private boolean loopInLoop;
  private boolean outerLoop;
  private Loop innerLoop;
  private boolean canBeAccelerated;

  private Timer timeToAnalyze;
  private TimeSpan analyzeTime;

  private static final int OUTPUT_VARIABLE_ARRAY_POSITION = 2;
  private static final String OUTPUT_NAME_SYMBOL_CUT = ":";
  private static final int FLAG_FOR_LAST_STRING = 1;
  private static final int ONLY_ENTERING_EDGE = 0;
  private static final int POSITION_OF_VARIABLE_IN_ARRAY_ZERO = 0;
  private static final int POSITION_OF_VARIABLE_IN_ARRAY_ONE = 1;
  private static final int POSITION_OF_VARIABLE_IN_ARRAY_TWO = 2;
  private static final int VALID_STATE = 0;
  private static final int ERROR_STATE = 1;
  private static final int LAST_POSITION_OF_LIST = 1;
  private static final int FIRST_POSITION_OF_LIST = 0;
  private static final int EMPTY_LIST = 0;
  private static final int NO_IF_CASE = -1;

  public LoopData(
      CFANode nameStart,
      CFANode endCondition,
      CFA cfa,
      List<CFANode> loopNodes,
      Loop loop,
      LogManager pLogger) {
    timeToAnalyze = new Timer();
    timeToAnalyze.start();
    loopStart = nameStart;
    this.endOfCondition = new ArrayList<>();
    conditionInFor = new ArrayList<>();
    output = new ArrayList<>();

    this.endOfCondition.add(endCondition);
    loopInLoop = isInnerLoop(loop, cfa);
    outerLoop = isOuterLoop(loop, cfa);
    loopType = findLoopType(loopStart);
    nodesInLoop = loopNodes;
    loopEnd = nodesInLoop.get(nodesInLoop.size() - LAST_POSITION_OF_LIST);
    nodesInCondition =
        nodesInCondition(cfa, pLogger, loopStart, loopType, nodesInLoop, endOfCondition, forStart);
    output = getAllOutputs(cfa, nodesInLoop);
    condition = nodesToCondition(nodesInCondition, loopType, endOfCondition, flagEndless);
    inputOutput = getAllIO(output, nodesInLoop);
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
    timeToAnalyze.stop();
    analyzeTime = timeToAnalyze.getLengthOfLastInterval();
  }

  /**
   * looks for the looptype of a loop
   *
   * @param firstNode while loops typically have the "while" in the entering edge of the first cfa
   *     node of the loop
   * @return returns the type of the loop, possible solutions are "while", "for" at the moment
   */
  private String findLoopType(CFANode firstNode) {
    String tempLoopType = "";

    if (firstNode.getNumEnteringEdges() > 0
        && firstNode.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription().equals("while")) {
      tempLoopType = firstNode.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription();
    } else {
      CFANode temp = firstNode.getEnteringEdge(ONLY_ENTERING_EDGE).getPredecessor();
      boolean flag = true;

      while (flag) {
        if (temp.getNumEnteringEdges() > 0
            && temp.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription().contains("for")) {
          tempLoopType = temp.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription();
          setForStart(temp);
          flag = false;
        }
        if (temp.getNumEnteringEdges() > 0) {
          temp = temp.getEnteringEdge(ONLY_ENTERING_EDGE).getPredecessor();
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
   */
  private List<String> getAllOutputs(CFA cfa, List<CFANode> loopNodes) {
    List<String> tempOutput = new ArrayList<>();

    for (CFANode node : loopNodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if ((node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.StatementEdge)
                || node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.DeclarationEdge))
            && (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null
                || CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null)) {
          boolean flag = true;

          boolean flagCPAchecker = true;
          if (CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null
              && CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i))
                  .contains("__CPAchecker_")) {
            flagCPAchecker = false;
          } else if (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null
              && CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i))
                  .toString()
                  .contains("__CPAchecker_")) {
            flagCPAchecker = false;
          }

          if (flag && flagCPAchecker) {

            String temp = "";
            if (CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null) {
              if (CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i)).toString().contains("[")) {
                String tmpType =
                    Iterables.get(
                        Splitter.on(')')
                            .split(CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i)).toString()),
                        0);
                tmpType = Iterables.get(Splitter.on('(').split(tmpType), 1);
                temp =
                    Iterables.get(
                            Splitter.onPattern(OUTPUT_NAME_SYMBOL_CUT)
                                .split(CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i))),
                            OUTPUT_VARIABLE_ARRAY_POSITION)
                        + "&"
                        + "Array:"
                        + tmpType
                        + ":"
                        + Iterables.get(
                            Splitter.on(']')
                                .split(
                                    Iterables.get(
                                        Splitter.on('[')
                                            .split(
                                                CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i))
                                                    .toString()),
                                        1)),
                            0);

              } else {
                if (CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)).contains(":")) {
                  temp =
                      Iterables.get(
                              Splitter.onPattern(OUTPUT_NAME_SYMBOL_CUT)
                                  .split(CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i))),
                              OUTPUT_VARIABLE_ARRAY_POSITION)
                          + "&"
                          + CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i));
                } else {
                  temp = CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i));
                }
              }
            } else if (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null) {
              if (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i))
                  .getClass()
                  .getName()
                  .contains("Array")) {
                temp =
                    Iterables.get(
                            Splitter.on('[')
                                .split(
                                    CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i))
                                        .toString()),
                            0)
                        + "&"
                        + "Array:"
                        + CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)).getExpressionType();
              }
            }
            tempOutput.add(temp);
          }
        }
      }
    }

    List<String> overwrite = new ArrayList<>();
    for (CFANode n : cfa.getAllNodes()) {
      for (int e = 0; e < n.getNumLeavingEdges(); e++) {
        if (n.getLeavingEdge(e).getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
          for (String s : tempOutput) {
            if (CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)) != null) {
              if (CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)).contains(":")
                  && Iterables.get(Splitter.on('&').split(s), 0)
                      .equals(
                          Iterables.get(
                              Splitter.on(':')
                                  .split(CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e))),
                              2))) {

                String tempNew = s;

                if (tempNew.contains("Array") && tempNew.split(":").length != 3) {
                  String tmpNewStart = Iterables.get(Splitter.on('&').split(tempNew), 0);
                  String tmpNewEnd = Iterables.get(Splitter.on('&').split(tempNew), 1);
                  String arraySize =
                      Iterables.get(
                          Splitter.on(']')
                              .split(
                                  Iterables.get(
                                      Splitter.on('[')
                                          .split(
                                              CFAEdgeUtils.getLeftHandType(n.getLeavingEdge(e))
                                                  .toString()),
                                      1)),
                          0);

                  tmpNewEnd = tmpNewEnd + ":" + arraySize;

                  tempNew = tmpNewStart + "&" + tmpNewEnd;
                }

                tempNew =
                    tempNew
                        + "&"
                        + ((ADeclarationEdge) n.getLeavingEdge(e))
                            .getDeclaration()
                            .getFileLocation()
                            .getStartingLineInOrigin();

                overwrite.add(tempNew);

              } else if (!CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)).contains(":")
                  && Iterables.get(Splitter.on('&').split(s), 0)
                      .equals(CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)))) {
                String tempNew = s;

                if (tempNew.contains("Array") && tempNew.split(":").length != 3) {
                  String tmpNewStart = Iterables.get(Splitter.on('&').split(tempNew), 0);
                  String tmpNewEnd = Iterables.get(Splitter.on('&').split(tempNew), 1);

                  String arraySize =
                      Iterables.get(
                          Splitter.on(']')
                              .split(
                                  Iterables.get(
                                      Splitter.on('[')
                                          .split(
                                              CFAEdgeUtils.getLeftHandType(n.getLeavingEdge(e))
                                                  .toString()),
                                      1)),
                          0);

                  tmpNewEnd = tmpNewEnd + ":" + arraySize;

                  tempNew = tmpNewStart + "&" + tmpNewEnd;
                }

                tempNew =
                    tempNew
                        + "&"
                        + ((ADeclarationEdge) n.getLeavingEdge(e))
                            .getDeclaration()
                            .getFileLocation()
                            .getStartingLineInOrigin();

                overwrite.add(tempNew);
              }
            }
          }
        }
      }
    }
    tempOutput = overwrite;
    List<String> removeDuplicates = new ArrayList<>();
    for (String duplicate : tempOutput) {
      if (!removeDuplicates.contains(duplicate)) {
        removeDuplicates.add(duplicate);
      }
    }
    tempOutput = removeDuplicates;
    return tempOutput;
  }

  private int getAllNumberOutputs(List<String> pOutput) {
    int tmpInt = 0;
    for (String tmp : pOutput) {
      if (tmp.contains("Array")) {
        tmpInt =
            tmpInt
                + Integer.parseInt(
                    Iterables.get(
                        Splitter.on(':').split(Iterables.get(Splitter.on('&').split(tmp), 1)), 2));

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
   */
  private List<String> getAllIO(List<String> tmpOutput, List<CFANode> loopNodes) {
    List<String> inputs = getAllInputs(loopNodes);
    List<String> outputs = tmpOutput;
    List<String> temp = new ArrayList<>();

    for (String o : outputs) {
      for (String i : inputs) {
        if (Iterables.get(Splitter.on('&').split(o), 0).contentEquals(i)) {
          boolean flagNO = true;
          for (String v : temp) {
            if (Iterables.get(Splitter.on('&').split(v), 0)
                .equals(Iterables.get(Splitter.on('&').split(o), 0))) {
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
   * This method looks for all of the input-variables in the loop
   *
   * @param loopNodes List of all the nodes in the loop to check if either of them has a edge that
   *     uses a variable that qualifies as an input
   * @return returns a list with the names of all the input variables
   */
  private List<String> getAllInputs(List<CFANode> loopNodes) {
    List<String> temp = new ArrayList<>();

    for (CFANode node : loopNodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if ((node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.StatementEdge)
                || node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.DeclarationEdge))
            && CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)) != null) {
          if (CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i))
              .toString()
              .contains("operand")) {
            getInputFromRightHandSide(
                temp, CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)).toString());
          } else {
            temp.add(CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)).toString());
          }
        } else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
          String edgeCode = node.getLeavingEdge(VALID_STATE).getCode();
          temp.add(
              Iterables.get(Splitter.on(' ').split(edgeCode), POSITION_OF_VARIABLE_IN_ARRAY_ZERO));
          temp.add(
              Iterables.get(Splitter.on(' ').split(edgeCode), POSITION_OF_VARIABLE_IN_ARRAY_TWO));
        } else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
          if (!node.getLeavingEdge(i).getCode().contains("()")) {
            temp.add(
                Iterables.get(
                    Splitter.on(')')
                        .split(
                            Iterables.get(
                                Splitter.on('(').split(node.getLeavingEdge(i).getCode()),
                                POSITION_OF_VARIABLE_IN_ARRAY_ONE)),
                    POSITION_OF_VARIABLE_IN_ARRAY_ZERO));
          }
        } else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.ReturnStatementEdge)) {
          temp.add(
              Iterables.get(
                  Splitter.on(' ').split(node.getLeavingEdge(i).getCode()),
                  POSITION_OF_VARIABLE_IN_ARRAY_ONE));
        }
      }
    }
    return temp;
  }

  /**
   * extracts the crucial information from the Variable-Right-Hand-Side String and adds them to a
   * InputVariable-List
   *
   * @param temp List where the information will be saved after extracting it
   * @param stringSplit String that has to be worked on by this method
   */
  private void getInputFromRightHandSide(List<String> temp, String stringSplit) {
    List<String> tempStorage = Splitter.on(',').splitToList(stringSplit);
    List<String> tempS = new ArrayList<>();

    for (int i = 0; i < tempStorage.size(); i++) {
      if (tempStorage.get(i).contains("operand")) {
        tempS.add(
            Iterables.get(
                Splitter.on(']')
                    .split(
                        Iterables.get(
                            Splitter.on('[').split(tempStorage.get(i)),
                            POSITION_OF_VARIABLE_IN_ARRAY_ONE)),
                POSITION_OF_VARIABLE_IN_ARRAY_ZERO));
      }
    }

    temp.addAll(tempS);
  }

  /**
   * This method looks for all the possible path that the loop can go in one iteration
   *
   * @return number of possible paths in one iteration
   */

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
      List<String> outputs) {
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
    for (String z : outputs) {
      if (z.contains("Array")) {
        // we add the number of array-cells to the number of paths in case there is an array
        // in the loop. this is an over-approximation that could be refined if you know the
        // amount of array values that will be used later on
        paths +=
            Integer.parseInt(
                Iterables.get(
                    Splitter.on(':').split(Iterables.get(Splitter.on('&').split(z), 1)), 2));
      }
    }
    return paths;
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
      String type,
      List<CFANode> loopNodes,
      List<CFANode> conditionEnd,
      CFANode startFor) {

    List<CFANode> nodes = new ArrayList<>();
    List<CFANode> tempNodes = new ArrayList<>();
    CFANode tempNode = start;
    boolean flag = true;

    if (type.contentEquals("while")) {

      while (flag) {

        if (tempNode.getLeavingEdge(VALID_STATE).getEdgeType().equals(CFAEdgeType.AssumeEdge)
            && loopNodes.contains(tempNode.getLeavingEdge(VALID_STATE).getSuccessor())
            && !nodes.contains(tempNode)) {
          nodes.add(tempNode);
        }
        for (int i = 1; i < tempNode.getNumLeavingEdges(); i++) {
          if (tempNode
                  .getLeavingEdge(i)
                  .getSuccessor()
                  .getLeavingEdge(VALID_STATE)
                  .getEdgeType()
                  .equals(CFAEdgeType.AssumeEdge)
              && loopNodes.contains(tempNode.getLeavingEdge(i).getSuccessor())) {

            tempNodes.add(tempNode.getLeavingEdge(i).getSuccessor());
          }
        }
        if (!conditionEnd.contains(tempNode) && loopNodes.contains(tempNode)) {
          tempNode = tempNode.getLeavingEdge(VALID_STATE).getSuccessor();
        } else if (!tempNodes.isEmpty()) {
          tempNode = tempNodes.get(tempNodes.size() - LAST_POSITION_OF_LIST);
          tempNodes.remove(tempNodes.size() - LAST_POSITION_OF_LIST);
        } else {
          flag = false;
        }
      }
    } else if (type.contentEquals("for")) {
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNodeNumber() >= startFor.getNodeNumber()
            && node.getNodeNumber() <= start.getNodeNumber() + 1) {
          nodes.add(node);
        }
      }

      List<CFANode> forNode = new ArrayList<>();
      while (flag) {
        for (CFANode x : nodes) {
          for (int i = 0; i < x.getNumLeavingEdges(); i++) {
            if (x.getLeavingEdge(i)
                    .getSuccessor()
                    .getLeavingEdge(VALID_STATE)
                    .getEdgeType()
                    .equals(CFAEdgeType.AssumeEdge)
                && !nodes.contains(x.getLeavingEdge(i).getSuccessor())) {
              forNode.add(x.getLeavingEdge(i).getSuccessor());
            }
          }
        }
        if (!forNode.isEmpty()) {
          nodes.addAll(forNode);
          forNode.clear();
        } else {
          flag = false;
        }
      }
    }
    if (!nodes.isEmpty()) {
      if (LoopGetIfAfterLoopCondition.getSmallestIf(nodes, pLogger) != NO_IF_CASE) {
        /**
         * List<CFANode> tempN = copyList(nodes);
         *
         * <p>for (Iterator<CFANode> tempIterator = tempN.iterator(); tempIterator.hasNext(); ) {
         * CFANode temps = tempIterator.next(); if
         * (temps.getLeavingEdge(VALID_STATE).getFileLocation().getStartingLineInOrigin() >=
         * LoopGetIfAfterLoopCondition.getSmallestIf(nodes, pLogger)) { conditionEnd.add(temps);
         * tempIterator.remove(); } }
         */
        List<CFANode> tempNodeList = new ArrayList<>();
        for (CFANode node : nodes) {
          if (node.getLeavingEdge(VALID_STATE).getFileLocation().getStartingLineInOrigin()
              < LoopGetIfAfterLoopCondition.getSmallestIf(nodes, pLogger)) {
            tempNodeList.add(node);
          } else {
            conditionEnd.add(node);
          }
        }

        nodes = tempNodeList;
      }
    }
    if (nodes.isEmpty()) {
      setEndless(true);
    }
    return nodes;
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
  public String nodesToCondition(
      List<CFANode> conditionNodes,
      String type,
      List<CFANode> conditionEnd,
      boolean endless) {

    // TODO mit Martin besprechen ob n Iterator nicht doch besser wäre

    String cond = "";
    List<CFANode> temp = copyList(conditionNodes);
    CFANode node;

    if (type.contentEquals("while")) {
      if (temp.isEmpty() || endless) {
        cond = "1";
      }

      while (!temp.isEmpty()) {
        node = temp.get(FIRST_POSITION_OF_LIST);
        temp.remove(FIRST_POSITION_OF_LIST);

        if (temp.size() > EMPTY_LIST) {
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
          cond = cond + node.getLeavingEdge(VALID_STATE).getCode();
          setFailedState(node.getLeavingEdge(ERROR_STATE).getSuccessor());
        }
      }
    } else if (type.contentEquals("for")) {

      CFANode start = temp.get(FIRST_POSITION_OF_LIST);
      temp.remove(FIRST_POSITION_OF_LIST);

      List<CFANode> forCondition = new ArrayList<>();
      for (Iterator<CFANode> tempIterator = temp.iterator(); tempIterator.hasNext(); ) {
        CFANode temps = tempIterator.next();
        if (temps.getLeavingEdge(VALID_STATE).getCode().contains("<")
            || temps.getLeavingEdge(VALID_STATE).getCode().contains(">")
            || temps.getLeavingEdge(VALID_STATE).getCode().contains("==")
            || temps.getLeavingEdge(VALID_STATE).getCode().contains("!=")
            || temps.getLeavingEdge(VALID_STATE).getCode().contentEquals("")) {
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

        if (forCondition.size() > EMPTY_LIST) {
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

  /**
   * This method looks for hints if it makes any sense to accelerate the loop or if a
   * Bounded-Model-Checker should be able to handle it
   *
   * @param conditionNodes all Nodes of the condition to check if the
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
      List<CFANode> conditionNodes,
      String type,
      int pathNumber,
      int outputNumber,
      boolean endless,
      List<CFANode> forCondition) {

    boolean canAccelerate = false;

    List<Boolean> temp = new ArrayList<>();
    if (!endless) {
    if (type.contentEquals("while")) {
      List<String> rightSideVariable = new ArrayList<>();
      for (CFANode node : conditionNodes) {
          rightSideVariable.add(
              Iterables.get(
                  Splitter.on(']')
                      .split(
                          Iterables.get(
                              Splitter.on("operand2=\\[")
                                  .split(node.getLeavingEdge(VALID_STATE).getRawAST().toString()),
                              POSITION_OF_VARIABLE_IN_ARRAY_ONE)),
                  POSITION_OF_VARIABLE_IN_ARRAY_ZERO));
      }

      for (String variable : rightSideVariable) {
        try {
          double d = Double.parseDouble(variable);
          if (d > pathNumber || d > outputNumber) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe) {
          temp.add(true);
        }
        try {
          int d = Integer.parseInt(variable);
          if (d > pathNumber || d > outputNumber) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe1) {
          temp.add(true);
        }
        try {
          long d = Long.parseLong(variable);
          if (d > pathNumber || d > outputNumber) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe2) {
          temp.add(true);
        }
        try {
          float d = Float.parseFloat(variable);
          if (d > pathNumber || d > outputNumber) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe3) {
          temp.add(true);
        }
        /*
         * try { BigInteger d = new BigInteger(variable); if (d.intValueExact() > pathNumber ||
         * d.intValueExact() > outputNumber) { temp.add(true); } } catch (NumberFormatException |
         * NullPointerException nfe4) { temp.add(true); }
         */
      }

    } else if (type.contentEquals("for")) {
        List<String> rightSideVariable = new ArrayList<>();
        for (CFANode node : forCondition) {
          rightSideVariable.add(
              Iterables.get(
                  Splitter.on(']')
                      .split(
                          Iterables.get(
                              Splitter.on("operand2=\\[")
                                  .split(node.getLeavingEdge(VALID_STATE).getRawAST().toString()),
                              POSITION_OF_VARIABLE_IN_ARRAY_ONE)),
                  POSITION_OF_VARIABLE_IN_ARRAY_ZERO));
        }

        for (String variable : rightSideVariable) {
          try {
            double d = Double.parseDouble(variable);
            if (d > pathNumber || d > outputNumber) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe) {
            temp.add(true);
          }
          try {
            int d = Integer.parseInt(variable);
            if (d > pathNumber || d > outputNumber) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe1) {
            temp.add(true);
          }
          try {
            long d = Long.parseLong(variable);
            if (d > pathNumber || d > outputNumber) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe2) {
            temp.add(true);
          }
          try {
            float d = Float.parseFloat(variable);
            if (d > pathNumber || d > outputNumber) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe3) {
            temp.add(true);
          }
          /*
           * try { BigInteger d = new BigInteger(variable); if (d.intValueExact() > pathNumber ||
           * d.intValueExact() > outputNumber) { temp.add(true); } } catch (NumberFormatException |
           * NullPointerException nfe4) { temp.add(true); }
           */
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

  @Override
  public int compareTo(LoopData otherLoop) {
    return (this.getLoopStart().getNodeNumber() < otherLoop.getLoopStart().getNodeNumber()
        ? -1
        : (this.getLoopStart().getNodeNumber() == otherLoop.getLoopStart().getNodeNumber()
            ? 0
            : 1));
  }

  @Override
  public boolean equals(Object otherLoop) {
    if (this.getLoopStart().getNodeNumber()
        != ((LoopData) otherLoop).getLoopStart().getNodeNumber()) {
      return false;
    } else {
      return true;
    }
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

  public List<String> getOutputs() {
    return output;
  }

  public String getLoopType() {
    return loopType;
  }

  public boolean getLoopInLoop() {
    return loopInLoop;
  }

  public int getAmountOfPaths() {
    return amountOfPaths;
  }

  public List<String> getInputsOutputs() {
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

  private void setConditionInFor(List<CFANode> tempForCondition) {
    conditionInFor = tempForCondition;
  }

  public long getAnalyzeTime() {
    return analyzeTime.asMillis();
  }

  public String outputToString() {
    StringBuilder temp = new StringBuilder();
    Object[] tempArray = output.toArray();
    for (int i = 0; i < tempArray.length; i++) {
      if (i < tempArray.length - FLAG_FOR_LAST_STRING) {
        temp.append((String) tempArray[i]);
        temp.append(",");
      } else {
        temp.append((String) tempArray[i]);
      }
    }

    return temp.toString();
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
}
