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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.util.logging.Level;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateMergeOperator;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;


public class RelyGuaranteeMergeOperator extends PredicateMergeOperator {


  public RelyGuaranteeMergeOperator(PredicateCPA pCpa) {
    super(pCpa.getLogger(), pCpa.getPathFormulaManager());

  }



  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    RelyGuaranteeAbstractElement elem1 = (RelyGuaranteeAbstractElement)element1;
    RelyGuaranteeAbstractElement elem2 = (RelyGuaranteeAbstractElement)element2;

    // this will be the merged element
    RelyGuaranteeAbstractElement merged;

    if (elem1 instanceof RelyGuaranteeAbstractElement.AbstractionElement || elem2 instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
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

        merged = new RelyGuaranteeAbstractElement(pathFormula, elem1.getAbstractionFormula());

        // now mark elem1 so that coverage check can find out it was merged
        elem1.setMergedInto(merged);

        totalMergeTime.stop();
      }
    }

    return merged;
  }

}
