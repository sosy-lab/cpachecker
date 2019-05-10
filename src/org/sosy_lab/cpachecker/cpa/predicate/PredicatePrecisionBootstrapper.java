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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
          "Provides additional invariants from witness to the initial predicate precision.")
  private Path correctnessWitnessFile = null;

  @Option(
      secure = true,
      name = "correctnessWitness.reuseInvariants",
      description = "extract invariants from witness and add them as predicates")
  private boolean reuseInvariantsFromCorrectnessWitness = false;

  @Option(
      secure = true,
      name = "correctnessWitness.atomPredicatesFromFormula",
      description = "invariants from witness are added as atom predicates")
  private boolean atomPredicatesFromFormula = false;

  @Option(
      secure = true,
      name = "correctnessWitness.witnessInvariantScope",
      description = "Where to apply the found invariants to?")
  private WitnessInvariantScope witnessInvariantScope = WitnessInvariantScope.LOCATION;
  private static enum WitnessInvariantScope {
    GLOBAL, // at all locations
    FUNCTION, // at all locations in the respective function
    LOCATION, // at all occurrences of the respective location
    ;
  }

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

  private Optional<WitnessInvariantsStatistics> witnessStats = Optional.empty();

  public PredicatePrecisionBootstrapper(
      Configuration config,
      LogManager logger,
      CFA cfa,
      AbstractionManager abstractionManager,
      FormulaManagerView formulaManagerView,
      Specification specification,
      ShutdownNotifier shutdownNotifier,
      PathFormulaManager pathFormulaManager,
      PredicateAbstractionManager predicateAbstractionManager)
      throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.cfa = cfa;

    this.abstractionManager = abstractionManager;
    this.formulaManagerView = formulaManagerView;

    this.specification = specification;
    this.shutdownNotifier = shutdownNotifier;
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
      witnessStats = Optional.of(new WitnessInvariantsStatistics(witnessInvariantScope));
      try {
        final Set<ExpressionTreeLocationInvariant> invariants = Sets.newLinkedHashSet();
        if (correctnessWitnessFile != null) {
          WitnessInvariantsExtractor extractor =
              new WitnessInvariantsExtractor(
                  config, specification, logger, cfa, shutdownNotifier, correctnessWitnessFile);
          extractor.extractInvariantsFromReachedSet(invariants);
        }

        for (ExpressionTreeLocationInvariant invariant : invariants) {

          ListMultimap<CFANode, AbstractionPredicate> localPredicates =
              MultimapBuilder.treeKeys().arrayListValues().build();
          Set<AbstractionPredicate> globalPredicates = Sets.newHashSet();
          ListMultimap<String, AbstractionPredicate> functionPredicates =
              MultimapBuilder.treeKeys().arrayListValues().build();

          List<AbstractionPredicate> predicates = new ArrayList<>();
          // get atom predicates from invariant
          if (atomPredicatesFromFormula) {
            predicates.addAll(
                predicateAbstractionManager.getPredicatesForAtomsOf(
                    invariant.getFormula(formulaManagerView, pathFormulaManager, null)));

          }
          // get predicate from invariant
          else {
            predicates.add(
                abstractionManager.makePredicate(
                    invariant.getFormula(formulaManagerView, pathFormulaManager, null)));
          }
          for (AbstractionPredicate predicate : predicates) {
            localPredicates.put(invariant.getLocation(), predicate);
            globalPredicates.add(predicate);
            functionPredicates.put(invariant.getLocation().getFunctionName(), predicate);
          }

          // add predicates according to the scope
          switch (witnessInvariantScope) {
            case FUNCTION:
              result = result.addFunctionPredicates(functionPredicates.entries());
              witnessStats.get().numberOfInitialFunctionPredicates +=
                  localPredicates.entries().size();
              break;
            case GLOBAL:
              result = result.addGlobalPredicates(globalPredicates);
              witnessStats.get().numberOfInitialGlobalPredicates += globalPredicates.size();
              break;
            case LOCATION:
              result = result.addLocalPredicates(localPredicates.entries());
              witnessStats.get().numberOfInitialLocalPredicates += localPredicates.entries().size();
              break;
            default:
              break;
          }
          witnessStats.get().numberOfLocationInvariants++;
        }
      } catch (CPAException | InterruptedException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Predicate from correctness witness invariants could not be computed");
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
    if (witnessStats.isPresent()) {
      pStatsCollection.add(witnessStats.get());
    }
  }

  private static class WitnessInvariantsStatistics implements Statistics {

    private WitnessInvariantScope witnessInvariantScope;
    private int numberOfLocationInvariants;
    private int numberOfInitialLocalPredicates;
    private int numberOfInitialFunctionPredicates;
    private int numberOfInitialGlobalPredicates;

    private WitnessInvariantsStatistics(WitnessInvariantScope pWitnessInvariantScope) {
      this.witnessInvariantScope = pWitnessInvariantScope;
      numberOfLocationInvariants = 0;
      numberOfInitialLocalPredicates = 0;
      numberOfInitialFunctionPredicates = 0;
      numberOfInitialGlobalPredicates = 0;
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      writer.put("Number of location invariants", numberOfLocationInvariants);
      switch (witnessInvariantScope) {
        case FUNCTION:
          writer.put("Number of initial function predicates", numberOfInitialFunctionPredicates);
          break;
        case GLOBAL:
          writer.put("Number of initial global predicates", numberOfInitialGlobalPredicates);
          break;
        case LOCATION:
          writer.put("Number of initial local predicates", numberOfInitialLocalPredicates);
          break;
        default:
          break;
      }
    }

    @Override
    public String getName() {
      return "Init Predicate Precision with Witness Invariants";
    }
  }
}
