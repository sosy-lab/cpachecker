/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.util.predicates.CSIsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatInterpolatingProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.mathsat.YicesTheoremProver;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * CPA that defines symbolic predicate abstraction.
 * @author Erkan
 *
 */
@Options(prefix="cpa.predicate")
public class PredicateCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PredicateCPA.class);
  }

  @Option(name="abstraction.solver", toUppercase=true, values={"MATHSAT", "YICES"})
  private String whichProver = "MATHSAT";

  @Option(name="interpolatingProver", toUppercase=true, values={"MATHSAT", "CSISAT"})
  private String whichItpProver = "MATHSAT";

  @Option(name="abstraction.initialPredicates", type=Option.Type.OPTIONAL_INPUT_FILE)
  private File predicatesFile = null;
  
  @Option
  private boolean checkBlockFeasibility = true;
  
  @Option(name="interpolation.changesolverontimeout")
  private boolean changeItpSolveOTF = false;
  
  
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
  private final PredicateRefinementManager<?, ?> predicateManager;
  private final PredicateCPAStatistics stats;
  private final AbstractElement topElement;

  private PredicateCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    this.config = config;
    this.logger = logger;

    regionManager = BDDRegionManager.getInstance();
    MathsatFormulaManager mathsatFormulaManager = new MathsatFormulaManager(config, logger);
    formulaManager = mathsatFormulaManager;

    TheoremProver thmProver;
    if (whichProver.equals("MATHSAT")) {
      thmProver = new MathsatTheoremProver(mathsatFormulaManager);
    } else if (whichProver.equals("YICES")) {
      thmProver = new YicesTheoremProver(mathsatFormulaManager);
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
    predicateManager = new PredicateRefinementManager<Integer, Integer>(regionManager, mathsatFormulaManager, thmProver, itpProver, alternativeItpProver, config, logger);
    transfer = new PredicateTransferRelation(this);
    
    topElement = new PredicateAbstractElement.AbstractionElement(predicateManager.makeEmptyPathFormula(), predicateManager.makeTrueAbstractionFormula(null));    
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
        logger.log(Level.WARNING, "Could not read predicates from file", predicatesFile,
            "(" + e.getMessage() + ")");
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not read predicates from file", predicatesFile,
            "(" + e.getMessage() + ")");
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

    stats = new PredicateCPAStatistics(this, domain);
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

  public FormulaManager getFormulaManager() {
    return formulaManager;
  }
  
  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode node) {
    return topElement;
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
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
