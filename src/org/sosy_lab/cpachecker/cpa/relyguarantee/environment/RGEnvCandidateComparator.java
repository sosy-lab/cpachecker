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

import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
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
   * Returns true if c1 is less or equal to c2.
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
      Formula f1 = c1.getRgSuccessor().getAbstractionFormula().asFormula();
      Formula pf1 = c1.getRgSuccessor().getPathFormula().getFormula();
      Formula f2 = c2.getRgSuccessor().getAbstractionFormula().asFormula();
      Formula pf2 = c2.getRgSuccessor().getPathFormula().getFormula();

      if (f1.isFalse() || pf1.isFalse() || (f2.isTrue() && pf2.isTrue())){
        return true;
      }

      return false;
    }

  }

}





