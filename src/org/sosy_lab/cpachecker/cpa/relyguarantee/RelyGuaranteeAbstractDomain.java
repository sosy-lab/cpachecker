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

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractDomain;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cpa.relyguarantee")

public class RelyGuaranteeAbstractDomain extends PredicateAbstractDomain {

  public RelyGuaranteeAbstractDomain(PredicateCPA pCpa)
      throws InvalidConfigurationException {
    super(pCpa);
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean isLessOrEqual(AbstractElement element1,
                                       AbstractElement element2) throws CPAException {
    coverageCheckTimer.start();
    try {

      RelyGuaranteeAbstractElement e1 = (RelyGuaranteeAbstractElement)element1;
      RelyGuaranteeAbstractElement e2 = (RelyGuaranteeAbstractElement)element2;

    // TODO time statistics (previously in formula manager)
    /*
  long start = System.currentTimeMillis();
  entails(f1, f2);
  long end = System.currentTimeMillis();
  stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
      (end - start));
  stats.bddCoverageCheckTime += (end - start);
  ++stats.numCoverageChecks;
     */

    if (e1 instanceof RelyGuaranteeAbstractElement.AbstractionElement && e2 instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
      bddCoverageCheckTimer.start();

      // if e1's predicate abstraction entails e2's pred. abst.
      boolean result = mRegionManager.entails(e1.getAbstractionFormula().asRegion(), e2.getAbstractionFormula().asRegion());

      bddCoverageCheckTimer.stop();
      return result;

    } else if (e2 instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
      if (symbolicCoverageCheck) {
        symbolicCoverageCheckTimer.start();

        boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());

        symbolicCoverageCheckTimer.stop();
        return result;

      } else {
        return false;
      }

    } else if (e1 instanceof RelyGuaranteeAbstractElement.AbstractionElement) {
      return false;

    } else {
      // only the fast check which returns true if a merge occurred for this element
      return e1.getMergedInto() == e2;
    }

    } finally {
      coverageCheckTimer.stop();
    }
  }

}