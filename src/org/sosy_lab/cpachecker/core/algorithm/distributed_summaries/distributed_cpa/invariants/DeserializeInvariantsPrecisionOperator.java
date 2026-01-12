// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants.SerializeDataflowAnalysisStateOperator.STRATEGY;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.AbstractionStrategyFactories;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsPrecision;

public class DeserializeInvariantsPrecisionOperator implements DeserializePrecisionOperator {

  private final InvariantsCPA invariantsCPA;
  private final CFA cfa;

  public DeserializeInvariantsPrecisionOperator(InvariantsCPA pInvariantsCPA, CFA pCFA) {
    invariantsCPA = pInvariantsCPA;
    cfa = pCFA;
  }

  @Override
  public Precision deserializePrecision(DssMessage pMessage) {
    ContentReader pContentReader = pMessage.getPrecisionContent(InvariantsPrecision.class);
    String strategyString = pContentReader.get(STRATEGY);
    return InvariantsPrecision.getEmptyPrecision(
        AbstractionStrategyFactories.valueOf(strategyString)
            .createStrategy(
                invariantsCPA.getCompoundIntervalFormulaManagerFactory(), cfa.getMachineModel()));
  }
}
