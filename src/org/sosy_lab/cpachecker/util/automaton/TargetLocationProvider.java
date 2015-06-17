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

import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.collect.ImmutableSet;


public class TargetLocationProvider {

  private final ReachedSetFactory reachedSetFactory;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logManager;
  private final Configuration config;
  private final CFA cfa;

  private final static String specificationPropertyName = "specification";

  public TargetLocationProvider(ReachedSetFactory pReachedSetFactory, ShutdownNotifier pShutdownNotifier,
      LogManager pLogManager, Configuration pConfig, CFA pCfa) {
    super();
    reachedSetFactory = pReachedSetFactory;
    shutdownNotifier = pShutdownNotifier;
    logManager = pLogManager.withComponentName("TargetLocationProvider");
    config = pConfig;
    cfa = pCfa;
  }

  public @Nullable ImmutableSet<CFANode> tryGetAutomatonTargetLocations(CFANode pRootNode) {
    try {
      // Create new configuration with default set of CPAs
      ConfigurationBuilder configurationBuilder = Configuration.builder();
      if (config.hasProperty(specificationPropertyName)) {
        configurationBuilder.copyOptionFrom(config, specificationPropertyName);
      }
      configurationBuilder.setOption("output.disable", "true");
      configurationBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA");
      configurationBuilder.setOption("cpa.callstack.skipRecursion", "true");

      Configuration configuration = configurationBuilder.build();
      CPABuilder cpaBuilder = new CPABuilder(configuration, logManager, shutdownNotifier, reachedSetFactory);
      ConfigurableProgramAnalysis cpa = cpaBuilder.buildCPAWithSpecAutomatas(cfa);

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

    } catch (InvalidConfigurationException | CPAException e) {

      if (!e.toString().toLowerCase().contains("recursion")) {
        logManager.logUserException(Level.WARNING, e, "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        logManager.log(Level.INFO, "Recursion detected. Defaulting to selecting all locations as potential target locations.");
        logManager.logDebugException(e);
      }

      return null;

    } catch (InterruptedException e) {
      if (!shutdownNotifier.shouldShutdown()) {
        logManager.logException(Level.WARNING, e, "Unable to find target locations. Defaulting to selecting all locations as potential target locations.");
      } else {
        logManager.logDebugException(e);
      }
      return null;
    }
  }
}
