// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class ExtendedWitnessExporter extends WitnessExporter {
  public ExtendedWitnessExporter(
      Configuration pConfig, LogManager pLogger, Specification pSpecification, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pSpecification, pCFA);
  }

  @Override
  public Witness generateErrorWitness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      CounterexampleInfo pCounterExample) {

    String defaultFileName = getInitialFileName(pRootState);
    WitnessFactory writer =
        new ExtendedWitnessFactory(
            options,
            cfa,
            logger,
            verificationTaskMetaData,
            factory,
            simplifier,
            defaultFileName,
            WitnessType.VIOLATION_WITNESS,
            InvariantProvider.TrueInvariantProvider.INSTANCE);
    return writer.produceWitness(
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Predicates.alwaysFalse(),
        Optional.empty(),
        Optional.of(pCounterExample),
        GraphBuilder.ARG_PATH);
  }
}
