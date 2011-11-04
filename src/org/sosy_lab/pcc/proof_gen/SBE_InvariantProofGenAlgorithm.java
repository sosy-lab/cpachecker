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

import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.FormulaHandler;

public class SBE_InvariantProofGenAlgorithm extends InvariantProofGenAlgorithm {

  private FormulaHandler              fh;

  public SBE_InvariantProofGenAlgorithm(Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger);
  }

  @Override
  protected boolean addOperation(ARTElement pSource, ARTElement pTarget) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pSource,
            PredicateAbstractElement.class);
    //check if it is not start of an edge (no abstraction, no function exit node, no function definition node, no node with entering summary edge
    CFANode corresponding = pSource.retrieveLocationElement().getLocationNode();
    if (!predicate.isAbstractionElement()
        && !(corresponding instanceof CFAFunctionDefinitionNode)
        && !(corresponding instanceof CFAFunctionExitNode)
        && corresponding.getEnteringSummaryEdge() == null) { return true; }
    CFAEdge edge = pSource.getEdgeToChild(pTarget);
    // get PredicateAbstractElement
    predicate =
        AbstractElements.extractElementByType(pTarget,
            PredicateAbstractElement.class);
    corresponding = pTarget.retrieveLocationElement().getLocationNode();
    // check if no abstraction needed
    if (!predicate.isAbstractionElement()
        && !(corresponding instanceof CFAFunctionDefinitionNode)
        && !(corresponding instanceof CFAFunctionExitNode)
        && corresponding.getEnteringSummaryEdge() == null) {
      /* get next successor nodes which are abstraction elements
       * and build the respective operations*/
      Vector<ARTElement> toVisit = new Vector<ARTElement>();
      toVisit.add(pTarget);
      ARTElement currentElem;
      boolean success;
      while (!toVisit.isEmpty()) {
        currentElem = toVisit.remove(0);
        for (ARTElement child : currentElem.getChildren()) {
          edge = currentElem.getEdgeToChild(child);
          predicate =
              AbstractElements.extractElementByType(child,
                  PredicateAbstractElement.class);
          corresponding = child.retrieveLocationElement().getLocationNode();
          if (!predicate.isAbstractionElement()
              && !(corresponding instanceof CFAFunctionExitNode)
              && !(corresponding instanceof CFAFunctionExitNode)
              && corresponding.getEnteringSummaryEdge() == null) {
            toVisit.add(child);
          } else {
            success = addSingleOperation(pSource, edge);
            if (!success) { return false; }
          }
        }
      }
    } else {
      return addSingleOperation(pSource, edge);
    }
    return true;
  }

  private boolean addSingleOperation(ARTElement pNode, CFAEdge pEdge) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pNode,
            PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    String operation =
        fh.getEdgeOperationWithSSA(predicate.getPathFormula(), pEdge);
    if (operation == null) { return false; }
    // build identification of edge
    String id =
        pNode.retrieveLocationElement().getLocationNode().getNodeNumber() + "#"
            + pEdge.getSuccessor().getNodeNumber();
    StringBuilder current = operationsPerEdge.get(id);
    if (current == null) {
      operationsPerEdge.put(id, new StringBuilder(operation + "#"));
    } else {
      current.append(operation + "#");
    }
    return true;
  }

  @Override
  protected boolean addInvariant(ARTElement pNode, String pStack) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pNode,
            PredicateAbstractElement.class);
    if(predicate == null){
      return false;
    }
    CFANode corresponding = pNode.retrieveLocationElement().getLocationNode();
    //check if it is an abstraction element, otherwise nothing to do
    if(predicate.isAbstractionElement()
    || (corresponding instanceof CFAFunctionDefinitionNode)
    || (corresponding instanceof CFAFunctionExitNode)
    || corresponding.getEnteringSummaryEdge() != null){
      StringBuilder builder = cfaNodeInvariants.get(corresponding.getNodeNumber());
      if(builder == null){
        cfaNodeInvariants.put(corresponding.getNodeNumber(), new StringBuilder(predicate.getAbstractionFormula().asFormula().toString()+"?" + pStack+ "#"));
      }else{
        builder.append(predicate.getAbstractionFormula().asFormula().toString()+"?" + pStack+ "#");
      }
    }
    return true;
  }
}
