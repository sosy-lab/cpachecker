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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

public class SBE_InvariantProofCheckAlgorithm extends
    InvariantProofCheckAlgorithm {

  private boolean                                          atLoop;
  private boolean                                          atFunction;
  private FormulaHandler                                   handler;
  private Hashtable<Integer, Vector<Pair<Formula, int[]>>> nodes             =
                                                                                 new Hashtable<Integer, Vector<Pair<Formula, int[]>>>();
  private Hashtable<String, Formula[]>                     edges             =
                                                                                 new Hashtable<String, Formula[]>();
  private Hashtable<Integer, CFANode>                      reachableCFANodes =
                                                                                 new Hashtable<Integer, CFANode>();

  public SBE_InvariantProofCheckAlgorithm(Configuration pConfig,
      LogManager pLogger, boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    atFunction = pAlwaysAtFunctions;
    atLoop = pAlwaysAtLoops;
    handler = new FormulaHandler(pConfig, pLogger);
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target, sourceEdge, numOps;
    CFANode nodeS, nodeT, nodeSourceOpEdge;
    String op;
    PCCCheckResult intermediateRes;
    Formula[] operations;
    while (pScan.hasNext()) {
      try {
        // read next edge
        source = pScan.nextInt();
        sourceEdge = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = reachableCFANodes.get(source);
        nodeSourceOpEdge = reachableCFANodes.get(sourceEdge);
        nodeT = reachableCFANodes.get(target);
        if (nodeS == null || nodeSourceOpEdge == null || nodeT == null) { return PCCCheckResult.UnknownCFAEdge; }
        // get all operations with respective SSA indices
        numOps = pScan.nextInt();
        if (numOps < 1) { return PCCCheckResult.UnknownCFAEdge; }
        operations = new Formula[numOps];
        for (int i = 0; i < numOps; i++) {
          op = pScan.next();
          if (op.length() == 0) {
            operations[i] = null;
          } else {
            operations[i] = handler.createFormula(op);
          }
        }
        //check edge
        intermediateRes =
            structuralCheckEdge(nodeS, nodeSourceOpEdge, nodeT, operations);
        if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
        //add edge
        edges.put(source + Separators.commonSeparator + target, operations);
      } catch (IllegalArgumentException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e1) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    return structuralCheckCoverageOfCFAEdges();
  }

  @Override
  protected PCCCheckResult readNodes(Scanner pScan) {
    // set up look up for CFA nodes
    Vector<CFANode> cfaNodes = new Vector<CFANode>();
    Hashtable<Integer, CFANode> visited = new Hashtable<Integer, CFANode>();
    CFANode current, child;
    int nextIndex = 0;
    cfaNodes.add(cfaForProof.getMainFunction());
    while (nextIndex >= cfaNodes.size()) {
      current = cfaNodes.get(nextIndex);
      nextIndex++;
      visited.put(current.getNodeNumber(), current);
      // add children
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        child = current.getLeavingEdge(i).getSuccessor();
        if (!visited.containsKey(child.getNodeNumber())) {
          cfaNodes.add(child);
        }
      }
    }
    // reading nodes
    cfaNodes = null;
    String next = "";
    String[] subStr;
    int readNode, numInvariants;
    Vector<Pair<Formula, int[]>> invariants;
    Formula invariant;
    int[] stack;
    try {
      while (pScan.hasNext()) {
        next = pScan.next();
        if (next.equals("}")) {
          break;
        }
        // read node
        readNode = Integer.parseInt(next);
        if (visited.containsKey(readNode)) {
          current = visited.get(readNode);
          // check if node is abstraction node
          if (isNonAbstractionNode(current)) { return PCCCheckResult.UnknownCFANode; }
          reachableCFANodes.put(readNode, current);
        } else {
          return PCCCheckResult.InvalidCFANode;
        }
        boolean isRoot = cfaForProof.getMainFunction().equals(current);
        boolean isError = false;
        if (current instanceof CFALabelNode) {
          isError =
              ((CFALabelNode) current).getLabel().toLowerCase().equals("error");
        }
        // read invariants
        invariants = new Vector<Pair<Formula, int[]>>();
        numInvariants = pScan.nextInt();
        for (int i = 0; i < numInvariants; i++) {
          // read invariant
          next = pScan.next();
          subStr = next.split("?");
          if (subStr.length < 1) { return PCCCheckResult.InvalidInvariant; }
          // get formula
          try {
            invariant = handler.createFormula(subStr[0]);
            if (isRoot) {
              if (!invariant.isTrue()) { return PCCCheckResult.InvalidARTRootSpecification; }
            }
            if (isError) {
              if (!invariant.isFalse()) { return PCCCheckResult.ErrorNodeReachable; }
            }
          } catch (IllegalArgumentException e1) {
            return PCCCheckResult.InvalidInvariant;
          }
          // get stack
          stack = new int[subStr.length - 1];
          try {
            for (int j = 0; i < stack.length; j++) {
              stack[j] = Integer.parseInt(subStr[j + 1]);
            }
          } catch (NumberFormatException e1) {
            return PCCCheckResult.InvalidStack;
          }
          // add invariant
          invariants.add(new Pair<Formula, int[]>(invariant, stack));
        }
        // check root node
        if (cfaForProof.getMainFunction().equals(current)) {

        }
        nodes.put(readNode, invariants);
      }
    } catch (NumberFormatException e1) {
      return PCCCheckResult.UnknownCFANode;
    } catch (InputMismatchException e2) {
      return PCCCheckResult.UnknownCFANode;
    } catch (NoSuchElementException e3) {
      return PCCCheckResult.UnknownCFANode;
    }
    // checking nodes
    return structuralCheckCoverageOfCFANodes(visited);
  }

  private PCCCheckResult structuralCheckCoverageOfCFANodes(
      Hashtable<Integer, CFANode> pAllNodes) {
    //check if same size
    if (pAllNodes.size() == reachableCFANodes.size()) { return PCCCheckResult.Success; }
    // get uncovered nodes
    HashSet<Integer> uncovered = new HashSet<Integer>();
    for (Integer nodeID : pAllNodes.keySet()) {
      if (!reachableCFANodes.containsKey(nodeID)) {
        uncovered.add(nodeID);
      }
    }
    // build strongly connected components for uncovered nodes
    Vector<StronglyConnectedComponent> comps =
        StronglyConnectedComponent.extractStronglyConnectedComponents(
            uncovered, pAllNodes, reachableCFANodes);
    // check if all external edges lead to only false abstraction or all nodes of component are non-abstraction nodes
    boolean result = true;
    for (int i = 0; result && i < comps.size(); i++) {
      // check external edges
      for (Integer external : comps.get(i).externalEndPoints) {
        result = result && allInvariantFormulaeFalse(external);
      }
      //check if only non-abstraction nodes
      if (!result) {
        for (Integer id : comps.get(i).nodes) {
          result = result && isNonAbstractionNode(pAllNodes.get(id));
        }
      }
    }
    if (!result) { return PCCCheckResult.UncoveredCFANode; }
    return PCCCheckResult.Success;
  }

  private boolean allInvariantFormulaeFalse(Integer pNode) {
    Vector<Pair<Formula, int[]>> formulae = nodes.get(pNode);
    for (int i = 0; i < formulae.size(); i++) {
      if (!formulae.get(i).getFirst().isFalse()) { return false; }
    }
    return true;
  }

  private boolean isNonAbstractionNode(CFANode pNode) {
    // check if kind of node requires abstraction
    if ((pNode instanceof CFAFunctionDefinitionNode)
        || (pNode instanceof CFAFunctionExitNode)
        || pNode.getEnteringSummaryEdge() != null
        || (atLoop && pNode.isLoopStart())) { return false; }
    //check if all entering edges have empty operation
    for (int i = 0; i < pNode.getNumEnteringEdges(); i++) {
      if (handler.getEdgeOperation(pNode.getEnteringEdge(i)).length() != 0) { return false; }

    }
    return true;
  }

  private PCCCheckResult structuralCheckEdge(CFANode pSource,
      CFANode pEdgeSource, CFANode pTarget, Formula[] operations) {
    CFAEdge edge = retrieveOperationEdge(pSource, pEdgeSource, pTarget);
    if (edge == null) { return PCCCheckResult.InvalidEdge; }
    // get operation formula
    String builtOp = handler.getEdgeOperation(edge);
    //check operations
    for (int i = 0; i < operations.length; i++) {
      if (operations[i] == null) {
        if (!handler.isSameFormulaWithoutSSAIndicies("", builtOp)) { return PCCCheckResult.InvalidOperation; }
      } else {
        if (!handler.isSameFormulaWithoutSSAIndicies(operations[i].toString(),
            builtOp)) { return PCCCheckResult.InvalidOperation; }
      }
    }
    return null;
  }

  private CFAEdge retrieveOperationEdge(CFANode pSource, CFANode pSourceEdge,
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
    return null;
  }

  private boolean isEndpoint(CFAEdge pEdge) {
    CFANode succ = pEdge.getSuccessor();
    if (succ instanceof CFAFunctionDefinitionNode
        || succ instanceof CFAFunctionExitNode
        || succ.getEnteringSummaryEdge() != null
        || (atLoop && succ.isLoopStart())) { return true; }
    if (handler.getEdgeOperation(pEdge).length() == 0) { return false; }
    return true;
  }

  private PCCCheckResult structuralCheckCoverageOfCFAEdges() {
    //build all edge identifications for reachable source nodes and test if they are in edges
    HashSet<Integer> visited = new HashSet<Integer>();
    Vector<CFANode> toVisit = new Vector<CFANode>();
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
        // if it is an abstraction node build and check edge
        if (isSource) {
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
    String id;
    if (isEndpoint(pEdge)) {
      id =
          new String(pNodeNumber + Separators.commonSeparator + pNodeNumber
              + Separators.commonSeparator
              + pEdge.getSuccessor().getNodeNumber());
      if (edges.containsKey(id)) {
        return PCCCheckResult.Success;
      } else {
        return PCCCheckResult.UncoveredEdge;
      }
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
          if (!edges.containsKey(id)) { return PCCCheckResult.UncoveredEdge; }
        } else {
          if (!visited.contains(succ.getNodeNumber())) {
            visited.add(succ.getNodeNumber());
            toVisit.add(succ);
          }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult checkProof() {
    // iterate over all edges
    int source, target;
    Vector<Pair<Formula, int[]>> invariantS, invariantT;
    Formula edgeFormula;
    for (String edge : edges.keySet()) {
      source = Integer.parseInt(edge.substring(edge.indexOf("#")));
      target =
          Integer
              .parseInt(edge.substring(edge.lastIndexOf("#"), edge.length()));
      invariantS = nodes.get(source);
      invariantT = nodes.get(target);
      // build edge formula
      edgeFormula = handler.buildConjunction(edges.get(edge));

      // iterate over all source predicates

      // add stack operation to edge formula

      if (edgeFormula == null) {
        // TODO return PCCCheckResult.
      }
      //TODO
    }
    // TODO Auto-generated method stub
    return null;
  }

  private Formula addStackOperation(Formula pOperationFormula, int[] pStack,
      boolean pFunctionCall, boolean pFunctionReturn) {
    if (pFunctionCall && pFunctionReturn) { return null; }
    // TODO
    return null;
  }

  private Formula addStackInvariant(Formula pInvariant, int[] pStack,
      boolean pLeft) {
    // TODO
    return null;
  }

  private static class StronglyConnectedComponent {
    private HashSet<Integer>                          nodes             =
                                                                            new HashSet<Integer>();
    private Vector<Integer>                           externalEndPoints =
                                                                            new Vector<Integer>();
    private static Vector<StronglyConnectedComponent> stronglyConComp;

    private StronglyConnectedComponent() {

    }

    public static Vector<StronglyConnectedComponent> extractStronglyConnectedComponents(
        HashSet<Integer> pGraph, Hashtable<Integer, CFANode> pAllNodes,
        Hashtable<Integer, CFANode> pReachableNodes) {
      stronglyConComp =
          new Vector<SBE_InvariantProofCheckAlgorithm.StronglyConnectedComponent>();
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
      StronglyConnectedComponent comp = new StronglyConnectedComponent();
      Vector<Integer> toCheck = new Vector<Integer>();
      toCheck.add(pInputGraph.iterator().next());
      CFANode current;
      int succ;
      while (!toCheck.isEmpty()) {
        current = pAllNodes.get(toCheck.remove(0));
        // check all incoming edges if part of component or external
        for (int i = 0; i < current.getNumEnteringEdges(); i++) {
          succ = current.getEnteringEdge(i).getSuccessor().getNodeNumber();
          // check if same component
          if (remaining.contains(succ)) {
            remaining.remove(succ);
            comp.nodes.add(succ);
            toCheck.add(succ);
          }
          //check if external edge
          if (pReachableNodes.containsKey(succ)) {
            comp.externalEndPoints.add(succ);
          }

        }
        // check all outgoing edges if part of component
        for (int i = 0; i < current.getNumLeavingEdges(); i++) {
          succ = current.getEnteringEdge(i).getSuccessor().getNodeNumber();
          // check if same component
          if (remaining.contains(succ)) {
            remaining.remove(succ);
            comp.nodes.add(succ);
            toCheck.add(succ);
          }
        }

      }
      stronglyConComp.add(comp);
      return remaining;
    }
  }

}
