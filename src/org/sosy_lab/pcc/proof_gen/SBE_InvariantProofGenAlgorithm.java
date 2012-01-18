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

import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.Separators;

public abstract class SBE_InvariantProofGenAlgorithm extends
    InvariantProofGenAlgorithm {

  protected FormulaHandler fh;

  public SBE_InvariantProofGenAlgorithm(Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger, whichProver);
  }

  @Override
  protected boolean addInvariant(ARTElement pNode, String pStack) {
    // if covered, this element is not part of proof, redirection to covering node
    if (pNode.isCovered()) { return true; }
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pNode,
            PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    CFANode corresponding = extractLocation(pNode);
    //check if it is an abstraction element, otherwise nothing to do
    if (predicate.isAbstractionElement()
        || (corresponding instanceof CFAFunctionDefinitionNode)
        || corresponding.getEnteringSummaryEdge() != null) {
      StringBuilder builder =
          cfaNodeInvariants.get(corresponding.getNodeNumber());
      if (builder == null) {
        builder = new StringBuilder();
        cfaNodeInvariants.put(corresponding.getNodeNumber(), builder);
      }
      // add invariant and stack
      logger.log(Level.INFO, "Add another region description.");
      String invariant = getAbstraction(predicate);
      if (invariant == null || invariant.length() == 0) { return false; }
      builder.append(invariant + pStack + Separators.commonSeparator);
    }
    return true;
  }

  @Override
  protected boolean addOperation(ARTElement pSource, ARTElement pTarget) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pSource,
            PredicateAbstractElement.class);
    if(predicate == null){return false;}
    //check if it is not start of an edge (no abstraction, no function exit node, no function definition node, no node with entering summary edge
    CFANode corresponding = extractLocation(pSource);
    if (!predicate.isAbstractionElement()
        && !(corresponding instanceof CFAFunctionDefinitionNode)
        && corresponding.getEnteringSummaryEdge() == null) { return true; }
    CFAEdge edge = pSource.getEdgeToChild(pTarget);
    ARTElement target;
    if (pTarget.isCovered()) {
      target = getFinalCoveringElement(pTarget);
    }
    else {
      target = pTarget;
    }
    // get PredicateAbstractElement
    predicate =
        AbstractElements.extractElementByType(target,
            PredicateAbstractElement.class);
    corresponding = extractLocation(target);
    // check if no abstraction needed for target, than look for children as target
    if ((!predicate.isAbstractionElement() || (pTarget.isCovered() && !AbstractElements.extractElementByType(pTarget,
        PredicateAbstractElement.class).isAbstractionElement()))
        && !(corresponding instanceof CFAFunctionDefinitionNode)
        && corresponding.getEnteringSummaryEdge() == null) {
      /* get next successor nodes which are abstraction elements
       * and build the respective operations*/
      Vector<ARTElement> toVisit = new Vector<ARTElement>();
      toVisit.add(target);
      ARTElement currentElem;
      boolean success;
      while (!toVisit.isEmpty()) {
        currentElem = toVisit.remove(0);
        for (ARTElement child : currentElem.getChildren()) {
          edge = currentElem.getEdgeToChild(child);
          predicate =
              AbstractElements.extractElementByType(child,
                  PredicateAbstractElement.class);
          corresponding = extractLocation(child);
          if (!predicate.isAbstractionElement()
              && !(corresponding instanceof CFAFunctionDefinitionNode)
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

  protected String buildEdgeId(CFANode pSource, CFAEdge pEdge) {
    // build identification of edge
    String id =
        pSource.getNodeNumber() + Separators.commonSeparator
            + pEdge.getPredecessor().getNodeNumber()
            + Separators.commonSeparator + pEdge.getSuccessor().getNodeNumber();
    return id;
  }

  protected abstract boolean addSingleOperation(ARTElement pNode, CFAEdge pEdge);

  protected abstract String getAbstraction(PredicateAbstractElement pPredicate);

}
