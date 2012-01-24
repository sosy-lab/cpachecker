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

import java.util.Hashtable;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.pcc.common.Separators;

public class SBEWithIndices_InvariantProofGenAlgorithm extends
    SBE_InvariantProofGenAlgorithm {

  protected Hashtable<String, StringBuilder> operationsPerEdge =
                                                                   new Hashtable<String, StringBuilder>();

  public SBEWithIndices_InvariantProofGenAlgorithm(Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
  }

  @Override
  protected boolean addSingleOperation(ARTElement pNode, CFAEdge pEdge) {
    PredicateAbstractElement predicate =
        AbstractElements.extractElementByType(pNode,
            PredicateAbstractElement.class);
    if (predicate == null) { return false; }
    String operation =
        fh.getEdgeOperationWithSSA(predicate.getPathFormula(), pEdge);
    if (operation == null) { return false; }
    // build identification of edge
    String id =
        buildEdgeId(extractLocation(pNode), pEdge);
    StringBuilder current = operationsPerEdge.get(id);
    // add operation
    logger.log(Level.INFO,"Add an edge which connects two regions");
    if (current == null) {
      operationsPerEdge.put(id, new StringBuilder(operation
          + Separators.commonSeparator));
    } else {
      current.append(operation + Separators.commonSeparator);
    }
    return true;
  }

  @Override
  protected String getAbstraction(PredicateAbstractElement pPredicate) {
    if (pPredicate == null) { return null; }
    return pPredicate.getAbstractionFormula().asFormula().toString();
  }

  @Override
  protected StringBuilder writeOperations() {
    StringBuilder output = new StringBuilder();
    StringBuilder toWrite;
    for (String edge : operationsPerEdge.keySet()) {
      output.append(edge + Separators.commonSeparator);
      toWrite = operationsPerEdge.get(edge);
      output.append(countNumOccurrences(toWrite, Separators.commonSeparator)
          + Separators.commonSeparator + toWrite);
    }
    return output;
  }

}
