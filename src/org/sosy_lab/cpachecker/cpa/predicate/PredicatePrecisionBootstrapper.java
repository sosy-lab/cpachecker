/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateMapParser.PredicateMapParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ImmutableSetMultimap;

@Options(prefix="cpa.predicate")
public class PredicatePrecisionBootstrapper implements StatisticsProvider {

  @Option(name="abstraction.initialPredicates",
      description="get an initial map of predicates from a file (see source doc/examples/predmap.txt for an example)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private File predicatesFile = null;

  @Option(description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @Option(description="Enable mining of predicates from the CFA (preprocessing).")
  private boolean enablePrecisionMiner = false;

  @Option(description="Enable sweeping the precision: remove predicates that make statements about variables that do not exist in the CFA.")
  private boolean enablePrecisionSweeper = false;

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final AbstractionManager abstractionManager;
  private final PredicatePrecisionSweeper sweeper;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  private class PrecisionBootstrapStatistics extends AbstractStatistics {}
  private final PrecisionBootstrapStatistics statistics = new PrecisionBootstrapStatistics();

  public PredicatePrecisionBootstrapper(Configuration config, LogManager logger, CFA cfa,
      PathFormulaManager pathFormulaManager, AbstractionManager abstractionManager, FormulaManagerView formulaManagerView,
      PredicatePrecisionSweeper sweeper) throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;
    this.sweeper = sweeper;

    this.pathFormulaManager = pathFormulaManager;
    this.abstractionManager = abstractionManager;
    this.formulaManagerView = formulaManagerView;

    config.inject(this);
  }

  protected PredicatePrecision internalPrepareInitialPredicates() throws InvalidConfigurationException {

    Set<AbstractionPredicate> initialPredicates = checkBlockFeasibility
        ? Collections.<AbstractionPredicate>singleton(abstractionManager.makeFalsePredicate())
        : Collections.<AbstractionPredicate>emptySet();

    if (predicatesFile != null) {
      try {
        PredicateMapParser parser = new PredicateMapParser(config, cfa, logger, formulaManagerView, abstractionManager);
        PredicatePrecision result = parser.parsePredicates(predicatesFile, initialPredicates);
        return enablePrecisionSweeper ? sweeper.sweepPrecision(result) : result;

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicate map from file");
        return PredicatePrecision.empty();

      } catch (PredicateMapParsingFailedException e) {
        logger.logUserException(Level.WARNING, e, "Could not read predicate map");
        return PredicatePrecision.empty();
      }
    } else if (enablePrecisionMiner) {
      try {
        PredicateMiner precMiner = new PredicateMiner(config, logger, pathFormulaManager, formulaManagerView, abstractionManager);
        return precMiner.minePrecisionFromCfa(cfa);

      } catch (CPATransferException e) {
        logger.logUserException(Level.WARNING, e, "Could not mine precision from CFA");
        return PredicatePrecision.empty();
      }
    }

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode, Integer>, AbstractionPredicate>of(),
        ImmutableSetMultimap.<CFANode, AbstractionPredicate>of(),
        ImmutableSetMultimap.<String, AbstractionPredicate>of(),
        initialPredicates);
  }

  /**
   * Read the (initial) precision (predicates to track) from a file.
   *
   * @return      Precision
   * @throws      InvalidConfigurationException
   */
  public PredicatePrecision prepareInitialPredicates() throws InvalidConfigurationException {
    PredicatePrecision result = internalPrepareInitialPredicates();

    statistics.addKeyValueStatistic("Init. global predicates", result.getGlobalPredicates().size());
    statistics.addKeyValueStatistic("Init. location predicates", result.getLocalPredicates().size());
    statistics.addKeyValueStatistic("Init. function predicates", result.getFunctionPredicates().size());

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
