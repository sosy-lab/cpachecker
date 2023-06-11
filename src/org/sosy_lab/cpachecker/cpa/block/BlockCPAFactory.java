// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

class BlockCPAFactory extends AbstractCPAFactory {

  private CFA cfa;
  private final AnalysisDirection analysisDirection;

  public BlockCPAFactory(AnalysisDirection pAnalysisDirection) {
    analysisDirection = pAnalysisDirection;
  }

  @CanIgnoreReturnValue
  @Override
  public <T> BlockCPAFactory set(T pObject, Class<T> pClass) {
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
    switch (analysisDirection) {
      case FORWARD:
        return BlockCPA.create();
      case BACKWARD:
        return BlockCPABackward.create();
      default:
        throw new AssertionError("AnalysisDirection " + analysisDirection + "does not exist");
    }
  }
}
