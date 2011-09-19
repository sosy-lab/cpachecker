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

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.NonAbstractionElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

class ImpactMergeOperator implements MergeOperator {

  private final LogManager logger;
  private final PathFormulaManager formulaManager;

  final Timer totalMergeTime = new Timer();

  public ImpactMergeOperator(LogManager pLogger, PathFormulaManager pPfmgr) {
    logger = pLogger;
    formulaManager = pPfmgr;
  }

  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    ImpactAbstractElement elem1 = (ImpactAbstractElement)element1;
    ImpactAbstractElement elem2 = (ImpactAbstractElement)element2;

    // this will be the merged element
    ImpactAbstractElement merged;

    if (elem1.isAbstractionElement() || elem2.isAbstractionElement()) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      AbstractionElement absElement1 = ImpactAbstractElement.getLastAbstraction(elem1);
      AbstractionElement absElement2 = ImpactAbstractElement.getLastAbstraction(elem2);

      // don't merge if the elements are in different blocks (they have different abstractions)
      if (absElement1 != absElement2) {
        merged = elem2;

      } else {
        totalMergeTime.start();
        // create a new element

        logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

        PathFormula pathFormula = formulaManager.makeOr(elem1.getPathFormula(), elem2.getPathFormula());

        logger.log(Level.ALL, "New path formula is", pathFormula);

        merged = new ImpactAbstractElement.NonAbstractionElement(pathFormula, absElement1);

        // now mark elem1 so that coverage check can find out it was merged
        ((NonAbstractionElement)elem1).setMergedInto(merged);

        totalMergeTime.stop();
      }
    }

    return merged;
  }
}
