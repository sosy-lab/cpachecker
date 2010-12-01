package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

public class ObserverAutomatonCPA extends ControlAutomatonCPA {
  
    private ObserverAutomatonCPA(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    super.getAutomaton().assertObserverAutomaton();
  }
    
    private static class AutomatonCPAFactory extends ControlAutomatonCPA.AutomatonCPAFactory {

      @Override
      public CPAFactory setAutomaton(Automaton pAutomaton) {
        throw new UnsupportedOperationException();
      }
      
      @Override
      public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
        return new ObserverAutomatonCPA(getConfiguration(), getLogger());
      }
    }

    public static AutomatonCPAFactory factory() {
      return new AutomatonCPAFactory();
    }

}
