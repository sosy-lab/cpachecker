package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public interface InvertableCPA<T extends ConfigurableProgramAnalysis> {
    public T invert() throws CPATransferException, InvalidConfigurationException, InterruptedException;
}
