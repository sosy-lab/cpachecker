// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BreakOnBlockEntryPrecisionAdjustment implements PrecisionAdjustment {
  private final PrecisionAdjustment defaultAdjustment;

  private final CFANode blockEntry;

  public BreakOnBlockEntryPrecisionAdjustment(
      PrecisionAdjustment pPrecisionAdjustment, Block pTarget) {
    defaultAdjustment = pPrecisionAdjustment;
    blockEntry = pTarget.getEntry();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    Optional<PrecisionAdjustmentResult> result =
        defaultAdjustment.prec(state, precision, states, stateProjection, fullState);

    CFANode location = extractLocation(state);
    if(location == blockEntry) {
      if(result.isPresent()) {
        return Optional.of(result.get().withAction(Action.BREAK));
      } else {
        return Optional.of(PrecisionAdjustmentResult.create(state, precision, Action.BREAK));
      }
    }
    
    return result;
  }
}
