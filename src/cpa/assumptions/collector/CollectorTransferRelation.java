/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.assumptions.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import assumptions.AssumptionWithLocation;
import assumptions.FormulaReportingUtils;
import assumptions.Assumption;

import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import cfa.objectmodel.CFAEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPATransferException;

/**
 * Transfer relation and strengthening for the DumpInvariant CPA
 * @author g.theoduloz
 */
public class CollectorTransferRelation implements TransferRelation {

  private final SymbolicFormulaManager symbolicManager;

  public CollectorTransferRelation(CollectorCPA cpa)
  {
    symbolicManager = cpa.getSymbolicFormulaManager();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge cfaEdge)
      throws CPATransferException {
    return Collections.singleton(CollectorElement.TOP);
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement el, List<AbstractElement> others, CFAEdge edge, Precision p)
  throws CPATransferException
  {
    boolean dumpAvoidanceAssumption = false;
    AssumptionWithLocation result = null;

    // collect invariants and determine whether we need to add an invariant
    // to avoid the current node.
    for (AbstractElement other : others) {
      if (other instanceof AssumptionReportingElement) {
        AssumptionWithLocation otherAssumption = ((AssumptionReportingElement)other).getAssumptionWithLocation();
        if (otherAssumption != null) {
          if (result == null)
            result = otherAssumption;
          else
            result = result.and(otherAssumption);
        }
      }
      if (other instanceof AvoidanceReportingElement) {
        if (((AvoidanceReportingElement)other).mustDumpAssumptionForAvoidance())
          dumpAvoidanceAssumption = true;
      }
    }

    // if necessary, add an invariant to avoid the current node
    if (dumpAvoidanceAssumption) {
      // collect data
      SymbolicFormula avoidanceInvariantFormula = symbolicManager.makeTrue();
      for (AbstractElement other : others) {
        SymbolicFormula reported = FormulaReportingUtils.extractReportedFormulas(symbolicManager, other);
        if (reported != null)
          avoidanceInvariantFormula = symbolicManager.makeAnd(avoidanceInvariantFormula, reported);
      }
      Assumption assumptionData = new Assumption(avoidanceInvariantFormula, false);
      AssumptionWithLocation avoidanceAssumption = assumptionData.atLocation(edge.getSuccessor());
      
      // add the invariant
      if (result == null)
        result = avoidanceAssumption;
      else
        result = result.and(avoidanceAssumption);
    }

    List<CollectorElement> retList = new ArrayList<CollectorElement>();

    if (result != null) {
      retList.add(new CollectorElement(result));
      return retList;
    } else {
      return null;
    }
  }

}
