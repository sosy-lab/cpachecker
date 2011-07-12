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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;

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
import com.google.common.io.Files;



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

  @Option(name="abstraction.initialPredicates", type=Option.Type.OPTIONAL_INPUT_FILE,
      description="get an initial set of predicates from a file in MSAT format")
      private File predicatesFile = null;

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @Option(name="interpolation.changesolverontimeout",
      description="try second interpolating solver if the first takes too long")
      private boolean changeItpSolveOTF = false;

  @Option(name="blk.useCache", description="use caching of path formulas")
  private boolean useCache = true;




    private static TheoremProver  tProver;

    private RelyGuaranteeCPAStatistics stats;



    private int tid;


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

    this.regionManager = BDDRegionManager.getInstance();
    // MathsatFormulaManager mathsatFormulaManager = new MathsatFormulaManager(config, logger);
    MathsatFormulaManager mathsatFormulaManager = MathsatFormulaManager.getInstance(config, logger);
    this.formulaManager = mathsatFormulaManager;

    //PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, config, logger);
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
      theoremProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      theoremProver = new YicesTheoremProver(mathsatFormulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }

    InterpolatingTheoremProver<Integer> itpProver;
    InterpolatingTheoremProver<Integer> alternativeItpProver = null;
    if (whichItpProver.equals("MATHSAT")) {
      itpProver = new MathsatInterpolatingProver(mathsatFormulaManager, false);
      if(changeItpSolveOTF){
        alternativeItpProver =  new CSIsatInterpolatingProver(mathsatFormulaManager, logger);
      }
    } else if (whichItpProver.equals("CSISAT")) {
      itpProver = new CSIsatInterpolatingProver(mathsatFormulaManager, logger);
      if(changeItpSolveOTF){
        alternativeItpProver = new MathsatInterpolatingProver(mathsatFormulaManager, false);
      }
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }
    this.predicateManager = new RelyGuaranteeRefinementManager<Integer, Integer>(regionManager, formulaManager, pathFormulaManager, theoremProver, itpProver, alternativeItpProver, config, logger);
    this.transfer = new RelyGuaranteeTransferRelation(this);

    this.topElement = new RelyGuaranteeAbstractElement.AbstractionElement(pathFormulaManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null));
    this.domain = new RelyGuaranteeAbstractDomain(this);

    this.merge = new RelyGuaranteeMergeOperator(this);
    this.prec = new RelyGuaranteePrecisionAdjustment(this);
    this.stop = new StopSepOperator(domain);

    Collection<AbstractionPredicate> predicates = null;
    if (predicatesFile != null) {
      try {
        String fileContent = Files.toString(predicatesFile, Charset.defaultCharset());
        Formula f = mathsatFormulaManager.parse(fileContent);
        predicates = this.predicateManager.getAtomsAsPredicates(f);
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING, "Could not read predicates from file", predicatesFile,
            "(" + e.getMessage() + ")");
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not read predicates from file", predicatesFile,
            "(" + e.getMessage() + ")");
      }
    }

    if (checkBlockFeasibility) {
      AbstractionPredicate p = this.predicateManager.makeFalsePredicate();
      if (predicates == null) {
        predicates = ImmutableSet.of(p);
      } else {
        predicates.add(p);
      }
    }


    // hardcode predicates
    this.initialPrecision= new PredicatePrecision(predicates);
    //this.initialPrecision = null;

    this.stats = new RelyGuaranteeCPAStatistics(this);

  }

  private RelyGuaranteePrecision hardcodedPredicates() {
    Formula fVariable=null;
    Collection<AbstractionPredicate> predicates=null;
    if (this.tid == 0){
      fVariable = this.formulaManager.makeVariable("cs1",2);
    }
    else if (this.tid == 1){
      fVariable = this.formulaManager.makeVariable("g",2);
    }
    Formula fNumeral0 = this.formulaManager.makeNumber(0);
    Formula fNumeral1 = this.formulaManager.makeNumber(1);
    Formula fNumeral2 = this.formulaManager.makeNumber(2);
    Formula fNumeral3 = this.formulaManager.makeNumber(3);


    Formula fPred0 = this.formulaManager.makeEqual(fVariable, fNumeral0);
    Formula fPred1 = this.formulaManager.makeEqual(fVariable, fNumeral1);
    Formula fPred2 = this.formulaManager.makeEqual(fVariable, fNumeral2);
    Formula fPred3 = this.formulaManager.makeEqual(fVariable, fNumeral3);

    Formula fPredAnd1 = this.formulaManager.makeAnd(fPred0, fPred1);
    Formula fPredAnd2 = this.formulaManager.makeAnd(fPredAnd1, fPred2);
    Formula fPredAnd3 = this.formulaManager.makeAnd(fPredAnd2, fPred3);
    predicates = this.predicateManager.getAtomsAsPredicates(fPredAnd1);

    return new RelyGuaranteePrecision(predicates);
  }


  public void setThreadId(int tid){
    this.tid = tid;
  }

  public void useHardcodedPredicates() {
    this.initialPrecision = this.hardcodedPredicates();
  }

  public int getThreadId(){
    return this.tid;
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
    pStatsCollection.add(this.stats);
  }

  RelyGuaranteeCPAStatistics getStats() {
    return this.stats;
  }

}