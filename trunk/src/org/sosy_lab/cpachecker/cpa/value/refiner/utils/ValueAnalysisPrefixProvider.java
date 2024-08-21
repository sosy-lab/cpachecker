// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisDelegatingRefiner;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;

public class ValueAnalysisPrefixProvider extends GenericPrefixProvider<ValueAnalysisState> {

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public ValueAnalysisPrefixProvider(
      LogManager pLogger, CFA pCfa, Configuration config, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    super(
        new ValueAnalysisStrongestPostOperator(pLogger, config, pCfa),
        new ValueAnalysisState(pCfa.getMachineModel()),
        pLogger,
        pCfa,
        config,
        ValueAnalysisCPA.class,
        pShutdownNotifier);
  }

  public static ValueAnalysisPrefixProvider create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ValueAnalysisCPA valueCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, ValueAnalysisDelegatingRefiner.class);
    return new ValueAnalysisPrefixProvider(
        valueCpa.getLogger(),
        valueCpa.getCFA(),
        valueCpa.getConfiguration(),
        valueCpa.getShutdownNotifier());
  }
}
