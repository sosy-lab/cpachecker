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
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.pcc.common.ARTEdge;
import org.sosy_lab.pcc.common.ARTNode;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.CoveredARTNode;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Separators;


public class ABE_ARTProofCheckAlgorithm extends ARTProofCheckAlgorithm {

  protected FormulaHandler handler;
  protected boolean atLoop;
  protected boolean atFunction;
  protected int threshold;
  protected Hashtable<Integer, ARTNode> art = new Hashtable<Integer, ARTNode>();

  public ABE_ARTProofCheckAlgorithm(Configuration pConfig, LogManager pLogger, String pProverType,
      boolean pAlwaysAtLoops, int pThreshold) throws InvalidConfigurationException {
    super(pConfig, pLogger);

    handler = new FormulaHandler(pConfig, pLogger, pProverType);
    atLoop = pAlwaysAtLoops;
    atFunction = true;
    threshold = pThreshold;
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
        if(cfaNode == null){
          return PCCCheckResult.UnknownCFANode;
        }
        pAbsType = AbstractionType.valueOf(pScan.next());
        if (pAbsType == AbstractionType.Abstraction) {
          next = pScan.next();
          if (!checkAbstraction(next)) {
            logger.log(Level.SEVERE, "Wrong abstraction: " + next + " .");
            return PCCCheckResult.InvalidInvariant;
          }
          newNode = new ARTNode(artId, cfaNode, pAbsType, next,false);
        } else {
          newNode = new CoveredARTNode(artId, cfaNode, pAbsType, pScan.nextInt(),false);
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
    PCCCheckResult intermediateRes;

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

        // check for correct abstraction type of target node
        //TODO really here or within proof

        edge = new ARTEdge(target);
        if(nodeS.isEdgeContained(edge)){
          return PCCCheckResult.ElementAlreadyRead;
        }
        nodeS.addEdge(edge);
      } catch (InputMismatchException e2) {System.out.println("Test2");
        return PCCCheckResult.UnknownCFAEdge;
      } catch (NoSuchElementException e3) {System.out.println("Test3");
        return PCCCheckResult.UnknownCFAEdge;
      } catch (IllegalArgumentException e4) {System.out.println("Test4");
        return PCCCheckResult.UnknownCFAEdge;
      }
    }
    return PCCCheckResult.Success;
  }

  @Override
  protected PCCCheckResult checkProof() {
    // TODO Auto-generated method stub
    return null;
  }

}
