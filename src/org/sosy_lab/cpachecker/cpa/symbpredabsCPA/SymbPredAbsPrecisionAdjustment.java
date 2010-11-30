/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsAbstractElement.ComputeAbstractionElement;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Abstraction;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionPredicate;

public class SymbPredAbsPrecisionAdjustment implements PrecisionAdjustment {
  
  // statistics
  final Timer totalPrecTime = new Timer();
  final Timer computingAbstractionTime = new Timer();

  int numAbstractions = 0;
  int numAbstractionsFalse = 0;
  int maxBlockSize = 0;
  int maxPredsPerAbstraction = 0;

  private final LogManager logger;
  private final SymbPredAbsFormulaManager formulaManager;
  
  public SymbPredAbsPrecisionAdjustment(SymbPredAbsCPA pCpa) {
    logger = pCpa.getLogger();
    formulaManager = pCpa.getFormulaManager();
  }
  
  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) {

    totalPrecTime.start();
    
    if (pElement instanceof ComputeAbstractionElement) {
      ComputeAbstractionElement element = (ComputeAbstractionElement)pElement;
      SymbPredAbsPrecision precision = (SymbPredAbsPrecision)pPrecision;
      
      pElement = computeAbstraction(element, precision);
    }
    
    totalPrecTime.stop();
    return new Triple<AbstractElement, Precision, Action>(
        pElement, pPrecision, Action.CONTINUE);
  }
  
  /**
   * Compute an abstraction.
   */
  private AbstractElement computeAbstraction(
      ComputeAbstractionElement element,
      SymbPredAbsPrecision precision) {

    Abstraction abstraction = element.getAbstraction();
    PathFormula pathFormula = element.getPathFormula();
    CFANode loc = element.getLocation();
    
    numAbstractions++;
    logger.log(Level.FINEST, "Computing abstraction on node", loc);

    Collection<AbstractionPredicate> preds = precision.getPredicates(loc);

    maxBlockSize = Math.max(maxBlockSize, pathFormula.getLength());
    maxPredsPerAbstraction = Math.max(maxPredsPerAbstraction, preds.size());

    computingAbstractionTime.start();

    // compute new abstraction
    Abstraction newAbstraction = formulaManager.buildAbstraction(
        abstraction, pathFormula, preds);

    computingAbstractionTime.stop();

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstraction.isFalse()) {
      numAbstractionsFalse++;
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
    }

    // create new empty path formula
    PathFormula newPathFormula = formulaManager.makeEmptyPathFormula(pathFormula);

    return new SymbPredAbsAbstractElement.AbstractionElement(newPathFormula, newAbstraction);
  }
}
