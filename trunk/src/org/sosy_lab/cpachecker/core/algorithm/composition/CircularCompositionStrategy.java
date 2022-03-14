// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "compositionAlgorithm.circular")
public class CircularCompositionStrategy extends AlgorithmCompositionStrategy
    implements Statistics {

  @Option(
      secure = true,
      description =
          "If adaptTimeLimits is set and all configurations support progress reports, in each cycle"
              + " the time limits per configuration are newly calculated based on the progress")
  private boolean adaptTimeLimits = false;

  private int inCycleCount;
  private int noOfRounds;

  protected Iterator<AlgorithmContext> algorithmContextCycle;

  public CircularCompositionStrategy(final Configuration pConfig, final LogManager pLogger)
      throws InvalidConfigurationException {
    super(pLogger);
    pConfig.inject(this);
  }

  @Override
  protected void initializeAlgorithmContexts(List<AnnotatedValue<Path>> pConfigFiles) {
    super.initializeAlgorithmContexts(pConfigFiles);
    algorithmContextCycle = Iterables.cycle(algorithmContexts).iterator();
    inCycleCount = 1;
    noOfRounds = 0;
  }

  private void computeAndSetNewTimeLimits() {
    long totalDistributableTimeBudget = 0;
    double totalRelativeProgress = 0.0;
    boolean mayAdapt = true;

    for (AlgorithmContext context : algorithmContexts) {
      totalDistributableTimeBudget += context.getTimeLimit() - AlgorithmContext.DEFAULT_TIME_LIMIT;
      totalRelativeProgress += (context.getProgress() / context.getTimeLimit());
      mayAdapt &= context.getProgress() >= 0;
    }

    if (totalDistributableTimeBudget <= algorithmContexts.size() || totalRelativeProgress <= 0) {
      mayAdapt = false;
    }

    for (AlgorithmContext context : algorithmContexts) {
      if (mayAdapt) {
        context.adaptTimeLimit(
            AlgorithmContext.DEFAULT_TIME_LIMIT
                + (int)
                    Math.round(
                        ((context.getProgress() / context.getTimeLimit()) / totalRelativeProgress)
                            * totalDistributableTimeBudget));
      }
    }
  }

  @Override
  public boolean hasNextAlgorithm() {
    return algorithmContextCycle.hasNext();
  }

  @Override
  public AlgorithmContext getNextAlgorithm() {

    if (inCycleCount == algorithmContexts.size()) { // TODO
      inCycleCount = 0;
      noOfRounds++;
      logger.log(Level.INFO, "Circular composition strategy starts next iteration...");
      if (adaptTimeLimits) {
        computeAndSetNewTimeLimits();
      }
      for (AlgorithmContext tempContext : algorithmContexts) {
        tempContext.resetProgress();
      }
    }
    inCycleCount++;

    return algorithmContextCycle.next();
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // TODO Auto-generated method stub
    pOut.println("Number of analyses per round: " + algorithmContexts.size());
    pOut.println("Number of completed rounds:  " + noOfRounds);
    pOut.println("Stopped in analysis:         " + inCycleCount);

    for (int i = 0; i < algorithmContexts.size(); i++) {
      pOut.println(
          "Time spent in analysis "
              + (i + 1)
              + ":    "
              + algorithmContexts.get(i).getTotalTimeSpent().asSeconds());
    }
  }

  @Override
  public @Nullable String getName() {
    return "Circular Composition";
  }
}
