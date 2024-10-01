

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;



import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;


/**
 * A factory class for creating instances of TubeCPA.
 */
class TubeCPAFactory extends AbstractCPAFactory {

  /**
   * This variable represents a Control Flow Automaton (CFA).
   */

  private final AnalysisDirection analysisDirection;

  private CFA cfa;


  public TubeCPAFactory(AnalysisDirection direction) {
    this.analysisDirection = direction;
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
  public <T> TubeCPAFactory set(T pObject, Class<T> pClass) {
    if (CFA.class.isAssignableFrom(pClass)) {
      cfa = (CFA) pObject;
    } else {
      super.set(pObject, pClass);
    }
    return this;
  }

  public static TubeCPAFactory factory() {
    return new TubeCPAFactory(AnalysisDirection.FORWARD);
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
    return switch (analysisDirection){
      case FORWARD ->  TubeCPA.create(getConfiguration(), getLogger(), getShutdownNotifier(), cfa);
      case BACKWARD -> null;
      default ->
          throw new AssertionError("AnalysisDirection " + analysisDirection + "does not exist");
    };
  }
}
