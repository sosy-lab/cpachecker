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

public class ObserverAutomatonCPA extends ControlAutomatonCPA {

  private ObserverAutomatonCPA(
      @OptionalAnnotation Automaton pAutomaton,
      Configuration pConfig,
      LogManager pLogger,
      CFA cfa,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pAutomaton, pConfig, pLogger, cfa, pShutdownNotifier);
    super.getAutomaton().assertObserverAutomaton();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ObserverAutomatonCPA.class);
  }
}
