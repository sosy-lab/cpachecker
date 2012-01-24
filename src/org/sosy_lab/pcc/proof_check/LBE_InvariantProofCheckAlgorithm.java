/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.pcc.proof_check;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;


public class LBE_InvariantProofCheckAlgorithm extends InvariantProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected Hashtable<Integer, Vector<Pair<String, int[]>>> nodes =
      new Hashtable<Integer, Vector<Pair<String, int[]>>>();

  protected Hashtable<Integer, CFANode> reachableCFANodes = new Hashtable<Integer, CFANode>();

  protected Hashtable<Integer, CFANode> allCFANodes = new Hashtable<Integer, CFANode>();

  public LBE_InvariantProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    handler = new FormulaHandler(pConfig, pLogger, pProverType);
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    // no edges to be read
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult readNodes(Scanner pScan) {
    // set up look up for CFA nodes
    logger.log(Level.INFO, "Collect all CFA nodes");
    Vector<CFANode> cfaNodes = new Vector<CFANode>();
    allCFANodes.clear();
    CFANode current, child;
    int nextIndex = 0;
    cfaNodes.add(cfaForProof.getMainFunction());
    while (nextIndex < cfaNodes.size()) {
      current = cfaNodes.get(nextIndex);
      nextIndex++;
      allCFANodes.put(current.getNodeNumber(), current);
      // add children
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        child = current.getLeavingEdge(i).getSuccessor();
        if (!allCFANodes.containsKey(child.getNodeNumber())) {
          cfaNodes.add(child);
        }
      }
    }
    // reading nodes
    boolean rootFound = false;
    cfaNodes = null;
    String next = "";
    String[] subStr;
    int readNode, numInvariants;
    Vector<Pair<String, int[]>> invariants;
    String invariant;
    int[] stack;
    try {
      while (pScan.hasNext()) {
        next = pScan.next();
        if (next.equals("}")) {
          break;
        }
        // read node
        logger.log(Level.INFO, "Read next CFA node regions.");
        readNode = Integer.parseInt(next);
        if (allCFANodes.containsKey(readNode)) {
          current = allCFANodes.get(readNode);
          // check if node is abstraction node
          if (!isAbstractionNode(current)) {
            logger.log(Level.SEVERE, "CFA node " + readNode
                + " is not a node which is allowed to have regions attached to.");
            return PCCCheckResult.UnknownCFANode;
          }
          reachableCFANodes.put(readNode, current);
        } else {
          logger.log(Level.SEVERE, readNode + " is not a valid CFA node");
          return PCCCheckResult.InvalidCFANode;
        }
        boolean isRoot = cfaForProof.getMainFunction().equals(current);
        boolean isError = false;
        if (current instanceof CFALabelNode) {
          isError =
              ((CFALabelNode) current).getLabel().toLowerCase().equals("error");
        }
        // read invariants
        invariants = new Vector<Pair<String, int[]>>();
        numInvariants = pScan.nextInt();
        if (numInvariants <= 0) { return PCCCheckResult.InvalidInvariant; }
        for (int i = 0; i < numInvariants; i++) {
          // read invariant
          next = pScan.next();
          subStr = next.split("\\" + Separators.stackEntrySeparator);
          if (subStr.length < 1) {
            logger.log(Level.SEVERE, "No valid regions specification: " + subStr);
            return PCCCheckResult.InvalidInvariant;
          }
          // get formula
          try {
            if (!checkAbstraction(subStr[0])) { return PCCCheckResult.InvalidInvariant; }
            invariant = subStr[0];
            if (isRoot) {
              rootFound = true;
              if (!handler.createFormula(invariant).isTrue()) {
                logger.log(Level.SEVERE, "Invalid region for root.");
                return PCCCheckResult.InvalidARTRootSpecification;
              }
            }
            if (isError) {
              if (!handler.isFalse(invariant)) {
                logger.log(Level.SEVERE, "Invalid region for error node.");
                return PCCCheckResult.ErrorNodeReachable;
              }
            }
          } catch (IllegalArgumentException e1) {
            return PCCCheckResult.InvalidInvariant;
          }
          // get stack
          stack = new int[subStr.length - 1];
          if (isRoot
              || (current instanceof CFAFunctionExitNode && current
                  .equals(cfaForProof.getMainFunction().getExitNode()))) {
            if (stack.length != 0) { return PCCCheckResult.InvalidStack; }
          } else {
            if (current instanceof CFAFunctionExitNode && !current
                .equals(cfaForProof.getMainFunction().getExitNode()) || !isRoot
                && current instanceof FunctionDefinitionNode) {
              if (stack.length == 0) { return PCCCheckResult.InvalidStack; }
            }
          }
          try {
            for (int j = 0; j < stack.length; j++) {
              stack[j] = Integer.parseInt(subStr[j + 1]);
            }
          } catch (NumberFormatException e1) {
            logger.log(Level.SEVERE, "Invalid stack specification.");
            return PCCCheckResult.InvalidStack;
          }
          // add invariant
          invariants.add(new Pair<String, int[]>(invariant, stack));
        }
        if (nodes.containsKey(readNode)) {
          return PCCCheckResult.ElementAlreadyRead;
        } else {
          nodes.put(readNode, invariants);
        }
      }
    } catch (NumberFormatException e1) {
      return PCCCheckResult.UnknownCFANode;
    } catch (InputMismatchException e2) {
      return PCCCheckResult.UnknownCFANode;
    } catch (NoSuchElementException e3) {
      return PCCCheckResult.UnknownCFANode;
    }
    if (!rootFound) { return PCCCheckResult.UncoveredCFANode; }
    return PCCCheckResult.Success;
  }

  private boolean isAbstractionNode(CFANode pNode) {
    if (pNode.isLoopStart() || pNode.getEnteringSummaryEdge() != null || pNode instanceof FunctionDefinitionNode) { return true; }
    return false;
  }

  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult checkProof() {
    HashSet<Integer> visited = new HashSet<Integer>();
    Vector<Integer> toVisit = new Vector<Integer>();
    Integer current = cfaForProof.getMainFunction().getNodeNumber();
    Pair<Formula[], SSAMap> pair;
    Formula left;
    PCCCheckResult intermediateRes;
    Vector<Pair<String, int[]>> invariants;
    int[][] stacks;
    // add  root
    visited.add(current);
    toVisit.add(current);
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      invariants = nodes.get(current);
      pair = buildLeftRegionsFormula(invariants, reachableCFANodes.get(current));
      if (pair == null) { return PCCCheckResult.InvalidInvariant; }
      left = handler.buildDisjunction(pair.getFirst());
      if (left == null) { return PCCCheckResult.InvalidInvariant; }
      if (handler.isFalse(left)) {
        continue;
      }
      stacks = new int[invariants.size()][];
      for (int i = 0; i < stacks.length; i++) {
        stacks[i] = invariants.get(i).getSecond();
      }
      intermediateRes =
          checkCFANode(reachableCFANodes.get(current), pair.getFirst(), stacks, pair.getSecond(), toVisit, visited);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult checkCFANode(CFANode pStart, Formula[] pFormulas, int[][] pStacks, SSAMap pMap,
      Vector<Integer> pToVisit, HashSet<Integer> pVisited) {
    Vector<Pair<PathFormula, CFANode>> toCheck = new Vector<Pair<PathFormula, CFANode>>();
    // add start node
    PathFormula pf = handler.getTrueFormula(pMap);
    Formula f;
    Formula left = handler.buildDisjunction(pFormulas);
    if (left == null) { return PCCCheckResult.InvalidInvariant; }
    Formula right, leftWithStackOp;
    Formula[] fList;
    if (pf == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    Pair<PathFormula, CFANode> current;
    CFANode node;
    toCheck.add(new Pair<PathFormula, CFANode>(pf, pStart));
    while (!toCheck.isEmpty()) {
      current = toCheck.remove(0);
      for (int i = 0; i < current.getSecond().getNumLeavingEdges(); i++) {
        node = current.getSecond().getLeavingEdge(i).getSuccessor();
        pf = handler.extendPath(current.getFirst(), current.getSecond().getLeavingEdge(i));
        if (pf == null) { return PCCCheckResult.InvalidEdge; }
        fList = new Formula[2];
        fList[0] = left;
        fList[1] = pf.getFormula();
        f = handler.buildConjunction(fList);
        if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
        // check if error node reached
        if ((node instanceof CFALabelNode) && (((CFALabelNode) node).getLabel().equalsIgnoreCase("error"))) {
          if (!handler.isFalse(f)) { return PCCCheckResult.ErrorNodeReachable; }
        }
        if (isAbstractionNode(node) && !unreachableFunctionReturnEdge(pFormulas, pStacks, node)) {
          if (!reachableCFANodes.containsKey(node.getNodeNumber())) { return PCCCheckResult.UncoveredCFANode; }
          // get operation description
          if (node instanceof FunctionDefinitionNode) {
            leftWithStackOp =
                buildStackOperation(pFormulas, pStacks, true, node.getEnteringSummaryEdge() != null, current
                    .getSecond()
                    .getLeavingEdge(i).getPredecessor().getLeavingSummaryEdge().getSuccessor().getNodeNumber());
          } else {
            leftWithStackOp = buildStackOperation(pFormulas, pStacks, false, node.getEnteringSummaryEdge() != null, 0);
          }
          if (leftWithStackOp == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          // get right abstraction
          right = buildRightRegionsFormula(nodes.get(node.getNodeNumber()), pf.getSsa(), node);
          if (right == null) {
            System.out.println(nodes.get(node.getNodeNumber()));
            return PCCCheckResult.InvalidFormulaSpecificationInProof;
          }
          f = handler.buildEdgeInvariant(leftWithStackOp, pf.getFormula(), right);
          if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          if (!handler.isFalse(f)) { return PCCCheckResult.InvalidInvariant; }
          if (!pVisited.contains(node.getNodeNumber())) {
            pVisited.add(node.getNodeNumber());
            pToVisit.add(node.getNodeNumber());
          }
        } else {
          if (!handler.isFalse(f) && !unreachableFunctionReturnEdge(pFormulas, pStacks, node)) {
            toCheck.add(new Pair<PathFormula, CFANode>(pf, node));
          }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private boolean unreachableFunctionReturnEdge(Formula[] pFormulas, int[][] pStacks, CFANode pNodeReturn) {
    if (pNodeReturn.getEnteringSummaryEdge() == null) { return false; }
    for (int i = 0; i < pStacks.length; i++) {
      if (pStacks[i][0] == pNodeReturn.getNodeNumber() && !handler.isFalse(pFormulas[i])) { return false; }
    }
    return true;
  }

  private Pair<Formula[], SSAMap> buildLeftRegionsFormula(Vector<Pair<String, int[]>> pRegions,
      CFANode pNode) {
    if (pRegions == null) { return null; }
    Formula[] regionFormulae = new Formula[pRegions.size()];
    Formula f;
    for (int i = 0; i < pRegions.size(); i++) {
      f = handler.createFormula(pRegions.get(i).getFirst());
      f = addStackInvariant(f, pRegions.get(i).getSecond(), true, pNode);
      if (f == null) { return null; }
      regionFormulae[i] = f;
    }
    // build disjunction
    f = handler.buildDisjunction(regionFormulae);
    if (f == null) { return null; }
    // instantiate formula
    Pair<Formula, SSAMap> result = handler.addIndices(null, f);
    Pair<Formula, SSAMap> intermediate;
    if (result == null || result.getFirst() == null || result.getSecond() == null) { return null; }
    for (int i = 0; i < pRegions.size(); i++) {
      intermediate = handler.addIndices(result.getSecond(), regionFormulae[i]);
      if (intermediate == null || intermediate.getFirst() == null) { return null; }
      regionFormulae[i] = intermediate.getFirst();
    }
    return new Pair<Formula[], SSAMap>(regionFormulae, result.getSecond());
  }

  private Formula buildRightRegionsFormula(Vector<Pair<String, int[]>> pRegions, SSAMap pMap, CFANode pNode) {
    if (pRegions == null) { return null; }
    Formula[] regionFormulae = new Formula[pRegions.size()];
    Formula f;
    for (int i = 0; i < pRegions.size(); i++) {
      f = handler.createFormula(pRegions.get(i).getFirst());
      f = addStackInvariant(f, pRegions.get(i).getSecond(), false, pNode);
      if (f == null) { return null; }
      regionFormulae[i] = f;
    }
    // build disjunction
    f = handler.buildDisjunction(regionFormulae);
    if (f == null) { return null; }
    // instantiate formula
    Pair<Formula, SSAMap> result = handler.addIndices(pMap, f);
    if (result == null || result.getFirst() == null) { return null; }
    return result.getFirst();
  }

  protected Formula addStackInvariant(Formula pInvariant, int[] pStack,
      boolean pLeft, CFANode pNode) {
    if (pInvariant == null || pStack == null) { return null; }
    Formula[] singleInvariant;
    int length = pStack.length + 1;
    if (!pLeft) {
      length++;
    }
    singleInvariant = new Formula[length];

    singleInvariant[0] = pInvariant;
    try {
      for (int j = 0; j < pStack.length; j++) {
        if (pLeft) {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + (pStack.length - (j + 1))
                  + Separators.SSAIndexSeparator + 1 + " = " + pStack[j]);
        } else {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + (pStack.length - (j + 1))
                  + Separators.SSAIndexSeparator + 2 + " = " + pStack[j]);
        }

        if (singleInvariant[j + 1] == null) { return null; }
      }
      if (!pLeft) {
        // add stack length
        singleInvariant[singleInvariant.length - 1] =
            handler.createFormula(stackLength + " = "
                + Integer.toString(pStack.length));
        if (singleInvariant[singleInvariant.length - 1] == null) { return null; }
      }
    } catch (IllegalArgumentException e1) {
      return null;
    }
    if (pLeft || pNode.getEnteringSummaryEdge() == null) {
      return handler.buildConjunction(singleInvariant);
    } else {
      Formula[] list = new Formula[2];
      list[0] = handler.buildConjunction(singleInvariant);
      if (list[0] == null) { return null; }
      list[1] = handler.createFormula(goalDes + Separators.SSAIndexSeparator + 2 + " = "
          + Integer.toString(pNode.getNodeNumber()));
      if (list[1] == null) { return null; }
      return handler.buildImplication(list[1], list[0]);
    }
  }

  protected Formula buildStackOperation(Formula[] pLeftAbstractions, int[][] pStacksBefore,
      boolean pFunctionCall, boolean pFunctionReturn, int pReturn) {
    if (pLeftAbstractions == null || pLeftAbstractions.length != pStacksBefore.length) { return null; }
    Formula[] subFormulaeStack, subFormulae;
    int start, toTake;
    if (pFunctionCall && pFunctionReturn) { return null; }
    subFormulae = new Formula[pStacksBefore.length];
    try {
      for (int i = 0; i < pStacksBefore.length; i++) {
        // build stack operation for this stack
        if (pFunctionCall) {
          subFormulaeStack = new Formula[pStacksBefore[i].length + 3];
          toTake = pStacksBefore[i].length;
          start = 1;
          // add new stack element
          subFormulaeStack[subFormulaeStack.length - 1] =
              handler.createFormula(stackName + 0
                  + Separators.SSAIndexSeparator + 2 + " = " + pReturn);
          if (subFormulaeStack[subFormulaeStack.length - 1] == null) { return null; }
          subFormulaeStack[subFormulaeStack.length - 2] =
              handler.createFormula(stackLength + " = "
                  + Integer.toString(pStacksBefore[i].length + 1));
          if (subFormulaeStack[subFormulaeStack.length - 2] == null) { return null; }
          subFormulaeStack[subFormulaeStack.length - 3] = pLeftAbstractions[i];
        } else {
          if (pFunctionReturn) {
            start = 2;
            toTake = pStacksBefore[i].length - 1;
            subFormulaeStack = new Formula[pStacksBefore[i].length + 2];
            subFormulaeStack[subFormulaeStack.length - 1] =
                handler.createFormula(goalDes + Separators.SSAIndexSeparator + 2 + " = "
                    + pStacksBefore[i][pStacksBefore[i].length - 1]);
            if (subFormulaeStack[subFormulaeStack.length - 1] == null) { return null; }
            subFormulaeStack[subFormulaeStack.length - 3] =
                handler.createFormula(stackLength + " = "
                    + Integer.toString(pStacksBefore[i].length - 1));
            if (subFormulaeStack[subFormulaeStack.length - 3] == null) { return null; }
            subFormulaeStack[subFormulaeStack.length - 2] = pLeftAbstractions[i];
          } else {
            start = 1;
            toTake = pStacksBefore[i].length;
            subFormulaeStack = new Formula[pStacksBefore[i].length + 2];
            subFormulaeStack[subFormulaeStack.length - 1] =
                handler.createFormula(stackLength + " = "
                    + Integer.toString(pStacksBefore[i].length));
            if (subFormulaeStack[subFormulaeStack.length - 1] == null) { return null; }
            subFormulaeStack[subFormulaeStack.length - 2] = pLeftAbstractions[i];
          }
        }
        for (int k = 0; k < toTake; k++) {
          if (pFunctionCall) {
            subFormulaeStack[k] =
                handler.createFormula(stackName + (k)
                    + Separators.SSAIndexSeparator + 1 + " = " + stackName
                    + (k + start) + Separators.SSAIndexSeparator + 2);
          } else {
            if (pFunctionReturn) {
              subFormulaeStack[k] =
                  handler.createFormula(stackName + (k + start)
                      + Separators.SSAIndexSeparator + 1 + " = " + stackName
                      + k + Separators.SSAIndexSeparator + 2);
            } else {
              subFormulaeStack[k] =
                  handler.createFormula(stackName + k
                      + Separators.SSAIndexSeparator + 1 + " = " + stackName
                      + k + Separators.SSAIndexSeparator + 2);
            }
          }
          if (subFormulaeStack[k] == null) { return null; }
        }
        // build conjunction
        subFormulae[i] = handler.buildConjunction(subFormulaeStack);
        if (subFormulae[i] == null) { return null; }
      }
    } catch (IllegalArgumentException e) {
      return null;
    }
    return handler.buildDisjunction(subFormulae);
  }

}
