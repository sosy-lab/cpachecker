/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
 * This class allows to create a delegating BAM-refiner
 * for a combination of value analysis and predicate analysis (in this order!).
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
            logger,
            ValueAnalysisRefiner.create(cpa).asARGBasedRefiner(),
            BAMPredicateRefiner.create0(cpa)),
        cpa);
  }
}
