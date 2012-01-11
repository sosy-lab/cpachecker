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
package org.sosy_lab.pcc.proof_gen;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.HashSet;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.FormulaHandler;


public class LBE_ARTProofGenAlgorithm extends ARTProofGenAlgorithm {

  protected FormulaHandler fh;

  private HashSet<Integer> targetsFound = new HashSet<Integer>();

  public LBE_ARTProofGenAlgorithm(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger, whichProver);
  }

  @Override
  protected boolean addARTNode(ARTElement pNode) {
    CFANode node =extractLocation(pNode);
    // only add abstraction nodes
    PredicateAbstractElement predicate = AbstractElements.extractElementByType(pNode, PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    if (predicate.isAbstractionElement()) {
      if (!pNode.isCovered()) {
        // build string of form ARTId#CFAId#NodeType#Abstraction#(return_address#)?
        StringBuilder nodeRep = new StringBuilder();
        nodeRep.append(pNode.getElementId() + "#");
        nodeRep.append(extractLocation(pNode).getNodeNumber() + "#");
        nodeRep.append(AbstractionType.Abstraction + "#");
        String f =
            fh.removeIndicesStr(predicate.getAbstractionFormula().asFormula());
        if (f == null) { return false; }
        nodeRep.append(f + "#");
        if((node instanceof FunctionDefinitionNode) && node.getNumEnteringEdges()!=0){
          if(pNode.getParents().size()!=1){
            System.out.println("Caller not well specified.");
            return false;
          }
          nodeRep.append(extractLocation(pNode.getParents().iterator().next()).
              getLeavingSummaryEdge().getSuccessor().getNodeNumber()+"#");
        }
        nodes.add(nodeRep.toString());
      }
    } else {
      if (pNode.isCovered()) {
        System.out.println("Unexpected case.");
        return false;
        /*
        Formula[] fList = new Formula[2];
        fList[0] = predicate.getAbstractionFormula().asFormula();
        fList[1] = predicate.getPathFormula().getFormula();
        if (fList[0] == null || fList[1] == null) { return false; }
        Formula complete = fh.buildConjunction(fList);
        if (complete == null) { return false; }
        if (fh.isFalse(complete)) {
          // build string of form ARTId#CFAId#NodeType#Abstraction#
          StringBuilder nodeRep = new StringBuilder();
          nodeRep.append(pNode.getElementId() + "#");
          nodeRep.append(pNode.retrieveLocationElement().getLocationNode().getNodeNumber() + "#");
          nodeRep.append(AbstractionType.Abstraction + "#");
          nodeRep.append(false + "#");// TODO check if it works
          nodes.add(nodeRep.toString());
        } else {
          System.out.println("Unexpected case.");
          return false;
          // build string of form ARTId#CFAId#NodeType#coveringID#
          StringBuilder nodeRep = new StringBuilder();
          nodeRep.append(pNode.getElementId() + "#");
          nodeRep.append(pNode.retrieveLocationElement().getLocationNode().getNodeNumber() + "#");
          nodeRep.append(AbstractionType.CoveredNonAbstraction + "#");
          ARTElement covering = getFinalCoveringElement(pNode);
          if (covering == null) { return false; }
          nodeRep.append(covering.getElementId() + "#");
          nodes.add(nodeRep.toString());
        }*/
      }
    }
    return true;
  }

  @Override
  protected boolean addARTEdge(ARTElement pSource, CFAEdge pEdge, ARTElement pTarget) {
    //only add edges between two abstraction nodes
    PredicateAbstractElement predicate = AbstractElements.extractElementByType(pSource, PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    if (predicate.isAbstractionElement()) {
      Integer[] targets = getAbstractionSuccessors(pSource);
      if (targets == null) { return false; }
      for (int i = 0; i < targets.length; i++) {
        edges.add(pSource.getElementId() + "#" + targets[i] + "#");
      }
    }
    return true;
  }

  private Integer[] getAbstractionSuccessors(ARTElement pSource) {
    if (targetsFound.contains(pSource.getElementId())) { return new Integer[0]; }
    targetsFound.add(pSource.getElementId());
    HashSet<Integer> found = new HashSet<Integer>();
    HashSet<Integer> visited = new HashSet<Integer>();
    Vector<ARTElement> toVisit = new Vector<ARTElement>();
    visited.add(pSource.getElementId());
    toVisit.add(pSource);
    ARTElement current;
    Integer id;
    PredicateAbstractElement predicate;
    while (!toVisit.isEmpty()) {
      current = toVisit.remove(0);
      predicate = AbstractElements.extractElementByType(current, PredicateAbstractElement.class);
      if (predicate == null) { return null; }
      if ((predicate.isAbstractionElement() || current.isCovered())&& !current.equals(pSource)) {
        if (predicate.isAbstractionElement() && current.isCovered()) {
          id = getFinalCoveringElement(current).getElementId();
        } else {
          id = current.getElementId();
        }
        if (!found.contains(id)) {
          found.add(id);
        }
      } else {
        for (ARTElement child : current.getChildren()) {
          if (!visited.contains(child.getElementId())) {
            visited.add(child.getElementId());
            toVisit.add(child);
          }
        }
      }
    }
    return found.toArray(new Integer[found.size()]);
  }
}
