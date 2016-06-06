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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import java.util.Optional;
import com.google.common.base.Preconditions;

/**
 * Base implementation for precision adjustment implementations wrap other
 * precision adjustment operators.
 */
public abstract class WrappingPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecOp;

  public WrappingPrecisionAdjustment(PrecisionAdjustment pWrappedPrecOp) {
    this.wrappedPrecOp = Preconditions.checkNotNull(pWrappedPrecOp);
  }

  protected abstract Optional<PrecisionAdjustmentResult> wrappingPrec(
      AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState) throws CPAException;

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState) throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> result = wrappedPrecOp.prec(pState, pPrecision, pStates, pProjection, pFullState);

    if (result.isPresent()) {
      if (result.get().action() == Action.BREAK) {
        return result;
      } else {
        return wrappingPrec(result.get().abstractState(), pPrecision, pStates, pProjection, pFullState);
      }
    } else {
      return wrappingPrec(pState, pPrecision, pStates, pProjection, pFullState);
    }
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
