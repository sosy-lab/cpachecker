// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.loopAbstraction.LoopAbstractionHeader;
import org.sosy_lab.cpachecker.util.loopInformation.LoopInformation;

@Options(prefix = "loopacc.algorithm")
public class LoopAccelerationAlgorithm extends NestingAlgorithm implements Algorithm {
  private final ShutdownManager singleShutdownManager;
  private final LoopInformation loopInformation;
  private final LoopAbstractionHeader loopAbstractionHeader;

  @Option(secure = true, description = "Configuration that shall be run with loop acceleration")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path wrappedConfig;

  public LoopAccelerationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      LoopInformation pLoopInformation,
      LoopAbstractionHeader pLoopAbstractionHeader)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
    pConfig.inject(this);
    singleShutdownManager = ShutdownManager.createWithParent(pShutdownNotifier);
    loopInformation = pLoopInformation;
    loopAbstractionHeader = pLoopAbstractionHeader;
  }

  public static LoopAccelerationAlgorithm create(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException, InterruptedException, CPAException {
    CFACreator cfaCreator = new CFACreator(pConfig, pLogger, pShutdownNotifier);
    LoopInformation loopInformation = new LoopInformation(pConfig, pLogger, pCfa);
    LoopAbstractionHeader loopAbstraction =
        new LoopAbstractionHeader(loopInformation, true, pConfig, pLogger);
    String abstractedSource = loopAbstraction.getAbstractedSource();
    assert abstractedSource != null : "Expected abstracted source to be present";

    pLogger.log(Level.INFO, "Creating CFA for abstracted program");
    CFA abstractedCFA;
    try {
      abstractedCFA = cfaCreator.parseSourceAndCreateCFA(abstractedSource);
    } catch (ParserException e) {
      throw new CPAException("Parsing of abstracted CFA failed!", e);
    }

    return new LoopAccelerationAlgorithm(
        pConfig,
        pLogger,
        abstractedCFA,
        pShutdownNotifier,
        pSpecification,
        loopInformation,
        loopAbstraction);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    ForwardingReachedSet reached = (ForwardingReachedSet) pReachedSet;
    ReachedSet wrappedReached;

    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> algoParts;
    Algorithm algorithm;
    try {
      algoParts =
          createAlgorithm(
              wrappedConfig,
              cfa.getMainFunction(),
              singleShutdownManager,
              new AggregatedReachedSets(),
              Sets.newHashSet("loopaccel.wrappedConfig", "analysis.algorithm.useLoopAcceleration"),
              new CopyOnWriteArrayList<>());
      algorithm = algoParts.getFirst();
      wrappedReached = algoParts.getThird();
    } catch (IOException | InvalidConfigurationException e) {
      throw new CPAException("Failed to create wrapped algorithm for loop acceleration!", e);
    }
    reached.setDelegate(wrappedReached);
    // mark the result as imprecise such that found errors will be identified as unknown
    return algorithm.run(reached).withPrecise(false);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    loopInformation.collectStatistics(pStatsCollection);
    loopAbstractionHeader.collectStatistics(pStatsCollection);
  }
}
