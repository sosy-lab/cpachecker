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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.Separators;

public class SBEWithIndices_ARTProofGenAlgorithm extends
    SBE_ARTProofGenAlgorithm {

  public SBEWithIndices_ARTProofGenAlgorithm(Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);

  }

  @Override
  protected String getAbstraction(PredicateAbstractElement pPredicate) {
    if (pPredicate == null) { return null; }
    return pPredicate.getAbstractionFormula().asFormula().toString();
  }

  @Override
  protected boolean addARTEdge(ARTElement pSource, CFAEdge pEdge,
      ARTElement pTarget) {
    // build string of form sourceId#targetId#operation#
    StringBuilder edgeRep = new StringBuilder();
    String edgeId = getEdgeIdentification(pSource, pTarget);
    if (edgeId == null || edgeId.length() == 0) { return false; }
    edgeRep.append(edgeId);

    //add operation
    String operation = getEdgeOperationFormula(pSource, pEdge);
    if (operation != null) {
      logger.log(Level.INFO, "Add an ART Edge");
      edgeRep.append(operation);
      edgeRep.append(Separators.commonSeparator);
      edges.add(edgeRep.toString());
      return true;
    } else {
      return false;
    }
  }

  private String getEdgeOperationFormula(ARTElement pSource, CFAEdge pEdge) {
    // get wrapped PredicateAbstractionElement
    PredicateAbstractElement predicate = AbstractElements.extractElementByType(pSource, PredicateAbstractElement.class);
    return fh.getEdgeOperationWithSSA(predicate.getPathFormula(), pEdge);
  }
}