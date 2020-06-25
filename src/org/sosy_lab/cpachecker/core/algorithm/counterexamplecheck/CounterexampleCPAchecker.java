/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.io.TempFile.DeleteOnCloseFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

@Options(prefix="counterexample.checker")
public class CounterexampleCPAchecker implements CounterexampleChecker {

  // The following options will be forced in the counterexample check
  // to have the same value as in the actual analysis.
  private static final ImmutableSet<String> OVERWRITE_OPTIONS =
      ImmutableSet.of(
          "analysis.machineModel",
          "cpa.predicate.handlePointerAliasing",
          "cpa.predicate.memoryAllocationsAlwaysSucceed",
          "testcase.targets.type",
          "testcase.targets.optimization.strategy",
          "testcase.generate.parallel");

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final Specification specification;
  private final CFA cfa;

  @Option(secure=true, name = "path.file",
      description = "File name where to put the path specification that is generated "
      + "as input for the counterexample check. A temporary file is used if this is unspecified.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path specFile;

  @Option(
    secure = true,
    name = "config",
    required = true,
    description = "configuration file for counterexample checks with CPAchecker"
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private @Nullable Path configFile;

  @Option(
    secure = true,
    name = "changeCEXInfo",
    description =
        "counterexample information should provide more precise information from counterexample check, if available"
  )
  private boolean provideCEXInfoFromCEXCheck = false;

  private final Function<ARGState, Optional<CounterexampleInfo>> getCounterexampleInfo;

  private WitnessExporter witnessExporter;

  public CounterexampleCPAchecker(
      Configuration config,
      Specification pSpecification,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Function<ARGState, Optional<CounterexampleInfo>> pGetCounterexampleInfo)
      throws InvalidConfigurationException {
    this.logger = logger;
    this.config = config;
    specification = pSpecification;
    config.inject(this);
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    getCounterexampleInfo = Objects.requireNonNull(pGetCounterexampleInfo);
    this.witnessExporter = new WitnessExporter(config, logger, specification, cfa);
  }

  @Override
  public boolean checkCounterexample(ARGState pRootState,
      ARGState pErrorState, Set<ARGState> pErrorPathStates)
      throws CPAException, InterruptedException {

    try {
      if (specFile != null) {
        return checkCounterexample(pRootState, pErrorState, pErrorPathStates, specFile);
      }

      // This temp file will be automatically deleted when the try block terminates.
      try (DeleteOnCloseFile automatonFile =
          TempFile.builder()
              .prefix("counterexample-automaton")
              .suffix(".graphml")
              .createDeleteOnClose()) {

        return checkCounterexample(pRootState, pErrorState, pErrorPathStates,
            automatonFile.toPath());
      }

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path automaton to file " + e.getMessage(), e);
    }
  }

  private boolean checkCounterexample(ARGState pRootState, ARGState pErrorState, Set<ARGState> pErrorPathStates,
      Path automatonFile) throws IOException, CPAException, InterruptedException {

    try (Writer w = IO.openOutputFile(automatonFile, Charset.defaultCharset())) {
      final Predicate<ARGState> relevantState = Predicates.in(pErrorPathStates);
      witnessExporter.writeErrorWitness(
          w,
          pRootState,
          relevantState,
          BiPredicates.bothSatisfy(relevantState),
          getCounterexampleInfo.apply(pErrorState).orElse(null));
    }

    // We assume only one initial node for an analysis, even for mutli-threaded tasks.
    CFANode entryNode = Iterables.getOnlyElement(extractLocations(pRootState));
    LogManager lLogger = logger.withComponentName("CounterexampleCheck");

    try {
      ConfigurationBuilder lConfigBuilder = Configuration.builder().loadFromFile(configFile);

      for (String option : OVERWRITE_OPTIONS) {
        lConfigBuilder.copyOptionFromIfPresent(config, option);
      }

      Configuration lConfig = lConfigBuilder.build();
      ShutdownManager lShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker.fromConfiguration(lConfig, lLogger, lShutdownManager).start();

      Specification lSpecification =
          Specification.fromFiles(
              specification.getProperties(),
              Iterables.concat(specification.getSpecFiles(), Collections.singleton(automatonFile)),
              cfa,
              lConfig,
              lLogger,
              shutdownNotifier);
      CoreComponentsFactory factory =
          new CoreComponentsFactory(
              lConfig, lLogger, lShutdownManager.getNotifier(), new AggregatedReachedSets());
      ConfigurableProgramAnalysis lCpas = factory.createCPA(cfa, lSpecification);
      Algorithm lAlgorithm = factory.createAlgorithm(lCpas, cfa, lSpecification);
      ReachedSet lReached = factory.createReachedSet();
      lReached.add(
          lCpas.getInitialState(entryNode, StateSpacePartition.getDefaultPartition()),
          lCpas.getInitialPrecision(entryNode, StateSpacePartition.getDefaultPartition()));

      lAlgorithm.run(lReached);

      lShutdownManager.requestShutdown("Analysis terminated");
      CPAs.closeCpaIfPossible(lCpas, lLogger);
      CPAs.closeIfPossible(lAlgorithm, lLogger);

      if (provideCEXInfoFromCEXCheck) {
        AbstractState target = from(lReached).firstMatch(IS_TARGET_STATE).orNull();
        if (target instanceof ARGState) {
          ARGState argTarget = (ARGState) target;
          if (argTarget.getCounterexampleInformation().isPresent()) {
            CounterexampleInfo cexInfo = argTarget.getCounterexampleInformation().get();
            if (!cexInfo.isSpurious() && cexInfo.isPreciseCounterExample()) {
              pErrorState.replaceCounterexampleInformation(
                  CounterexampleInfo.feasiblePrecise(
                      pErrorState.getCounterexampleInformation().isPresent()
                          ? pErrorState.getCounterexampleInformation().get().getTargetPath()
                          : ARGUtils.getOnePathTo(pErrorState),
                      cexInfo.getCFAPathWithAssignments()));
              assert (pErrorPathStates.containsAll(
                  pErrorState.getCounterexampleInformation().get().getTargetPath().asStatesList()));
            }
          }
        }
      }

      // counterexample is feasible if a target state is reachable
      return lReached.hasViolatedProperties();

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw new CounterexampleAnalysisFailed("Counterexample check aborted", e);
    }
  }

}
