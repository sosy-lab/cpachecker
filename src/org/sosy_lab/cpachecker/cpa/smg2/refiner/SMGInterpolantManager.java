// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** InterpolantManager for interpolants of {@link SMGState}. */
public class SMGInterpolantManager implements InterpolantManager<SMGState, SMGInterpolant> {

  private final SMGOptions options;
  private final MachineModel machineModel;
  private final LogManager logger;

  /** The cfa of this analysis. Only needed for its entry function. * */
  private final CFA cfa;

  private SMGInterpolantManager(
      SMGOptions pOptions, MachineModel pMachineModel, LogManager pLogger, CFA pCfa) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
    cfa = pCfa;
  }

  public static SMGInterpolantManager getInstance(
      SMGOptions pOptions, MachineModel pMachineModel, LogManager pLogger, CFA pCfa) {
    return new SMGInterpolantManager(pOptions, pMachineModel, pLogger, pCfa);
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return SMGInterpolant.createInitial(
        options, machineModel, logger, (CFunctionEntryNode) cfa.getMainFunction());
  }

  @Override
  public SMGInterpolant createInterpolant(SMGState state) {
    return state.createInterpolant();
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    return SMGInterpolant.createTRUE(
        options, machineModel, logger, (CFunctionEntryNode) cfa.getMainFunction());
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.createFALSE(
        options,
        machineModel,
        logger,
        (CFunctionDeclaration) cfa.getMainFunction().getFunctionDefinition());
  }
}
