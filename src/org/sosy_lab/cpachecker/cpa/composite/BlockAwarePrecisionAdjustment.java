// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action.BREAK;
import static org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action.CONTINUE;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockAwarePrecisionAdjustment implements PrecisionAdjustment {
  private final PrecisionAdjustment defaultAdjustment;

  private final Block block;

  private final AnalysisDirection direction;

  BlockAwarePrecisionAdjustment(
      final PrecisionAdjustment pDefaultAdjustment,
      final Block pBlock,
      final AnalysisDirection pDirection) {
    defaultAdjustment = pDefaultAdjustment;
    block = pBlock;
    direction = pDirection;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    assert state instanceof BlockAwareARGState;

    Optional<PrecisionAdjustmentResult> result =
        defaultAdjustment.prec(state, precision, states, stateProjection, fullState);

    if (result.isPresent()) {
      PrecisionAdjustmentResult adjustment = result.get();

      assert adjustment.abstractState() instanceof CompositeState;
      ARGState adjustedState = (ARGState) adjustment.abstractState();

      BlockAwareARGState newState =
          BlockAwareARGState.create(adjustedState, block, direction);
      
      Action action = CONTINUE;
      if(adjustedState.isTarget()) {
        action = BREAK;  
      }
      return Optional.of(adjustment.withAbstractState(newState).withAction(action));
    }

    return Optional.empty();
  }
}
