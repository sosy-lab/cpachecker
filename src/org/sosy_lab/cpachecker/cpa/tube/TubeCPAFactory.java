

package org.sosy_lab.cpachecker.cpa.tube;



import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * A factory class for creating instances of TubeCPA.
 */
class TubeCPAFactory extends AbstractCPAFactory {

  /**
   * This variable represents a Control Flow Automaton (CFA).
   */
  private CFA cfa;
  /**
   * Represents the analysis direction for a configurable program analysis (CPA).
   *
   * <p>The analysis direction can either be FORWARD or BACKWARD.
   */
  private final AnalysisDirection analysisDirection;




  public TubeCPAFactory(AnalysisDirection pAnalysisDirection) {
    analysisDirection = pAnalysisDirection;
  }

  /**
   * Sets the specified object of the given class.
   *
   * @param pObject the object to set
   * @param pClass the class of the object
   * @param <T> the type of the object
   * @return the TubeCPAFactory instance
   */
  @CanIgnoreReturnValue
  @Override
  public <T> org.sosy_lab.cpachecker.cpa.tube.TubeCPAFactory set(T pObject, Class<T> pClass) {
    if (CFA.class.isAssignableFrom(pClass)) {
      cfa = (CFA) pObject;
    } else {
      super.set(pObject, pClass);
    }
    return this;
  }

  /**
   * Creates an instance of ConfigurableProgramAnalysis using TubeCPA implementation.
   *
   * @return an instance of ConfigurableProgramAnalysis
   * @throws InvalidConfigurationException if the configuration is invalid
   */
  @Override
  public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
    checkNotNull(cfa, "CFA instance needed to create LocationCPA");
    return TubeCPA.create(getConfiguration(), getLogger(), getShutdownNotifier(), cfa);
  }
}