package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.Optional;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;

public class ObserverAutomatonCPA extends ControlAutomatonCPA {
  
  private ObserverAutomatonCPA(@Optional Automaton pAutomaton, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pAutomaton, pConfig, pLogger);
    super.getAutomaton().assertObserverAutomaton();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ObserverAutomatonCPA.class);
  }

}
