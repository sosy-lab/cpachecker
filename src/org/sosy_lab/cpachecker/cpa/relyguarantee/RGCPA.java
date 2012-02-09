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

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.RGCFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvTransitionManagerFactory;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.RGLocationRefinementManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.RGRefinementManager;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMapManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableSet;



@Options(prefix="cpa.rg")
public class RGCPA implements ConfigurableProgramAnalysis, StatisticsProvider{

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RGCPA.class);
  }

  @Option(name="abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"},
      description="which solver to use?")
      private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = true;

  @Option(name="interpolation.changesolverontimeout",
      description="try second interpolating solver if the first takes too long")
      private boolean changeItpSolveOTF = false;

  @Option(toUppercase=true, values={"FA", "SA", "ST"},
      description="How to abstract environmental transitions:"
      + "ST - no abstraction, SA - precondition abstracted only, FA - precondition and operation abstracted")
  private String abstractEnvTransitions = "FA";

  private int tid;
  public RGVariables variables;

  protected final Configuration config;
  protected final LogManager logger;
  protected final RGAbstractDomain domain;
  protected final RGTransferRelation transfer;
  protected final RGMergeOperator merge;
  protected final RGPrecisionAdjustment prec;
  protected final StopOperator stop;

  protected RGPrecision initialPrecision;
  protected AbstractElement topElement;
  private int tidNo;
  private RGCFA[] cfas;

  // managers
  protected final RegionManager rManager;
  protected final FormulaManager fManager;
  protected final PathFormulaManager pfManager;
  protected final TheoremProver thmProver;
  protected final RGAbstractionManager absManager;
  protected final AbstractionManager aManager;
  protected final SSAMapManager ssaManager;
  protected final RGEnvTransitionManager etManager;
  protected final RGRefinementManager<?, ?> refManager;
  private RGEnvironmentManager envManager;
  protected final InterpolatingTheoremProver<Integer> itpProver;
  protected  RGLocationRefinementManager locrefManager;


  @Option(name="blk.useCache", description="use caching of path formulas")
  private boolean useCache = true;

  private RGCPAStatistics stats;







  public RGCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    config.inject(this, RGCPA.class);

    this.rManager = BDDRegionManager.getInstance();
    MathsatFormulaManager mathsatFormulaManager = MathsatFormulaManager.getInstance(config, logger);
    this.fManager = mathsatFormulaManager;

    PathFormulaManager pfMgr  = PathFormulaManagerImpl.getInstance(mathsatFormulaManager, config, logger);
    if (useCache) {
      pfMgr = CachingPathFormulaManager.getInstance(pfMgr);
    }
    this.pfManager = pfMgr;

    /*if (whichProver.equals("MATHSAT")) {
      this.theoremProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      this.theoremProver = new YicesTheoremProver(mathsatFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }*/

    if (whichProver.equals("MATHSAT")) {
      thmProver =  MathsatTheoremProver.getInstance(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      thmProver =  YicesTheoremProver.getInstance(mathsatFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> alternativeItpProver = null;
    if (whichItpProver.equals("MATHSAT")) {
      this.itpProver = MathsatInterpolatingProver.getInstance(mathsatFormulaManager, false);
      if(changeItpSolveOTF){
        alternativeItpProver = CSIsatInterpolatingProver.getInstance(mathsatFormulaManager, logger);
      }
    } else if (whichItpProver.equals("CSISAT")) {
      this.itpProver = CSIsatInterpolatingProver.getInstance(mathsatFormulaManager, logger);
      if(changeItpSolveOTF){
        alternativeItpProver = MathsatInterpolatingProver.getInstance(mathsatFormulaManager, false);
      }
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    this.absManager = RGAbstractionManager.getInstance(rManager, fManager, pfManager, thmProver, config, logger);
    this.aManager = AbstractionManagerImpl.getInstance(rManager, mathsatFormulaManager, pfManager, config, logger);
    this.ssaManager = SSAMapManagerImpl.getInstance(fManager, config, logger);
    this.etManager  = RGEnvTransitionManagerFactory.getInstance(abstractEnvTransitions, fManager, pfManager, absManager, ssaManager, thmProver, rManager, variables, config, logger);
    this.refManager = RGRefinementManager.getInstance(rManager, fManager,  ssaManager, pfManager, etManager, thmProver, itpProver, alternativeItpProver, config, logger);


    this.transfer = new RGTransferRelation(this);
    this.domain = new RGAbstractDomain(this);
    this.merge = new RGMergeOperator(this);
    this.prec = new RGPrecisionAdjustment(this);
    this.stop = new StopSepOperator(domain);
    this.stats = new RGCPAStatistics(this);



  }

  public void setData(int tid,RGVariables variables, RGEnvironmentManager envManager, RGCFA[] cfas) throws InvalidConfigurationException{
    this.tid = tid;
    this.cfas = cfas;
    this.variables = variables;
    Collection<AbstractionPredicate> predicates = null;

    if (checkBlockFeasibility) {
      AbstractionPredicate p = absManager.makeFalsePredicate();
      predicates = ImmutableSet.of(p);
    }
    // TODO make-shift solution
    this.topElement = new RGAbstractElement.AbstractionElement(pfManager.makeEmptyPathFormula(), absManager.makeTrueAbstractionFormula(null),  tid, pfManager.makeEmptyPathFormula(), null);

    this.initialPrecision= new RGPrecision(predicates);

    this.envManager = envManager;

    this.locrefManager = RGLocationRefinementManager.getInstance(fManager, pfManager, etManager, absManager, ssaManager, thmProver, rManager, envManager, cfas, variables, config, logger);

  }

  public int getTid(){
    return this.tid;
  }

  @Override
  public AbstractElement getInitialElement(CFANode node) {
    return topElement;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    return;
  }

  RGCPAStatistics getStats() {
    return null;
  }

  public RGRefinementManager<?, ?> getRelyGuaranteeManager() {
    return this.refManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.transfer;
  }


  @Override
  public MergeOperator getMergeOperator() {
    return this.merge;
  }


  @Override
  public StopOperator getStopOperator() {
    return this.stop;
  }


  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.prec;
  }


  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return this.initialPrecision;
  }

  public Configuration getConfiguration() {
    return this.config;
  }

  public LogManager getLogger() {
    return this.logger;
  }

  public RegionManager getrManager() {
    return rManager;
  }

  public FormulaManager getFormulaManager() {
    return fManager;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pfManager;
  }

  public RGAbstractionManager getAbstractionManager() {
    return absManager;
  }

  public SSAMapManager getSsaManager() {
    return ssaManager;
  }

  public RGEnvTransitionManager getEtManager() {
    return etManager;
  }

  public RGLocationRefinementManager getLocrefManager() {
    return locrefManager;
  }


  /**
   * Finds the last rely-guarantee abstraction element that is an ancestor of the argument.
   * @param pElement
   * @return
   */
  public static ARTElement findLastAbstractionARTElement(ARTElement pElement) {
    // TODO put in some more sutiable class
    ARTElement laARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();
    toProcess.add(pElement);

    while (!toProcess.isEmpty()){
      ARTElement element = toProcess.poll();
      visisted.add(element);

      if (element.isDestroyed()){
        element = element.getMergedWith();
        assert !element.isDestroyed();
      }

      AbstractionElement aElement = AbstractElements.extractElementByType(element, AbstractionElement.class);
      if (aElement != null){
        laARTElement = element;
        break;
      }

      for (ARTElement parent : element.getParentARTs()){
        if (!visisted.contains(parent)){
          toProcess.addFirst(parent);
        }
      }
    }

    return laARTElement;
  }



}