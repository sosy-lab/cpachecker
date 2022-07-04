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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryBasedRefiner;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryStrategyRefiner;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class SummaryValueRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    LogManager logger;
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SummaryValueRefiner.class);
    ValueAnalysisCPA valueCPA =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, SummaryValueRefiner.class);
    logger = argCpa.getLogger();

    return new SummaryBasedRefiner(
        new SummaryStrategyRefiner(logger, pCpa, argCpa.getCfa()),
        logger,
        valueCPA.getShutdownNotifier(),
        pCpa,
        valueCPA.getConfiguration());
  }
}
