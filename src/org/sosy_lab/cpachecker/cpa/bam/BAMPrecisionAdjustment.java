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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Map;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMPrecisionAdjustment implements PrecisionAdjustment {

  private Map<AbstractState, Precision> forwardPrecisionToExpandedPrecision;
  private final PrecisionAdjustment wrappedPrecisionAdjustment;
  private final BAMTransferRelation trans;

  public BAMPrecisionAdjustment(PrecisionAdjustment pWrappedPrecisionAdjustment, BAMTransferRelation pTransfer) {
    this.wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
    this.trans = pTransfer;
  }

  void setForwardPrecisionToExpandedPrecision(
      Map<AbstractState, Precision> pForwardPrecisionToExpandedPrecision) {
    forwardPrecisionToExpandedPrecision = pForwardPrecisionToExpandedPrecision;
  }

  @Override
  public PrecisionAdjustmentResult prec(AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) throws CPAException, InterruptedException {
    if (trans.breakAnalysis) {
      return PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK);
    }

    PrecisionAdjustmentResult result = wrappedPrecisionAdjustment.prec(pElement, pPrecision, pElements);

    result = result.withAbstractState(trans.attachAdditionalInfoToCallNode(result.abstractState()));

    Precision newPrecision = forwardPrecisionToExpandedPrecision.get(pElement);
    if (newPrecision != null) {
      return result.withPrecision(newPrecision);
    } else {
      return result;
    }
  }
}
