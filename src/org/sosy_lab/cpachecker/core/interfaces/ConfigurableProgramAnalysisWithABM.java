package org.sosy_lab.cpachecker.core.interfaces;

public interface ConfigurableProgramAnalysisWithABM extends
    ConfigurableProgramAnalysis {

  Reducer getReducer();
}
