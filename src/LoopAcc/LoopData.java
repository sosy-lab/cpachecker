// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package LoopAcc;

import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
      List<CFANode> loopNodes,
      Loop loop,
      LogManager pLogger) {
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
    nodesInCondition = nodesInCondition(cfa, pLogger);
    output = getAllOutputs(cfa);
    condition = nodesToCondition();
    getAllPaths();
    inputOutput = getAllIO();
    numberAllOutputs = getAllNumberOutputs(output);
    amountOfPaths = getAllPaths();
    canBeAccelerated = canLoopBeAccelerated();
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

    for (Loop tempLoop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (tempLoop.isOuterLoopOf(loop)) {
        tempInnerLoop = true;
      }
    }

    return tempInnerLoop;
  }

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
   * @return returns a list with all of the variable names that are outputs in a loop
   */
  private List<String> getAllOutputs(CFA cfa) {
    List<String> tempOutput = new ArrayList<>();

    for (CFANode node : nodesInLoop) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        if ((node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.StatementEdge)
            || node.getLeavingEdge(i).getEdgeType().equals(CFAEdgeType.DeclarationEdge))
            && (CFAEdgeUtils.getLeftHandSide(node.getLeavingEdge(i)) != null
                || CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)) != null)) {
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
                if (CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i)).contains(":")) {
              temp =
                  CFAEdgeUtils.getLeftHandVariable(
                      node.getLeavingEdge(
                          i))
                    .split(OUTPUT_NAME_SYMBOL_CUT)[OUTPUT_VARIABLE_ARRAY_POSITION]
                    + "&"
                    + CFAEdgeUtils.getLeftHandType(node.getLeavingEdge(i));
            } else {
              temp = CFAEdgeUtils.getLeftHandVariable(node.getLeavingEdge(i));
            }
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

    List<String> overwrite = new ArrayList<>();
    for (CFANode n : cfa.getAllNodes()) {
      for (int e = 0; e < n.getNumLeavingEdges(); e++) {
        if (n.getLeavingEdge(e).getEdgeType().equals(CFAEdgeType.DeclarationEdge)) {
          for (String s : tempOutput) {
            if (CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)) != null){
if(CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)).contains(":") &&
                 s.split(
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

            } else if (!CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)).contains(":")
                && s.split("&")[0].equals(CFAEdgeUtils.getLeftHandVariable(n.getLeavingEdge(e)))) {
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
    }
    tempOutput = overwrite;
    List<String> removeDuplicates = new ArrayList<>();
    for(String duplicate:tempOutput) {
      if(!removeDuplicates.contains(duplicate)){
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
  private List<String> getAllIO() {
    List<String> inputs = getAllInputs();
    List<String> outputs = output;
    List<String> temp = new ArrayList<>();

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
    return temp;
  }

  /**
   * This method looks for all of the input-variables in the loop
   *
   * @return returns a list with the names of all the input variables
   */
  private List<String> getAllInputs() {
    List<String> temp = new ArrayList<>();

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

  private void getInputFromRightHandSide(List<String> temp, String stringSplit) {
    String[] tempStorage = stringSplit.split(",");
    List<String> tempS = new ArrayList<>();

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
   * @param cfa used to get a list of all nodes to see which variables are already initialized
   * @return returns a list with all the nodes that are part of the condition
   */
  public List<CFANode> nodesInCondition(CFA cfa, LogManager pLogger) {

    List<CFANode> nodes = new ArrayList<>();
    List<CFANode> tempNodes = new ArrayList<>();
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

      List<CFANode> forNode = new ArrayList<>();
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
      List<CFANode> tempN = copyList(nodes);


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
    List<CFANode> temp = copyList(nodesInCondition);
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

      List<CFANode> forCondition = new ArrayList<>();
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
    List<CFANode> nodes = copyList(nodesInCondition);

    boolean canAccelerate = false;

    List<Boolean> temp = new ArrayList<>();

    if (loopType.contentEquals("while")) {
      List<String> rightSideVariable = new ArrayList<>();
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
          if (d > amountOfPaths || d > numberAllOutputs) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe) {
          temp.add(true);
        }
          try {
            int d = Integer.parseInt(variable);
            if (d > amountOfPaths || d > numberAllOutputs) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe1) {
            temp.add(true);
          }
            try {
              long d = Long.parseLong(variable);
              if (d > amountOfPaths || d > numberAllOutputs) {
                temp.add(true);
              }
            } catch (NumberFormatException | NullPointerException nfe2) {
              temp.add(true);
            }
              try {
                float d = Float.parseFloat(variable);
                if (d > amountOfPaths || d > numberAllOutputs) {
                  temp.add(true);
                }
              } catch (NumberFormatException | NullPointerException nfe3) {
                temp.add(true);
              }
                try {
                  BigInteger d = new BigInteger(variable);
                  if (d.intValueExact() > amountOfPaths || d.intValueExact() > numberAllOutputs) {
                    temp.add(true);
                  }
                } catch (NumberFormatException | NullPointerException nfe4) {
                  temp.add(true);
                }
      }

    } else if (loopType.contentEquals("for")) {
      if (!flagEndless) {
        List<String> rightSideVariable = new ArrayList<>();
      for (CFANode node : conditionInFor) {
        rightSideVariable.add(
            node.getLeavingEdge(VALID_STATE).getRawAST().toString().split("operand2=\\[")[1]
                .split("\\]")[POSITION_OF_VARIABLE_IN_ARRAY_ZERO]);
      }

      for (String variable : rightSideVariable) {
        try {
          double d = Double.parseDouble(variable);
          if (d > amountOfPaths || d > numberAllOutputs) {
            temp.add(true);
          }
        } catch (NumberFormatException | NullPointerException nfe) {
          temp.add(true);
        }
          try {
            int d = Integer.parseInt(variable);
            if (d > amountOfPaths || d > numberAllOutputs) {
              temp.add(true);
            }
          } catch (NumberFormatException | NullPointerException nfe1) {
            temp.add(true);
          }
            try {
              long d = Long.parseLong(variable);
              if (d > amountOfPaths || d > numberAllOutputs) {
                temp.add(true);
              }
            } catch (NumberFormatException | NullPointerException nfe2) {
              temp.add(true);
            }
              try {
                float d = Float.parseFloat(variable);
                if (d > amountOfPaths || d > numberAllOutputs) {
                  temp.add(true);
                }
              } catch (NumberFormatException | NullPointerException nfe3) {
                temp.add(true);
              }
                try {
                  BigInteger d = new BigInteger(variable);
                  if (d.intValueExact() > amountOfPaths || d.intValueExact() > numberAllOutputs) {
                    temp.add(true);
                  }
                } catch (NumberFormatException | NullPointerException nfe4) {
                  temp.add(true);
                }
      }
    } else {
      temp.add(true);
    }
  }
    for (Boolean b : temp) {
      if (b) {
        canAccelerate = true;
      }
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