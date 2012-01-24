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
package org.sosy_lab.cpachecker.cpa.impact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

class ImpactTransferRelation implements TransferRelation {

  private final LogManager logger;

  private final BlockOperator blk;
  private final FormulaManager fmgr;
  private final PathFormulaManager pfmgr;
  private final TheoremProver prover;

  public ImpactTransferRelation(LogManager pLogger, BlockOperator pBlk,
      FormulaManager pFmgr, PathFormulaManager pPfmgr, TheoremProver pProver) {
    logger = pLogger;
    blk = pBlk;
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    prover = pProver;
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
      newElement = new NonAbstractionElement(newPathFormula, ImpactAbstractElement.getLastAbstraction(element));
    }

    return Collections.singleton(newElement);
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

        return strengthenSatCheck(element);
      }
    }

    return null;
  }

  private Collection<? extends AbstractElement> strengthenSatCheck(ImpactAbstractElement pElement) {
    logger.log(Level.FINEST, "Checking for feasibility of path because error has been found");

    Formula f = fmgr.makeAnd(pElement.getStateFormula(), pElement.getPathFormula().getFormula());

    boolean unsat;
    try {
      prover.init();
      unsat = prover.isUnsat(f);
    } finally {
      prover.reset();
    }

    if (unsat) {
      logger.log(Level.FINEST, "Path is infeasible.");
      return Collections.emptySet();

    } else {
      // although this is not an abstraction location, we fake an abstraction
      // because refinement code expects it to be like this
      logger.log(Level.FINEST, "Last part of the path is not infeasible.");

      // set abstraction to true (we don't know better)
      PathFormula blockFormula = pElement.getPathFormula();
      PathFormula newPathFormula = pfmgr.makeEmptyPathFormula(blockFormula);
      return Collections.singleton(new AbstractionElement(newPathFormula, fmgr.makeTrue(), blockFormula));
    }
  }
}