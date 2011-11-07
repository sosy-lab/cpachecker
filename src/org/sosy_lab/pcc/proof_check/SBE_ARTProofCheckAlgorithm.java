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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Stack;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTEdge;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;

public class SBE_ARTProofCheckAlgorithm extends ARTProofCheckAlgorithm {

  private Hashtable<Integer, ARTNode> art = new Hashtable<Integer, ARTNode>();
  private FormulaHandler              handler;
  private boolean                     atLoop;
  private boolean                     atFunction;

  public SBE_ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger,
      boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    handler = new FormulaHandler(pConfig, pLogger);
    atLoop = pAlwaysAtLoops;
    atFunction = pAlwaysAtFunctions;
  }

  @Override
  protected PCCCheckResult readNodes(Scanner pScan) {
    // create fast look up for nodes
    Hashtable<Integer, CFANode> nodes = new Hashtable<Integer, CFANode>();
    for (CFANode node : cfaForProof.getAllNodes()) {
      nodes.put(new Integer(node.getNodeNumber()), node);
    }

    root = null;
    boolean rootFound = false;
    int artId, cfaId;
    AbstractionType pAbsType;
    ARTNode newNode;
    CFANode cfaNode;

    // reading nodes
    String next = "";
    while (pScan.hasNext()) {
      next = pScan.next();
      if (next.equals(Separators.nodesFromEdgesSeparator)) {
        break;
      }
      // read next node description
      try {
        artId = Integer.parseInt(next);
        cfaId = pScan.nextInt();
        // get corresponding CFA node
        cfaNode = nodes.get(new Integer(cfaId));
        pAbsType = AbstractionType.valueOf(pScan.next());
        if (pAbsType == AbstractionType.Abstraction) {
          next = pScan.next();
          newNode = new ARTNode(artId, cfaNode, pAbsType, next);
        } else {
          newNode = new ARTNode(artId, cfaNode, pAbsType);
        }
        art.put(new Integer(artId), newNode);
        if (cfaNode != null && cfaForProof.getMainFunction().equals(cfaNode)) {
          if (!rootFound) {
            //check root properties
            if (root.getAbstractionType() != AbstractionType.Abstraction
                || handler.isFalse(root.getAbstraction())) { return PCCCheckResult.InvalidARTRootSpecification; }
            root = newNode;
          } else {
            return PCCCheckResult.AmbigiousRoot;
          }
        }
      } catch (NumberFormatException e1) {
        return PCCCheckResult.UnknownCFANode;
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFANode;
      } catch (NoSuchElementException e3) {
        return PCCCheckResult.UnknownCFANode;
      } catch (IllegalArgumentException e4) {
        return PCCCheckResult.UnknownCFANode;
      }
    }
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target;
    String operation;
    CFAEdge cfaEdge;
    ARTNode nodeS, nodeT;
    ARTEdge edge;

    while (pScan.hasNext()) {
      try {
        // read next edge
        source = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = art.get(new Integer(source));
        nodeT = art.get(new Integer(target));
        if (nodeS != null && nodeT != null) {
          cfaEdge =
              nodeS.getCorrespondingCFANode().getEdgeTo(
                  nodeT.getCorrespondingCFANode());
        } else {
          return PCCCheckResult.UnknownCFAEdge;
        }
        operation = pScan.next();
        // check for correct operation
        String createdOperation = handler.getEdgeOperation(cfaEdge);
        if (createdOperation == null
            || !handler.isSameFormulaWithNormalizedIndices(createdOperation,
                operation)) { return PCCCheckResult.InvalidART; }

        // check for correct abstraction type of target node
        if (operation.length() == 0) {
          if ((atLoop && nodeT.getCorrespondingCFANode().isLoopStart())
              || (atFunction && (nodeT.getCorrespondingCFANode() instanceof CFAFunctionDefinitionNode || nodeT
                  .getCorrespondingCFANode().getLeavingSummaryEdge() != null))) {
            if (nodeT.getAbstractionType() != AbstractionType.Abstraction) { return PCCCheckResult.InvalidART; }
          } else {
            if (nodeT.getAbstractionType() != AbstractionType.NeverAbstraction) { return PCCCheckResult.InvalidART; }
          }
        } else {
          if (nodeT.getAbstractionType() != AbstractionType.Abstraction) { return PCCCheckResult.InvalidART; }
        }
        edge = new ARTEdge(target, cfaEdge, operation);
        nodeS.addEdge(edge);
      } catch (InputMismatchException e2) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e3) {
        return PCCCheckResult.UnknownCFAEdge;
      } catch (IllegalArgumentException e4) {
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult checkProof() {
    HashSet<Integer> visited = new HashSet<Integer>();
    Stack<Pair<Integer, String>> waiting = new Stack<Pair<Integer, String>>();
    //add root
    visited.add(root.getID());
    waiting.push(new Pair<Integer, String>(root.getID(), ""));
    PCCCheckResult intermediateRes;
    Pair<Integer, String> current;
    // check ART
    while (!waiting.isEmpty()) {
      current = waiting.pop();
      intermediateRes =
          checkARTNode(art.get(current.getFirst()), current.getSecond(),
              visited, waiting);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult checkARTNode(ARTNode pNode, String pCallReturnStack,
      HashSet<Integer> pVisited, Stack<Pair<Integer, String>> pWaiting) {
    CFANode cfaNode = pNode.getCorrespondingCFANode();
    String abstraction = pNode.getAbstraction();

    // check if ERROR node
    if (cfaNode instanceof CFALabelNode) {
      if (((CFALabelNode) cfaNode).getLabel().toLowerCase().equals("error")
          && (pNode.getAbstractionType() != AbstractionType.Abstraction || !handler
              .isFalse(abstraction))) { return PCCCheckResult.ErrorNodeReachable; }
    }

    // stop if abstraction is false, no edges allowed
    if (pNode.getAbstractionType() == AbstractionType.Abstraction
        && handler.isFalse(abstraction)) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
    } else {

      // check if all edges of CFA are covered
      // leaving function
      if (cfaNode instanceof CFAFunctionExitNode) {

        //check if end of program reached
        if (pCallReturnStack.length() == 0) {
          if (!cfaForProof.getMainFunction().getExitNode().equals(cfaNode)
              || pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
        } else {

          // return function edge
          // only one return edge allowed, already checked if it is correct edge
          if (pNode.getNumberOfEdges() != 1) { return PCCCheckResult.InvalidART; }
          // check if correct return
          int target = pNode.getEdges()[0].getTarget();
          int returnID =
              art.get(target).getCorrespondingCFANode().getNodeNumber();
          if (returnID != Integer.parseInt(pCallReturnStack.substring(
              pCallReturnStack.lastIndexOf(Separators.stackEntrySeparator) + 1,
              pCallReturnStack.length()))) { return PCCCheckResult.InvalidART; }
          // proof edge
          PCCCheckResult result;
          if (pNode.getAbstractionType() != AbstractionType.NeverAbstraction) {
            if (art.get(target).getAbstractionType() != AbstractionType.NeverAbstraction) {
              result =
                  buildAndCheckFormula(pNode.getAbstraction(),
                      pNode.getEdges()[0].getOperation(), art.get(target)
                          .getAbstraction(),
                      pNode.getEdges()[0].getCorrespondingCFAEdge()
                          .getEdgeType() == CFAEdgeType.AssumeEdge);
            } else {// target node does not contain abstraction -> use target nodes children
              result =
                  buildAndCheckFormulaeForNonAbstractionTarget(pNode,
                      pNode.getEdges()[0]);
            }
            if (result != PCCCheckResult.Success) { return result; }
            addTargetNode(target, pCallReturnStack.substring(0,
                pCallReturnStack.lastIndexOf(Separators.stackEntrySeparator)),
                pVisited, pWaiting);
          }
        }
      } else {

        // check other edges
        ARTEdge[] edges = pNode.getEdges();
        //check if all edges are covered exactly once
        if (edges.length != cfaNode.getNumLeavingEdges()
            || !containsCFAEdges(edges, cfaNode)) { return PCCCheckResult.InvalidART; }
        // formula only needs to be checked if location has abstraction, otherwise already checked
        if (pNode.getAbstractionType() != AbstractionType.NeverAbstraction) {
          PCCCheckResult intermediateRes;
          // check formula for all edges and add target node to visit if necessary
          for (int i = 0; i < edges.length; i++) {
            // target node does not contain abstraction -> use target nodes children
            if (art.get(edges[i].getTarget()).getAbstractionType() == AbstractionType.NeverAbstraction) {
              intermediateRes =
                  buildAndCheckFormulaeForNonAbstractionTarget(pNode, edges[i]);
            } else {
              intermediateRes =
                  buildAndCheckFormula(pNode.getAbstraction(),
                      edges[i].getOperation(), art.get(edges[i].getTarget())
                          .getAbstraction(), edges[i].getCorrespondingCFAEdge()
                          .getEdgeType() == CFAEdgeType.AssumeEdge);
            }
            if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
            //add target ART element if not checked yet
            if (edges[i].getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.FunctionCallEdge) {
              addTargetNode(edges[i].getTarget(), pCallReturnStack
                  + Separators.stackEntrySeparator
                  + cfaNode.getLeavingSummaryEdge().getSuccessor()
                      .getNodeNumber(), pVisited, pWaiting);
            } else {
              addTargetNode(edges[i].getTarget(), pCallReturnStack, pVisited,
                  pWaiting);
            }
          }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult buildAndCheckFormulaeForNonAbstractionTarget(
      ARTNode pSource, ARTEdge pEdge) {

    ARTNode target = art.get(pEdge.getTarget());
    ArrayList<ARTEdge[]> toCheck = new ArrayList<ARTEdge[]>();
    if (target.getNumberOfEdges() > 0) {
      toCheck.add(target.getEdges());
    }

    ArrayList<Pair<ARTNode, ARTEdge>> targets =
        new ArrayList<Pair<ARTNode, ARTEdge>>();
    ARTEdge[] edges;

    // insert all target nodes and their operation
    while (!toCheck.isEmpty()) {
      edges = toCheck.remove(0);
      for (int i = 0; i < edges.length; i++) {
        target = art.get(edges[i].getTarget());
        if (target.getAbstractionType() == AbstractionType.NeverAbstraction) {
          if (target.getNumberOfEdges() > 0) {
            toCheck.add(target.getEdges());
          }
        } else {
          targets.add(new Pair<ARTNode, ARTEdge>(target, edges[i]));
        }
      }
    }
    // check all targets
    PCCCheckResult intermediateRes;
    Pair<ARTNode, ARTEdge> current;
    for (int i = 0; i < targets.size(); i++) {
      current = targets.get(i);
      intermediateRes =
          buildAndCheckFormula(
              pSource.getAbstraction(),
              current.getSecond().getOperation(),
              current.getFirst().getAbstraction(),
              current.getSecond().getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.AssumeEdge);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  private void addTargetNode(int pTarget, String pCallReturnStack,
      HashSet<Integer> pVisited, Stack<Pair<Integer, String>> pWaiting) {
    if (!pVisited.contains(pTarget)) {
      pVisited.add(pTarget);
      pWaiting.push(new Pair<Integer, String>(pTarget, pCallReturnStack));
    }
  }

  private boolean containsCFAEdges(ARTEdge[] pEdges, CFANode pCFA) {
    CFAEdge edge;
    boolean found;
    for (int i = 0; i < pCFA.getNumLeavingEdges(); i++) {
      edge = pCFA.getLeavingEdge(i);
      found = false;
      for (int j = 0; j < pEdges.length; j++) {
        if (pEdges[j].getCorrespondingCFAEdge().equals(edge)) {
          found = true;
          break;
        }
      }
      if (!found) { return false; }
    }
    return true;
  }

  private PCCCheckResult buildAndCheckFormula(String pAbstractionLeft,
      String pOperation, String pAbstractionRight, boolean pAssume) {
    // check if operation fits to left abstraction
    if (handler.operationFitsToLeftAbstraction(pAbstractionLeft, pOperation,
        pAssume)) { return PCCCheckResult.InvalidEdge; }
    // check if right abstraction fits to left abstraction and operation
    if (handler.rightAbstractionFitsToOperationAndLeftAbstraction(
        pAbstractionLeft, pOperation, pAbstractionRight)) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    Formula f =
        handler.buildEdgeInvariant(pAbstractionLeft, pOperation,
            pAbstractionRight);
    if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
    if (f.isFalse()) {
      return PCCCheckResult.Success;
    } else {
      return PCCCheckResult.InvalidART;
    }
  }

}