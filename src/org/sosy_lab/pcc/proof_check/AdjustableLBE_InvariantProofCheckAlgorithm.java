/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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


public class AdjustableLBE_InvariantProofCheckAlgorithm extends InvariantProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected Hashtable<Integer, Vector<Pair<String, int[]>>> nodes =
      new Hashtable<Integer, Vector<Pair<String, int[]>>>();
  protected HashSet<String> edges = new HashSet<String>();
  protected Hashtable<Integer, CFANode> reachableCFANodes = new Hashtable<Integer, CFANode>();

  protected Hashtable<Integer, CFANode> allCFANodes = new Hashtable<Integer, CFANode>();

  protected int threshold;

  public AdjustableLBE_InvariantProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType,
      int pThreshold)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    handler = new FormulaHandler(pConfig, pLogger, pProverType);
    threshold = pThreshold;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target, sourceEdge;
    CFANode nodeS, nodeT, nodeSourceOpEdge;

    while (pScan.hasNext()) {
      try {
        // read next edge
        logger.log(Level.INFO, "Read next edge.");
        source = pScan.nextInt();
        sourceEdge = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = reachableCFANodes.get(source);

        nodeSourceOpEdge = allCFANodes.get(sourceEdge);
        nodeT = reachableCFANodes.get(target);
        if (nodeS == null
            || nodeSourceOpEdge == null || nodeT == null || !nodeSourceOpEdge.hasEdgeTo(nodeT)) {
          logger.log(Level.SEVERE, "Edge " + source + "#" + sourceEdge + "#" + target
              + "not possible because one is no CFA node or is not an abstraction node.");
          return PCCCheckResult.UnknownCFAEdge;
        }

        //add edge
        if (edges.contains(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target)) { return PCCCheckResult.ElementAlreadyRead; }
        edges.add(source + Separators.commonSeparator + sourceEdge + Separators.commonSeparator + target);
      } catch (IllegalArgumentException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e1) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
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
            for (int j = 0; i < stack.length; j++) {
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

  protected boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  private boolean isAbstractionNode(CFANode pNode, int pPathLength) {
    if (pNode.isLoopStart() || pNode.getEnteringSummaryEdge() != null || pNode instanceof FunctionDefinitionNode
        || pPathLength == threshold) { return true; }
    return false;
  }

  @Override
  protected PCCCheckResult checkProof() {
    HashSet<Integer> visited = new HashSet<Integer>();
    Vector<Integer> toVisit = new Vector<Integer>();
    Integer current = cfaForProof.getMainFunction().getNodeNumber();
    Pair<Formula, SSAMap> pair;
    PCCCheckResult intermediateRes;
    Vector<Pair<String, int[]>> invariants;
    int[][] stacks;
    // add  root
    visited.add(current);
    toVisit.add(current);
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      invariants = nodes.get(current);
      pair = buildRegionsFormula(invariants, null, reachableCFANodes.get(current), true);
      if (pair == null) { return PCCCheckResult.InvalidInvariant; }
      if (handler.isFalse(pair.getFirst())) {
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

  private PCCCheckResult checkCFANode(CFANode pStart, Formula pLeftAbstractions, int[][] pStacks, SSAMap pMap,
      Vector<Integer> pToVisit, HashSet<Integer> pVisited) {
    Vector<Pair<PathFormula, CFANode>> toCheck = new Vector<Pair<PathFormula, CFANode>>();
    // add start node
    PathFormula pf = handler.getTrueFormula(pMap);
    Formula f;
    Pair<Formula, SSAMap> right;
    Formula[] fList;
    if (pf == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    Pair<PathFormula, CFANode> current;
    CFANode node;
    String edgeID;
    toCheck.add(new Pair<PathFormula, CFANode>(pf, pStart));
    while (!toCheck.isEmpty()) {
      current = toCheck.remove(0);
      for (int i = 0; i < current.getSecond().getNumLeavingEdges(); i++) {
        node = current.getSecond().getLeavingEdge(i).getSuccessor();
        edgeID =
            pStart.getNodeNumber() + Separators.commonSeparator + current.getSecond().getNodeNumber()
                + Separators.commonSeparator + node.getNodeNumber();
        pf = handler.extendPath(current.getFirst(), current.getSecond().getLeavingEdge(i));
        if (pf == null) { return PCCCheckResult.InvalidEdge; }
        fList = new Formula[2];
        fList[0] = pLeftAbstractions;
        fList[1] = pf.getFormula();
        f = handler.buildConjunction(fList);
        if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
        // check if error node reached
        if ((node instanceof CFALabelNode) && (((CFALabelNode) node).getLabel().equalsIgnoreCase("error"))) {
          if (!handler.isFalse(f)) { return PCCCheckResult.ErrorNodeReachable; }
        }
        if (isAbstractionNode(node, pf.getLength())&& !edges.contains(edgeID)) {
          return PCCCheckResult.UncoveredEdge;
        }
        // stop because path marked as stop point
        if(edges.contains(edgeID)){
          // get operation description
          fList = new Formula[2];
          fList[0] = pf.getFormula();
          if (node instanceof FunctionDefinitionNode) {
            fList[1] =
                buildStackOperation(pStacks, true, node.getEnteringSummaryEdge() != null, current.getSecond()
                    .getLeavingEdge(i).getPredecessor().getLeavingSummaryEdge().getSuccessor().getNodeNumber());
          } else {
            fList[1] = buildStackOperation(pStacks, false, node.getEnteringSummaryEdge() != null, 0);
          }
          if (fList[0] == null || fList[1] == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          f = handler.buildConjunction(fList);
          // get right abstraction
          right = buildRegionsFormula(nodes.get(node.getNodeNumber()), pf.getSsa(), node, false);
          if (right == null || right.getFirst() == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          f = handler.buildEdgeInvariant(pLeftAbstractions, f, right.getFirst());
          if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
          if (!handler.isFalse(f)) { return PCCCheckResult.InvalidInvariant; }
          if (!pVisited.contains(node.getNodeNumber())) {
            pVisited.add(node.getNodeNumber());
            pToVisit.add(node.getNodeNumber());
          }
        } else {
          if (!handler.isFalse(f)) {
            toCheck.add(new Pair<PathFormula, CFANode>(pf, node));
          }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private Pair<Formula, SSAMap> buildRegionsFormula(Vector<Pair<String, int[]>> pRegions, SSAMap pMap, CFANode pNode,
      boolean pLeft) {
    if (pRegions == null) { return null; }
    Formula[] regionFormulae = new Formula[pRegions.size()];
    Formula f;
    for (int i = 0; i < pRegions.size(); i++) {
      f = handler.createFormula(pRegions.get(i).getFirst());
      f = addStackInvariant(f, pRegions.get(i).getSecond(), pLeft, pNode);
      if (f == null) { return null; }
      regionFormulae[i] = f;
    }
    // build disjunction
    f = handler.buildDisjunction(regionFormulae);
    if (f == null) { return null; }
    // instantiate formula
    Pair<Formula, SSAMap> result = handler.addIndices(pMap, f);
    if (result == null || result.getFirst() == null) { return null; }
    return result;
  }

  protected Formula addStackInvariant(Formula pInvariant, int[] pStack,
      boolean pLeft, CFANode pNode) {
    if (pInvariant == null || pStack == null) { return null; }
    boolean isReturn =
        pNode.getEnteringSummaryEdge() != null;
    Formula[] singleInvariant;
    int length = pStack.length + 1;
    if (isReturn) {
      length++;
    }
    singleInvariant = new Formula[length];

    singleInvariant[0] = pInvariant;
    try {
      for (int j = 0; j < pStack.length; j++) {
        if (pLeft) {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + (pStack.length - j)
                  + Separators.SSAIndexSeparator + 1 + " = " + pStack[j]);
        } else {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + (pStack.length - j)
                  + Separators.SSAIndexSeparator + 2 + " = " + pStack[j]);
        }

        if (singleInvariant[j + 1] == null) { return null; }
      }
      if (isReturn) {
        singleInvariant[singleInvariant.length - 1] =
            handler.createFormula(goalDes + " = "
                + reachableCFANodes.get(pNode).getNodeNumber());
        if (singleInvariant[singleInvariant.length - 1] == null) { return null; }
      }
    } catch (IllegalArgumentException e1) {
      return null;
    }
    return handler.buildConjunction(singleInvariant);
  }

  protected Formula buildStackOperation(int[][] pStacksBefore,
      boolean pFunctionCall, boolean pFunctionReturn, int pReturn) {
    Formula[] subFormulaeStack, subFormulae;
    int start, toTake;
    if (pFunctionCall && pFunctionReturn) { return null; }
    subFormulae = new Formula[pStacksBefore.length];
    try {
      for (int i = 0; i < pStacksBefore.length; i++) {
        // build stack operation for this stack
        if (pFunctionCall) {
          subFormulaeStack = new Formula[pStacksBefore[i].length + 1];
          toTake = pStacksBefore[i].length;
          start = 1;
          // add new stack element
          subFormulaeStack[subFormulaeStack.length - 1] =
              handler.createFormula(stackName + 0
                  + Separators.SSAIndexSeparator + 2 + " = " + pReturn);
          if (subFormulaeStack[subFormulaeStack.length - 1] == null) { return null; }
        } else {
          if (pFunctionReturn) {
            start = 2;
            toTake = pStacksBefore[i].length - 1;
            subFormulaeStack = new Formula[pStacksBefore[i].length];
            subFormulaeStack[subFormulaeStack.length - 1] =
                handler.createFormula(goalDes + " = " + pStacksBefore[i][pStacksBefore[i].length - 1]);
            if (subFormulaeStack[subFormulaeStack.length - 1] == null) { return null; }
          } else {
            start = 1;
            toTake = pStacksBefore[i].length;
            subFormulaeStack = new Formula[pStacksBefore[i].length];
          }
        }
        for (int k = 0; k < toTake; k++) {
          subFormulaeStack[k] =
              handler.createFormula(stackName + (start)
                  + Separators.SSAIndexSeparator + 1 + " = " + stackName
                  + (start) + Separators.SSAIndexSeparator + 2);
          start++;
          if (subFormulae[k] == null) { return null; }
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
