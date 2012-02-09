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
package org.sosy_lab.pcc.proof_gen;

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.FormulaHandler;
import org.sosy_lab.pcc.common.Separators;


public class ABE_ARTProofGenAlgorithm extends ARTProofGenAlgorithm {


  protected FormulaHandler fh;

  public ABE_ARTProofGenAlgorithm(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger, whichProver);
  }

  @Override
  protected boolean addARTNode(ARTElement pNode) {
    //write ARG node
    PredicateAbstractElement predicate = AbstractElements.extractElementByType(pNode, PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    if (predicate.isAbstractionElement()) {
      if (!pNode.isCovered()) {
        // build string of form ARTId#CFAId#NodeType#Abstraction#
        StringBuilder nodeRep = new StringBuilder();
        nodeRep.append(pNode.getElementId() + "#");
        nodeRep.append(extractLocation(pNode).getNodeNumber() + "#");
        nodeRep.append(AbstractionType.Abstraction + "#");
        String f =
            fh.removeIndicesStr(predicate.getAbstractionFormula().asFormula());
        if (f == null) { return false; }
        nodeRep.append(f + "#");
        nodes.add(nodeRep.toString());
      }
    } else {
      if (pNode.isCovered()) {
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
          nodeRep.append(extractLocation(pNode).getNodeNumber() + "#");
          nodeRep.append(AbstractionType.Abstraction + "#");
          nodeRep.append(false + "#");// TODO check if it works
          nodes.add(nodeRep.toString());
        } else {
          // build string of form ARTId#CFAId#NodeType#coveringID#
          StringBuilder nodeRep = new StringBuilder();
          nodeRep.append(pNode.getElementId() + "#");
          nodeRep.append(extractLocation(pNode).getNodeNumber() + "#");
          nodeRep.append(AbstractionType.CoveredNonAbstraction + "#");
          ARTElement covering = getFinalCoveringElement(pNode);
          if (covering == null) { return false; }
          nodeRep.append(covering.getElementId() + "#");
          nodes.add(nodeRep.toString());
        }
      } else {
        // build string of form ARTId#CFAId#NodeType#
        StringBuilder nodeRep = new StringBuilder();
        nodeRep.append(pNode.getElementId() + "#");
        nodeRep.append(extractLocation(pNode).getNodeNumber() + "#");
        nodeRep.append(AbstractionType.NoAbstraction + "#");
        nodes.add(nodeRep.toString());
      }
    }
    return true;
  }

  @Override
  protected boolean addARTEdge(ARTElement pSource, CFAEdge pEdge, ARTElement pTarget) {
    // add all edges (ARG)
    // build string of form sourceId#targetId#
    StringBuilder edgeRep = new StringBuilder();
    edgeRep.append(pSource.getElementId());
    edgeRep.append(Separators.commonSeparator);
    // if target element is covered -> set covering element as target
    if (pTarget.isCovered()) {
      PredicateAbstractElement predicate;
      predicate = AbstractElements.extractElementByType(pTarget, PredicateAbstractElement.class);
      if (predicate == null) { return false; }
      if (predicate.isAbstractionElement()) {
        pTarget = getFinalCoveringElement(pTarget);
      }
    }
    if (pTarget == null) { return false; }
    edgeRep.append(pTarget.getElementId());
    edgeRep.append(Separators.commonSeparator);
    edges.add(edgeRep.toString());
    return true;
  }

}