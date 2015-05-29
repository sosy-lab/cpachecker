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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.ci.AppliedCustomInstructionParser;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionRequirementsWriter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

@Options(prefix="custominstructions")
public class CustomInstructionRequirementsExtractingAlgorithm implements Algorithm {

  private final Algorithm analysis;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(secure=true, name="definitionFile", description = "File to be parsed")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path appliedCustomInstructionsDefinition;

  @Option(secure=true, description="Prefix for files containing the custom instruction requirements.")
  private String ciFilePrefix = "ci";

  @Option(secure=true, description="Qualified name of class for abstract state which provides custom instruction requirements.")
  private String requirementsStateClassName;

  private Class<? extends AbstractState> requirementsStateClass;

  private CFA cfa;
  private final Configuration config;

  /**
   * Constructor of CustomInstructionRequirementsExtractingAlgorithm
   * @param analysisAlgorithm Algorithm
   * @param cpa ConfigurableProgramAnalysis
   * @param config Configuration
   * @param logger LogManager
   * @param sdNotifier ShutdownNotifier
   * @throws InvalidConfigurationException if the given Path not exists
   */
  @SuppressWarnings("unchecked")
  public CustomInstructionRequirementsExtractingAlgorithm(final Algorithm analysisAlgorithm,
      final ConfigurableProgramAnalysis cpa, final Configuration config, final LogManager logger,
      final ShutdownNotifier sdNotifier, final CFA cfa) throws InvalidConfigurationException {

    config.inject(this);

    analysis = analysisAlgorithm;
    this.logger = logger;
    this.shutdownNotifier = sdNotifier;
    this.config = config;

    if (cpa instanceof ARGCPA) {
      throw new InvalidConfigurationException("The given cpa " + cpa + "is not an instance of ARGCPA");
    }

    if (!appliedCustomInstructionsDefinition.toFile().exists()) {
      throw new InvalidConfigurationException("The given path '" + appliedCustomInstructionsDefinition + "' is not a valid path to a file.");
    }

    try {
      requirementsStateClass = (Class<? extends AbstractState>) Class.forName(requirementsStateClassName);
    } catch (ClassNotFoundException e) {
      throw new InvalidConfigurationException("The abstract state " + requirementsStateClassName + " is unknown.");
    } catch (ClassCastException ex) {
      throw new InvalidConfigurationException(requirementsStateClassName + "is not an abstract state.");
    }

    if (AbstractStates.extractStateByType(cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
                                          requirementsStateClass) == null) {
      throw new InvalidConfigurationException(requirementsStateClass + "is not an abstract state.");
    }

    // TODO to be continued: CFA integration
    this.cfa = cfa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, " Start analysing to compute requirements.");

    AlgorithmStatus status = analysis.run(pReachedSet);

    // analysis was unsound
    if (!status.isSound()) {
      logger.log(Level.SEVERE, "Do not extract requirements since analysis failed.");
      return status;
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Get custom instruction applications in program.");

    CustomInstructionApplications cia = null;
    try {
      cia = new AppliedCustomInstructionParser(shutdownNotifier, cfa).parse(appliedCustomInstructionsDefinition);
    } catch (FileNotFoundException ex) {
      logger.log(Level.SEVERE, "The file '" + appliedCustomInstructionsDefinition + "' was not found", ex);
      return status.withSound(false);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Parsing the file '" + appliedCustomInstructionsDefinition + "' failed.", e);
      return status.withSound(false);
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Start extracting requirements for applied custom instructions");

    extractRequirements((ARGState)pReachedSet.getFirstState(), cia);
    return status;
  }

  /**
   * Extracts all start and end nodes of the ARGState root and writes them via
   * CustomInstrucionRequirementsWriter.
   * @param root ARGState
   * @param cia CustomInstructionApplications
   * @throws InterruptedException if a shutdown was requested
   */
  private void extractRequirements(final ARGState root, final CustomInstructionApplications cia)
      throws InterruptedException, CPAException {
    CustomInstructionRequirementsWriter writer = new CustomInstructionRequirementsWriter(ciFilePrefix, requirementsStateClass, config, shutdownNotifier, logger);
    Collection<ARGState> ciStartNodes = getCustomInstructionStartNodes(root, cia);

    for (ARGState node : ciStartNodes) {
      shutdownNotifier.shutdownIfNecessary();
      try {
        writer.writeCIRequirement(node, findEndStatesFor(node, cia), cia.getAppliedCustomInstructionFor(node));
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Writing  the CIRequirement failed at node " + node + ".", e);
      }
    }
  }

  /**
   * Returns a Set of ARGState with all states of the root-tree which are startStates
   * of the given CustomInstructionApplications
   * @param root ARGState
   * @param pCustomIA CustomInstructionApplication
   * @return ImmutableSet of ARGState
   * @throws InterruptedException
   * @throws CPAException
   */
  private Collection<ARGState> getCustomInstructionStartNodes(final ARGState root, final CustomInstructionApplications pCustomIA)
      throws InterruptedException, CPAException{

    Builder<ARGState> set = new ImmutableSet.Builder<>();
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
        set.add(tmp);
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
   * @param ciStart ARGState
   * @return Collection of ARGState
   * @throws InterruptedException if a shutdown was requested
   * @throws CPAException
   */
  private Collection<ARGState> findEndStatesFor(final ARGState ciStart, final CustomInstructionApplications pCustomIA)
      throws InterruptedException, CPAException {
    ArrayList<ARGState> list = new ArrayList<>();
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
        if (!visitedNodes.contains(child)) {
          queue.add(child);
          visitedNodes.add(child);
        }
      }
    }

    return list;
  }
}