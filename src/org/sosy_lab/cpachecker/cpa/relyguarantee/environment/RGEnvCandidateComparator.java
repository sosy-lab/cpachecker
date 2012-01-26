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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;


/**
 * Compares candidates for environmental transitions.
 */
public abstract class RGEnvCandidateComparator {

  public static RGEnvCandidateComparator getComparator(int abstraction, TheoremProver thmProver, FormulaManager fManager, RegionManager rManager){
    if (abstraction == 2){
      return new RGEnvCandidateComparatorFA(thmProver, fManager, rManager);
    }
    return null;
  }

  /**
   * Returns true only if c1 is less or equal to c2.
   * @param c1
   * @param c2
   * @return
   */
  public abstract boolean isLessOrEqual(RGEnvCandidate c1, RGEnvCandidate c2);


  private static class RGEnvCandidateComparatorFA extends RGEnvCandidateComparator{

    protected final TheoremProver thmProver;
    protected final FormulaManager fManager;
    protected final RegionManager rManager;

    public RGEnvCandidateComparatorFA(TheoremProver thmProver, FormulaManager fManager, RegionManager rManager){
      this.thmProver = thmProver;
      this.fManager  = fManager;
      this.rManager  = rManager;
    }

    @Override
    public boolean isLessOrEqual(RGEnvCandidate c1, RGEnvCandidate c2) {
      /*
       * c1 is less or equal to c2 iff (psi1^phi1^psi1'^phi1') -> (psi2^phi2^psi2'^phi2'),
       * where unprimed and primed formulas refer to element and successor element.
       */

      RGAbstractElement rgSucc1 = c1.getRgSuccessor();
      RGAbstractElement rgSucc2 = c2.getRgSuccessor();

      AbstractionFormula abs1 = rgSucc1.getAbstractionFormula();
      AbstractionFormula abs2 = rgSucc2.getAbstractionFormula();
      Formula f1 = rgSucc1.getPathFormula().getFormula();
      Formula f2 = rgSucc2.getPathFormula().getFormula();

      if (f1.isTrue() && f2.isTrue()){
        // we may use bdd for a precheck
        Region r1 = abs1.asRegion();
        Region r2 = abs2.asRegion();
        return rManager.entails(r1, r2);
      }

      f1 = fManager.makeAnd(abs1.asFormula(), f1);
      f2 = fManager.makeAnd(abs2.asFormula(), f2);
      Formula nf2 = fManager.makeNot(f2);
      Formula f = fManager.makeAnd(f1, nf2);

      thmProver.init();
      boolean valid = thmProver.isUnsat(f);
      thmProver.reset();
      return valid;
    }

  }

}





