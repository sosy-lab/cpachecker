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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.ARTEdge;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.CoveredARTNode;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;
import org.sosy_lab.pcc.common.Separators;


public class AdjustableLBE_ARTProofCheckAlgorithm extends ARTProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected boolean atLoop;
  protected boolean atFunction;
  int threshold;

  protected Hashtable<Integer, ARTNode> art = new Hashtable<Integer, ARTNode>();

  public AdjustableLBE_ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType,
      int pThreshold)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    handler = new FormulaHandler(pConfig, pLogger, pProverType);
    atLoop = true;
    atFunction = true;
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
          newNode = new ARTNode(artId, cfaNode, pAbsType, next, false);
        } else {
          newNode = new CoveredARTNode(artId, cfaNode, pAbsType, pScan.nextInt(), false);
        }

        if (art.containsKey(artId)) { return PCCCheckResult.ElementAlreadyRead; }
        art.put(new Integer(artId), newNode);
        if (cfaForProof.getMainFunction().equals(cfaNode)) {
          if (!rootFound) {
            // set root
            root = newNode;
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
        System.out.println("Error1");
        return PCCCheckResult.UnknownCFANode;
      } catch (InputMismatchException e2) {
        System.out.println("Error2");
        return PCCCheckResult.UnknownCFANode;
      } catch (NoSuchElementException e3) {
        System.out.println("Error3");
        return PCCCheckResult.UnknownCFANode;
      } catch (IllegalArgumentException e4) {
        System.out.println("Error4");
        return PCCCheckResult.UnknownCFANode;
      }
    }
    return PCCCheckResult.Success;
  }

  private boolean checkAbstraction(String pAbstraction) {
    if (pAbstraction.contains(Separators.SSAIndexSeparator)) { return false; }
    return true;
  }

  @Override
  protected PCCCheckResult readEdges(Scanner pScan) {
    int source, target;
    ARTNode nodeS, nodeT;
    ARTEdge edge;

    while (pScan.hasNext()) {
      try {
        // read next edge
        logger.log(Level.INFO, "Read next edge");
        source = pScan.nextInt();
        target = pScan.nextInt();
        nodeS = art.get(new Integer(source));
        nodeT = art.get(new Integer(target));
        if (nodeS == null || nodeT == null) {
          logger.log(Level.SEVERE,
              "Either source or target node of edge is no valid ART node.");
          return PCCCheckResult.ART_CFA_Mismatch;
        }

        //correct abstraction type, correct edges are checked within proof

        edge = new ARTEdge(target);
        if (nodeS.isEdgeContained(edge)) { return PCCCheckResult.ElementAlreadyRead; }
        nodeS.addEdge(edge);
      } catch (InputMismatchException e2) {
        System.out.println("Test2");
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e3) {
        System.out.println("Test3");
        return PCCCheckResult.UnknownCFAEdge;
      } catch (IllegalArgumentException e4) {
        System.out.println("Test4");
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
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
    ARTNode node, child;
    ARTEdge[] edges;
    String stack;
    // check ART
    while (!waiting.isEmpty()) {
      current = waiting.pop();
      logger.log(Level.INFO, "Check ART node " + current.getFirst() + " .");
      node = art.get(current.getFirst());
      intermediateRes =
          checkARTNode(node, current.getSecond());
      if (intermediateRes != PCCCheckResult.Success) { return intermediateRes; }
      // add children
      edges = node.getEdges();
      for (int i = 0; i < edges.length; i++) {
        child = art.get(edges[i].getTarget());
        if (child.getCorrespondingCFANode() instanceof FunctionDefinitionNode) {
          stack = current.getSecond() + Separators.stackEntrySeparator
              + node.getCorrespondingCFANode().getLeavingSummaryEdge().getSuccessor().getNodeNumber();
        } else {
          if (node.getCorrespondingCFANode().getEnteringSummaryEdge() != null) {
            stack = current.getSecond().substring(0,
                current.getSecond().lastIndexOf(Separators.stackEntrySeparator));
          } else {
            stack = current.getSecond();
          }
        }
        if (!visited.containsKey(child.getID())) {
          waiting.push(new Pair<Integer, String>(child.getID(), stack));
          visited.put(child.getID(), stack);
        }
        else {
          if (visited.get(child.getID()) == null || !visited.get(child.getID()).equals(stack)) { return PCCCheckResult.InvalidART; }
        }
      }
    }
    return PCCCheckResult.Success;
  }

  private PCCCheckResult checkARTNode(ARTNode pNode, String pStack) {
    CFANode cfaNode = pNode.getCorrespondingCFANode();
    // check ART node
    // check error node
    if (cfaNode instanceof CFALabelNode
        && ((CFALabelNode) cfaNode).getLabel().toLowerCase().equals("error")) {
      if (!handler.isFalse(pNode.getAbstraction())) { return PCCCheckResult.ErrorNodeReachable; }
    }
    // check false abstractions
    if (handler.isFalse(pNode.getAbstraction())) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
    }
    // check stack
    if (cfaNode.getEnteringSummaryEdge() != null) {
      try {
        if (cfaNode.getNodeNumber() != Integer.parseInt(pStack.substring(
            pStack.lastIndexOf(Separators.stackEntrySeparator) + 1, pStack.length()))) { return PCCCheckResult.ART_CFA_Mismatch; }
      } catch (NumberFormatException e) {
        return PCCCheckResult.InvalidStack;
      }
    }
    // check program end
    if (cfaForProof.getMainFunction().getExitNode().equals(cfaNode)) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.ART_CFA_Mismatch; }
      if (!pStack.equals("")) { return PCCCheckResult.InvalidStack; }
    }
    // check covered element
    if (pNode instanceof CoveredARTNode) {
      if (pNode.getNumberOfEdges() != 0) { return PCCCheckResult.InvalidART; }
    } else {
      // check edges
      Stack<Pair<CFANode, PathFormula>> toCheck = new Stack<Pair<CFANode, PathFormula>>();
      Pair<Formula, SSAMap> sourceAbstraction = handler.addIndices(null, pNode.getAbstraction());
      if (sourceAbstraction == null || sourceAbstraction.getFirst() == null || sourceAbstraction.getSecond() == null) { return PCCCheckResult.InvalidART; }
      PathFormula pf = handler.getTrueFormula(sourceAbstraction.getSecond());
      Formula f;
      Formula[] leftAndOp = new Formula[2];
      toCheck.add(new Pair<CFANode, PathFormula>(cfaNode, pf));
      Pair<CFANode, PathFormula> current;
      boolean[] edgeCovered = new boolean[pNode.getNumberOfEdges()];
      ARTEdge[] edges;
      ARTNode target;
      // get allowed paths starting at this ARTNode to next abstraction node
      while (!toCheck.empty()) {
        current = toCheck.pop();
        cfaNode = current.getFirst();
        // check all children
        for (int i = 0; i < cfaNode.getNumLeavingEdges(); i++) {
          // get path formula
          pf = handler.extendPath(current.getSecond(), cfaNode.getLeavingEdge(i));
          if (pf == null || pf.getFormula() == null) { return PCCCheckResult.InvalidART; }
          // check if path ends in abstraction ART node
          if (isAbstraction(cfaNode.getLeavingEdge(i).getSuccessor(), pf.getLength())) {
            // check if ART node is available
            if (!hasAbstractionEdgeTo(cfaNode.getLeavingEdge(i).getSuccessor(), pNode.getEdges())) { return PCCCheckResult.UncoveredEdge; }
            // check if edge is feasible
            if (!checkARTEdgePath(sourceAbstraction.getFirst(), pf, cfaNode.getLeavingEdge(i), pNode.getEdges(),
                edgeCovered)) { return PCCCheckResult.UncoveredEdge; }
          } else {
            // check if it is a covered art node
            if (hasCoveringEdgeTo(cfaNode.getLeavingEdge(i).getSuccessor(), pNode.getEdges())) {
              // every non-abstraction path must fulfill covered criteria
              if (!checkCoveredARTEdgePath(sourceAbstraction.getFirst(), pf, cfaNode.getLeavingEdge(i),
                  pNode.getEdges(), edgeCovered)) { return PCCCheckResult.InvalidART; }
            } else {
              // check if ends in abstraction node, not important at end of if then else construct, after goto (not loop)
              if ((cfaNode.getLeavingEdge(i).getSuccessor().getNumEnteringEdges() == 1
              && hasAbstractionEdgeTo(cfaNode.getLeavingEdge(i).getSuccessor(), pNode.getEdges()))) {
                // check if path feasible to that abstraction node
                if (!checkARTEdgePath(sourceAbstraction.getFirst(), pf, cfaNode.getLeavingEdge(i), pNode.getEdges())) { return PCCCheckResult.InvalidART; }
              } else {
                // check if path formula combined with invariant is false -> stop, path not reachable
                leftAndOp[0] = sourceAbstraction.getFirst();
                leftAndOp[1] = pf.getFormula();
                f = handler.buildConjunction(leftAndOp);
                if (f == null) { return PCCCheckResult.InvalidFormulaSpecificationInProof; }
                if (handler.isFalse(f)) {
                  // check if it covers abstraction target node which has abstraction false
                  // TODO possibly cannot stop here, because influences paths elsewhere (stop earlier)
                  edges = pNode.getEdges();
                  for (int j = 0; j < edges.length; j++) {
                    target = art.get(edges[j].getTarget());
                    if (target != null
                        && target.getCorrespondingCFANode().equals(cfaNode.getLeavingEdge(i).getSuccessor())
                        && handler.isFalse(target.getAbstraction())) {
                      edgeCovered[j] = true;
                    }
                  }
                } else {
                  toCheck.add(new Pair<CFANode, PathFormula>(cfaNode.getLeavingEdge(i).getSuccessor(), pf));
                }
              }
            }
          }
        }
      }
      // check if every successor is covered -> correct abstraction
      for (int i = 0; i < edgeCovered.length; i++) {
        if (!edgeCovered[i]) { return PCCCheckResult.UncoveredEdge; }
      }
    }
    return PCCCheckResult.Success;
  }

  private boolean isAbstraction(CFANode pCFANode, int pPathLength) {
    if (pCFANode instanceof CFALabelNode) {
      // check if error node
      if (((CFALabelNode) pCFANode).getLabel().toLowerCase().equals("error")) { return true; }
    }
    if (pCFANode instanceof FunctionDefinitionNode
        || pCFANode.getEnteringSummaryEdge() != null
        || (pCFANode.isLoopStart() && atLoop) || pPathLength == threshold) { return true; }
    return false;
  }

  private boolean checkARTEdgePath(Formula pSourceAbstraction, PathFormula pPath, CFAEdge pCfaEdge,
      ARTEdge[] pEdges, boolean[] pEdgeCovered) {
    if (pEdges.length != pEdgeCovered.length || pPath == null || pPath.getFormula() == null
        || pSourceAbstraction == null || pCfaEdge == null) { return false; }
    ARTNode target;
    Formula rightAbstraction;
    boolean success = false;
    for (int i = 0; i < pEdges.length; i++) {
      target = art.get(pEdges[i].getTarget());
      if (target == null || target instanceof CoveredARTNode) { return false; }
      if (target.getCorrespondingCFANode().equals(pCfaEdge.getSuccessor())) {
        // check feasible abstraction
        rightAbstraction = handler.createFormula(target.getAbstraction());
        if (rightAbstraction == null) { return false; }
        rightAbstraction = handler.buildEdgeInvariant(pSourceAbstraction, pPath.getFormula(), rightAbstraction);
        if (rightAbstraction == null) { return false; }
        if (handler.isFalse(rightAbstraction)) {
          pEdgeCovered[i] = true;
          success = true;
        }
      }
    }
    return success;
  }

  private boolean checkARTEdgePath(Formula pSourceAbstraction, PathFormula pPath, CFAEdge pCfaEdge,
      ARTEdge[] pEdges) {
    if (pPath == null || pPath.getFormula() == null
        || pSourceAbstraction == null || pCfaEdge == null) { return false; }
    ARTNode target;
    Formula rightAbstraction;
    boolean success = false;
    for (int i = 0; i < pEdges.length; i++) {
      target = art.get(pEdges[i].getTarget());
      if (target == null || target instanceof CoveredARTNode) { return false; }
      if (target.getCorrespondingCFANode().equals(pCfaEdge.getSuccessor())) {
        // check feasible abstraction
        rightAbstraction = handler.createFormula(target.getAbstraction());
        if (rightAbstraction == null) { return false; }
        rightAbstraction = handler.buildEdgeInvariant(pSourceAbstraction, pPath.getFormula(), rightAbstraction);
        if (rightAbstraction == null) { return false; }
        if (handler.isFalse(rightAbstraction)) {
          success = true;
        }
      }
    }
    return success;
  }

  private boolean checkCoveredARTEdgePath(Formula pSourceAbstraction, PathFormula pPath, CFAEdge pEdge,
      ARTEdge[] pEdges, boolean[] pEdgeCovered) {
    if (pEdges.length != pEdgeCovered.length || pPath == null || pPath.getFormula() == null
        || pSourceAbstraction == null || pEdge == null) { return false; }
    ARTNode target;
    Formula rightAbstraction;
    Pair<Formula, SSAMap> intermediate;
    boolean success = false;
    for (int i = 0; i < pEdges.length; i++) {
      target = art.get(pEdges[i].getTarget());
      if (target == null) { return false; }
      if (!(target instanceof CoveredARTNode)) {
        continue;
      }
      if (target.getCorrespondingCFANode().equals(pEdge.getSuccessor())) {
        // check feasible coverage
        rightAbstraction = handler.createFormula(target.getAbstraction());
        intermediate = handler.addIndices(pPath.getSsa(), rightAbstraction);
        if (intermediate == null) { return false; }
        rightAbstraction = intermediate.getFirst();
        if (rightAbstraction == null) { return false; }
        rightAbstraction = handler.buildEdgeInvariant(pSourceAbstraction, pPath.getFormula(), rightAbstraction);
        if (rightAbstraction == null) { return false; }
        if (handler.isFalse(rightAbstraction)) {
          pEdgeCovered[i] = true;
          success = true;
        }
      }
    }
    return success;
  }


  private boolean hasAbstractionEdgeTo(CFANode pTarget, ARTEdge[] pEdges) {
    if (pEdges == null || pTarget == null) { return false; }
    int artTarget;
    for (int i = 0; i < pEdges.length; i++) {
      artTarget = pEdges[i].getTarget();
      if (!(art.get(artTarget) instanceof CoveredARTNode)
          && art.get(artTarget).getCorrespondingCFANode().equals(pTarget)) { return true; }
    }
    return false;
  }

  private boolean hasCoveringEdgeTo(CFANode pTarget, ARTEdge[] pEdges) {
    if (pEdges == null || pTarget == null) { return false; }
    int artTarget;
    for (int i = 0; i < pEdges.length; i++) {
      artTarget = pEdges[i].getTarget();
      if ((art.get(artTarget) instanceof CoveredARTNode)
          && art.get(artTarget).getCorrespondingCFANode().equals(pTarget)) { return true; }
    }
    return false;
  }
}
