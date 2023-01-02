// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

public class SingletonCPAFactory extends AbstractCPAFactory {

  private final ConfigurableProgramAnalysis instance;

  public SingletonCPAFactory(ConfigurableProgramAnalysis pInstance) {
    instance = checkNotNull(pInstance);
  }

  public static SingletonCPAFactory forInstance(ConfigurableProgramAnalysis pInstance) {
    return new SingletonCPAFactory(pInstance);
  }

  @Override
  public ConfigurableProgramAnalysis createInstance() {
    return instance;
  }
}
