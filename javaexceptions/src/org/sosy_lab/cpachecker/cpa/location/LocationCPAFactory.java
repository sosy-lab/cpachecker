// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

class LocationCPAFactory extends AbstractCPAFactory {

  private final AnalysisDirection locationType;

  private CFA cfa;

  public LocationCPAFactory(AnalysisDirection pLocationType) {
    locationType = pLocationType;
  }

  @CanIgnoreReturnValue
  @Override
  public <T> LocationCPAFactory set(T pObject, Class<T> pClass) {
    if (CFA.class.isAssignableFrom(pClass)) {
      cfa = (CFA) pObject;
    } else {
      super.set(pObject, pClass);
    }
    return this;
  }

  @Override
  public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
    checkNotNull(cfa, "CFA instance needed to create LocationCPA");

    switch (locationType) {
      case BACKWARD:
        return LocationCPABackwards.create(cfa, getConfiguration());
      case FORWARD:
        return LocationCPA.create(cfa, getConfiguration());
    }
    throw new AssertionError();
  }
}
