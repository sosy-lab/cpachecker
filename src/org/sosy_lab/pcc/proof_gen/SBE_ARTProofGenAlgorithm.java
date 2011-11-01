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

import java.util.Iterator;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.pcc.common.AbstractionType;
import org.sosy_lab.pcc.common.FormulaHandler;

public class SBE_ARTProofGenAlgorithm extends ARTProofGenAlgorithm {

  private FormulaHandler fh;

  public SBE_ARTProofGenAlgorithm(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    fh = new FormulaHandler(pConfig, pLogger);
  }

  @Override
  protected boolean addARTNode(ARTElement pNode) {
    StringBuilder nodeRep = new StringBuilder();
    // build string of form ARTId#CFAId#isAbstractionNode(#Abstraction)?#
    nodeRep.append(pNode.getElementId());
    nodeRep.append("#");
    nodeRep.append(pNode.retrieveLocationElement().getLocationNode()
        .getNodeNumber());
    nodeRep.append("#");
    // get PredicateAbstractElement
    PredicateAbstractElement predicate =
        getWrappedPredicateAbstractElement(pNode);
    if (predicate == null) { return false; }
    // isAbstractionNode == true --> write 1 otherwise 0
    if (predicate.isAbstractionElement()) {
      nodeRep.append(AbstractionType.Abstraction);
      nodeRep.append("#");
      nodeRep.append(predicate.getAbstractionFormula().asFormula().toString());
    } else {
      nodeRep.append(AbstractionType.NeverAbstraction);
    }
    nodeRep.append("#");
    nodes.add(nodeRep.toString());
    return true;
  }

  private PredicateAbstractElement getWrappedPredicateAbstractElement(
      ARTElement pNode) {
    AbstractElement wrappedElement;
    Iterator<? extends AbstractElement> it =
        pNode.getWrappedElements().iterator();
    while (it.hasNext()) {
      wrappedElement = it.next();
      if (wrappedElement instanceof PredicateAbstractElement) { return (PredicateAbstractElement) wrappedElement; }
    }
    return null;
  }

  @Override
  protected boolean addARTEdge(ARTElement pSource, CFAEdge pEdge,
      ARTElement pTarget) {
    StringBuilder edgeRep = new StringBuilder();
    // build string of form sourceId#targetId#operation#
    edgeRep.append(pSource.getElementId());
    edgeRep.append("#");
    edgeRep.append(pTarget.getElementId());
    edgeRep.append("#");
    // append operation
    edgeRep.append(getEdgeOperationFormula(pSource, pEdge));
    edgeRep.append("#");
    //add operation
    String operation = getEdgeOperationFormula(pSource, pEdge);
    if (operation != null) {
      edgeRep.append(operation);
      edges.add(edgeRep.toString());
      return true;
    } else {
      return false;
    }
  }

  private String getEdgeOperationFormula(ARTElement pSource, CFAEdge pEdge) {
    // get wrapped PredicateAbstractionElement
    PredicateAbstractElement predicate =
        getWrappedPredicateAbstractElement(pSource);
    return fh.getEdgeOperationWithSSA(predicate.getPathFormula(), pEdge);
  }

}