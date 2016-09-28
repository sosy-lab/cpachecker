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
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGPrecision.SMGPrecisionAbstractionOptions;

import java.util.logging.Level;

public class SMGAnalysisRefiner {

  private final LogManager logger;
  private final SMGPrecision originalPrecision;

  public SMGAnalysisRefiner(SMGPrecision pOriginalPrecision, LogManager pLogger) {
    logger = pLogger;
    originalPrecision = pOriginalPrecision;
  }

  public SMGAnalysisRefinerResult refineAnalysis() {

    if (originalPrecision.forgetDeadVariables() || originalPrecision.useSMGMerge()
        || originalPrecision.useHeapAbstraction()) {

      SMGPrecisionAbstractionOptions options = disableNextFeature();
      SMGPrecision refinedPrecision = originalPrecision.refineOptions(options);
      return new SMGAnalysisRefinerResult(true, refinedPrecision);
    } else {
      return new SMGAnalysisRefinerResult(false, originalPrecision);
    }
  }

  private SMGPrecisionAbstractionOptions disableNextFeature() {

    if (originalPrecision.useSMGMerge()) {
      logger.log(Level.INFO, "Disable merge.");
      return new SMGPrecisionAbstractionOptions(
        originalPrecision.useHeapAbstraction(),
        originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
        originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(), false);
    }

    if (originalPrecision.forgetDeadVariables()) {
      logger.log(Level.INFO, "Disable live variable Analysis.");
      return new SMGPrecisionAbstractionOptions(
        originalPrecision.useHeapAbstraction(),
        originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
        false, originalPrecision.useInterpoaltion(), originalPrecision.useSMGMerge());
    }

    if (originalPrecision.useHeapAbstraction()) {
      logger.log(Level.INFO, "Disable heap abstraction.");
      return new SMGPrecisionAbstractionOptions(false,
        originalPrecision.useFieldAbstraction(), originalPrecision.useStackAbstraction(),
        originalPrecision.forgetDeadVariables(), originalPrecision.useInterpoaltion(),
        originalPrecision.useSMGMerge());
    }

    return new SMGPrecisionAbstractionOptions(false, false, false, false, false, false);
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