// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryRefinerStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "refiner.composition")
public class RefinerComposition implements Refiner, StatisticsProvider {

  private final LogManager logger;

  private final ImmutableList<Refiner> refiners;
  private final SummaryRefinerStatistics stats;
  protected final ARGCPA argCpa;
  public List<Integer> amntRefinements = new ArrayList<>();

  @Option(
      secure = true,
      name = "maxAmntInnerRefinements",
      description =
          "Max amount of Inner refinements. A negative number deactivates this parameter.")
  public int maxAmntInnerRefinements = -1;

  @Option(
      secure = true,
      name = "wrappedrefiners",
      required = true,
      description =
          "Which wrapped refinement algorithms to use? "
              + "(give class name, required for CEGAR) If the package name starts with "
              + "'org.sosy_lab.cpachecker.', this prefix can be omitted."
              + "The refiners are written as one would function composition"
              + "i.e. first in the config the last one to be applied and "
              + "last in the config the first one to be apllied.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private List<Refiner.Factory> refinerFactories;

  private RefinerComposition(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    List<Refiner> refinersMutable = new ArrayList<>();
    for (Refiner.Factory rf: refinerFactories) {
      refinersMutable.add(rf.create(pCpa, pLogger, pShutdownNotifier));
      amntRefinements.add(0);
    }
    // Reverse the refiners in order for their call sequence to be consistent with the way they are given in the config
    Collections.reverse(refinersMutable);
    refiners = ImmutableList.copyOf(refinersMutable);

    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);

    stats = new SummaryRefinerStatistics(argCpa.getCfa(), pConfig, logger);
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    LogManager logger;
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, RefinerComposition.class);

    Optional<ShutdownNotifier> shutdownNotifier = Optional.empty();
    Optional<Configuration> config = Optional.empty();

    // TODO: Improve this code, since this is currently a very ugly hack
    try {
      @SuppressWarnings("resource")
      PredicateCPA predicateCPA =
          CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, RefinerComposition.class);

      // The shutdown notifier has a pointer into the MathSat5 solver, so if the object goes out
      // scope, the pointer points to nothing which makes sense. So do not free the resource.
      shutdownNotifier = Optional.of(predicateCPA.getShutdownNotifier());
      config = Optional.of(predicateCPA.getConfiguration());
    } catch (InvalidConfigurationException e) {
      ValueAnalysisCPA valueCPA =
          CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, RefinerComposition.class);
      shutdownNotifier = Optional.of(valueCPA.getShutdownNotifier());
      config = Optional.of(valueCPA.getConfiguration());
    }

    logger = argCpa.getLogger();

    return new RefinerComposition(
        logger, shutdownNotifier.orElseThrow(), pCpa, config.orElseThrow());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    for (Refiner r : refiners) {
      if (r instanceof StatisticsProvider) {
        ((StatisticsProvider) r).collectStatistics(pStatsCollection);
      }
    }

    if (!pStatsCollection.contains(stats)) {
      pStatsCollection.add(stats);
    }
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    for (int i = 0; i < refiners.size(); i++) {
      if (maxAmntInnerRefinements >= 0 && amntRefinements.get(i) >= maxAmntInnerRefinements + 1) {
        amntRefinements.set(i, 0);
        continue;
      }

      // TODO: Log statistics

      Refiner refiner = refiners.get(i);
      amntRefinements.set(i, amntRefinements.get(i) + 1);
      if (refiner.performRefinement(pReached)) {
        return true;
      }
    }

    // If nothing was refined, we apply them again, since the maximum amount of refinements may have
    // happened
    for (int i = 0; i < refiners.size(); i++) {
      // TODO: Log statistics
      Refiner refiner = refiners.get(i);
      amntRefinements.set(i, amntRefinements.get(i) + 1);
      if (refiner.performRefinement(pReached)) {
        return true;
      }
    }

    return false;
  }
}
