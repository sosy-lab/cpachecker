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

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;

@Options(prefix="cpa.rg")

public class RGAbstractDomain implements AbstractDomain {

  @Option(description="whether to include the symbolic path formula in the "
      + "coverage checks or do only the fast abstract checks")
    protected boolean symbolicCoverageCheck = false;

  public final Timer coverageCheckTimer = new Timer();
  public final Timer bddCoverageCheckTimer = new Timer();
  public final Timer symbolicCoverageCheckTimer = new Timer();

  protected final RegionManager mRegionManager;
  protected final RGAbstractionManager mgr;

  public RGAbstractDomain(RGCPA pCpa)  throws InvalidConfigurationException {
    pCpa.config.inject(this, RGAbstractDomain.class);
    mRegionManager = pCpa.rManager;
    mgr = pCpa.absManager;
  }

  @Override
  public boolean isLessOrEqual(AbstractElement element1,
                                       AbstractElement element2) throws CPAException {
    coverageCheckTimer.start();
    try {

      RGAbstractElement e1 = (RGAbstractElement)element1;
      RGAbstractElement e2 = (RGAbstractElement)element2;

      Formula f1 = e1.getAbsPathFormula().getFormula();
      Formula f2 = e2.getAbsPathFormula().getFormula();

      if (f1.isFalse() || e1.getAbstractionFormula().isFalse()){
        return true;
      }

      if (f1.isTrue() && f2.isTrue()) {
        bddCoverageCheckTimer.start();

        // if e1's predicate abstraction entails e2's pred. abst.
        boolean result = mRegionManager.entails(e1.getAbstractionFormula().asRegion(), e2.getAbstractionFormula().asRegion());

        bddCoverageCheckTimer.stop();
        return result;

      } else if (f2.isTrue()) {
        if (symbolicCoverageCheck) {
          symbolicCoverageCheckTimer.start();

          boolean result = mgr.checkCoverage(e1.getAbstractionFormula(), e1.getAbsPathFormula(), e2.getAbstractionFormula());

          symbolicCoverageCheckTimer.stop();
          return result;

        } else {
          return false;
        }

      } else if (e1 instanceof RGAbstractElement.AbstractionElement) {
        return false;

      } else{
        return e1.getMergedInto() == e2;
      }


    } finally {
      coverageCheckTimer.stop();
    }
  }

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    throw new UnsupportedOperationException();
  }

}