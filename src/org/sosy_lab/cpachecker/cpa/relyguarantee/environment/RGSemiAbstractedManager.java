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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.InterpolationTreeNode;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Multimap;

/**
 * Manager for semi-abstracted environmental transitions.
 */
public class RGSemiAbstractedManager extends RGEnvTransitionManagerFactory {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final LogManager logger;

  protected RGSemiAbstractedManager(FormulaManager pFManager, PathFormulaManager pPfManager, PredicateAbstractionManager pPaManager, SSAMapManager pSsaManager, TheoremProver pThmProver, RegionManager pRManager, RGVariables variables, Configuration pConfig,  LogManager pLogger) {
    this.fManager = pFManager;
    this.pfManager = pPfManager;
    this.ssaManager = pSsaManager;
    this.thmProver = pThmProver;
    this.rManager = pRManager;
    this.logger  = pLogger;
  }

  @Override
  public RGEnvTransition generateEnvTransition(RGEnvCandidate pCand,
      Collection<AbstractionPredicate> pGlobalPreds,
      Multimap<CFANode, AbstractionPredicate> pLocalPreds) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PathFormula formulaForAbstraction(RGAbstractElement pElem,
      RGEnvTransition pEt) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PathFormula formulaForRefinement(RGAbstractElement pElem,
      RGEnvTransition pEt, int pUnique) {
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
  public boolean isLessOrEqual(RGEnvTransition pEt1, RGEnvTransition pEt2) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub

  }







}
