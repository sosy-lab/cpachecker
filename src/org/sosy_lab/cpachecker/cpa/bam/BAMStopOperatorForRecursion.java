/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;


/** This stop-operator just forwards towards the wrapped stop-operator of the analysis.
 * Additionally, we never 'stop' at a function-call, because of the influence
 * of the predecessor of the function-call in the 'rebuild'-step. */
public class BAMStopOperatorForRecursion extends BAMStopOperator {

  public BAMStopOperatorForRecursion(StopOperator pWrappedStopOperator,
      BAMTransferRelation pTransfer) {
    super(pWrappedStopOperator, pTransfer);
  }

  @Override
  public boolean stop(AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
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
