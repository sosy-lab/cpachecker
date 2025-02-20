// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ObserverAutomatonCPA extends ControlAutomatonCPA {
  private final CFA pcfa;
  private final LogManager logger;
  private final ShutdownNotifier pShutdownNotifier;
  private final Configuration config;

  private ObserverAutomatonCPA(
      @OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig,
      LogManager pLogger,
      CFA cfa,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    super(pAutomaton, pConfig, pLogger, cfa, shutdownNotifier);
    super.getAutomaton().assertObserverAutomaton();
    pcfa = cfa;
    logger = pLogger;
    pShutdownNotifier = shutdownNotifier;
    config = pConfig;
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ObserverAutomatonCPA.class);
  }

  @Override
  public ControlAutomatonCPA invert()
      throws CPATransferException, InvalidConfigurationException, InterruptedException {
    return new ObserverAutomatonCPA(
        super.getAutomaton(),
        Configuration.builder()
            .copyFrom(config)
            .setOption("cpa.automaton.invertTransferRelation", "true")
            .build(),
        logger,
        pcfa,
        pShutdownNotifier);
  }
}
