// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.collect.Collections2;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.defaults.AbstractWrapperStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** This stop operator handles the {@link MissingBlockAbstractionState} that is never covered. */
public class BAMStopOperatorWithBreakOnMissingBlock extends AbstractWrapperStopOperator {

  public BAMStopOperatorWithBreakOnMissingBlock(StopOperator pWrappedStopOperator) {
    super(pWrappedStopOperator);
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (pState instanceof MissingBlockAbstractionState) {
      return false;
    }
    Collection<AbstractState> filteredReached =
        Collections2.filter(pReached, s -> !(s instanceof MissingBlockAbstractionState));
    return super.stop(pState, filteredReached, pPrecision);
  }
}
