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
package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;


/**
 * Merge operator for symbolic predicate abstraction.
 * This is not a trivial merge operator in the sense that it implements
 * mergeSep and mergeJoin together. If the abstract element is on an
 * abstraction location we don't merge, otherwise we merge two elements
 * and update the {@link PredicateAbstractElement}'s pathFormula.
 */
public class PredicateMergeOperator implements MergeOperator {

  protected final LogManager logger;
  protected final PathFormulaManager formulaManager;

  public final Timer totalMergeTime = new Timer();

  public PredicateMergeOperator(PredicateCPA pCpa) {
    this.logger = pCpa.getLogger();
    formulaManager = pCpa.getPathFormulaManager();
  }

  public PredicateMergeOperator(LogManager logger, PathFormulaManager formulaManager){
    this.logger = logger;
    this.formulaManager = formulaManager;
  }

  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    PredicateAbstractElement elem1 = (PredicateAbstractElement)element1;
    PredicateAbstractElement elem2 = (PredicateAbstractElement)element2;

    // this will be the merged element
    PredicateAbstractElement merged;

    if (elem1 instanceof AbstractionElement || elem2 instanceof AbstractionElement) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      // don't merge if the elements are in different blocks (they have different abstractions)
      if (!elem1.getAbstractionFormula().equals(elem2.getAbstractionFormula())) {
        merged = elem2;

      } else {
        totalMergeTime.start();
        // create a new element

        logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

        PathFormula pathFormula = formulaManager.makeOr(elem1.getPathFormula(), elem2.getPathFormula());

        logger.log(Level.ALL, "New path formula is", pathFormula);

        merged = new PredicateAbstractElement(pathFormula, elem1.getAbstractionFormula());

        // now mark elem1 so that coverage check can find out it was merged
        elem1.setMergedInto(merged);

        totalMergeTime.stop();
      }
    }

    return merged;
  }

}
