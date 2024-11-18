// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** InterpolantManager for interpolants of {@link SMGState}. */
public class SMGInterpolantManager implements InterpolantManager<SMGState, SMGInterpolant> {

  private final SMGOptions options;
  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  /** The cfa of this analysis. Only needed for its entry function. */
  private final CFA cfa;

  /** Remember if we need to take memsafety into account for interpolants. * */
  private final boolean isRefineMemorySafety;

  private final SMGCPAExpressionEvaluator evaluator;

  private final SMGCPAStatistics statistics;

  private SMGInterpolantManager(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFA pCfa,
      boolean pIsRefineMemorySafety,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    options = pOptions;
    machineModel = pMachineModel;
    logger = pLogger;
    cfa = pCfa;
    isRefineMemorySafety = pIsRefineMemorySafety;
    evaluator = pEvaluator;
    statistics = pStatistics;
  }

  public static SMGInterpolantManager getInstance(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFA pCfa,
      boolean pIsRefineMemorySafety,
      SMGCPAExpressionEvaluator pEvaluator,
      SMGCPAStatistics pStatistics) {
    return new SMGInterpolantManager(
        pOptions, pMachineModel, pLogger, pCfa, pIsRefineMemorySafety, pEvaluator, pStatistics);
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return SMGInterpolant.createInitial(
        options,
        machineModel,
        logger,
        (CFunctionEntryNode) cfa.getMainFunction(),
        evaluator,
        statistics);
  }

  @Override
  public SMGInterpolant createInterpolant(SMGState state) {
    return state.createInterpolant(isRefineMemorySafety);
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    return SMGInterpolant.createTRUE(
        options,
        machineModel,
        logger,
        (CFunctionEntryNode) cfa.getMainFunction(),
        evaluator,
        statistics);
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.createFALSE(
        options,
        machineModel,
        logger,
        (CFunctionDeclaration) cfa.getMainFunction().getFunctionDefinition(),
        statistics);
  }
}
