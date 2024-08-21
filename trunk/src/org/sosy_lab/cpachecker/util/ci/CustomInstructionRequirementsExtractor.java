// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.ci.redundancyremover.RedundantRequirementsRemover;

@Options(prefix = "custominstructions")
public class CustomInstructionRequirementsExtractor {

  @Option(
      secure = true,
      description =
          "Try to remove requirements that are covered by another requirment and are, thus,"
              + " irrelevant for custom instruction behavior")
  private boolean removeCoveredRequirements = false;

  @Option(
      secure = true,
      description =
          "Try to remove parts of requirements that are not related to custom instruction and are,"
              + " thus, irrelevant for custom instruction behavior")
  private boolean enableRequirementsSlicing = false;

  @Option(
      secure = true,
      description = "Where to dump the requirements on custom instruction extracted from analysis")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathCounterTemplate dumpCIRequirements = PathCounterTemplate.ofFormatString("ci%d.smt");

  @Option(
      secure = true,
      description =
          "Qualified name of class for abstract state which provides custom instruction"
              + " requirements.")
  private String requirementsStateClassName;

  private final Class<? extends AbstractState> requirementsStateClass;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ConfigurableProgramAnalysis cpa;

  @SuppressWarnings("unchecked")
  public CustomInstructionRequirementsExtractor(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    try {
      requirementsStateClass =
          (Class<? extends AbstractState>) Class.forName(requirementsStateClassName);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException(
          "The abstract state " + requirementsStateClassName + " is unknown.");
    } catch (ClassCastException ex) {
      throw new InvalidConfigurationException(
          requirementsStateClassName + "is not an abstract state.");
    }
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cpa = pCpa;
  }

  /**
   * Extracts all start and end nodes of the ARGState root and writes them via
   * CustomInstrucionRequirementsWriter.
   *
   * @param root ARGState
   * @param cia CustomInstructionApplications
   * @throws InterruptedException if a shutdown was requested
   */
  public void extractRequirements(final ARGState root, final CustomInstructionApplications cia)
      throws InterruptedException, CPAException {
    if (dumpCIRequirements == null) {
      logger.log(
          Level.WARNING,
          "Output files for saving requirements for custom instruction not specified.");
      return;
    }
    CustomInstructionRequirementsWriter writer =
        new CustomInstructionRequirementsWriter(
            dumpCIRequirements, requirementsStateClass, logger, cpa, enableRequirementsSlicing);
    Collection<ARGState> ciStartNodes = getCustomInstructionStartNodes(root, cia);

    List<Pair<ARGState, Collection<ARGState>>> requirements = new ArrayList<>(ciStartNodes.size());
    List<Pair<List<String>, List<String>>> signatures = new ArrayList<>(ciStartNodes.size());
    for (ARGState start : ciStartNodes) {
      shutdownNotifier.shutdownIfNecessary();
      requirements.add(Pair.of(start, findEndStatesFor(start, cia)));
      signatures.add(
          Pair.of(
              cia.getAppliedCustomInstructionFor(start).getInputVariablesAndConstants(),
              cia.getAppliedCustomInstructionFor(start).getOutputVariables()));
    }

    if (removeCoveredRequirements) {
      requirements =
          RedundantRequirementsRemover.removeRedundantRequirements(
              requirements, signatures, requirementsStateClass);
    }

    for (Pair<ARGState, Collection<ARGState>> requirement : requirements) {
      shutdownNotifier.shutdownIfNecessary();
      try {
        writer.writeCIRequirement(
            requirement.getFirst(),
            requirement.getSecond(),
            cia.getAppliedCustomInstructionFor(requirement.getFirst()));
      } catch (IOException e) {
        logger.log(
            Level.SEVERE,
            "Writing  the CIRequirement failed at node " + requirement.getFirst() + ".",
            e);
      }
    }
  }

  public Class<? extends AbstractState> getRequirementsStateClass() {
    return requirementsStateClass;
  }

  /**
   * Returns a Set of ARGState with all states of the root-tree which are startStates of the given
   * CustomInstructionApplications
   *
   * @param root ARGState
   * @param pCustomIA CustomInstructionApplication
   * @return ImmutableSet of ARGState
   */
  private Collection<ARGState> getCustomInstructionStartNodes(
      final ARGState root, final CustomInstructionApplications pCustomIA)
      throws InterruptedException, CPAException {

    ImmutableSet.Builder<ARGState> set = new ImmutableSet.Builder<>();
    Set<ARGState> visitedNodes = new HashSet<>();
    Queue<ARGState> queue = new ArrayDeque<>();

    queue.add(root);
    visitedNodes.add(root);

    ARGState tmp;

    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      tmp = queue.poll();
      visitedNodes.add(tmp);

      if (pCustomIA.isStartState(tmp)) {
        set.add(uncover(tmp));
      }

      // breadth-first-search
      for (ARGState child : tmp.getChildren()) {
        if (!visitedNodes.contains(child)) {
          queue.add(child);
          visitedNodes.add(child);
        }
      }
    }

    return set.build();
  }

  /**
   * Returns a Collection of ARGState of all EndStates which are in the tree of ciStart
   *
   * @param ciStart ARGState
   * @return Collection of ARGState
   * @throws InterruptedException if a shutdown was requested
   */
  private Collection<ARGState> findEndStatesFor(
      final ARGState ciStart, final CustomInstructionApplications pCustomIA)
      throws InterruptedException, CPAException {
    List<ARGState> list = new ArrayList<>();
    Queue<ARGState> queue = new ArrayDeque<>();
    Set<ARGState> visitedNodes = new HashSet<>();

    queue.add(ciStart);
    visitedNodes.add(ciStart);

    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      ARGState tmp = queue.poll();

      if (pCustomIA.isEndState(tmp, ciStart)) {
        list.add(tmp);
        continue;
      }

      // breadth-first-search
      for (ARGState child : tmp.getChildren()) {
        child = uncover(child);
        if (!visitedNodes.contains(child)) {
          queue.add(child);
          visitedNodes.add(child);
        }
      }
    }

    return list;
  }

  private ARGState uncover(final ARGState state) {
    if (state.isCovered()) {
      return uncover(state.getCoveringState());
    }
    return state;
  }
}
