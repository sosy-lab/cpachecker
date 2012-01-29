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

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransitionType;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGFullyAbstracted;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.InterpolationTreeNode;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

/**
 * Manager for fully-abstracted environmental transitions.
 */
public class RGFullyAbstractedManager extends RGEnvTransitionManagerFactory {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final LogManager logger;

  private static RGFullyAbstractedManager singleton;

  protected RGFullyAbstractedManager(FormulaManager fManager, PathFormulaManager pfManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, Configuration config, LogManager logger){
    this.fManager = fManager;
    this.pfManager = pfManager;
    this.ssaManager = ssaManager;
    this.thmProver = thmProver;
    this.rManager  = rManager;
    this.logger = logger;
  }

  @Override
  public RGEnvTransition generateEnvTransition(RGEnvCandidate pCand) {
    // TODO Auto-generated method stub
    return null;
  }



  @Override
  public PathFormula formulaForAbstraction(PathFormula pPf, RGEnvTransition pEt) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PathFormula formulaForRefinement(PathFormula pPf, RGEnvTransition pEt,
      int pUnique) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<AbstractionPredicate> getPredicates(Formula pItp,
      InterpolationTreeNode pNode) {
    // TODO Auto-generated method stub
    return null;
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
