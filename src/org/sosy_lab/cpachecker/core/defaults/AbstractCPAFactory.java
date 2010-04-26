package org.sosy_lab.cpachecker.core.defaults;

import java.util.List;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.core.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

public abstract class AbstractCPAFactory implements CPAFactory {

  private LogManager logger = null;
  private Configuration configuration = null;
  
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
  public CPAFactory setConfiguration(Configuration pConfiguration) {
    Preconditions.checkNotNull(pConfiguration);
    Preconditions.checkState(configuration == null, "setConfiguration called twice on CPAFactory");
    
    configuration = pConfiguration;
    return this;
  }

  @Override
  public CPAFactory setLogger(LogManager pLogger) {
    Preconditions.checkNotNull(pLogger);
    Preconditions.checkState(logger == null, "setConfiguration called twice on CPAFactory");
    
    logger = pLogger;
    return this;
  }
  
  protected LogManager getLogger() {
    Preconditions.checkState(logger != null, "LogManager object needed to create CPA");
    return logger;
  }
  
  protected Configuration getConfiguration() {
    Preconditions.checkState(configuration != null, "Configuration object needed to create CPA");
    return configuration;
  }
}
