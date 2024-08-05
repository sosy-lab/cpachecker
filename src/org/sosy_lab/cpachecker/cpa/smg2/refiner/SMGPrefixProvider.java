// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAExportOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisDelegatingRefiner;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;

public class SMGPrefixProvider extends GenericPrefixProvider<SMGState> {

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public SMGPrefixProvider(
      ConstraintsSolver pSolver,
      LogManagerWithoutDuplicates pLogger,
      CFA pCfa,
      Configuration config,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    super(
        new SMGStrongestPostOperator(pSolver, pLogger, config, pCfa),
        SMGState.of(
            pCfa.getMachineModel(),
            pLogger,
            new SMGOptions(config),
            pCfa,
            new SMGCPAExpressionEvaluator(
                pCfa.getMachineModel(),
                pLogger,
                SMGCPAExportOptions.getNoExportInstance(),
                new SMGOptions(config),
                null)),
        pLogger,
        pCfa,
        config,
        SMGCPA.class,
        pShutdownNotifier);
  }

  public static SMGPrefixProvider create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    Preconditions.checkArgument(pCpa instanceof SMGCPA);
    @NonNull SMGCPA smgCpa =
        CPAs.retrieveCPAOrFail(pCpa, SMGCPA.class, ValueAnalysisDelegatingRefiner.class);
    return new SMGPrefixProvider(
        smgCpa.getSolver(),
        smgCpa.getLogger(),
        smgCpa.getCFA(),
        smgCpa.getConfiguration(),
        smgCpa.getShutdownNotifier());
  }
}
