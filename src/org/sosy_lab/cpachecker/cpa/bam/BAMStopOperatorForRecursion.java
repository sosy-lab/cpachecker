// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractWrapperStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This stop-operator just forwards towards the wrapped stop-operator of the analysis. Additionally,
 * we never 'stop' at a function-call, because of the influence of the predecessor of the
 * function-call in the 'rebuild'-step.
 */
public class BAMStopOperatorForRecursion extends AbstractWrapperStopOperator {

  public BAMStopOperatorForRecursion(StopOperator pWrappedStopOperator) {
    super(pWrappedStopOperator);
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    // we never 'stop' at a function-call, because of the influence
    // of the predecessor of the function-call in the 'rebuild'-step.
    // example that might cause problems: BallRajamani-SPIN2000-Fig1_false-unreach-call.c
    if (AbstractStates.extractLocation(pState) instanceof FunctionEntryNode) {
      return false;
    }
    return super.stop(pState, pReached, pPrecision);
  }
}
