// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.delegation;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * {@link Refiner} for {@link ValueAnalysisCPA} using symbolic values and {@link ConstraintsCPA}
 * that extracts a precision for both CPAs from the precision created by {@link PredicateRefiner}.
 */
public abstract class PredicateExtractionRefiner implements Refiner {

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, PredicateExtractionRefiner.class);
    final ConstraintsCPA constraintsCpa =
        CPAs.retrieveCPAOrFail(pCpa, ConstraintsCPA.class, PredicateExtractionRefiner.class);
    final PredicateCPA predicateCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateExtractionRefiner.class);

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();

    RefinementStrategy strategy =
        new SymbolicPrecisionRefinementStrategy(
            config, logger, predicateCPA.getPredicateManager(), predicateCPA.getSolver());

    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new PredicateCPARefinerFactory(pCpa).forbidStaticRefinements().create(strategy), pCpa);
  }
}
