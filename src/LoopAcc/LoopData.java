/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package LoopAcc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAEdgeUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This class collects and saves all of the data in one loop
 */
public class LoopData implements Comparable<LoopData> {

  private CFANode loopStart;
  private CFANode loopEnd;
  private CFANode failedState;
  private CFANode forStart = null;

  private ArrayList<CFANode> conditionInFor;
  private ArrayList<CFANode> nodesInLoop;
  private ArrayList<CFANode> nodesInCondition;
  private ArrayList<String> output;
  private ArrayList<String> inputOutput;
  private ArrayList<CFANode> endOfCondition;

  private String condition;
  private String loopType = "";

  private int amountOfPaths;
  private int numberAllOutputs;

  private boolean flagEndless = false;
  private boolean loopInLoop;
  private boolean outerLoop;
  private Loop innerLoop;
  private boolean canBeAccelerated;

  private final int OUTPUT_VARIABLE_ARRAY_POSITION = 2;
  private final String OUTPUT_NAME_SYMBOL_CUT = ":";
  private final String EMPTY_STRING = "";
  private final int FLAG_FOR_LAST_STRING = 1;
  private final int ONLY_ENTERING_EDGE = 0;
  private final int POSITION_OF_VARIABLE_IN_ARRAY_ZERO = 0;
  private final int POSITION_OF_VARIABLE_IN_ARRAY_ONE = 1;
  private final int POSITION_OF_VARIABLE_IN_ARRAY_TWO = 2;
  private final int VALID_STATE = 0;
  private final int ERROR_STATE = 1;
  private final int LAST_POSITION_OF_LIST = 1;
  private final int FIRST_POSITION_OF_LIST = 0;
  private final int EMPTY_LIST = 0;
  private final int NO_IF_CASE = -1;

  public LoopData(
      CFANode nameStart,
      CFANode endCondition,
      CFA cfa,
      ArrayList<CFANode> loopNodes,
      Loop loop,
      LogManager pLogger,
      boolean loopTrueFalse) {
    this.loopStart = nameStart;
    this.endOfCondition = new ArrayList<>();
    conditionInFor = new ArrayList<>();
    output = new ArrayList<>();

    this.endOfCondition.add(endCondition);
    loopInLoop = isInnerLoop(loop, cfa);
    outerLoop = isOuterLoop(loop, cfa);
    loopType = findLoopType();
    nodesInLoop = loopNodes;
    loopEnd = nodesInLoop.get(nodesInLoop.size() - LAST_POSITION_OF_LIST);
    nodesInCondition = nodesInCondition(cfa, pLogger, loopTrueFalse);
    output = getAllOutputs(cfa);
    condition = nodesToCondition();
    getAllPaths();
    inputOutput = getAllIO();
    numberAllOutputs = getAllNumberOutputs(output);
    canBeAccelerated = canLoopBeAccelerated();
    amountOfPaths = getAllPaths();
  }

  public int getNumberOutputs() {
    return numberAllOutputs;
  }
  /**
   * looks for the looptype of a loop
   *
   * @return returns the type of the loop, possible solutions are "while", "for" at the moment
   */
  private String findLoopType() {
    String tempLoopType = "";

    if (loopStart.getNumEnteringEdges() > 0
        && loopStart.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription().equals("while")) {
      tempLoopType = loopStart.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription();
    } else {
      CFANode temp = loopStart.getEnteringEdge(ONLY_ENTERING_EDGE).getPredecessor();
      boolean flag = true;

      while (flag) {
        if (temp.getNumEnteringEdges() > 0
            && temp.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription().contains("for")) {
          tempLoopType = temp.getEnteringEdge(ONLY_ENTERING_EDGE).getDescription();
          forStart = temp;
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
   *        loop of this one
   * @return true if this is a inner loop or false if this isn't a inner loop
   */
  private boolean isInnerLoop(Loop loop, CFA cfa) {
    boolean tempInnerLoop = false;

    for (Loop tempLoop : cfa.getLoopStructure().get().getAllLoops()) {
      if (tempLoop.isOuterLoopOf(loop)) {
        tempInnerLoop = true;
      }
    }

    return tempInnerLoop;
  }

  private boolean isOuterLoop(Loop loop, CFA cfa) {
    boolean tempOuterLoop = false;

    for (Loop tempLoop : cfa.getLoopStructure().get().getAllLoops()) {
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
   * @return returns a list with all of the variable names that are outputs in a loop
   */
  private ArrayList<String> getAllOutputs(CFA cfa) {
    ArrayList<String> tempOutput = new ArrayList<>();

    for (CFANode node : nodesInLoop) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.StatementEdge) ||
        node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
          boolean flag = true;

          for (String s : output) {
            if (
            s.contentEquals(
                CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i))
                    .split(OUTPUT_NAME_SYMBOL_CUT)[OUTPUT_VARIABLE_ARRAY_POSITION])) {
              flag = false;

            }
          }

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
                String tmpType = CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i)).toString().split("\\)")[0];
                tmpType = tmpType.split("\\(")[1];
                temp =
                    CFAEdgeUtils.getLeftHandVariable(
                        node.getLeavingEdge(
                            i))
                      .split(OUTPUT_NAME_SYMBOL_CUT)[OUTPUT_VARIABLE_ARRAY_POSITION]
                      + "&"
                      + "Array:"
                      + tmpType
                      + ":"
                        + CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i))
                            .toString()
                            .split("\\[")[1].split("\\]")[0];
              }else {
              temp =
                  CFAEdgeUtils.getLeftHandVariable(
                      node.getLeavingEdge(
                          i))
                    .split(OUTPUT_NAME_SYMBOL_CUT)[OUTPUT_VARIABLE_ARRAY_POSITION]
                    + "&"
                    + CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i));
              }
            } else if (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null) {
              if (CFAEdgeUtils.getLeftHandSide(
                  node.getLeavingEdge(
                      i))
                .getClass()
                .getName()
                .contains("Array")) {
              temp =
                  CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i))
                      .toString()
                      .split(
                          "\\[")[0]
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

    ArrayList<String> overwrite = new ArrayList<>();
    for (CFANode n : cfa.getAllNodes()) {
      for (int e = 0; e < n.getNumLeavingEdges(); e++) {
        if (n.getLeavingEdge(e).getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
          for (String s : tempOutput) {
            if (CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)) != null
                && s.split(
                    "&")[0]
                    .equals(CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)).split(":")[2])) {
              String tempNew = s;

              if (tempNew.contains("Array") && tempNew.split(":").length != 3) {
                String tmpNewStart = tempNew.split("&")[0];
                String tmpNewEnd = tempNew.split("&")[1];

                String arraySize =
                    CFAEdgeUtils.getLeftHandType(n.getLeavingEdge(e)).toString().split("\\[")[1]
                        .split("\\]")[0];

                tmpNewEnd =
                    tmpNewEnd
                        +
                    ":"
                        + arraySize;

                tempNew = tmpNewStart + "&" + tmpNewEnd;
              }

              tempNew =
                  tempNew
                      + "&"
                  + ((ADeclarationEdge) n.getLeavingEdge(e)).getDeclaration()
                  .getFileLocation()
                  .getStartingLineInOrigin();

              overwrite.add(tempNew);

            }
          }
        }
      }
    }
    tempOutput = overwrite;
    ArrayList<String> removeDuplicates = new ArrayList<>();
    for(String duplicate:tempOutput) {
      if(!removeDuplicates.contains(duplicate)){
        removeDuplicates.add(duplicate);
      }
    }
    tempOutput = removeDuplicates;
    return tempOutput;
  }

  private int getAllNumberOutputs(ArrayList<String> o) {
    int tmpInt = 0;
    for (String tmp : o) {
      if (tmp.contains("Array")) {
        tmpInt = tmpInt + Integer.parseInt(tmp.split("&")[1].split(":")[2]);
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
   * @return returns a list of variables that are inputs and outputs at the same time
   */
  private ArrayList<String> getAllIO() {
    ArrayList<String> inputs = getAllInputs();
    ArrayList<String> outputs = output;
    ArrayList<String> temp = new ArrayList<>();

    for (String o : outputs) {
      for (String i : inputs) {
          if ((o.split("&")[0].contentEquals(i))) {
            boolean flagNO = true;
            for (String v : temp) {
              if (v.split("&")[0].equals(o.split("&")[0])) {
                flagNO = false;
              }
            }
              if (flagNO) {
              temp.add(o);
            }
        }
        }

      }
      for (String z : temp) {
        if (temp.contains("Array")) {

        }
      }
    return temp;
  }

  /**
   * This method looks for all of the input-variables in the loop
   *
   * @return returns a list with the names of all the input variables
   */
  private ArrayList<String> getAllInputs() {
    ArrayList<String> temp = new ArrayList<>();

    for (CFANode node : nodesInLoop) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if ((node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.StatementEdge)
            || node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.DeclarationEdge))
            && CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)) != null) {
          if (CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i))
              .toString()
              .contains("operand")) {
            getInputFromRightHandSide(
                temp,
                CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)).toString());
          } else {
            temp.add(CFAEdgeUtils.getRightHandSide(node.getLeavingEdge(i)).toString());
          }
        }
        else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
          String edgeCode = node.getLeavingEdge(VALID_STATE).getCode();
          temp.add(edgeCode.split(" ")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
          temp.add(edgeCode.split(" ")[POSITION_OF_VARIABLE_IN_ARRAY_TWO]);
        } else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
          temp.add(
              node.getLeavingEdge(i).getCode().split("\\(")[POSITION_OF_VARIABLE_IN_ARRAY_ONE]
                  .split("\\)")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
        } else if (node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.ReturnStatementEdge)) {
          temp.add(node.getLeavingEdge(i).getCode().split(" ")[POSITION_OF_VARIABLE_IN_ARRAY_ONE]);
        }
      }
    }
    return temp;
  }

  private void getInputFromRightHandSide(ArrayList<String> temp, String stringSplit) {
    String[] tempStorage = stringSplit.split(",");
    ArrayList<String> tempS = new ArrayList<>();

    for (int i = 0; i < tempStorage.length; i++) {
      if (tempStorage[i].contains("operand")) {
        tempS.add(
            tempStorage[i].split("\\[")[POSITION_OF_VARIABLE_IN_ARRAY_ONE]
                .split("\\]")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
      }
    }

    temp.addAll(tempS);
  }

  /**
   * This method looks for all the possible path that the loop can go in one iteration
   *
   * @return number of possible paths in one iteration
   */
  private int getAllPaths() {
    int paths = 1;
    for (CFANode node : nodesInLoop) {
      if ((!node.equals(loopEnd))
          && (!nodesInCondition.contains(node))
          && !node.equals(failedState)) {
        paths += (node.getNumLeavingEdges() - LAST_POSITION_OF_LIST);
      }
    }
    for (String z : output) {
      if (z.contains("Array")) {
        paths += Integer.parseInt(z.split("&")[1].split(":")[2]);
      }
    }
    return paths;
  }

  /**
   * This method looks for all of the nodes in the condition and even cuts out the nodes that belong
   * to an if-case
   *
   * @param cfa
   * @return returns a list with all the nodes that are part of the condition
   */
  public ArrayList<CFANode> nodesInCondition(CFA cfa, LogManager pLogger, boolean loopTF) {

    ArrayList<CFANode> nodes = new ArrayList<>();
    ArrayList<CFANode> tempNodes = new ArrayList<>();
    CFANode tempNode = loopStart;
    boolean flag = true;


    if (loopType.contentEquals("while")) {

    while (flag) {

        if (tempNode.getLeavingEdge(VALID_STATE).getEdgeType().equals(CFAEdgeType.AssumeEdge)
            && nodesInLoop.contains(tempNode.getLeavingEdge(VALID_STATE).getSuccessor())
          && !nodes.contains(tempNode)) {
        nodes.add(tempNode);
      }
      for (int i = 1; i < tempNode.getNumLeavingEdges(); i++) {
        if (tempNode.getLeavingEdge(i)
            .getSuccessor()
              .getLeavingEdge(VALID_STATE)
            .getEdgeType()
            .equals(CFAEdgeType.AssumeEdge)
            && nodesInLoop.contains(tempNode.getLeavingEdge(i).getSuccessor())) {

        tempNodes.add(tempNode.getLeavingEdge(i).getSuccessor());
        }
        }
        if (!endOfCondition.contains(tempNode) && nodesInLoop.contains(tempNode)) {
          tempNode = tempNode.getLeavingEdge(VALID_STATE).getSuccessor();
      }
      else if (!tempNodes.isEmpty()) {
          tempNode = tempNodes.get(tempNodes.size() - LAST_POSITION_OF_LIST);
          tempNodes.remove(tempNodes.size() - LAST_POSITION_OF_LIST);
      } else {
        flag = false;
      }
    }
    } else if (loopType.contentEquals("for")) {
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNodeNumber() >= forStart.getNodeNumber()
            && node.getNodeNumber() <= loopStart.getNodeNumber() + 1) {
          nodes.add(node);
        }
      }

      ArrayList<CFANode> forNode = new ArrayList<>();
      while (flag) {
      for(CFANode x:nodes) {
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
    LoopGetIfAfterLoopCondition l = new LoopGetIfAfterLoopCondition(nodes, pLogger);
    if (l.getSmallestIf() != NO_IF_CASE) {
      ArrayList<CFANode> tempN = copyList(nodes);


      for (Iterator<CFANode> tempIterator = tempN.iterator(); tempIterator.hasNext();) {
        CFANode temps = tempIterator.next();
        if (temps.getLeavingEdge(VALID_STATE).getFileLocation().getStartingLineInOrigin() >= l
            .getSmallestIf()) {
          endOfCondition.add(temps);
          tempIterator.remove();
      }
    }

      nodes = tempN;
    }
  }
    return nodes;
  }

  /**
   * This method takes all of the nodes in the condition of this loop and returns a readable string
   * that shows the condition of the loop
   *
   * @return string that shows the condition of the loop
   */
  public String nodesToCondition() {

    String cond = "";
    ArrayList<CFANode> temp = copyList(nodesInCondition);
    CFANode node;

    if (loopType.contentEquals("while")) {
      if (temp.isEmpty()) {
        cond = "1";
      }

    while (!temp.isEmpty()) {
        node = temp.get(FIRST_POSITION_OF_LIST);
        temp.remove(FIRST_POSITION_OF_LIST);

        if (temp.size() > EMPTY_LIST) {
        boolean notNodeToEndCondition = true;

        for(int i = 0; i < node.getNumLeavingEdges(); i++) {
            if (endOfCondition.contains(node.getLeavingEdge(i).getSuccessor())) {
            notNodeToEndCondition = false;
          }
        }

        if(notNodeToEndCondition) {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " && ";
        }
        else {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " || ";
        }
      } else {
          cond = cond + node.getLeavingEdge(VALID_STATE).getCode();
          failedState = node.getLeavingEdge(ERROR_STATE).getSuccessor();
      }
    }
    } else if (loopType.contentEquals("for")) {

      CFANode start = temp.get(FIRST_POSITION_OF_LIST);
      temp.remove(FIRST_POSITION_OF_LIST);

      ArrayList<CFANode> forCondition = new ArrayList<>();
      for (Iterator<CFANode> tempIterator = temp.iterator(); tempIterator.hasNext();) {
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


      conditionInFor = copyList(forCondition);

      cond += start.getLeavingEdge(VALID_STATE).getDescription();

      while (!forCondition.isEmpty()) {
        node = forCondition.get(FIRST_POSITION_OF_LIST);
        forCondition.remove(FIRST_POSITION_OF_LIST);

        if (forCondition.size() > EMPTY_LIST) {
          boolean notNodeToEndCondition = true;

          for(int i = 0; i < node.getNumLeavingEdges(); i++) {
            if (endOfCondition.contains(node.getLeavingEdge(i).getSuccessor())) {
              notNodeToEndCondition = false;
            }
          }

          if(notNodeToEndCondition) {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " && ";
          }
          else {
            cond = cond + node.getLeavingEdge(VALID_STATE).getCode() + " || ";
          }
        } else {
          if (node.getLeavingEdge(VALID_STATE).getCode().contentEquals("")) {
            cond = cond + "1";
            flagEndless = true;
          } else {
          cond = cond + node.getLeavingEdge(VALID_STATE).getCode();
          failedState = node.getLeavingEdge(ERROR_STATE).getSuccessor();
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
   * @return returns true if the loop should be accelerated or false if it doesn't make that much
   *         sense to accelerate it
   */
  private boolean canLoopBeAccelerated() {
    ArrayList<CFANode> nodes = copyList(nodesInCondition);

    boolean canAccelerate = false;

    ArrayList<Boolean> temp = new ArrayList<>();

    if (loopType.contentEquals("while")) {
      ArrayList<String> rightSideVariable = new ArrayList<>();
      for (CFANode node : nodes) {
        rightSideVariable.add(
            node.getLeavingEdge(VALID_STATE)
                .getRawAST()
                .toString()
                .split("operand2=\\[")[POSITION_OF_VARIABLE_IN_ARRAY_ONE]
                    .split("\\]")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
      }

      for (String variable : rightSideVariable) {
        try {
          double d = Double.parseDouble(variable);
        } catch (NumberFormatException | NullPointerException nfe) {
          try {
            int d = Integer.parseInt(variable);
          } catch (NumberFormatException | NullPointerException nfe1) {
            try {
              long d = Long.parseLong(variable);
            } catch (NumberFormatException | NullPointerException nfe2) {
              try {
                float d = Float.parseFloat(variable);
              } catch (NumberFormatException | NullPointerException nfe3) {
                try {
                  new BigInteger(variable);
                } catch (NumberFormatException | NullPointerException nfe4) {
                  temp.add(true);
                }
              }
            }
          }
        }
      }

    } else if (loopType.contentEquals("for")) {
      if (!flagEndless) {
      ArrayList<String> rightSideVariable = new ArrayList<>();
      for (CFANode node : conditionInFor) {
        rightSideVariable.add(
            node.getLeavingEdge(VALID_STATE).getRawAST().toString().split("operand2=\\[")[1]
                .split("\\]")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
      }

      for (String variable : rightSideVariable) {
        try {
          double d = Double.parseDouble(variable);
        } catch (NumberFormatException | NullPointerException nfe) {
          try {
            int d = Integer.parseInt(variable);
          } catch (NumberFormatException | NullPointerException nfe1) {
            try {
              long d = Long.parseLong(variable);
            } catch (NumberFormatException | NullPointerException nfe2) {
              try {
                float d = Float.parseFloat(variable);
              } catch (NumberFormatException | NullPointerException nfe3) {
                try {
                  new BigInteger(variable);
                } catch (NumberFormatException | NullPointerException nfe4) {
                  temp.add(true);
                }
              }
            }
          }
        }
      }
    } else {
      temp.add(true);
    }
  }
    for (Boolean b : temp) {
      if (b == true) {
        canAccelerate = true;
      }
    }
    return canAccelerate;
  }

  private ArrayList<CFANode> copyList(ArrayList<CFANode> list) {
    ArrayList<CFANode> temp = new ArrayList<CFANode>();
    for (CFANode n : list) {
      temp.add(n);
    }
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

  public CFANode getLoopStart() {
    return loopStart;
  }

  public CFANode getLoopEnd() {
    return loopEnd;
  }

  public ArrayList<CFANode> getNodesInLoop() {
    return nodesInLoop;
  }

  public String getCondition() {
    return condition;
  }

  public ArrayList<String> getOutputs() {
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

  public ArrayList<String> getInputsOutputs() {
    return inputOutput;
  }

  public boolean getCanBeAccelerated() {
    return canBeAccelerated;
  }

  public CFANode getFaileState() {
    return failedState;
  }

  public ArrayList<CFANode> getNodesInCondition() {
    return nodesInCondition;
  }

  public boolean getIsOuterLoop() {
    return outerLoop;
  }

  public Loop getInnerLoop() {
    return innerLoop;
  }

  public String outputToString() {
    String temp = EMPTY_STRING;
    Object[] tempArray = output.toArray();
    for (int i = 0; i < tempArray.length; i++) {
      if (i < tempArray.length - FLAG_FOR_LAST_STRING) {
        temp += (String) tempArray[i];
        temp += ",";
      } else {
        temp += (String) tempArray[i];
      }
    }

    return temp;
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