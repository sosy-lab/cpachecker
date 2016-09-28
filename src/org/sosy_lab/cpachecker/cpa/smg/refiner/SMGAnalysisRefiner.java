/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision.SMGPrecisionAbstractionOptions;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.logging.Level;

public class SMGAnalysisRefiner {

  private final LogManager logger;
  private final SMGPrecision originalPrecision;
  private final SMGFeasibilityChecker checker;
  private final ARGPath errorPath;

  public SMGAnalysisRefiner(SMGPrecision pOriginalPrecision, LogManager pLogger, ARGPath pErrorPath, SMGFeasibilityChecker pChecker) {
    logger = pLogger;
    originalPrecision = pOriginalPrecision;
    checker = pChecker;
    errorPath = pErrorPath;
  }

  public SMGAnalysisRefinerResult refineAnalysis() throws CPAException, InterruptedException {

    if (originalPrecision.forgetDeadVariables() || originalPrecision.useSMGMerge()
        || originalPrecision.useHeapAbstraction()) {

      SMGPrecision refinedPrecision = disableNextFeature();
      return new SMGAnalysisRefinerResult(true, refinedPrecision);
    } else {
      return new SMGAnalysisRefinerResult(false, originalPrecision);
    }
  }

  private SMGPrecision disableNextFeature() throws CPAException, InterruptedException {

    SMGPrecisionAbstractionOptions options = originalPrecision.getAbstractionOptions();
    SMGHeapAbstractionThreshold threshold = originalPrecision.getHeapAbsThreshold();

    boolean noChange = true;

    if (originalPrecision.useHeapAbstraction()
        && heapAbstractionCausesError(threshold)) {

      if (threshold.getIncombarableThreshold() == 2) {
        logger.log(Level.INFO, "Increase heap abstraction threshold.");
        threshold = new SMGHeapAbstractionThreshold(2, 2, 3);
        noChange = false;
      } else {
        logger.log(Level.INFO, "Disable heap abstraction.");
        options = new SMGPrecisionAbstractionOptions(false,
            originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
            originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(),
            originalPrecision.useSMGMerge(), originalPrecision.joinIntegerWhenMerging(), originalPrecision.forgetNonRelevantVariables());
        noChange = false;
      }
    }

    if (originalPrecision.forgetDeadVariables() &&
        originalPrecision.forgetNonRelevantVariables() && liveAnalysisCausesError(true)) {
      logger.log(Level.INFO, "Disable forgetting non relevant variables.");
      options = new SMGPrecisionAbstractionOptions(
          originalPrecision.useHeapAbstraction(),
          originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
          originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(), originalPrecision.useSMGMerge(),
          originalPrecision.joinIntegerWhenMerging(), false);
      noChange = false;
    }

    if (originalPrecision.forgetDeadVariables() && liveAnalysisCausesError(false)) {
      logger.log(Level.INFO, "Disable live variable Analysis.");
      options = new SMGPrecisionAbstractionOptions(
          originalPrecision.useHeapAbstraction(),
          originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
          false, originalPrecision.useInterpoaltion(), originalPrecision.useSMGMerge(),
          originalPrecision.joinIntegerWhenMerging(), false);
      noChange = false;
    }

    if (originalPrecision.useSMGMerge() && originalPrecision.joinIntegerWhenMerging() && noChange) {
      logger.log(Level.INFO, "Don't join explicit values when merging.");
      options = new SMGPrecisionAbstractionOptions(
          originalPrecision.useHeapAbstraction(),
          originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
          originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(), false,
          false, originalPrecision.forgetNonRelevantVariables());
      noChange = false;
    }

    if (originalPrecision.useSMGMerge() && !originalPrecision.joinIntegerWhenMerging() && noChange) {
      logger.log(Level.INFO, "Disable merge.");
      options = new SMGPrecisionAbstractionOptions(
          originalPrecision.useHeapAbstraction(),
          originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
          originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(), false,
          false, originalPrecision.forgetNonRelevantVariables());
      noChange = false;
    }

    if(noChange) {
      logger.log(Level.INFO, "Use strongest precision.");
      options = new SMGPrecisionAbstractionOptions(false, false, false, false, false, false, false, false);
    }

    return originalPrecision.refineOptions(options, threshold);
  }

  private boolean liveAnalysisCausesError(boolean forgetNonRelevantVariables) throws CPAException, InterruptedException {

    SMGPrecision precision =
        SMGPrecision.createStaticPrecision(false, logger, originalPrecision.getBlockOperator(),
            false, true, originalPrecision.getVarClass(), originalPrecision.getLiveVars(), SMGHeapAbstractionThreshold.defaultThreshold(), forgetNonRelevantVariables);
    return checker.isFeasible(errorPath,
        AbstractStates.extractStateByType(errorPath.getFirstState(), SMGState.class), precision,
        true);
  }

  private boolean heapAbstractionCausesError(SMGHeapAbstractionThreshold pThreshold)
      throws CPAException, InterruptedException {

    SMGPrecision precision = SMGPrecision.createStaticPrecision(true, logger,
        originalPrecision.getBlockOperator(), false, false, originalPrecision.getVarClass(),
        originalPrecision.getLiveVars(), pThreshold, false);
    return checker.isFeasible(errorPath,
        AbstractStates.extractStateByType(errorPath.getFirstState(), SMGState.class), precision,
        true);
  }

  public static class SMGAnalysisRefinerResult {

    private final boolean changed;
    private final SMGPrecision precision;

    public SMGAnalysisRefinerResult(boolean pChanged, SMGPrecision pPrecision) {
      super();
      changed = pChanged;
      precision = pPrecision;
    }

    public boolean isChanged() {
      return changed;
    }

    public SMGPrecision getPrecision() {
      return precision;
    }
  }
}