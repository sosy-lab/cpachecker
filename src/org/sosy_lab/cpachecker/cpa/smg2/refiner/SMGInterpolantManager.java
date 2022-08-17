// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** InterpolantManager for interpolants of {@link SMGState}. */
public class SMGInterpolantManager implements InterpolantManager<SMGState, SMGInterpolant> {

  private final SMGOptions options;
  private final MachineModel machineModel;
  private final LogManager logger;

  private SMGInterpolantManager(
      SMGOptions pOptions, MachineModel pMachineModel, LogManager pLogger) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
  }

  public static SMGInterpolantManager getInstance(
      SMGOptions pOptions, MachineModel pMachineModel, LogManager pLogger) {
    return new SMGInterpolantManager(pOptions, pMachineModel, pLogger);
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return SMGInterpolant.createInitial(options, machineModel, logger);
  }

  @Override
  public SMGInterpolant createInterpolant(SMGState state) {
    return state.createInterpolant();
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    return SMGInterpolant.createTRUE(options, machineModel, logger);
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.createFALSE(options, machineModel, logger);
  }
}
