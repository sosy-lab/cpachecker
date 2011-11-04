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
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.PCCCheckResult;
import org.sosy_lab.pcc.common.Pair;

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

    // TODO Auto-generated method stub
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
          reachableCFANodes.put(readNode, current);
        } else {
          return PCCCheckResult.UnknownCFANode;
        }
        // read invariants
        invariants = new Vector<Pair<Formula, int[]>>();
        numInvariants = pScan.nextInt();
        for(int i=0;i<numInvariants;i++){
          // read invariant
          next = pScan.next();
          subStr = next.split("?");//TODO nachschauen
          // get formula
          subStr = next.substring(arg0)
          //

        }
        nodes.put(readNode, )
      }
    } catch (NumberFormatException e1) {
      return PCCCheckResult.UnknownCFANode;
    } catch (InputMismatchException e2) {
      return PCCCheckResult.UnknownCFANode;
    } catch (NoSuchElementException e3) {
      return PCCCheckResult.UnknownCFANode;
    }
    // TODO Auto-generated method stub
    return structuralCheckCoverageOfCFANodes();
  }

  private PCCCheckResult structuralCheckCoverageOfCFANodes() {
    //TODO
    return null;
  }

  private PCCCheckResult structuralCheckEdge() {
    //TODO check valid edge, source and target are reachable
    return null;
  }

  private PCCCheckResult structuralCheckCoverageOfCFAEdges() {
    //TODO
    return null;
  }

  @Override
  protected PCCCheckResult checkProof() {
    // iterate over all edges
    int source, target;
    Formula edgeFormula;
    for (String edge : edges.keySet()) {
      source = Integer.parseInt(edge.substring(edge.indexOf("#")));
      target =
          Integer.parseInt(edge.substring(edge.indexOf("#"), edge.length()));
      // iterate over all source predicates

      // build edge formula
      edgeFormula =
          buildEdgeFormula(edges.get(edge), reachableCFANodes.get(source)
              .getEdgeTo(reachableCFANodes.get(target)), nodes.get(source));
      if (edgeFormula == null) {
        // TODO return PCCCheckResult.
      }
      //TODO
    }
    // TODO Auto-generated method stub
    return null;
  }

  private Formula buildEdgeFormula(Formula[] pOperations, CFAEdge pEdge,
      int[] pSourceStack) {
    // TODO
    return null;
  }
}
