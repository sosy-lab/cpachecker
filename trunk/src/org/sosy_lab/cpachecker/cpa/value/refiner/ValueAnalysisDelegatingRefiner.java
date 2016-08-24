/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateBasedPrefixProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.DelegatingARGBasedRefinerWithRefinementSelection;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;

/**
 * Refiner implementation that delegates to a value refiner
 * and if this fails, optionally delegates also to a predicate refiner.
 * Also supports Refinement Selection and can delegate to the refiner
 * which has prefixes with the better score.
 */
public abstract class ValueAnalysisDelegatingRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa) throws InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    ValueAnalysisCPA valueCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Configuration config      = valueCpa.getConfiguration();
    LogManager logger         = valueCpa.getLogger();
    CFA cfa                   = valueCpa.getCFA();

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new DelegatingARGBasedRefinerWithRefinementSelection(
            config,
            new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure()),
            ValueAnalysisRefiner.create(cpa).asARGBasedRefiner(),
            new ValueAnalysisPrefixProvider(logger, cfa, config),
            PredicateRefiner.create0(cpa),
            new PredicateBasedPrefixProvider(
                config, logger, predicateCpa.getSolver(), predicateCpa.getPathFormulaManager(), predicateCpa.getShutdownNotifier())),
        cpa);
  }
}

