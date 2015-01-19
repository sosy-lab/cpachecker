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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

@Options(prefix="customInstructions")
public class CustomInstructionRequirementsExtractingAlgorithm implements Algorithm {

  private final Algorithm analysis;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @Option(secure=true, name="definitionFile", description = "") //TODO
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path appliedCustomInstructionsDefinition;

  /**
   * TODO
   * @param analysisAlgorithm
   * @param cpa
   * @param config
   * @param logger
   * @param sdNotifier
   * @throws InvalidConfigurationException
   */
  public CustomInstructionRequirementsExtractingAlgorithm(final Algorithm analysisAlgorithm,
      final ConfigurableProgramAnalysis cpa, final Configuration config, final LogManager logger,
      final ShutdownNotifier sdNotifier) throws InvalidConfigurationException {

    config.inject(this);

    analysis = analysisAlgorithm;
    this.logger = logger;
    this.shutdownNotifier = sdNotifier;

    if (!appliedCustomInstructionsDefinition.toFile().exists()) {
      throw new InvalidConfigurationException("The given path " + appliedCustomInstructionsDefinition + "is not a valid path to a file.");
    }

    // TODO cpa spaeter
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {

    logger.log(Level.INFO);

    // analysis was unsound
    if (!analysis.run(pReachedSet)) {
      logger.log(Level.SEVERE, "Do not extract requirements since analysis failed.");
      return false;
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Get custom instruction applications in program.");

    try (BufferedReader br = new BufferedReader(new FileReader(appliedCustomInstructionsDefinition.toFile()))) {

      // TODO file parsen mit geschriebenen parser

    } catch (FileNotFoundException ex) {
      logger.log(Level.SEVERE, "The file '" + appliedCustomInstructionsDefinition + "' was not found", ex);
      return false;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Parsing the file '" + appliedCustomInstructionsDefinition + "' failed.", e);
      return false;
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO);

    // TODO welche parameter?
//    extractRequirements(root, pCustomIA);

    return true;
  }

  /**
   * TODO
   * @param root
   * @param pCustomIA
   */
  private void extractRequirements(final ARGState root, final CustomInstructionApplications pCustomIA) {
    // TODO was genau? spaeter
//    getCustomInstructionState(root, pCustomIA); ?
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
  private ImmutableSet<ARGState> getCustomInstructionState(
      final ARGState root, final CustomInstructionApplications pCustomIA)
      throws InterruptedException, CPAException{

    Builder<ARGState> set = new ImmutableSet.Builder<>();
    Set<ARGState> visitedNodes = new HashSet<>();
    Queue<ARGState> queue = new LinkedList<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      ARGState tmp = queue.poll();
      visitedNodes.add(tmp);

        if (pCustomIA.isEndState(tmp, root)) {
          set.add(tmp);
        }

      // breadth-first-search
      Collection<ARGState> children = tmp.getChildren();
      for (ARGState child : children) {
        if (!visitedNodes.contains(child)) {
          queue.add(child);
        }
      }
    }

    return set.build();
  }

}