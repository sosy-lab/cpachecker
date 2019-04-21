/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateMapParser;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.statistics.KeyValueStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix="cpa.predicate")
public class PredicatePrecisionBootstrapper implements StatisticsProvider {

  @Option(secure=true, name="abstraction.initialPredicates",
      description="get an initial map of predicates from a list of files (see source doc/examples/predmap.txt for an example)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> predicatesFiles = ImmutableList.of();

  @Option(secure=true, description="always check satisfiability at end of block, even if precision is empty")
  private boolean checkBlockFeasibility = false;

  @FileOption(Type.OPTIONAL_INPUT_FILE)
  @Option(
      secure = true,
      description =
          "Provides additional candidate invariants from witness to the initial predicates.")
  private Path correctnessWitnessFile = null;

  @Option(
      secure = true,
      name = "correctnessWitness.reuseInvariants",
      description = "extract invariants from witness and add them as predicates")
  private boolean reuseInvariantsFromCorrectnessWitness = false;

  @Option(
      secure = true,
      name = "correctnessWitness.atomPredicatesFromFormula",
      description = "extract atoms from candidates in witness and create predicates")
  private boolean atomPredicatesFromFormula = false;

  private final FormulaManagerView formulaManagerView;
  private final AbstractionManager abstractionManager;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

  private final Specification specification;
  private final ShutdownNotifier shutdownNotifier;
  private final PathFormulaManager pathFormulaManager;
  private final PredicateAbstractionManager predicateAbstractionManager;

  private final KeyValueStatistics statistics = new KeyValueStatistics();
  final WitnessInvariantsStatistics witnessStats = new WitnessInvariantsStatistics();

  public PredicatePrecisionBootstrapper(
      Configuration config,
      LogManager logger,
      CFA cfa,
      AbstractionManager abstractionManager,
      FormulaManagerView formulaManagerView,
      Specification specification,
      ShutdownNotifier shutDownNotifier,
      PathFormulaManager pathFormulaManager,
      PredicateAbstractionManager predicateAbstractionManager)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    this.abstractionManager = abstractionManager;
    this.formulaManagerView = formulaManagerView;

    this.specification = specification;
    this.shutdownNotifier = shutDownNotifier;
    this.pathFormulaManager = pathFormulaManager;
    this.predicateAbstractionManager = predicateAbstractionManager;

    config.inject(this);
  }

  private PredicatePrecision internalPrepareInitialPredicates() throws InvalidConfigurationException {

    PredicatePrecision result = PredicatePrecision.empty();

    if (checkBlockFeasibility) {
      result = result
          .addGlobalPredicates(Collections.singleton(abstractionManager.makeFalsePredicate()));
    }

    if (!predicatesFiles.isEmpty()) {
      PredicateMapParser parser = new PredicateMapParser(config, cfa, logger, formulaManagerView, abstractionManager);

      for (Path predicatesFile : predicatesFiles) {
        try {
          result = result.mergeWith(parser.parsePredicates(predicatesFile));

        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate map from file");

        } catch (PredicateParsingFailedException e) {
          logger.logUserException(Level.WARNING, e, "Could not read predicate map");
        }
      }
    }

    if (reuseInvariantsFromCorrectnessWitness) {
      witnessStats.invariantGeneration.start();
      try {
        final Set<ExpressionTreeLocationInvariant> invariants = Sets.newLinkedHashSet();
        if (correctnessWitnessFile != null) {
          WitnessInvariantsExtractor extractor =
              new WitnessInvariantsExtractor(
                  config, specification, logger, cfa, shutdownNotifier, correctnessWitnessFile);
          extractor.extractInvariantsFromReachedSet(invariants);
        }

        for (ExpressionTreeLocationInvariant invariant : invariants) {

          ListMultimap<CFANode, AbstractionPredicate> localPredicates = ArrayListMultimap.create();

          // get atom predicates from invariant
          // splitItpAtoms has to be true
          if (atomPredicatesFromFormula) {
            Collection<AbstractionPredicate> atomPredicates =
                predicateAbstractionManager.getPredicatesForAtomsOf(
                    invariant.getFormula(formulaManagerView, pathFormulaManager, null));
              for (AbstractionPredicate atomPredicate : atomPredicates) {
              localPredicates.put(invariant.getLocation(), atomPredicate);
              }
          }
          // get predicates from invariant
          else {
            Set<AbstractionPredicate> predicate =
                Collections.singleton(
                    abstractionManager.makePredicate(
                        invariant.getFormula(formulaManagerView, pathFormulaManager, null)));
            localPredicates.put(invariant.getLocation(), predicate.iterator().next());
          }

          // add all predicates
          result = result.addLocalPredicates(localPredicates.entries());
          witnessStats.numberOfInitialPredicates += localPredicates.entries().size();
        }
        witnessStats.numberOfInvariants += invariants.size();
      } catch (CPAException | InterruptedException e) {
        logger.logUserException(
            Level.WARNING, e, "Predicate from correctness witness file could not be computed");
      } finally {
        witnessStats.invariantGeneration.stop();
      }
    }

    return result;
  }

  /**
   * Read the (initial) precision (predicates to track) from a file.
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
    if (reuseInvariantsFromCorrectnessWitness) {
      pStatsCollection.add(witnessStats);
    }
  }

  public static class WitnessInvariantsStatistics implements Statistics {

    final Timer invariantGeneration = new Timer();
    private int numberOfInvariants = 0;
    private int numberOfInitialPredicates = 0;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      writer.put("Time for invariant generation", invariantGeneration);
      writer.put("Number of invariants", numberOfInvariants);
      writer.put("Number of initial predicates", numberOfInitialPredicates);
    }

    @Override
    public String getName() {
      return "Witness Invariants Initial Predicate Precision";
    }
  }
}
