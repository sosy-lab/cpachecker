// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import static org.sosy_lab.cpachecker.core.algorithm.tubes.AlgorithmFactory.createAlgorithm;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tubes.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.distance.DistanceCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "tubes")
public class TubeAlgorithm implements Algorithm {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final Specification specification;
  private final CFA cfa;
  private final ShutdownManager shutdown;

  @Option(description = "Configuration for bounded predicate analysis")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path boundedConfig = Path.of("config/boundedPredicateAnalysis.properties");

  public TubeAlgorithm(
      Algorithm pAlgorithm,
      LogManager pLogger,
      Specification pSpecification,
      CFA pCfa,
      Configuration pConfig,
      ShutdownManager pShutdown)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    algorithm = pAlgorithm;
    logger = pLogger;
    specification = pSpecification;
    cfa = pCfa;
    shutdown = pShutdown;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    ImmutableList<@NonNull CounterexampleInfo> cexs =
        AbstractStates.getTargetStates(reachedSet)
            .filter(ARGState.class)
            .transform(state -> state.getCounterexampleInformation())
            .filter(info -> info.isPresent())
            .transform(info -> info.orElseThrow())
            .toList();
    PathSummary<String> summarize = new FirstAssumePathSummary();
    if (cexs.isEmpty()) {
      return status;
    }
    try {
      Configuration bounded = Configuration.builder().loadFromFile(boundedConfig).build();
      for (CounterexampleInfo cex : cexs) {
        AnalysisComponents analysisComponents =
            createAlgorithm(
                logger,
                specification,
                cfa,
                bounded,
                shutdown,
                cpa ->
                    Optional.ofNullable(CPAs.retrieveCPA(cpa, DistanceCPA.class))
                        .ifPresent(d -> d.init(cex)));
        ReachedSet distanceReachedSet = analysisComponents.reached();
        Algorithm distanceAlgorithm = analysisComponents.algorithm();
        do {
          status = status.update(distanceAlgorithm.run(distanceReachedSet));
        } while (distanceReachedSet.hasWaitingState());
        ImmutableList<@NonNull ARGPath> targetPathsWithinDistance =
            AbstractStates.getTargetStates(distanceReachedSet)
                // filter all target states at target location of cex
                .filter(
                    state ->
                        Objects.requireNonNull(AbstractStates.extractLocation(state))
                                .getNodeNumber()
                            == Objects.requireNonNull(
                                    AbstractStates.extractLocation(
                                        cex.getTargetPath().getLastState()))
                                .getNodeNumber())
                // collect all ARGPaths from root to target state
                .transformAndConcat(
                    state -> ARGUtils.getAllPaths(distanceReachedSet, (ARGState) state))
                .toList();
        logger.log(Level.INFO, summarize.summarize(targetPathsWithinDistance));
      }
    } catch (InvalidConfigurationException | IOException pE) {
      throw new CPAException("Could not create algorithm", pE);
    }
    return status;
  }
}
