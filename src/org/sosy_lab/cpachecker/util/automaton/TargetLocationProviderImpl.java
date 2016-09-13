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
package org.sosy_lab.cpachecker.util.automaton;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import java.util.logging.Level;


public class TargetLocationProviderImpl implements TargetLocationProvider {

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logManager;
  private final CFA cfa;
  private final ImmutableSet<CFANode> allNodes;

  public TargetLocationProviderImpl(
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogManager,
      CFA pCfa) {
    shutdownNotifier = pShutdownNotifier;
    logManager = pLogManager.withComponentName("TargetLocationProvider");
    cfa = pCfa;
    allNodes = ImmutableSet.copyOf(cfa.getAllNodes());
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      CFANode pRootNode, Specification specification) {
    try {
      // Create new configuration with default set of CPAs
      ConfigurationBuilder configurationBuilder = Configuration.builder();
      configurationBuilder.loadFromResource(getClass(), "find-target-locations.properties");
      Configuration configuration = configurationBuilder.build();

      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration);
      CPABuilder cpaBuilder = new CPABuilder(configuration, logManager, shutdownNotifier, reachedSetFactory);
      final ConfigurableProgramAnalysis cpa =
          cpaBuilder.buildCPAs(cfa, specification, new AggregatedReachedSets());

      ReachedSet reached = reachedSetFactory.create();
      reached.add(
          cpa.getInitialState(pRootNode, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(pRootNode, StateSpacePartition.getDefaultPartition()));
      CPAAlgorithm targetFindingAlgorithm = CPAAlgorithm.create(cpa, logManager, configuration, shutdownNotifier);
      try {

        while (reached.hasWaitingState()) {
          targetFindingAlgorithm.run(reached);
        }

      } finally {
        CPAs.closeCpaIfPossible(cpa, logManager);
        CPAs.closeIfPossible(targetFindingAlgorithm, logManager);
      }

      // Order of reached is the order in which states were created,
      // toSet() keeps ordering, so the result is deterministic.
      return from(reached)
          .filter(AbstractStates.IS_TARGET_STATE)
          .transform(AbstractStates.EXTRACT_LOCATION)
          .toSet();

    } catch (InvalidConfigurationException e) {
      // Supplied configuration should not fail.
      throw new AssertionError("Configuration of TargetLocationProviderImpl failed", e);

    } catch (CPAException e) {
      if (!e.toString().toLowerCase().contains("recursion")) {
        logManager.logUserException(Level.WARNING, e, "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        logManager.log(Level.INFO, "Recursion detected. Defaulting to selecting all locations as potential target locations.");
        logManager.logDebugException(e);
      }

      return allNodes;

    } catch (InterruptedException e) {
      if (!shutdownNotifier.shouldShutdown()) {
        logManager.logException(
            Level.WARNING,
            e,
            "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        // can happen, if several algorithms are executed in parallel,
        // one of them finishes earlier and kills the other one.
        logManager.log(
            Level.WARNING,
            "Finding target locations was interrupted. Defaulting to select all locations as potential target locations.");
      }
      return allNodes;
    }
  }
}
