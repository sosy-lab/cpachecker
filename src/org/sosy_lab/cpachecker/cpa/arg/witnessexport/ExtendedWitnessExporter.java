/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.IOException;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;

public class ExtendedWitnessExporter extends WitnessExporter {
  public ExtendedWitnessExporter(
      Configuration pConfig, LogManager pLogger, Specification pSpecification, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pSpecification, pCFA);
  }

  @Override
  public void writeErrorWitness(
      Appendable pTarget,
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      CounterexampleInfo pCounterExample)
      throws IOException {

    String defaultFileName = getInitialFileName(pRootState);
    WitnessWriter writer =
        new ExtendedWitnessWriter(
            options,
            cfa,
            verificationTaskMetaData,
            factory,
            simplifier,
            defaultFileName,
            WitnessType.VIOLATION_WITNESS,
            InvariantProvider.TrueInvariantProvider.INSTANCE);
    writer.writePath(
        pTarget,
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        Predicates.alwaysFalse(),
        Optional.empty(),
        Optional.of(pCounterExample),
        GraphBuilder.ARG_PATH);
  }
}
