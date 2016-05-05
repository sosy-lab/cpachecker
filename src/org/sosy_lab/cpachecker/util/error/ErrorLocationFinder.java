/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.error;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Find all error locations in a CFA.
 */
public class ErrorLocationFinder {
  public static Optional<Set<CFANode>> findErrorLocations(
      final MutableCFA pMutableCFA,
      final LogManager logger,
      final ShutdownNotifier pShutdownNotifier,
      Configuration pConfiguration
  ) throws InterruptedException {

    CFA cfa = pMutableCFA.makeImmutableCFA(
        Optional.<VariableClassification>absent());
    Set<CFANode> out = new HashSet<>();
    try {
      String spec = pConfiguration.getProperty("specification");
      if (spec == null) {
        logger.log(Level.WARNING,
            "No specification set, error locations can not be found");
        return Optional.absent();
      }
      Configuration config = Configuration.builder()
          .copyFrom(pConfiguration)
          .setOption("output.disable", "true")
          .clearOption("cpa")
          .clearOption("cpas")
          .clearOption("CompositeCPA.cpas")
          .setOption("cpa", "cpa.composite.CompositeCPA")
          .setOption("CompositeCPA.cpas", "cpa.location.LocationCPA")
          .setOption("cpa.automaton.breakOnTargetState", "0")
          .build();
      ReachedSetFactory reachedFactory = new ReachedSetFactory(config);
      ConfigurableProgramAnalysis cpa = new CPABuilder(
          config, logger, pShutdownNotifier, reachedFactory
      ).buildCPAWithSpecAutomatas(cfa);
      Algorithm algorithm = CPAAlgorithm.create(
          cpa, logger, config, pShutdownNotifier);
      ReachedSet reached = reachedFactory.create();
      reached.add(
          cpa.getInitialState(cfa.getMainFunction(),
              StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(cfa.getMainFunction(),
              StateSpacePartition.getDefaultPartition()));
      algorithm.run(reached);
      CPAs.closeCpaIfPossible(cpa, logger);
      CPAs.closeIfPossible(algorithm, logger);
      out.addAll(FluentIterable.from(reached).filter(AbstractStates.IS_TARGET_STATE)
          .transform(AbstractStates.EXTRACT_LOCATION).toSet());
      return Optional.of(out);
    } catch (CPAException|InvalidConfigurationException e) {
      logger.logUserException(Level.WARNING, e,
          "Error during CFA reduction");
      return Optional.absent();
    }
  }
}
