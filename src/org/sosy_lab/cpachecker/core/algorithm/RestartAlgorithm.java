package org.sosy_lab.cpachecker.core.algorithm;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.seqcomposite.SeqCompositeCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class RestartAlgorithm implements Algorithm, StatisticsProvider {

  private final Algorithm algorithm;
  
  public RestartAlgorithm(Algorithm algorithm, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
  }
  
  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException,
      InterruptedException {

    boolean sound = true;
    
    boolean continueAnalysis;
    do {
      continueAnalysis = false;

      // run algorithm
      sound &= algorithm.run(reached);

      if(!sound){
        // TODO we need to create a new reached set here
        // or modify the reached set
                
        //we switch to the next cpa
        ((SeqCompositeCPA)getCPA()).switchCPA();
        continueAnalysis = true;
      }
      
    } while (continueAnalysis);

    return sound;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO user wrapper statictics
  }
}
