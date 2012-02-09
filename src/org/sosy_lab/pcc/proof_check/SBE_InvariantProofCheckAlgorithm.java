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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

public abstract class SBE_InvariantProofCheckAlgorithm extends
    InvariantProofCheckAlgorithm {

  private boolean atLoop;
  //private boolean                                          atFunction;
  protected FormulaHandler handler;
  protected Hashtable<Integer, Vector<Pair<String, int[]>>> nodes =
      new Hashtable<Integer, Vector<Pair<String, int[]>>>();
  protected HashSet<String> edges =
      new HashSet<String>();
  protected Hashtable<Integer, CFANode> reachableCFANodes =
      new Hashtable<Integer, CFANode>();

  protected Hashtable<Integer, CFANode> allCFANodes =
      new Hashtable<Integer, CFANode>();

  public SBE_InvariantProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, String pProverType, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    //atFunction = pAlwaysAtFunctions;
    atLoop = pAlwaysAtLoops;
    handler = new FormulaHandler(pConfig, pLogger, pProverType);
  }

  protected CFAEdge retrieveOperationEdge(CFANode pSource, CFANode pSourceEdge,
      CFANode pTarget) {
    HashSet<Integer> visitedTargets = new HashSet<Integer>();
    Vector<CFANode> toVisit = new Vector<CFANode>();
    toVisit.add(pSource);
    visitedTargets.add(pSource.getNodeNumber());
    CFANode current;
    CFAEdge edge;
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      if (current.equals(pSourceEdge)) {
        if ((!current.hasEdgeTo(pTarget) || !isEndpoint(current
            .getEdgeTo(pTarget)))) {
          logger.log(Level.SEVERE, pSource + "#" + pSourceEdge + "#" + pTarget
              + " is not a valid edge which connects regions");
          return null;
        } else {
          return current.getEdgeTo(pTarget);
        }
      }
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        edge = current.getLeavingEdge(i);
        if (!isEndpoint(edge) && !visitedTargets.contains(edge.getSuccessor())) {
          visitedTargets.add(edge.getSuccessor().getNodeNumber());
          toVisit.add(edge.getSuccessor());
        }
      }
    }
    logger.log(Level.SEVERE, pSource + "#" + pSourceEdge + "#" + pTarget
        + " is not a valid edge which connects regions");
    return null;
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
          if (isNonAbstractionNode(current)) {
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
              if (!handler.createFormula(invariant).isTrue()) {
                logger.log(Level.SEVERE, "Invalid region for root.");
                return PCCCheckResult.InvalidARTRootSpecification;
              }
              rootFound = true;
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
          if (isRoot) {
            if (stack.length != 0) { return PCCCheckResult.InvalidARTRootSpecification; }
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
    // checking nodes
    logger.log(Level.INFO, "Check coverage of CFA nodes");
    return structuralCheckCoverageOfCFANodes();
  }

  private PCCCheckResult structuralCheckCoverageOfCFANodes() {
    //check if same size
    if (allCFANodes.size() == reachableCFANodes.size()) { return PCCCheckResult.Success; }
    // get uncovered nodes
    logger.log(Level.INFO, "Get all uncovered CFA nodes");
    HashSet<Integer> uncovered = new HashSet<Integer>();
    for (Integer nodeID : allCFANodes.keySet()) {
      if (!reachableCFANodes.containsKey(nodeID)) {
        uncovered.add(nodeID);
      }
    }
    // build connected components for uncovered nodes
    logger.log(Level.INFO, "Build connected components from CFA subgraph induced by uncovered nodes");
    Vector<ConnectedComponent> comps =
        ConnectedComponent.extractConnectedComponents(
            uncovered, allCFANodes, reachableCFANodes);
    // check if all external edges lead to only false abstraction or all nodes of component are non-abstraction nodes
    logger.log(Level.INFO, "Check if nodes must be uncovered");
    boolean result = true;
    boolean functionEnd;

    for (int i = 0; result && i < comps.size(); i++) {
      // check external edges
      for (Integer external : comps.get(i).externalEndPoints) {
        functionEnd = false;
        if (allCFANodes.get(external).getNumLeavingEdges() > 0
            && allCFANodes.get(external).getLeavingEdge(0) instanceof FunctionReturnEdge) {
          functionEnd = true;
        }
        if (functionEnd) {
          for (int j = 0; j < allCFANodes.get(external).getNumLeavingEdges(); j++) {
            if (comps.get(i).nodes.contains(allCFANodes.get(external).getLeavingEdge(j).getSuccessor().getNodeNumber())) {
              result =
                  result
                      && (allInvariantFormulaeFalse(external, allCFANodes.get(external).getLeavingEdge(j).getSuccessor()
                          .getNodeNumber())|| noUncoveredAbstractionReachable(external,comps.get(i).nodes));
            }
          }
        } else {
          result = result && (allInvariantFormulaeFalse(external, -1)||noUncoveredAbstractionReachable(external, comps.get(i).nodes));
        }
      }
      //check if only non-abstraction nodes which are non error nodes
      if (!result) {
        result = true;
        for (Integer id : comps.get(i).nodes) {
          result = result && isNonAbstractionNode(allCFANodes.get(id));
          if (allCFANodes.get(id) instanceof CFALabelNode
              && ((CFALabelNode) allCFANodes.get(id)).getLabel().equalsIgnoreCase("error")) {
            result = false;
          }
        }
      }
    }
    if (!result) { return PCCCheckResult.UncoveredCFANode; }
    return PCCCheckResult.Success;
  }

  private boolean noUncoveredAbstractionReachable(Integer pExternalStart, HashSet<Integer> pConnectedComponentNodes){
    Vector<Integer> toCheck = new Vector<Integer>();
    HashSet<Integer> checked = new HashSet<Integer>();
    CFANode node, succ;
    toCheck.add(pExternalStart);
    while(!toCheck.isEmpty()){
      node = allCFANodes.get(toCheck.remove(0));
      for(int i=0;i<node.getNumLeavingEdges();i++){
        succ = node.getLeavingEdge(i).getSuccessor();
        if(pConnectedComponentNodes.contains(succ.getNodeNumber()) && !checked.contains(succ.getNodeNumber())){
          checked.add(succ.getNodeNumber());
          toCheck.add(succ.getNodeNumber());
          if(!isNonAbstractionNode(succ)){
            return false;
          }
        }
      }
    }
    return true;
  }

  protected PCCCheckResult structuralCheckCoverageOfCFAEdges() {
    //build all edge identifications for reachable source nodes and test if they are in edges
    HashSet<Integer> visited = new HashSet<Integer>();
    Vector<CFANode> toVisit = new Vector<CFANode>();
    logger.log(Level.INFO, "Check if all edges which must be in proof are contained.");
    //add root node
    toVisit.add(cfaForProof.getMainFunction());
    visited.add(toVisit.get(0).getNodeNumber());
    CFANode current, succ;
    CFAEdge edge;
    PCCCheckResult intermediateRes;
    boolean isSource;
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      isSource = reachableCFANodes.containsKey(current.getNodeNumber());
      // treat every leaving edge
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        edge = current.getLeavingEdge(i);
        // if it is an abstraction node and abstraction is not false, build and check edge
        if (isSource && !allInvariantFormulaeFalse(current.getNodeNumber(), edge.getSuccessor().getNodeNumber())) {
          logger.log(Level.INFO, "Check if all edges for node " + current + " are available.");
          intermediateRes =
              buildLeavingEdgesAndCheck(current.getNodeNumber(), edge);
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
        }
        succ = edge.getSuccessor();
        if (!visited.contains(succ.getNodeNumber())) {
          toVisit.add(succ);
          visited.add(succ.getNodeNumber());
        }
      }
    }
    return PCCCheckResult.Success;
  }


  private PCCCheckResult buildLeavingEdgesAndCheck(int pNodeNumber,
      CFAEdge pEdge) {
    Vector<String> edges = buildLeavingEdges(pNodeNumber, pEdge);
    for (int i = 0; i < edges.size(); i++) {
      if (!this.edges.contains(edges.get(i))) { return PCCCheckResult.UncoveredEdge; }
    }
    return PCCCheckResult.Success;
  }

  protected Vector<String> buildLeavingEdges(int pNodeNumber,
      CFAEdge pEdge) {
    Vector<String> foundEdges = new Vector<String>();
    String id;
    if (isEndpoint(pEdge)) {
      id =
          new String(pNodeNumber + Separators.commonSeparator + pNodeNumber
              + Separators.commonSeparator
              + pEdge.getSuccessor().getNodeNumber());
      foundEdges.add(id);
      return foundEdges;
    }
    HashSet<Integer> visited = new HashSet<Integer>();
    visited.add(pNodeNumber);
    Vector<CFANode> toVisit = new Vector<CFANode>();
    toVisit.add(pEdge.getSuccessor());
    visited.add(pEdge.getSuccessor().getNodeNumber());
    CFANode current, succ;
    CFAEdge edge;
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        edge = current.getLeavingEdge(i);
        succ = edge.getSuccessor();
        if (isEndpoint(edge)) {
          id =
              pNodeNumber + Separators.commonSeparator
                  + current.getNodeNumber() + Separators.commonSeparator
                  + succ.getNodeNumber();
          foundEdges.add(id);
        } else {
          if (!visited.contains(succ.getNodeNumber())) {
            visited.add(succ.getNodeNumber());
            toVisit.add(succ);
          }
        }
      }
    }
    return foundEdges;
  }

  protected Vector<CFANode> getDirectSuccessors(int pSource, int pSourceEdge, int pTarget) {
    Vector<CFANode> succ = new Vector<CFANode>();
    if (pSource == pSourceEdge) {
      if (reachableCFANodes.get(pSource).hasEdgeTo(reachableCFANodes.get(pTarget))) {
        succ.add(reachableCFANodes.get(pTarget));
      } else {
        return null;
      }
    } else {
      for (int i = 0; i < reachableCFANodes.get(pSource).getNumLeavingEdges(); i++) {
        if (isSuccessor(reachableCFANodes.get(pSource).getLeavingEdge(i).getSuccessor(), pSourceEdge, pTarget)) {
          succ.add(reachableCFANodes.get(pSource).getLeavingEdge(i).getSuccessor());
        }
      }
      if (succ.isEmpty()) { return null; }
    }
    return succ;
  }

  private boolean isSuccessor(CFANode pSuccessor, int pSourceEdge, int pTarget) {
    HashSet<Integer> visitedTargets = new HashSet<Integer>();
    Vector<CFANode> toVisit = new Vector<CFANode>();
    toVisit.add(pSuccessor);
    visitedTargets.add(pSuccessor.getNodeNumber());
    CFANode current;
    CFAEdge edge;
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      if (current.getNodeNumber() == pSourceEdge) {
        if ((!current.hasEdgeTo(reachableCFANodes.get(pTarget)) || !isEndpoint(current
            .getEdgeTo(reachableCFANodes.get(pTarget))))) {
          logger.log(Level.SEVERE, pSuccessor.getNodeNumber() + "#" + pSourceEdge + "#" + pTarget
              + " is not a valid edge which connects regions");
          return false;
        } else {
          return true;
        }
      }
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        edge = current.getLeavingEdge(i);
        if (!isEndpoint(edge) && !visitedTargets.contains(edge.getSuccessor())) {
          visitedTargets.add(edge.getSuccessor().getNodeNumber());
          toVisit.add(edge.getSuccessor());
        }
      }
    }
    return false;
  }

  protected boolean allInvariantFormulaeFalse(Integer pNode, int pReturn) {
    Vector<Pair<String, int[]>> formulae = nodes.get(pNode);
    boolean functionEnd = false;
    if (allCFANodes.get(pNode).getNumLeavingEdges() > 0
        && allCFANodes.get(pNode).getLeavingEdge(0) instanceof FunctionReturnEdge) {
      functionEnd = true;
    }
    for (int i = 0; i < formulae.size(); i++) {
      if (!handler.isFalse(formulae.get(i).getFirst())
          && (!functionEnd || formulae.get(i).getSecond()[formulae.get(i).getSecond().length - 1] == pReturn)) { return false; }
    }
    return true;
  }

  private boolean isNonAbstractionNode(CFANode pNode) {
    // check if kind of node requires abstraction
    if ((pNode instanceof CFAFunctionDefinitionNode)
        || pNode.getEnteringSummaryEdge() != null
        || (atLoop && pNode.isLoopStart())) { return false; }
    //check if all entering edges have empty operation
    for (int i = 0; i < pNode.getNumEnteringEdges(); i++) {
      if (handler.getEdgeOperation(pNode.getEnteringEdge(i)).length() != 0) { return false; }

    }
    return true;
  }

  private boolean isEndpoint(CFAEdge pEdge) {
    CFANode succ = pEdge.getSuccessor();
    if (succ instanceof CFAFunctionDefinitionNode
        || succ.getEnteringSummaryEdge() != null
        || (atLoop && succ.isLoopStart())) { return true; }
    if (handler.getEdgeOperation(pEdge).length() == 0) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult checkProof() {
    // iterate over all edges
    int source, sourceEdge, target;
    PCCCheckResult intermediateRes;
    Vector<Pair<String, int[]>> invariantS, invariantT;
    CFAEdge cfaEdge;
    logger.log(Level.INFO, "Start proving inductions edge by edge.");
    for (String edge : edges) {
      source =
          Integer.parseInt(edge.substring(0, edge
              .indexOf(Separators.commonSeparator)));
      target =
          Integer.parseInt(edge.substring(
              edge.lastIndexOf(Separators.commonSeparator) + 1, edge.length()));
      sourceEdge =
          Integer.parseInt(edge.substring(
              (edge.indexOf(Separators.commonSeparator)) + 1,
              edge.lastIndexOf(Separators.commonSeparator)));
      cfaEdge =
          allCFANodes.get(sourceEdge).getEdgeTo(reachableCFANodes.get(target));
      invariantS = nodes.get(source);
      invariantT = nodes.get(target);
      intermediateRes = proveEdge(edge, source, target, invariantS, cfaEdge, invariantT);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  protected Formula addStackOperation(Formula pOperations, int[] pStackBefore,
      boolean pFunctionCall, boolean pFunctionReturn, int pReturn) {
    Formula[] subFormulae;
    int elementsTakenFromStack;
    if (pFunctionCall && pFunctionReturn) { return null; }
    try {
      if (pFunctionCall) {
        subFormulae = new Formula[pStackBefore.length + 3];
        elementsTakenFromStack = pStackBefore.length;
        // add new stack element
        subFormulae[subFormulae.length - 1] =
            handler.createFormula(stackName + (pStackBefore.length)
                + Separators.SSAIndexSeparator + 2 + " = " + pReturn);
        if (subFormulae[subFormulae.length - 1] == null) { return null; }
        // add stack length
        subFormulae[subFormulae.length - 2] =
            handler.createFormula(stackLength + Separators.SSAIndexSeparator + 2 + " = "
                + Integer.toString(pStackBefore.length + 1));
        if (subFormulae[subFormulae.length - 2] == null) { return null; }
      } else {
        subFormulae = new Formula[pStackBefore.length + 2];
        if (pFunctionReturn) {
          elementsTakenFromStack = pStackBefore.length - 1;
          // add return statement
          subFormulae[subFormulae.length - 1] =
              handler.createFormula(goalDes + Separators.SSAIndexSeparator + 2 + "  = "
                  + pStackBefore[pStackBefore.length - 1]);
          if (subFormulae[subFormulae.length - 1] == null) { return null; }
          // add stack length
          subFormulae[subFormulae.length - 2] =
              handler.createFormula(stackLength + Separators.SSAIndexSeparator + 2 + " = "
                  + Integer.toString(pStackBefore.length - 1));
          if (subFormulae[subFormulae.length - 2] == null) { return null; }
        } else {
          elementsTakenFromStack = pStackBefore.length;
          // add stack length
          subFormulae[subFormulae.length - 1] =
              handler.createFormula(stackLength + Separators.SSAIndexSeparator + 2 + " = "
                  + Integer.toString(pStackBefore.length));
          if (subFormulae[subFormulae.length - 1] == null) { return null; }
        }
      }
      for (int i = 1; i <= elementsTakenFromStack; i++) {
        subFormulae[i] =
            handler.createFormula(stackName + (i - 1)
                + Separators.SSAIndexSeparator + 1 + " = " + stackName
                + (i - 1) + Separators.SSAIndexSeparator + 2);
        if (subFormulae[i] == null) { return null; }
      }
    } catch (IllegalArgumentException e) {
      return null;
    } catch (ArrayIndexOutOfBoundsException e2) {
      return null;
    }
    subFormulae[0] = pOperations;
    return handler.buildConjunction(subFormulae);
  }

  protected Formula addStackInvariant(Formula pInvariant, int[] pStack,
      boolean pLeft, int pNode) {
    if (pInvariant == null || pStack == null) { return null; }
    Formula[] singleInvariant;
    if (!pLeft) {
      singleInvariant = new Formula[pStack.length + 2];
    } else {
      singleInvariant = new Formula[pStack.length + 1];
    }

    singleInvariant[0] = pInvariant;
    try {
      for (int j = 0; j < pStack.length; j++) {
        if (pLeft) {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + j
                  + Separators.SSAIndexSeparator + 1 + " = " + pStack[j]);
        } else {
          singleInvariant[j + 1] =
              handler.createFormula(stackName + j
                  + Separators.SSAIndexSeparator + 2 + " = " + pStack[j]);
        }

        if (singleInvariant[j + 1] == null) { return null; }
      }
      if (!pLeft) {
        // add stack length
        singleInvariant[singleInvariant.length - 1] =
            handler.createFormula(stackLength + Separators.SSAIndexSeparator + 2 + " = "
                + Integer.toString(pStack.length));
        if (singleInvariant[singleInvariant.length - 1] == null) { return null; }
      }
    } catch (IllegalArgumentException e1) {
      return null;
    }
    if (pLeft || reachableCFANodes.get(pNode).getEnteringSummaryEdge() == null) {
      return handler.buildConjunction(singleInvariant);
    } else {
      Formula[] list = new Formula[2];
      list[0] = handler.buildConjunction(singleInvariant);
      if (list[0] == null) { return null; }
      list[1] = handler.createFormula(goalDes + Separators.SSAIndexSeparator + 2 + " = "
          + Integer.toString(pNode));
      if (list[1] == null) { return null; }
      return handler.buildImplication(list[1], list[0]);
    }
  }

  protected abstract boolean checkAbstraction(String pAbstraction);

  protected abstract PCCCheckResult proveEdge(String pEdge, int pSource, int pTarget,
      Vector<Pair<String, int[]>> pInvariantS, CFAEdge pCfaEdge,
      Vector<Pair<String, int[]>> pInvariantT);

  private static class ConnectedComponent {

    private HashSet<Integer> nodes =
        new HashSet<Integer>();
    private Vector<Integer> externalEndPoints =
        new Vector<Integer>();
    private static Vector<ConnectedComponent> stronglyConComp;

    private ConnectedComponent() {

    }

    public static Vector<ConnectedComponent> extractConnectedComponents(
        HashSet<Integer> pGraph, Hashtable<Integer, CFANode> pAllNodes,
        Hashtable<Integer, CFANode> pReachableNodes) {
      stronglyConComp =
          new Vector<SBE_InvariantProofCheckAlgorithm.ConnectedComponent>();
      HashSet<Integer> rest = pGraph;
      while (rest != null) {
        rest = extractComponent(rest, pAllNodes, pReachableNodes);
      }
      return stronglyConComp;
    }

    private static HashSet<Integer> extractComponent(
        HashSet<Integer> pInputGraph, Hashtable<Integer, CFANode> pAllNodes,
        Hashtable<Integer, CFANode> pReachableNodes) {
      if (pInputGraph.isEmpty()) { return null; }
      HashSet<Integer> remaining = pInputGraph;
      ConnectedComponent comp = new ConnectedComponent();
      Vector<Integer> toCheck = new Vector<Integer>();
      toCheck.add(pInputGraph.iterator().next());
      remaining.remove(toCheck.get(0));
      CFANode current;
      int succ;
      while (!toCheck.isEmpty()) {
        current = pAllNodes.get(toCheck.remove(0));
        comp.nodes.add(current.getNodeNumber());
        // check all incoming edges if part of component or external
        for (int i = 0; i < current.getNumEnteringEdges(); i++) {
          succ = current.getEnteringEdge(i).getPredecessor().getNodeNumber();
          // check if same component
          if (remaining.contains(succ)) {
            remaining.remove(succ);
            toCheck.add(succ);
          }
          //check if external edge
          if (pReachableNodes.containsKey(succ)) {
            comp.externalEndPoints.add(succ);
          }

        }
        // check all outgoing edges if part of component
        for (int i = 0; i < current.getNumLeavingEdges(); i++) {
          succ = current.getLeavingEdge(i).getSuccessor().getNodeNumber();
          // check if same component
          if (remaining.contains(succ)) {
            remaining.remove(succ);
            toCheck.add(succ);
          }
        }

      }
      stronglyConComp.add(comp);
      return remaining;
    }
  }
}
