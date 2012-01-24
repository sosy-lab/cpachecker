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

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.impact.ImpactAbstractElement.NonAbstractionElement;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

class ImpactAbstractDomain implements AbstractDomain {

  private final FormulaManager fmgr;
  private final TheoremProver prover;

  public ImpactAbstractDomain(FormulaManager pFmgr, TheoremProver pProver) {
    fmgr = pFmgr;
    prover = pProver;
  }

  @Override
  public AbstractElement join(AbstractElement pElement1, AbstractElement pElement2) {
    throw new UnsupportedOperationException();
/*
    LazyAbstractionElement element1 = (LazyAbstractionElement)pElement1;
    LazyAbstractionElement element2 = (LazyAbstractionElement)pElement2;

    if (element1.getStateFormula().equals(element2.getStateFormula())) {
      PathFormula mergedFormula = pfmgr.makeOr(element1.getPathFormula(), element2.getPathFormula());
      return new LazyAbstractionElement(mergedFormula, element1.getStateFormula());
    }
*/
  }

  @Override
  public boolean isLessOrEqual(AbstractElement pElement1, AbstractElement pElement2) {

    ImpactAbstractElement element1 = (ImpactAbstractElement)pElement1;
    ImpactAbstractElement element2 = (ImpactAbstractElement)pElement2;

    if (element1 instanceof NonAbstractionElement) {
      if (((NonAbstractionElement)element1).getMergedInto() == element2) {
        return true;
      }
    }

    if (element1.isAbstractionElement() && element2.isAbstractionElement()) {

      Formula f1 = element1.getStateFormula();
      Formula f2 = element2.getStateFormula();

      Formula implication = fmgr.makeAnd(f1, fmgr.makeNot(f2));

      prover.init();
      try {
        return prover.isUnsat(implication);
      } finally {
        prover.reset();
      }
    }

    return false;
  }

}
