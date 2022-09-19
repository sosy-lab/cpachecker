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
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

@Options(prefix = "cpa.value.interpolation")
public class ValueAnalysisEdgeInterpolator
    extends GenericEdgeInterpolator<
        ValueAnalysisState, ValueAnalysisInformation, ValueAnalysisInterpolant> {

  /** This method acts as the constructor of the class. */
  public ValueAnalysisEdgeInterpolator(
      final FeasibilityChecker<ValueAnalysisState> pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(
        pStrongestPostOperator,
        pFeasibilityChecker,
        ValueAnalysisInterpolantManager.getInstance(),
        new ValueAnalysisState(pCfa.getMachineModel()),
        ValueAnalysisCPA.class,
        pConfig,
        pShutdownNotifier,
        pCfa);
  }
}
