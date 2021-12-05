// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request;

import static java.util.logging.Level.INFO;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ReusableCoreComponents;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

abstract public class CPACreatingRequest {
  protected final ShutdownNotifier shutdownNotifier;
  protected final MessageFactory messageFactory;
  protected final LogManager logManager;

  protected Algorithm algorithm = null;
  protected ARGCPA cpa = null;
  protected ReachedSet reached = null;

  public CPACreatingRequest(
      final MessageFactory pMessageFactory,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier) {
    messageFactory = pMessageFactory;
    logManager = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  protected void prepareCPA(
      final Configuration pTaskConfiguration,
      final CFA pCFA,
      final Specification pSpecification,
      final Block pBlock,
      final Optional<ReusableCoreComponents> pReusableCoreComponents)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    CoreComponentsFactory factory =
        new CoreComponentsFactory(pTaskConfiguration, logManager, shutdownNotifier,
            AggregatedReachedSets.empty());
    CompositeCPA compositeCPA;
    
    if(pReusableCoreComponents.isPresent()) {
      ReusableCoreComponents components = pReusableCoreComponents.orElseThrow();
      compositeCPA = components.getCpa();
      
      logManager.log(INFO, "Reusing existing core components.");
    } else {
      WrapperCPA configuredCPA = (WrapperCPA) factory.createCPA(pCFA, pSpecification);
      compositeCPA = configuredCPA.retrieveWrappedCpa(CompositeCPA.class);
      assert compositeCPA != null : "Configured CPAs must contain CompositeCPA!";
    }

    cpa = injectBlockAwareCPA(compositeCPA, pSpecification, pCFA, pBlock, pTaskConfiguration);
    reached = factory.createReachedSet(cpa);
    algorithm = factory.createAlgorithm(cpa, pCFA, pSpecification);
  }

  private ARGCPA injectBlockAwareCPA(
      final CompositeCPA baseCPA,
      final Specification pSpecification,
      final CFA pCFA, final Block pBlock, final Configuration pTaskConfiguration)
      throws InvalidConfigurationException, CPAException {
    BlockAwareCompositeCPA blockAwareCPA =
        (BlockAwareCompositeCPA)
            BlockAwareCompositeCPA.factory()
                .setConfiguration(pTaskConfiguration)
                .setLogger(logManager)
                .setShutdownNotifier(shutdownNotifier)
                .set(pCFA, CFA.class)
                .set(pBlock, Block.class)
                .set(baseCPA, CompositeCPA.class)
                .createInstance();

    return (ARGCPA) ARGCPA.factory()
        .setConfiguration(pTaskConfiguration)
        .setLogger(logManager)
        .setShutdownNotifier(shutdownNotifier)
        .set(blockAwareCPA, ConfigurableProgramAnalysis.class)
        .set(pSpecification, Specification.class)
        .set(pCFA, CFA.class)
        .createInstance();
  }
}
