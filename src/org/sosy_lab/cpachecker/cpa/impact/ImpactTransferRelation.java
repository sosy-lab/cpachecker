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
package org.sosy_lab.cpachecker.cpa.impact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.NonAbstractionElement;
import org.sosy_lab.cpachecker.cpa.predicate.BlockOperator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

class ImpactTransferRelation implements TransferRelation {

  private final BlockOperator blk;
  private final FormulaManager fmgr;
  private final PathFormulaManager pfmgr;

  public ImpactTransferRelation(FormulaManager pFmgr, PathFormulaManager pPfmgr, BlockOperator pBlk) {
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    blk = pBlk;
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    ImpactAbstractElement element = (ImpactAbstractElement)pElement;

    PathFormula newPathFormula = pfmgr.makeAnd(element.getPathFormula(), pCfaEdge);

    ImpactAbstractElement newElement;

    if (blk.isBlockEnd(pCfaEdge.getSuccessor(), newPathFormula)) {
      PathFormula blockFormula = newPathFormula;
      newPathFormula = pfmgr.makeEmptyPathFormula(blockFormula);

      newElement = new AbstractionElement(newPathFormula, fmgr.makeTrue(), blockFormula);

    } else {
      newElement = new NonAbstractionElement(newPathFormula, getLastAbstraction(element));
    }

    return Collections.singleton(newElement);
  }

  private AbstractionElement getLastAbstraction(ImpactAbstractElement element) {
    if (element.isAbstractionElement()) {
      return (AbstractionElement)element;
    } else {
      return ((NonAbstractionElement)element).getLastAbstraction();
    }
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    ImpactAbstractElement element = (ImpactAbstractElement)pElement;
    if (element.isAbstractionElement()) {
      // no need to do anything
      return null;
    }

    for (AbstractElement lElement : pOtherElements) {
      if (AbstractElements.isTargetElement(lElement)) {

        // force abstraction at target states
        PathFormula blockFormula = element.getPathFormula();
        PathFormula newPathFormula = pfmgr.makeEmptyPathFormula(blockFormula);

        return Collections.singleton(new AbstractionElement(newPathFormula, fmgr.makeTrue(), blockFormula));
      }
    }

    return null;
  }
}