// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.smg2.SMGInformation;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

@Options(prefix = "cpa.smg2.interpolation")
public class SMGEdgeInterpolator
    extends GenericEdgeInterpolator<SMGState, SMGInformation, SMGInterpolant> {

  /** This method acts as the constructor of the class. */
  public SMGEdgeInterpolator(
      final FeasibilityChecker<SMGState> pFeasibilityChecker,
      final StrongestPostOperator<SMGState> pStrongestPostOperator,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final LogManager pLogger)
      throws InvalidConfigurationException {

    super(
        pStrongestPostOperator,
        pFeasibilityChecker,
        SMGInterpolantManager.getInstance(),
        SMGState.of(pCfa.getMachineModel(), pLogger, new SMGOptions(pConfig)),
        ValueAnalysisCPA.class,
        pConfig,
        pShutdownNotifier,
        pCfa);
  }
}
