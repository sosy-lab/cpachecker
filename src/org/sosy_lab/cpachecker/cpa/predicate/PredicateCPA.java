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
package org.sosy_lab.cpachecker.cpa.predicate;

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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFactory;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * CPA that defines symbolic predicate abstraction.
 */
@Options(prefix="cpa.predicate")
public class PredicateCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCPA.class);
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

  private final Configuration config;
  private final LogManager logger;

  private final PredicateAbstractDomain domain;
  private final PredicateTransferRelation transfer;
  private final PredicateMergeOperator merge;
  private final PredicatePrecisionAdjustment prec;
  private final StopOperator stop;
  private final PredicatePrecision initialPrecision;
  private final RegionManager regionManager;
  private final FormulaManager formulaManager;
  private final PathFormulaManager pathFormulaManager;
  private final TheoremProver theoremProver;
  private final PredicateRefinementManager<?, ?> predicateManager;
  private final PredicateCPAStatistics stats;
  private final AbstractElement topElement;
  private final InterpolatingTheoremProver<Integer> itpProver;
  private final InterpolatingTheoremProver<Integer> alternativeItpProver;

  protected PredicateCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, PredicateCPA.class);

    this.config = config;
    this.logger = logger;

    regionManager = BDDRegionManager.getInstance();
    MathsatFormulaManager mathsatFormulaManager = MathsatFactory.createFormulaManager(config, logger);
    formulaManager = mathsatFormulaManager;

    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, config, logger);
    if (useCache) {
      pfMgr = new CachingPathFormulaManager(pfMgr);
    }
    pathFormulaManager = pfMgr;

    if (whichProver.equals("MATHSAT")) {
      theoremProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      theoremProver = new YicesTheoremProver(formulaManager);
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }


    if (whichItpProver.equals("MATHSAT")) {
      itpProver = new MathsatInterpolatingProver(mathsatFormulaManager, false);
      if(changeItpSolveOTF){
        alternativeItpProver = new CSIsatInterpolatingProver(formulaManager, logger);
      } else {
        alternativeItpProver = null;
      }
    } else if (whichItpProver.equals("CSISAT")) {
      itpProver = new CSIsatInterpolatingProver(formulaManager, logger);
      if(changeItpSolveOTF){
        alternativeItpProver = new MathsatInterpolatingProver(mathsatFormulaManager, false);
      } else {
        alternativeItpProver = null;
      }
    } else {
      throw new InternalError("Update list of allowed solvers!");
    }
    predicateManager = new PredicateRefinementManager<Integer, Integer>(regionManager, formulaManager, pathFormulaManager, theoremProver, itpProver, alternativeItpProver, config, logger);
    transfer = new PredicateTransferRelation(this);

    topElement = new PredicateAbstractElement.AbstractionElement(pathFormulaManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null));
    domain = new PredicateAbstractDomain(this);

    merge = new PredicateMergeOperator(this);
    prec = new PredicatePrecisionAdjustment(this);
    stop = new StopSepOperator(domain);

    Collection<AbstractionPredicate> predicates = null;
    if (predicatesFile != null) {
      try {
        String fileContent = Files.toString(predicatesFile, Charset.defaultCharset());
        Formula f = mathsatFormulaManager.parse(fileContent);
        predicates = predicateManager.getAtomsAsPredicates(f);
      } catch (IllegalArgumentException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file " + predicatesFile);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicates from file");
      }
    }

    if (checkBlockFeasibility) {
      AbstractionPredicate p = predicateManager.makeFalsePredicate();
      if (predicates == null) {
        predicates = ImmutableSet.of(p);
      } else {
        predicates.add(p);
      }
    }
    initialPrecision = new PredicatePrecision(predicates);

    stats = createStatistics();
  }

  protected PredicateCPAStatistics createStatistics() throws InvalidConfigurationException {
    return new PredicateCPAStatistics(this);
  }

  @Override
  public PredicateAbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public PredicateTransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public PredicateMergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  protected RegionManager getRegionManager() {
    return regionManager;
  }

  protected PredicateRefinementManager<?, ?> getPredicateManager() {
    return predicateManager;
  }

  InterpolatingTheoremProver<Integer> getItpProver() {
    return itpProver;
  }

  InterpolatingTheoremProver<Integer> getAlternativeItpProver() {
    return alternativeItpProver;
  }

  public FormulaManager getFormulaManager() {
    return formulaManager;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }

  public TheoremProver getTheoremProver() {
    return theoremProver;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public AbstractElement getInitialElement(CFANode node) {
    return topElement;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return initialPrecision;
  }

  @Override
  public PredicatePrecisionAdjustment getPrecisionAdjustment() {
    return prec;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  PredicateCPAStatistics getStats() {
    return stats;
  }
}
