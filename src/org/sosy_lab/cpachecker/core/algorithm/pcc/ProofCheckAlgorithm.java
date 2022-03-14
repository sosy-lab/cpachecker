// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;

@Options(prefix = "pcc")
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer readTimer = new Timer();

    @Override
    public String getName() {
      return "Proof Check algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println();
      out.println("Proof Checking statistics");
      out.println("-------------------------------------");
      out.println("Total time for proof check algorithm:     " + totalTimer);
      out.println(
          "  Time for reading in proof (not complete time in interleaved modes):  " + readTimer);
    }
  }

  private final CPAStatistics stats = new CPAStatistics();
  protected final LogManager logger;

  protected final PCCStrategy checkingStrategy;

  @Option(
      secure = true,
      name = "proof",
      description = "file in which proof representation needed for proof checking is stored")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path proofFile = Path.of("arg.obj");

  public ProofCheckAlgorithm(
      ConfigurableProgramAnalysis cpa,
      Configuration pConfig,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification specification)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this, ProofCheckAlgorithm.class);

    if (!proofFile.toFile().exists()) {
      throw new InvalidConfigurationException(
          "Cannot find proof file. File " + proofFile + " does not exists.");
    }
    checkingStrategy =
        PCCStrategyBuilder.buildStrategy(
            pConfig, logger, pShutdownNotifier, proofFile, cpa, pCfa, specification);

    this.logger = logger;

    logger.log(Level.INFO, "Start reading proof.");
    stats.totalTimer.start();
    stats.readTimer.start();
    try {
      checkingStrategy.readProof();
    } catch (ClassNotFoundException | InvalidConfigurationException | IOException e) {
      throw new CPAException("Failed reading proof", e);
    } finally {
      stats.readTimer.stop();
      stats.totalTimer.stop();
    }
    logger.log(Level.INFO, "Finished reading proof.");
  }

  protected ProofCheckAlgorithm(
      Configuration pConfig,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      ReachedSet pReachedSet,
      CFA pCfa,
      Specification specification)
      throws InvalidConfigurationException, InterruptedException {

    pConfig.inject(this, ProofCheckAlgorithm.class);

    ConfigurableProgramAnalysis cpa = pReachedSet.getCPA();
    checkingStrategy =
        PCCStrategyBuilder.buildStrategy(
            pConfig, logger, pShutdownNotifier, proofFile, cpa, pCfa, specification);
    this.logger = logger;

    checkArgument(
        pReachedSet != null && !pReachedSet.hasWaitingState(),
        "Parameter pReachedSet may not be null and may not have any states in its waitlist.");

    stats.totalTimer.start();
    checkingStrategy.constructInternalProofRepresentation(pReachedSet, cpa);
    stats.totalTimer.stop();
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Proof check algorithm started.");
    stats.totalTimer.start();

    boolean result;
    result = checkingStrategy.checkCertificate(reachedSet);

    stats.totalTimer.stop();
    logger.log(Level.INFO, "Proof check algorithm finished.");

    if (!result) {
      reachedSet.add(
          new DummyErrorState(reachedSet.getFirstState()), SingletonPrecision.getInstance());
    }

    return AlgorithmStatus.SOUND_AND_PRECISE.withSound(result);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (checkingStrategy instanceof StatisticsProvider) {
      ((StatisticsProvider) checkingStrategy).collectStatistics(pStatsCollection);
    }
  }
}
