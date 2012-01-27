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

import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransitionType;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGFullyAbstracted;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

/**
 * Compares candidates for environmental transitions.
 */
public abstract class RGEnvTransitionComparator {

  public static RGEnvTransitionComparator getComparator(int abstraction, TheoremProver thmProver, FormulaManager fManager, RegionManager rManager){
    if (abstraction == 2){
      return new RGEnvTransitionComparatorFA(thmProver, fManager, rManager);
    }
    return null;
  }


  /**
   * Returns true  if et1 is less or equal to et2.
   * @param c1
   * @param c2
   * @return
   */
  public abstract boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2);


  private static class RGEnvTransitionComparatorFA extends RGEnvTransitionComparator{

    protected final TheoremProver thmProver;
    protected final FormulaManager fManager;
    protected final RegionManager rManager;

    public RGEnvTransitionComparatorFA(TheoremProver thmProver, FormulaManager fManager, RegionManager rManager){
      this.thmProver = thmProver;
      this.fManager  = fManager;
      this.rManager  = rManager;
    }

    @Override
    public boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2) {
      assert et1.getRGType() == RGEnvTransitionType.FullyAbstracted;
      assert et1.getRGType() == RGEnvTransitionType.FullyAbstracted;

      RGFullyAbstracted efa1 = (RGFullyAbstracted) et1;
      RGFullyAbstracted efa2 = (RGFullyAbstracted) et2;
      Region r1 = efa1.getAbstractTransitionRegion();
      Region r2 = efa2.getAbstractTransitionRegion();

      return rManager.entails(r1, r2);
    }


  }

}
