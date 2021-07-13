// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.loopsummary.LoopSummaryBasedRefiner;
import org.sosy_lab.cpachecker.cpa.loopsummary.LoopSummaryCPA;
import org.sosy_lab.cpachecker.cpa.loopsummary.LoopSummaryStrategyRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class ValueAnalysisLoopSummaryRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    LogManager logger;
    if (pCpa instanceof LoopSummaryCPA) {
      logger = ((LoopSummaryCPA) pCpa).getLogger();
    } else {
      logger = null;
    }
    ValueAnalysisCPA valueCPA =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, ValueAnalysisLoopSummaryRefiner.class);
    return new LoopSummaryBasedRefiner(
        AbstractARGBasedRefiner.forARGBasedRefiner(ValueAnalysisRefiner.create0(pCpa), pCpa),
        new LoopSummaryStrategyRefiner(logger, pCpa),
        logger,
        pCpa,
        valueCPA.getConfiguration());
  }

}
