// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

@Options
public class ProofGenerator {

  @Option(
      secure = true,
      name = "pcc.sliceProof",
      description =
          "Make proof more abstract, remove some of the information not needed to prove the"
              + " property.")
  private boolean slicingEnabled = false;

  @Option(
      secure = true,
      name = "pcc.proofFile",
      description = "file in which proof representation will be stored")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  protected Path file = Path.of("arg.obj");

  private final PCCStrategy checkingStrategy;

  private final LogManager logger;
  private final Timer writingTimer = new Timer();

  private final @Nullable ProofSlicer slicer;

  private final Statistics proofGeneratorStats =
      new Statistics() {

        @Override
        public void printStatistics(
            PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
          pOut.println();
          pOut.println(getName() + " statistics");
          pOut.println("------------------------------------");
          pOut.println("Time for proof writing: " + writingTimer);

          if (checkingStrategy != null) {
            for (Statistics stats : checkingStrategy.getAdditionalProofGenerationStatistics()) {
              StatisticsUtils.printStatistics(stats, pOut, logger, pResult, pReached);
            }
          }
        }

        @Override
        public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
          if (checkingStrategy != null) {
            for (Statistics stats : checkingStrategy.getAdditionalProofGenerationStatistics()) {
              StatisticsUtils.writeOutputFiles(stats, logger, pResult, pReached);
            }
          }
        }

        @Override
        public String getName() {
          return "Proof Generation";
        }
      };

  public ProofGenerator(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;

    checkingStrategy =
        PCCStrategyBuilder.buildStrategy(
            pConfig, pLogger, pShutdownNotifier, file, null, null, null);
    if (slicingEnabled) {
      slicer = new ProofSlicer(pLogger);
    } else {
      slicer = null;
    }
  }

  public void generateProof(CPAcheckerResult pResult) {
    // check result
    if (pResult.getResult() != Result.TRUE) {
      logger.log(
          Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return;
    }

    if (pResult.getReached() == null) {
      logger.log(Level.SEVERE, "Proof cannot be generated because reached set not available");
    }

    constructAndWriteProof(pResult.getReached());

    pResult.addProofGeneratorStatistics(proofGeneratorStats);
  }

  private void constructAndWriteProof(final ReachedSet pReached) {
    UnmodifiableReachedSet reached = pReached;
    if (slicer != null) {
      logger.log(Level.INFO, "Start slicing of proof");
      reached = slicer.sliceProof(reached, pReached.getCPA());
    }

    // saves the proof
    logger.log(Level.INFO, "Proof Generation started.");

    writingTimer.start();

    checkingStrategy.writeProof(reached, pReached.getCPA());

    writingTimer.stop();
    logger.log(
        Level.INFO, "Writing proof took " + writingTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
  }

  protected Statistics generateProofUnchecked(final ReachedSet pReached) {
    constructAndWriteProof(pReached);

    return proofGeneratorStats;
  }
}
