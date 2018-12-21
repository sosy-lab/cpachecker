/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDCPA;
import org.sosy_lab.cpachecker.cpa.bdd.BDDState;
import org.sosy_lab.cpachecker.cpa.bdd.BDDTransferRelation;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class BDDUtils {

  private BDDCPA bddCpa;
  private LogManager logger;

  public BDDUtils(ConfigurableProgramAnalysis pCPA, LogManager pLogger) {
    bddCpa = getBddCpa(pCPA);
    logger = pLogger;
  }

  public boolean isVariabilityAware() {
    return bddCpa != null;
  }

  public BDDCPA getBddCpa(ConfigurableProgramAnalysis pCpa) {
    if (pCpa instanceof WrapperCPA) {
      // TODO: This returns the *first* BDDCPA. Currently I cannot get/match which name the cpa
      // has in the config. Might lead to problems when more than one BDDCPA is configured.
      BDDCPA bddCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(BDDCPA.class);
      return bddCpa;
    } else if (pCpa instanceof BDDCPA) {
      return ((BDDCPA) pCpa);
    }

    return null;
  }

  public String dumpRegion(Region pPresenceCondition) {
    if (pPresenceCondition == null || bddCpa == null) {
      return "";
    }

    return bddCpa.getManager().dumpRegion(pPresenceCondition).toString();
  }

  public Region makeTrue() {
    if (bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().makeTrue();
  }

  public Region makeFalse() {
    if (bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().makeFalse();
  }

  public Region makeNot(Region pRegion) {
    if (pRegion == null || bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().makeNot(pRegion);
  }

  public Region makeOr(Region pRegion1, Region pRegion2) {
    if (pRegion1 == null || pRegion2 == null || bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().makeOr(pRegion1, pRegion2);
  }

  public Region makeAnd(Region pRegion1, Region pRegion2) {
    if (pRegion1 == null || pRegion2 == null || bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().makeAnd(pRegion1, pRegion2);
  }

  // public static NamedRegionManager getBddCpaNamedRegionManagerFromCpa(
  // ConfigurableProgramAnalysis pCpa,
  // boolean pUseTigerAlgorithm_with_pc)
  // throws InvalidConfigurationException {
  // NamedRegionManager bddCpaNamedRegionManager = null;
  //
  // if (pUseTigerAlgorithm_with_pc) {
  // if (pCpa instanceof WrapperCPA) {
  // // TODO: This returns the *first* BDDCPA. Currently I cannot get/match which name the cpa
  // // has in the config. Might lead to problems when more than one BDDCPA is configured.
  // BDDCPA bddcpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(BDDCPA.class);
  // if (bddcpa != null) {
  // bddCpaNamedRegionManager = bddcpa.getManager();
  // } else {
  // throw new InvalidConfigurationException(
  // "CPAtiger-variability-aware started without BDDCPA. We need BDDCPA!");
  // }
  // } else if (pCpa instanceof BDDCPA) {
  // bddCpaNamedRegionManager = ((BDDCPA) pCpa).getManager();
  // }
  // }
  //
  // return bddCpaNamedRegionManager;
  // }

  public Region createPredicate(String pName) {
    if (pName == null || bddCpa == null) {
      return null;
    }

    return bddCpa.getManager().createPredicate(pName);
  }

  public Region getRegionFromWrappedBDDstate(AbstractState pAbstractState) {
    if (!isVariabilityAware()) {
      return null;
    }

    // TODO: This returns the *first* BDDCPAState. Currently I cannot get/match which name the cpa
    // has in the config. Might lead to problems when more than one BDDCPA is configured.
    BDDState wrappedBDDState = getWrappedBDDState(pAbstractState);
    if (wrappedBDDState == null) {
      throw new RuntimeException("Did not find a BDD state component in a state!");
    }
    Region bddStateRegion = wrappedBDDState.getRegion();

    // assert wrappedBDDState.getNamedRegionManager() == bddCpaNamedRegionManager;

    return bddStateRegion;
  }

  private BDDState getWrappedBDDState(AbstractState inState) {
    if (inState instanceof BDDState) {
      return (BDDState) inState;
    } else if (inState instanceof AbstractWrapperState) {
      for (AbstractState subState : ((AbstractWrapperState) inState).getWrappedStates()) {
        if (subState instanceof BDDState) {
          return (BDDState) subState;
        } else if (subState instanceof AbstractWrapperState) {
          BDDState res = getWrappedBDDState(subState);
          if (res != null) {
            return res;
          }
        }
      }
    }

    return null;
  }

  public Region composeRemainingPresenceConditions(Goal pGoal, TestSuite testsuite) {
    if (bddCpa == null) {
      return null;
    }

    return makeOr(makeFalse(), testsuite.getRemainingPresenceCondition(pGoal));
  }

  public void restrictBdd(Region pRemainingPresenceCondition) {
    if (pRemainingPresenceCondition == null || bddCpa == null) {
      return;
    }

    if (bddCpa.getTransferRelation() instanceof BDDTransferRelation) {
      BDDTransferRelation transferRelation = ((BDDTransferRelation) bddCpa.getTransferRelation());
      transferRelation.setGlobalConstraint(pRemainingPresenceCondition);
      // logger.logf(Level.INFO, "Restrict global BDD.");
      logger.logf(
          Level.INFO,
          "Restrict BDD to %s.",
          dumpRegion(pRemainingPresenceCondition));
    }
  }

}
