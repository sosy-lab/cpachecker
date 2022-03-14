// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Base implementation for precision adjustment implementations which fulfill these three
 * requirements: - prec does not change the state - prec does not change the precision - prec does
 * not need access to the reached set
 *
 * <p>By inheriting from this class, implementations give callers the opportunity to directly call
 * {@link #prec(AbstractState, Precision)}, which is faster.
 */
public abstract class SimplePrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException {

    Action action = prec(pState, pPrecision);

    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, action));
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
