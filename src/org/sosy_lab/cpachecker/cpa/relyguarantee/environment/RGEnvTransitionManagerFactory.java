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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
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

  public static RGEnvTransitionManagerFactory getInstance(String abstractionLevel, FormulaManager fManager, PathFormulaManager pfManager, RGAbstractionManager absManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, ParallelCFAS pcfa, Configuration config, LogManager logger) throws InvalidConfigurationException{
    if (singleton == null){

      // instantiate to the appriorate manager
      if (abstractionLevel.equals("ST")){
        singleton = new RGSimpleTransitionManager(fManager, pfManager, absManager, ssaManager, thmProver, rManager, pcfa, config, logger);
      } else if (abstractionLevel.equals("SA")){
        singleton = new RGSemiAbstractedManager(fManager, pfManager, absManager, ssaManager, thmProver, rManager, pcfa, config, logger);
      } else if (abstractionLevel.equals("FA")){
        singleton = new RGFullyAbstractedManager(fManager, pfManager, absManager, ssaManager, thmProver, rManager, pcfa, config, logger);
      } else {
        throw new UnsupportedOperationException("Unknown abstraction level: "+abstractionLevel);
      }
    }

    return singleton;
  }
}
