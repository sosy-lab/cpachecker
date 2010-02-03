package cpa.common.interfaces;

import java.util.Collection;


public interface CPAWithStatistics extends ConfigurableProgramAnalysis {

  public void collectStatistics(Collection<CPAStatistics> statsCollection);
  
}
