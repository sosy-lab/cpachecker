package cpa.common.interfaces;

import java.util.Collection;

import cpaplugin.CPAStatistics;

public interface CPAWithStatistics extends ConfigurableProgramAnalysis {

  public void collectStatistics(Collection<CPAStatistics> statsCollection);
  
}
