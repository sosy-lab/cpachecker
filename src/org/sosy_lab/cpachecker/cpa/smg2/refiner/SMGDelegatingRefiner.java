// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateBasedPrefixProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.DelegatingARGBasedRefinerWithRefinementSelection;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;

/**
 * Refiner implementation that delegates to a value refiner and if this fails, optionally delegates
 * also to a predicate refiner. Also supports Refinement Selection and can delegate to the refiner
 * which has prefixes with the better score.
 */
public abstract class SMGDelegatingRefiner implements Refiner {

  @SuppressWarnings("resource")
  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    SMGCPA valueCpa = CPAs.retrieveCPAOrFail(cpa, SMGCPA.class, SMGDelegatingRefiner.class);
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, SMGDelegatingRefiner.class);

    Configuration config = valueCpa.getConfiguration();
    LogManager logger = valueCpa.getLogger();
    CFA cfa = valueCpa.getCFA();

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new DelegatingARGBasedRefinerWithRefinementSelection(
            config,
            new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure(), logger),
            SMGRefiner.create0(cpa),
            new ValueAnalysisPrefixProvider(logger, cfa, config, valueCpa.getShutdownNotifier()),
            PredicateRefiner.create0(cpa),
            new PredicateBasedPrefixProvider(
                config, logger, predicateCpa.getSolver(), predicateCpa.getShutdownNotifier())),
        cpa);
  }
}
