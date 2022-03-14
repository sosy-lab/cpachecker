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
import org.sosy_lab.cpachecker.cpa.bam.BAMBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.DelegatingARGBasedRefiner;

/**
 * This class allows to create a delegating BAM-refiner for a combination of value analysis and
 * predicate analysis (in this order!).
 */
public abstract class ValueAnalysisDelegatingBAMRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    ValueAnalysisCPA valueCpa =
        CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, ValueAnalysisDelegatingRefiner.class);
    LogManager logger = valueCpa.getLogger();

    // first value analysis refiner, then predicate analysis refiner
    return BAMBasedRefiner.forARGBasedRefiner(
        new DelegatingARGBasedRefiner(
            logger, ValueAnalysisRefiner.create0(cpa), BAMPredicateRefiner.create0(cpa)),
        cpa);
  }
}
