package cpa.common.defaults;

import java.util.List;

import com.google.common.base.Preconditions;

import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;

public abstract class AbstractCPAFactory implements CPAFactory {

  private LogManager logger = null;
  private CPAConfiguration configuration = null;
  
  @Override
  public CPAFactory setChild(ConfigurableProgramAnalysis pChild)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPA");
  }

  @Override
  public CPAFactory setChildren(List<ConfigurableProgramAnalysis> pChildren)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Cannot wrap CPAs");
  }

  @Override
  public CPAFactory setConfiguration(CPAConfiguration pConfiguration) {
    Preconditions.checkNotNull(pConfiguration);
    Preconditions.checkState(configuration == null, "setConfiguration called twice von CPAFactory");
    
    configuration = pConfiguration;
    return this;
  }

  @Override
  public CPAFactory setLogger(LogManager pLogger) {
    Preconditions.checkNotNull(pLogger);
    Preconditions.checkState(logger == null, "setConfiguration called twice von CPAFactory");
    
    logger = pLogger;
    return this;
  }
  
  protected LogManager getLogger() {
    Preconditions.checkState(logger != null, "LogManager object needed to create CPA");
    return logger;
  }
  
  protected CPAConfiguration getConfiguration() {
    Preconditions.checkState(configuration != null, "Configuration object needed to create CPA");
    return configuration;
  }
}
