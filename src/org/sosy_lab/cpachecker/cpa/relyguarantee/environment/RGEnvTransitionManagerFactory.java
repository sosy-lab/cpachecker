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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

/**
 *  Creates {@link RGEnvTransitionManager} appropriate for the abstraction level.
 */
public abstract class RGEnvTransitionManagerFactory implements RGEnvTransitionManager, StatisticsProvider {

  private static RGEnvTransitionManagerFactory singleton;

  public static RGEnvTransitionManagerFactory getInstance(int abstractionLevel, FormulaManager fManager, PathFormulaManager pfManager, PredicateAbstractionManager paManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, RGVariables variables, Configuration config, LogManager logger){
    if (singleton == null){
      // instantiate to appriorate manager
      switch (abstractionLevel){
      case 0 :  singleton = new RGSimpleTransitionManager(fManager, pfManager, paManager, ssaManager, thmProver, rManager, variables, config, logger);
                break;
      case 1 :  singleton = new RGSemiAbstractedManager(fManager, pfManager, paManager, ssaManager, thmProver, rManager, variables, config, logger);
                break;
      case 2 :  singleton = new RGFullyAbstractedManager(fManager, pfManager, paManager, ssaManager, thmProver, rManager, variables, config, logger);
                break;
      default: throw new UnsupportedOperationException("Unknown abstraction level "+abstractionLevel);
      }
    }

    return singleton;
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
