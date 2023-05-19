// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

@Options(prefix = "counterexample.checker")
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

  @Option(
      secure = true,
      name = "path.file",
      description =
          "File name where to put the path specification that is generated as input for the"
              + " counterexample check. A temporary file is used if this is unspecified.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path specFile;

  @Option(
      secure = true,
      name = "config",
      required = true,
      description = "configuration file for counterexample checks with CPAchecker")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private @Nullable Path configFile;

  @Option(
      secure = true,
      name = "changeCEXInfo",
      description =
          "counterexample information should provide more precise information from counterexample"
              + " check, if available")
  private boolean provideCEXInfoFromCEXCheck = false;

  @Option(
      secure = true,
      name = "forceCEXChange",
      description =
          "counterexample check should fully replace existing counterexamples with own ones, if"
              + " available")
  private boolean replaceCexWithCexFromCheck = false;

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
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    getCounterexampleInfo = Objects.requireNonNull(pGetCounterexampleInfo);
    witnessExporter = new WitnessExporter(config, logger, specification, cfa);
  }

  @Override
  public boolean checkCounterexample(
      ARGState pRootState, ARGState pErrorState, Set<ARGState> pErrorPathStates)
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

        return checkCounterexample(
            pRootState, pErrorState, pErrorPathStates, automatonFile.toPath());
      }

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(
          "Could not write path automaton to file " + e.getMessage(), e);
    }
  }

  private boolean checkCounterexample(
      ARGState pRootState, ARGState pErrorState, Set<ARGState> pErrorPathStates, Path automatonFile)
      throws IOException, CPAException, InterruptedException {

    final Predicate<ARGState> relevantState = Predicates.in(pErrorPathStates);
    final Witness witness =
        witnessExporter.generateErrorWitness(
            pRootState,
            relevantState,
            BiPredicates.bothSatisfy(relevantState),
            getCounterexampleInfo.apply(pErrorState).orElse(null));
    try (Writer w = IO.openOutputFile(automatonFile, Charset.defaultCharset())) {
      WitnessToOutputFormatsUtils.writeToGraphMl(witness, w);
    }

    // We assume only one initial node for an analysis, even for mutli-threaded tasks.
    CFANode entryNode = Iterables.getOnlyElement(extractLocations(pRootState));
    LogManager lLogger = logger.withComponentName("CounterexampleCheck");

    try {
      ConfigurationBuilder lConfigBuilder = Configuration.builder().loadFromFile(configFile);

      for (String option : OVERWRITE_OPTIONS) {
        lConfigBuilder.copyOptionFromIfPresent(config, option);
      }

      if (provideCEXInfoFromCEXCheck) {
        CFAEdge targetEdge = pErrorState.getParents().iterator().next().getEdgeToChild(pErrorState);
        lConfigBuilder.setOption(
            "testcase.targets.edge",
            targetEdge.getPredecessor().getNodeNumber()
                + "#"
                + System.identityHashCode(targetEdge));
      }

      Configuration lConfig = lConfigBuilder.build();
      ShutdownManager lShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker.fromConfiguration(lConfig, lLogger, lShutdownManager).start();

      Specification lSpecification =
          specification.withAdditionalSpecificationFile(
              ImmutableSet.of(automatonFile), cfa, lConfig, lLogger, shutdownNotifier);
      CoreComponentsFactory factory =
          new CoreComponentsFactory(
              lConfig, lLogger, lShutdownManager.getNotifier(), AggregatedReachedSets.empty());
      ConfigurableProgramAnalysis lCpas = factory.createCPA(cfa, lSpecification);
      Algorithm lAlgorithm = factory.createAlgorithm(lCpas, cfa, lSpecification);
      ReachedSet lReached = factory.createReachedSet(lCpas);
      lReached.add(
          lCpas.getInitialState(entryNode, StateSpacePartition.getDefaultPartition()),
          lCpas.getInitialPrecision(entryNode, StateSpacePartition.getDefaultPartition()));

      lAlgorithm.run(lReached);

      lShutdownManager.requestShutdown("Analysis terminated");
      CPAs.closeCpaIfPossible(lCpas, lLogger);
      CPAs.closeIfPossible(lAlgorithm, lLogger);

      if (provideCEXInfoFromCEXCheck || replaceCexWithCexFromCheck) {
        Optional<CounterexampleInfo> counterexampleFromCheck =
            Collections3.filterByClass(lReached.stream(), ARGState.class)
                .filter(AbstractStates::isTargetState)
                .findFirst()
                .flatMap(ARGState::getCounterexampleInformation);
        if (counterexampleFromCheck.isPresent()) {
          if (replaceCexWithCexFromCheck) {
            replaceCounterexampleInformation(
                pRootState, pErrorState, counterexampleFromCheck.orElseThrow());

          } else if (provideCEXInfoFromCEXCheck) {
            improveCounterexampleInformation(pErrorState, counterexampleFromCheck.orElseThrow());
            assert pErrorPathStates.containsAll(
                pErrorState
                    .getCounterexampleInformation()
                    .orElseThrow()
                    .getTargetPath()
                    .asStatesList());
          }
        }
      }

      // counterexample is feasible if a target state is reachable
      return lReached.wasTargetReached();

    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed(
          "Invalid configuration in counterexample-check config: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      throw new CounterexampleAnalysisFailed("Counterexample check aborted", e);
    }
  }

  /**
   * Reconstructs the {@link ARGPath} in the original ARG using the edges from the specified
   * ARG-path. The {@link ARGState}s in the resulting path are from the original ARG, the states
   * from the specified path may be from a different ARG.
   *
   * @param pRoot the root of the original ARG.
   * @param pPath the {@link ARGPath} (from a different ARG).
   * @return the {@link ARGPath} in the original ARG.
   */
  private ARGPath reconstructArgPath(final ARGState pRoot, final ARGPath pPath) {

    List<CFAEdge> pathEdges = pPath.getFullPath();
    List<ARGState> argStates = new ArrayList<>();
    ARGState argCurrent = pRoot;
    int pathIndex = 0;

    while (pathIndex < pathEdges.size()) {
      CFAEdge pathEdge = pathEdges.get(pathIndex);

      boolean found = false;
      for (ARGState argChild : argCurrent.getChildren()) {
        List<CFAEdge> argEdges = argCurrent.getEdgesToChild(argChild);
        List<CFAEdge> subPathEdges =
            pathEdges.subList(pathIndex, Math.min(pathEdges.size(), pathIndex + argEdges.size()));

        if (!argEdges.isEmpty() && argEdges.equals(subPathEdges)) {
          argStates.add(argCurrent);
          argCurrent = argChild;
          pathIndex += argEdges.size();

          found = true;
          break;
        }
      }

      assert found : "Next ARG-state not found for edge: " + pathEdge;
    }

    argStates.add(argCurrent);

    assert argCurrent.isTarget()
        : "Last state of counterexample-path is not a target state: " + argCurrent;

    return new ARGPath(argStates);
  }

  private void replaceCounterexampleInformation(
      final ARGState pRootState,
      final ARGState pStateForCounterexample,
      final CounterexampleInfo pNewInfo) {

    final CounterexampleInfo newInfo;
    if (pNewInfo.isPreciseCounterExample()) {
      // target path is only valid in temporary, local ARG computed by counterexample-check.
      // To make this counterexample usable in other parts of CPAchecker, create a new, exchangeable
      // ARGpath
      ARGPath strippedDownPath = getExchangeableTargetPath(pNewInfo.getTargetPath());
      assert strippedDownPath.getLastState().isTarget()
          : "Last state of exchangeable target path is no target: "
              + strippedDownPath.getLastState();
      ARGPath reconstructedPath = reconstructArgPath(pRootState, strippedDownPath);
      newInfo =
          CounterexampleInfo.feasiblePrecise(
              reconstructedPath, pNewInfo.getCFAPathWithAssignments());
    } else {
      newInfo = pNewInfo;
    }
    Optional<CounterexampleInfo> counterexampleFromArg =
        pStateForCounterexample.getCounterexampleInformation();
    if (counterexampleFromArg.isPresent()) {
      pStateForCounterexample.replaceCounterexampleInformation(newInfo);
    } else {
      pStateForCounterexample.addCounterexampleInformation(newInfo);
    }
  }

  /**
   * Returns an exchangeable version of the given {@link ARGPath}. This exchangeable version will be
   * independent of the ARG and the components it was computed with, but will also miss information
   * in the ARGStates.
   *
   * @param pTargetPath the path to translate into an exchangable version
   * @return exchangable version of the given path
   */
  private ARGPath getExchangeableTargetPath(ARGPath pTargetPath) {
    ARGPathBuilder pathBuilder = ARGPath.builder();

    PathIterator it = pTargetPath.fullPathIterator();
    // use variable to store previous state instead of PathIterator#getPreviousState
    // so that we don't have to handle the special case that the current state is the first state
    // (PathIterator#getPreviousState would fail then)
    ARGState previousState = null;
    do {
      it.advance();

      final ARGState currentState = it.getPreviousAbstractState();
      final CFAEdge edgeLeavingCurrentState = it.getIncomingEdge();

      assert !(previousState == null) || currentState.getParents().isEmpty()
          : "Iterator didn't start at first state, but at " + currentState;
      final ARGState currentDummy = getStateWithIndependentInformation(currentState, previousState);

      pathBuilder.add(currentDummy, edgeLeavingCurrentState);

      if (!it.hasNext()) {
        return pathBuilder.build(it.getAbstractState());
      }
      previousState = currentDummy;
    } while (it.hasNext());
    throw new AssertionError("This location shouldn't be reachable");
  }

  /**
   * Returns an exchangeable version of the given {@link ARGState}. This exchangeable version will
   * be independent of the ARG and the components it was computed with, but will also miss
   * information.
   *
   * <p>At the moment, the created exchangeable version only contains the {@link LocationState} and
   * all {@link AutomatonState AutomatonStates} of the given ARGState.
   *
   * @param pOriginal state to translate into exchangeable version
   * @param pParent new parent of the created, exchangeable version. This should also be an
   *     exchangeable ARGState that was returned by this method, or <code>null</code>
   * @return an exchangeable version of the given ARGState
   */
  private ARGState getStateWithIndependentInformation(
      final ARGState pOriginal, final @Nullable ARGState pParent) {
    final LocationState currentLocation =
        AbstractStates.extractStateByType(pOriginal, LocationState.class);
    final FluentIterable<? extends AbstractState> automatonStates =
        AbstractStates.asIterable(pOriginal).filter(AutomatonState.class);
    final List<AbstractState> allWrappedStates =
        ImmutableList.<AbstractState>builder().add(currentLocation).addAll(automatonStates).build();
    final CompositeState composition = new CompositeState(allWrappedStates);
    return new ARGState(composition, pParent);
  }

  private void improveCounterexampleInformation(
      final ARGState pStateWithCounterexample, final CounterexampleInfo pNewInfo) {
    if (!pNewInfo.isSpurious() && pNewInfo.isPreciseCounterExample()) {
      pStateWithCounterexample.replaceCounterexampleInformation(
          CounterexampleInfo.feasiblePrecise(
              pStateWithCounterexample.getCounterexampleInformation().isPresent()
                  ? pStateWithCounterexample
                      .getCounterexampleInformation()
                      .orElseThrow()
                      .getTargetPath()
                  : ARGUtils.getOnePathTo(pStateWithCounterexample),
              pNewInfo.getCFAPathWithAssignments()));
    }
  }
}
