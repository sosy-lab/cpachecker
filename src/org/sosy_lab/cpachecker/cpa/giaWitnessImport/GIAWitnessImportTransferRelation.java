// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giaWitnessImport;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class GIAWitnessImportTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  public GIAWitnessImportTransferRelation(LogManager pLogger) {
    this.logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    assert state instanceof GIAWitnessImportState;
    return Collections.singleton(((GIAWitnessImportState) state).cleanAndCopy());
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    assert state instanceof GIAWitnessImportState;
    GIAWitnessImportState witnessState = (GIAWitnessImportState) state;

    for (AbstractState other : otherStates) {
      if (other instanceof AutomatonState
          && ((AutomatonState) other)
              .getOwningAutomatonName()
              .equals(GIAGenerator.ASSUMPTION_AUTOMATON_NAME)) {
        AutomatonState autoState = (AutomatonState) other;

        if (!ExpressionTrees.isConstant(autoState.getCandidateInvariants())) {
          witnessState =
              new GIAWitnessImportState(
                  autoState.getCandidateInvariants(), autoState, logger);
          return Collections.singleton(witnessState);
        }
      }
    }
    return Collections.singleton(witnessState);
  }
}
