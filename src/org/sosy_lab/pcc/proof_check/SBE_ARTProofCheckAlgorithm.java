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
import java.util.Hashtable;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;
import org.sosy_lab.pcc.common.WithCorrespondingCFAEdgeARTEdge;

public abstract class SBE_ARTProofCheckAlgorithm extends ARTProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected boolean atLoop;
  protected boolean atFunction;
  protected Hashtable<Integer, ARTNode> art = new Hashtable<Integer, ARTNode>();

  public SBE_ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType,
      boolean pAlwaysAtLoops, boolean pAlwaysAtFunctions)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    handler = new FormulaHandler(pConfig, pLogger, pProverType);
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
        logger.log(Level.INFO, "Read next ART node from file.");
        artId = Integer.parseInt(next);
        cfaId = pScan.nextInt();
        // get corresponding CFA node
        cfaNode = nodes.get(new Integer(cfaId));
        if (cfaNode == null) { return PCCCheckResult.UnknownCFANode; }
        pAbsType = AbstractionType.valueOf(pScan.next());
        if (pAbsType == AbstractionType.Abstraction) {
          next = pScan.next();
          if (!checkAbstraction(next)) {
            logger.log(Level.SEVERE, "Wrong abstraction: " + next + " .");
            return PCCCheckResult.InvalidInvariant;
          }
          newNode = new ARTNode(artId, cfaNode, pAbsType, next, true);
        } else {
          newNode = new ARTNode(artId, cfaNode, pAbsType, true);
        }

        if (art.containsKey(artId)) { return PCCCheckResult.ElementAlreadyRead; }
        art.put(new Integer(artId), newNode);
        if (cfaForProof.getMainFunction().equals(cfaNode)) {
          if (!rootFound) {
            // set root
            root = newNode;
            rootFound=true;
            //check root properties
            if (root.getAbstractionType() != AbstractionType.Abstraction
                || (!(handler.createFormula(root.getAbstraction())).isTrue())) {
              logger.log(Level.SEVERE, "Wrong root specification: " + root);
              return PCCCheckResult.InvalidARTRootSpecification;
            }
          } else {
            logger.log(Level.SEVERE, "Ambigious root specification: " + root.getID() + " and " + newNode.getID());
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
    if(root==null){return PCCCheckResult.UncoveredCFANode;}
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult checkProof() {
    Hashtable<Integer, String> visited = new Hashtable<Integer, String>();
    Stack<Pair<Integer, String>> waiting = new Stack<Pair<Integer, String>>();
    //add root
    visited.put(root.getID(), "");
    waiting.push(new Pair<Integer, String>(root.getID(), ""));
    PCCCheckResult intermediateRes;
    Pair<Integer, String> current;
    // check ART
    while (!waiting.isEmpty()) {
      current = waiting.pop();
      logger.log(Level.INFO, "Check ART node " + current.getFirst() + " .");
      intermediateRes =
          checkARTNode(art.get(current.getFirst()), current.getSecond(),
              visited, waiting);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult checkARTNode(ARTNode pNode, String pCallReturnStack,
      Hashtable<Integer, String> pVisited, Stack<Pair<Integer, String>> pWaiting) {
    CFANode cfaNode = pNode.getCorrespondingCFANode();
    String abstraction = pNode.getAbstraction();

    // check if ERROR node
    if (cfaNode instanceof CFALabelNode) {
      if (((CFALabelNode) cfaNode).getLabel().toLowerCase().equals("error")
          && (pNode.getAbstractionType() != AbstractionType.Abstraction || !handler
              .isFalse(abstraction))) {
        if (pNode.getAbstractionType() != AbstractionType.Abstraction) {
          logger.log(Level.SEVERE, "No abstraction for error node " + pNode.getID() + " .");
        }
        else {
          logger.log(Level.SEVERE, "Abstraction for error node " + pNode.getID() + " is not false");
        }
        return PCCCheckResult.ErrorNodeReachable;
      }
    }

    // stop if abstraction is false, no edges allowed
    if (pNode.getAbstractionType() == AbstractionType.Abstraction
        && handler.isFalse(abstraction)) {
      if (pNode.getNumberOfEdges() != 0) {
        logger.log(Level.SEVERE, "ART node " + pNode.getID() + " has edges although its abstraction is false.");
        return PCCCheckResult.InvalidART;
      }
    } else {

      // check if all edges of CFA are covered
      // leaving function
      if (cfaNode instanceof CFAFunctionExitNode) {

        //check if end of program reached
        if (pCallReturnStack.length() == 0) {
          if (!cfaForProof.getMainFunction().getExitNode().equals(cfaNode)
              || pNode.getNumberOfEdges() != 0) {
            if (pNode.getNumberOfEdges() != 0) {
              logger.log(Level.SEVERE, "Outgoing ART edges although the program should be finished.");
            } else {
              logger.log(Level.SEVERE, "Invalid callstack used for ART.");
            }
            return PCCCheckResult.InvalidART;
          }
        } else {

          // return function edge
          // only one return edge allowed, already checked if it is correct edge
          if (pNode.getNumberOfEdges() != 1) {
            logger.log(Level.SEVERE, " Too many edges. Only one edge possible if function is exited.");
            return PCCCheckResult.InvalidART;
          }
          // check if correct return
          int target = pNode.getEdges()[0].getTarget();
          int returnID =
              art.get(target).getCorrespondingCFANode().getNodeNumber();
          if (returnID != Integer.parseInt(pCallReturnStack.substring(
              pCallReturnStack.lastIndexOf(Separators.stackEntrySeparator) + 1,
              pCallReturnStack.length()))) {
            logger.log(Level.SEVERE,
                "Invalid callstack used in ART. The target node does not fit to the callstack return address.");
            return PCCCheckResult.InvalidART;
          }
          // proof edge
          PCCCheckResult result;
          if (pNode.getAbstractionType() != AbstractionType.NeverAbstraction) {
            logger.log(Level.INFO, "Check if feasible abstraction.");
            if (art.get(target).getAbstractionType() != AbstractionType.NeverAbstraction) {
              result =
                  checkEdgeFormula(pNode, (WithCorrespondingCFAEdgeARTEdge) pNode.getEdges()[0], art.get(target));

            } else {// target node does not contain abstraction -> use target nodes children
              result =
                  checkEdgeFormulaeForNonAbstractionTarget(pNode,
                      (WithCorrespondingCFAEdgeARTEdge) pNode.getEdges()[0]);
            }
            if (result != PCCCheckResult.Success) { return result; }
          }
          result = addTargetNode(
              target,
              pCallReturnStack.substring(0,
                  pCallReturnStack.lastIndexOf(Separators.stackEntrySeparator)),
              pVisited, pWaiting);
          if (result != PCCCheckResult.Success) { return result; }
        }
      } else {

        // check other edges
        WithCorrespondingCFAEdgeARTEdge[] edges = (WithCorrespondingCFAEdgeARTEdge[]) pNode.getEdges();
        //check if all edges are covered exactly once
        logger.log(Level.INFO, "Check if all CFA edges of the corresponding CFA node are covered by ART node");
        if (edges.length != cfaNode.getNumLeavingEdges()
            || !containsCFAEdges(edges, cfaNode)) { return PCCCheckResult.InvalidART; }
        PCCCheckResult intermediateRes;
        // check formula for all edges and add target node to visit if necessary
        for (int i = 0; i < edges.length; i++) {
          // formula only needs to be checked if location has abstraction, otherwise already checked
          if (pNode.getAbstractionType() != AbstractionType.NeverAbstraction) {
            logger.log(Level.INFO, "Check if feasible abstraction.");
            // target node does not contain abstraction -> use target nodes children
            if (art.get(edges[i].getTarget()).getAbstractionType() == AbstractionType.NeverAbstraction) {
              intermediateRes =
                  checkEdgeFormulaeForNonAbstractionTarget(pNode, edges[i]);
            } else {
              intermediateRes =
                  checkEdgeFormula(pNode, edges[i],
                      art.get(edges[i].getTarget()));
            }
            if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
          }
          //add target ART element if not checked yet
          if (edges[i].getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            intermediateRes = addTargetNode(edges[i].getTarget(), pCallReturnStack
                + Separators.stackEntrySeparator
                + cfaNode.getLeavingSummaryEdge().getSuccessor()
                    .getNodeNumber(), pVisited, pWaiting);
          } else {
            intermediateRes = addTargetNode(edges[i].getTarget(), pCallReturnStack, pVisited,
                pWaiting);
          }
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult addTargetNode(int pTarget, String pCallReturnStack,
      Hashtable<Integer, String> pVisited, Stack<Pair<Integer, String>> pWaiting) {
    if (!pVisited.containsKey(pTarget)) {
      pVisited.put(pTarget, pCallReturnStack);
      pWaiting.push(new Pair<Integer, String>(pTarget, pCallReturnStack));
    } else {
      if (pVisited.get(pTarget) == null || !pVisited.get(pTarget).equals(pCallReturnStack)) { return PCCCheckResult.InvalidART; }
    }
    return PCCCheckResult.Success;
  }

  private boolean containsCFAEdges(WithCorrespondingCFAEdgeARTEdge[] pEdges, CFANode pCFA) {
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
      if (!found) {
        logger.log(Level.SEVERE, "CFA edge " + pCFA.getNodeNumber() + "->" + (edge.getSuccessor()).getNodeNumber()
            + " not covered.");
        return false;
      }
    }
    return true;
  }

  private PCCCheckResult checkEdgeFormulaeForNonAbstractionTarget(
      ARTNode pSource, WithCorrespondingCFAEdgeARTEdge pEdge) {

    ARTNode target = art.get(pEdge.getTarget());
    ArrayList<WithCorrespondingCFAEdgeARTEdge[]> toCheck = new ArrayList<WithCorrespondingCFAEdgeARTEdge[]>();
    if (target.getNumberOfEdges() > 0) {
      toCheck.add((WithCorrespondingCFAEdgeARTEdge[]) target.getEdges());
    }

    ArrayList<Pair<ARTNode, WithCorrespondingCFAEdgeARTEdge>> targets =
        new ArrayList<Pair<ARTNode, WithCorrespondingCFAEdgeARTEdge>>();
    WithCorrespondingCFAEdgeARTEdge[] edges;

    // insert all target nodes and their operation
    while (!toCheck.isEmpty()) {
      edges = toCheck.remove(0);
      for (int i = 0; i < edges.length; i++) {
        target = art.get(edges[i].getTarget());
        if (target.getAbstractionType() == AbstractionType.NeverAbstraction) {
          if (target.getNumberOfEdges() > 0) {
            toCheck.add((WithCorrespondingCFAEdgeARTEdge[]) target.getEdges());
          }
        } else {
          targets.add(new Pair<ARTNode, WithCorrespondingCFAEdgeARTEdge>(target, edges[i]));
        }
      }
    }
    // check all targets
    PCCCheckResult intermediateRes;
    Pair<ARTNode, WithCorrespondingCFAEdgeARTEdge> current;
    for (int i = 0; i < targets.size(); i++) {
      current = targets.get(i);
      intermediateRes =
          checkEdgeFormula(pSource, current.getSecond(), current.getFirst());
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  protected PCCCheckResult checkTargetAbstractionType(ARTNode pTarget,
      String pOperation) {
    // check for correct abstraction type of target node
    if (pOperation.length() == 0) {
      if ((atLoop && pTarget.getCorrespondingCFANode().isLoopStart())
          || (atFunction && (pTarget.getCorrespondingCFANode() instanceof CFAFunctionDefinitionNode || pTarget
              .getCorrespondingCFANode().getEnteringSummaryEdge() != null))) {
        if (pTarget.getAbstractionType() != AbstractionType.Abstraction) { return PCCCheckResult.InvalidART; }
      }
    } else {
      if (pTarget.getAbstractionType() != AbstractionType.Abstraction) { return PCCCheckResult.InvalidART; }
    }
    return PCCCheckResult.Success;
  }

  protected abstract boolean checkAbstraction(String pAbstraction);

  protected abstract PCCCheckResult checkEdgeFormula(ARTNode pSource,
      WithCorrespondingCFAEdgeARTEdge pEdge, ARTNode pTarget);

}
