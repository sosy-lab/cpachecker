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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableSet;



@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeCPA extends PredicateCPA{

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RelyGuaranteeCPA.class);
  }

  @Option(name="abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"},
      description="which solver to use?")
      private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"},
      description="which interpolating solver to use for interpolant generation?")
      private String whichItpProver = "MATHSAT";

  @Option(name="abstraction.initialPredicates0", type=Option.Type.OPTIONAL_INPUT_FILE,
      description="get an initial set of predicates from a file in MSAT format")
      private File predicatesFile0 = null;

  @Option(name="abstraction.initialPredicates1", type=Option.Type.OPTIONAL_INPUT_FILE,
      description="get an initial set of predicates from a file in MSAT format")
      private File predicatesFile1 = null;

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = true;

  @Option(name="interpolation.changesolverontimeout",
      description="try second interpolating solver if the first takes too long")
      private boolean changeItpSolveOTF = false;

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};

  public Set<String> globalVariablesSet;

  @Option(name="blk.useCache", description="use caching of path formulas")
  private boolean useCache = true;

  private static TheoremProver  tProver;
  private RelyGuaranteeCPAStatistics stats;
  private int tid;
  public RelyGuaranteeVariables variables;


  public static TheoremProver getTheoremProver(Configuration config, LogManager logger,String type) throws InvalidConfigurationException{
    if (tProver == null){
      MathsatFormulaManager msatFormulaManager =  MathsatFormulaManager.getInstance(config, logger);

    }
    return tProver;
  }

  public RelyGuaranteeCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    super();

    this.config = config;
    this.logger = logger;
    config.inject(this, RelyGuaranteeCPA.class);

    globalVariablesSet = new HashSet<String>();
    for (String var : globalVariables){
      globalVariablesSet.add(var);
    }

    this.regionManager = BDDRegionManager.getInstance();
    MathsatFormulaManager mathsatFormulaManager = MathsatFormulaManager.getInstance(config, logger);
    this.formulaManager = mathsatFormulaManager;

    PathFormulaManager pfMgr  = PathFormulaManagerImpl.getInstance(mathsatFormulaManager, config, logger);
    if (useCache) {
      pfMgr = CachingPathFormulaManager.getInstance(pfMgr);
    }
    this.pathFormulaManager = pfMgr;

    /*if (whichProver.equals("MATHSAT")) {
      this.theoremProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      this.theoremProver = new YicesTheoremProver(mathsatFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }*/

    if (whichProver.equals("MATHSAT")) {
      theoremProver =  MathsatTheoremProver.getInstance(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      theoremProver =  YicesTheoremProver.getInstance(mathsatFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> itpProver;
    InterpolatingTheoremProver<Integer> alternativeItpProver = null;
    if (whichItpProver.equals("MATHSAT")) {
      itpProver = MathsatInterpolatingProver.getInstance(mathsatFormulaManager, false);
      if(changeItpSolveOTF){
        alternativeItpProver = CSIsatInterpolatingProver.getInstance(mathsatFormulaManager, logger);
      }
    } else if (whichItpProver.equals("CSISAT")) {
      itpProver = CSIsatInterpolatingProver.getInstance(mathsatFormulaManager, logger);
      if(changeItpSolveOTF){
        alternativeItpProver = MathsatInterpolatingProver.getInstance(mathsatFormulaManager, false);
      }
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    this.predicateManager = RelyGuaranteeRefinementManager.getInstance(regionManager, formulaManager, pathFormulaManager, theoremProver, itpProver, alternativeItpProver, config, logger);
    this.transfer = new RelyGuaranteeTransferRelation(this);
    this.domain = new RelyGuaranteeAbstractDomain(this);
    this.merge = new RelyGuaranteeMergeOperator(this);
    this.prec = new RelyGuaranteePrecisionAdjustment(this);
    this.stop = new StopSepOperator(domain);
    this.stats = new RelyGuaranteeCPAStatistics(this);

  }



  public void setTid(int tid){
    this.tid = tid;
    Collection<AbstractionPredicate> predicates = null;

    if (checkBlockFeasibility) {
      AbstractionPredicate p = predicateManager.makeFalsePredicate();
      predicates = ImmutableSet.of(p);
    }
    // TODO make-shift solution
    this.topElement = new RelyGuaranteeAbstractElement.AbstractionElement(pathFormulaManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null),  tid, new HashMap<Integer, RelyGuaranteeCFAEdge>());

    this.initialPrecision= new RelyGuaranteePrecision(predicates);

  }

  public int getThreadId(){
    return this.tid;
  }

  public void setVariables(RelyGuaranteeVariables pVariables) {
    variables = pVariables;
    assert globalVariablesSet.containsAll(variables.allVars);
    assert variables.allVars.containsAll(globalVariablesSet);
  }


  // set the inital predicates as a formula
  // TODO for debugging
  public void setPredicates(Formula predicateFormula){
    Collection<AbstractionPredicate> predicates = this.predicateManager.getAtomsAsPredicates(predicateFormula);
    this.initialPrecision = new RelyGuaranteePrecision(predicates);
  }

  public PredicatePrecision getPredicates(){
    return this.initialPrecision;
  }

  @Override
  public AbstractElement getInitialElement(CFANode node) {
    return topElement;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    return;
  }

  RelyGuaranteeCPAStatistics getStats() {
    return null;
  }

  public RelyGuaranteeRefinementManager<?, ?> getRelyGuaranteeManager() {
    return (RelyGuaranteeRefinementManager<?, ?>) this.predicateManager;
  }


}