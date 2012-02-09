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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.CoveredARTNode;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.FourTuple;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;
import org.sosy_lab.pcc.common.WithCorrespondingCFAEdgeARTEdge;


public class ABE_ARTProofCheckAlgorithm extends ARTProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected boolean atLoop;
  protected boolean atFunction;
  int threshold;

  protected Hashtable<Integer, ARTNode> art = new Hashtable<Integer, ARTNode>();

  public ABE_ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType,
      boolean pAlwaysAtFunctions, int pThreshold) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    handler = new FormulaHandler(pConfig, pLogger, pProverType);
    atLoop = true;
    atFunction = pAlwaysAtFunctions;
    threshold = pThreshold;
    if (threshold < 0) { throw new IllegalArgumentException("Invalid threshold specification."); }
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
          if (pAbsType == AbstractionType.CoveredNonAbstraction) {
            newNode = new CoveredARTNode(artId, cfaNode, pAbsType, pScan.nextInt(), false);
          }
          newNode = new ARTNode(artId, cfaNode, pAbsType, true);
        }

        if (art.containsKey(artId)) { return PCCCheckResult.ElementAlreadyRead; }
        art.put(new Integer(artId), newNode);
        if (cfaForProof.getMainFunction().equals(cfaNode)) {
          if (!rootFound) {
            // set root
            root = newNode;
            rootFound = true;
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
    if(root == null){return PCCCheckResult.UncoveredCFANode;}
    return PCCCheckResult.Success;
  }

  private boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target;
    CFAEdge cfaEdge;
    ARTNode nodeS, nodeT;
    WithCorrespondingCFAEdgeARTEdge edge;

    while (pScan.hasNext()) {
      try {
        // read next edge
        logger.log(Level.INFO, "Read next edge");
        source = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = art.get(new Integer(source));
        nodeT = art.get(new Integer(target));
        if (nodeS != null && nodeT != null) {
          cfaEdge =
              nodeS.getCorrespondingCFANode().getEdgeTo(
                  nodeT.getCorrespondingCFANode());
        } else {
          logger.log(Level.SEVERE, "Edge " + source + "#" + target + " has no corresponding CFA edge.");
          return PCCCheckResult.UnknownCFAEdge;
        }

        // add edge
        edge = new WithCorrespondingCFAEdgeARTEdge(target, cfaEdge);
        if (nodeS.isEdgeContained(edge)) {
          //return PCCCheckResult.ElementAlreadyRead;
        } else {
          nodeS.addEdge(edge);
        }
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
    Hashtable<String, String> visited = new Hashtable<String, String>();
    Stack<FourTuple<Integer, String, Formula, PathFormula>> waiting =
        new Stack<FourTuple<Integer, String, Formula, PathFormula>>();
    //add root
    visited.put(Integer.toString(root.getID()), "");
    // create abstraction for root
    Pair<Formula, PathFormula> pair = buildNewPredicateAbstraction(root, null, null);
    if (pair == null || pair.getFirst() == null || pair.getSecond() == null) { return PCCCheckResult.InvalidART; }
    waiting.push(new FourTuple<Integer, String, Formula, PathFormula>(root.getID(), "", pair.getFirst(), pair
        .getSecond()));
    PCCCheckResult intermediateRes;
    FourTuple<Integer, String, Formula, PathFormula> current;
    // check ART
    while (!waiting.isEmpty()) {
      current = waiting.pop();
      logger.log(Level.INFO, "Check ART node " + current.getFirst() + " .");
      intermediateRes =
          checkARTNode(art.get(current.getFirst()), current.getSecond(), current.getThird(), current.getFourth(),
              visited, waiting);
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult checkARTNode(ARTNode pNode, String pStack, Formula pLeftAbstraction, PathFormula pPath,
      Hashtable<String, String> pVisited,
      Stack<FourTuple<Integer, String, Formula, PathFormula>> pWaiting) {
    CFANode cfaNode = pNode.getCorrespondingCFANode();
    Formula[] fList;
    Formula f;
    PathFormula pf;
    PCCCheckResult intermediateRes;
    ARTNode targetARTNode;
    Pair<Formula, PathFormula> newPred;
    // check ART node
    // check error node
    if (cfaNode instanceof CFALabelNode
        && ((CFALabelNode) cfaNode).getLabel().toLowerCase().equals("error")) {
      fList = new Formula[2];
      fList[0] = pLeftAbstraction;
      fList[1] = pPath.getFormula();
      f = handler.buildConjunction(fList);
      if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      if (!handler.isFalse(f)) { return PCCCheckResult.ErrorNodeReachable; }
    }
    // check false abstractions
    if (handler.isFalse(pNode.getAbstraction())) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
    }
    // check program end
    if (cfaForProof.getMainFunction().getExitNode().equals(cfaNode)) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.ART_CFA_Mismatch; }
      if (!pStack.equals("")) { return PCCCheckResult.InvalidStack; }
    }
    // check covered element
    if (pNode instanceof CoveredARTNode) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
      newPred = buildNewPredicateAbstraction(art.get(pNode), pLeftAbstraction, pPath);
      if (newPred == null || newPred.getFirst() == null || newPred.getSecond() == null) { return PCCCheckResult.InvalidART; }
      // add covering element
      intermediateRes =
          addTargetNode(art.get(((CoveredARTNode) pNode).getCoveringElement()), pStack, newPred.getFirst(),
              newPred.getSecond(), pVisited, pWaiting);
      return intermediateRes;
    } else {
      // check if all edges of CFA are covered
      // leaving function
      if (cfaNode instanceof CFAFunctionExitNode) {
        //check if end of program reached
        if (pStack.length() == 0) {
          if (!cfaForProof.getMainFunction().getExitNode().equals(cfaNode)) {
            logger.log(Level.SEVERE, "Invalid callstack used for ART.");
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
          targetARTNode = art.get(target);
          int returnID =
              targetARTNode.getCorrespondingCFANode().getNodeNumber();
          if (returnID != Integer.parseInt(pStack.substring(
              pStack.lastIndexOf(Separators.stackEntrySeparator) + 1,
              pStack.length()))) {
            logger.log(Level.SEVERE,
                "Invalid callstack used in ART. The target node does not fit to the callstack return address.");
            return PCCCheckResult.InvalidART;
          }
          // build path formula extension
          pf =
              handler.extendPath(pPath,
                  ((WithCorrespondingCFAEdgeARTEdge) pNode.getEdges()[0]).getCorrespondingCFAEdge());
          if (pf == null || pf.getFormula() == null) { return PCCCheckResult.InvalidART; }
          // check edge, correct edge, correct target abstraction
          intermediateRes = checkARTEdge(targetARTNode, pLeftAbstraction, pf);
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
          // build new predicate abstraction for target
          newPred = buildNewPredicateAbstraction(targetARTNode, pLeftAbstraction, pf);
          if (newPred == null || newPred.getFirst() == null || newPred.getSecond() == null) { return PCCCheckResult.InvalidART; }
          // add target node for visit
          intermediateRes = addTargetNode(
              targetARTNode,
              pStack.substring(0,
                  pStack.lastIndexOf(Separators.stackEntrySeparator)), newPred.getFirst(), newPred.getSecond(),
              pVisited, pWaiting);
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
        }
      } else {
        // check other edges
        WithCorrespondingCFAEdgeARTEdge[] edges = (WithCorrespondingCFAEdgeARTEdge[]) pNode.getEdges();
        fList = new Formula[2];
        fList[0] = pLeftAbstraction;
        fList[1] = pPath.getFormula();
        f = handler.buildConjunction(fList);
        if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
        //check if all edges are covered exactly once if node reachable
        logger.log(Level.INFO, "Check if all CFA edges of the corresponding CFA node are covered by ART node");
        if (!handler.isFalse(f) && (edges.length <cfaNode.getNumLeavingEdges()
            || !containsCFAEdges(edges, cfaNode))) { return PCCCheckResult.InvalidART; }
        // check formula for all edges and add target node to visit if necessary
        for (int i = 0; i < edges.length; i++) {
          targetARTNode = art.get(edges[i].getTarget());
          // build path formula extension
          pf = handler.extendPath(pPath, edges[i].getCorrespondingCFAEdge());
          if (pf == null || pf.getFormula() == null) { return PCCCheckResult.InvalidART; }
          // check edge
          intermediateRes = checkARTEdge(targetARTNode, pLeftAbstraction, pf);
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
          // get new predicate abstraction
          newPred = buildNewPredicateAbstraction(targetARTNode, pLeftAbstraction, pf);
          if (newPred == null || newPred.getFirst() == null || newPred.getSecond() == null) { return PCCCheckResult.InvalidART; }
          //add target ART element if not checked yet
          if (edges[i].getCorrespondingCFAEdge().getEdgeType() == CFAEdgeType.FunctionCallEdge) {
            intermediateRes = addTargetNode(targetARTNode, pStack
                + Separators.stackEntrySeparator
                + cfaNode.getLeavingSummaryEdge().getSuccessor()
                    .getNodeNumber(), newPred.getFirst(), newPred.getSecond(), pVisited, pWaiting);
          } else {
            intermediateRes = addTargetNode(targetARTNode, pStack, newPred.getFirst(), newPred.getSecond(), pVisited,
                pWaiting);
          }
          if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult addTargetNode(ARTNode pTarget, String pStack, Formula pLeftAbstraction, PathFormula pPath,
      Hashtable<String, String> pVisited,
      Stack<FourTuple<Integer, String, Formula, PathFormula>> pWaiting) {
    String id;
    if (pTarget.getAbstractionType() == AbstractionType.Abstraction) {
      id = Integer.toString(pTarget.getID());
    } else {
      id = Integer.toString(pTarget.getID()) + Separators.commonSeparator + pPath.toString();
    }
    if (!pVisited.containsKey(id)) {
      pVisited.put(id, pStack);
      pWaiting.push(new FourTuple<Integer, String, Formula, PathFormula>(pTarget.getID(), pStack, pLeftAbstraction,
          pPath));
    } else {
      if (pVisited.get(id) == null || !pVisited.get(id).equals(pStack)) { return PCCCheckResult.InvalidART; }
    }
    return PCCCheckResult.Success;
  }

  private boolean isAbstraction(CFANode pCFANode, int pPathLength) {
    if (pCFANode instanceof CFALabelNode) {
      // check if error node
      if (((CFALabelNode) pCFANode).getLabel().toLowerCase().equals("error")) { return true; }
    }
    if (((pCFANode instanceof FunctionDefinitionNode
        || pCFANode.getEnteringSummaryEdge() != null) && atFunction)
        || (pCFANode.isLoopStart() && atLoop) || (pPathLength == threshold && threshold != 0)) { return true; }
    return false;
  }

  private PCCCheckResult checkARTEdge(ARTNode pTarget, Formula pLeftAbstraction, PathFormula pPath) {
    // check if abstraction needed and available
    Pair<Formula, SSAMap> right;
    Formula result;
    if (isAbstraction(pTarget.getCorrespondingCFANode(), pPath.getLength())) {
      if (pTarget.getAbstractionType() != AbstractionType.Abstraction || pTarget.getAbstraction() == null) { return PCCCheckResult.InvalidInvariant; }
    }
    if (pTarget.getAbstractionType() == AbstractionType.Abstraction) {
      if (pTarget.getAbstraction() == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      // check if abstraction feasible
      //instantiate right abstraction
      right = handler.addIndices(pPath.getSsa(), pTarget.getAbstraction());
      if (right == null || right.getFirst() == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      // build edge proof
      result = handler.buildEdgeInvariant(pLeftAbstraction, pPath.getFormula(), right.getFirst());
      if (result == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
      if (!handler.isFalse(result)) { return PCCCheckResult.InvalidInvariant; }
    } else {
      if (pTarget.getAbstractionType() == AbstractionType.CoveredNonAbstraction) {
        ARTNode covering = art.get(((CoveredARTNode) pTarget).getCoveringElement());
        // check if same CFA node
        if (covering == null || !pTarget.getCorrespondingCFANode().equals(covering.getCorrespondingCFANode())
            || covering.getAbstraction() == null) { return PCCCheckResult.InvalidART; }
        // check feasible covering
        right = handler.addIndices(pPath.getSsa(), covering.getAbstraction());
        if (right == null || right.getFirst() == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
        // build edge proof
        result = handler.buildEdgeInvariant(pLeftAbstraction, pPath.getFormula(), right.getFirst());
        if (result == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
        if (!handler.isFalse(result)) { return PCCCheckResult.InvalidInvariant; }
      } else {
        // nothing to check
      }
    }
    return PCCCheckResult.Success;
  }

  private Pair<Formula, PathFormula> buildNewPredicateAbstraction(ARTNode pTarget, Formula pLeftAbstractionBefore,
      PathFormula pPathToTarget) {
    if (pTarget == null) { return null; }
    if (pTarget.getAbstractionType() == AbstractionType.Abstraction) {
      Formula f = handler.createFormula(pTarget.getAbstraction());
      if (f == null) { return null; }
      Pair<Formula, SSAMap> pair = handler.addIndices(null, f);
      if (pair == null || pair.getFirst() == null) { return null; }
      f = pair.getFirst();
      PathFormula pf = handler.getTrueFormula(pair.getSecond());
      if (pf == null) { return null; }
      return new Pair<Formula, PathFormula>(f, pf);
    } else {
      if (pLeftAbstractionBefore == null || pPathToTarget == null) { return null; }
      return new Pair<Formula, PathFormula>(pLeftAbstractionBefore, pPathToTarget);
    }
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

}
