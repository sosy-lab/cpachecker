// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.witness;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.CEGARAlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraphModification.Modification;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.path.PathCPA;
import org.sosy_lab.cpachecker.cpa.path.ViolationWitness;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ARGToYAMLWitnessExport;

public class DssWitnessExporter {

  private final ConfigurableProgramAnalysis violationCPA;
  private final LogManager logger;
  private final Configuration configuration;
  private final ShutdownManager shutdownManager;
  private final Specification specification;

  public DssWitnessExporter(
      ConfigurableProgramAnalysis pViolationCPA,
      LogManager pLogger,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      Specification pSpecification) {
    violationCPA = pViolationCPA;
    logger = pLogger;
    configuration = pConfiguration;
    shutdownManager = pShutdownManager;
    specification = pSpecification;
  }

  public void export(
      ResultWithWitnessInformation resultWithWitness,
      ReachedSet reachedSet,
      Modification pModification)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    Result result = resultWithWitness.getResult();

    if (result == Result.TRUE) {
      reachedSet.clear();
      if (resultWithWitness.hasWitnessInformation()) {
        if (resultWithWitness.getCorrectnessPreConditionCollector().recievedAllStates()) {
          logger.log(Level.INFO, "Exporting Correctness Witness");
          exportCorrectnessWitness(resultWithWitness, reachedSet, pModification);
        } else {
          logger.log(
              Level.SEVERE,
              "Unable to export Witness because not all necessary information has been recived");
        }
      }
    } else if (result == Result.FALSE) {

      if (resultWithWitness.hasWitnessInformation()) {
        logger.log(Level.INFO, "Preparing Violation Witness");
        fillReachedSetWithViolation(
            reachedSet, resultWithWitness.getViolationPath(), pModification);
        logger.log(Level.INFO, "Violation Witness prepared");

      } else {
        addDummyTargetToReachedSet(reachedSet);
      }
    }
  }

  private void addDummyTargetToReachedSet(ReachedSet reachedSet) {
    ARGState state = (ARGState) reachedSet.getFirstState();
    assert state != null;
    CompositeState cState = (CompositeState) state.getWrappedState();
    Precision initialPrecision = reachedSet.getPrecision(state);
    assert cState != null;
    List<AbstractState> states = new ArrayList<>(cState.getWrappedStates());
    states.add(DummyTargetState.withoutTargetInformation());
    reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
  }

  private void exportCorrectnessWitness(
      ResultWithWitnessInformation resultWithWitness,
      ReachedSet reachedSet,
      Modification pModification)
      throws InvalidConfigurationException {
    ARGToYAMLWitnessExport exporter =
        new ARGToYAMLWitnessExport(
            configuration,
            pModification.metadata().originalCfa(),
            specification,
            logger,
            resultWithWitness.getCorrectnessPreConditionCollector());
    PathTemplate correctnessWitnessPath =
        new DssAnalysisOptions(configuration).getYamlCorrectnessWitnessOutputFileTemplate();
    try {

      exporter.export(
          AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class),
          reachedSet,
          correctnessWitnessPath);

    } catch (IOException | ReportingMethodNotImplementedException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Could not export the YAML correctness witness directly from the collected ARG states."
              + "Therefore no YAML witness will be exported.");
    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
    }
  }

  private void fillReachedSetWithViolation(
      ReachedSet reachedSet, ViolationWitness pViolationPath, Modification modification)
      throws CPAException, InterruptedException, InvalidConfigurationException {

    ViolationWitness mappedViolation = convertToOriginalEdges(pViolationPath, modification);
    Optional.ofNullable(CPAs.retrieveCPA(violationCPA, PathCPA.class))
        .ifPresent(p -> p.init(mappedViolation));

    reachedSet.clear();

    // TODO which analysis should this actually use to be most efficient? With which configuration?
    reachedSet.add(
        violationCPA.getInitialState(
            modification.metadata().originalCfa().getMainFunction(),
            StateSpacePartition.getDefaultPartition()),
        violationCPA.getInitialPrecision(
            modification.metadata().originalCfa().getMainFunction(),
            StateSpacePartition.getDefaultPartition()));

    try (CEGARAlgorithm violationAlgorithm =
        new CEGARAlgorithmFactory(
                CPAAlgorithm.create(
                    violationCPA, logger, configuration, shutdownManager.getNotifier()),
                violationCPA,
                logger,
                configuration,
                shutdownManager.getNotifier())
            .newInstance()) {

      violationAlgorithm.run(reachedSet);
    }

    if (!reachedSet.wasTargetReached()) {
      logger.log(Level.SEVERE, "Violation path for witness was not correct, cancelling export");
      reachedSet.clear();
      reachedSet.add(
          violationCPA.getInitialState(
              modification.metadata().originalCfa().getMainFunction(),
              StateSpacePartition.getDefaultPartition()),
          violationCPA.getInitialPrecision(
              modification.metadata().originalCfa().getMainFunction(),
              StateSpacePartition.getDefaultPartition()));

      addDummyTargetToReachedSet(reachedSet);
    }
  }

  private ViolationWitness convertToOriginalEdges(
      ViolationWitness pViolationPath, Modification pModification) {

    return pViolationPath.transformEdges(
        e -> ViolationWitness.edgeToString(parseEdge(e, pModification)));
  }

  private CFAEdge parseEdge(String edge, Modification pModification) {

    CFAEdge modifiedEdge =
        Iterables.getOnlyElement(
            FluentIterable.from(pModification.cfa().edges())
                .filter(
                    e ->
                        edge.equals(
                            "N"
                                + e.getPredecessor().getNodeNumber()
                                + "N"
                                + e.getSuccessor().getNodeNumber())));

    if (modifiedEdge instanceof BlankEdge blank
        && blank.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
      return null;
    }

    return Iterables.getOnlyElement(
        FluentIterable.from(
                pModification.metadata().mappingInfo().originalToInstrumentedEdges().entrySet())
            .filter(entry -> entry.getValue().equals(modifiedEdge))
            .transform(entry -> entry.getKey()));
  }
}
