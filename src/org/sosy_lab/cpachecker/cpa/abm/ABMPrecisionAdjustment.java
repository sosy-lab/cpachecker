/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.abm;

import java.util.Map;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ABMPrecisionAdjustment implements PrecisionAdjustment {

  private Map<AbstractElement, Precision> forwardPrecisionToExpandedPrecision;

  public final PrecisionAdjustment wrappedPrecisionAdjustment;

  public ABMPrecisionAdjustment(PrecisionAdjustment pWrappedPrecisionAdjustment) {
    this.wrappedPrecisionAdjustment = pWrappedPrecisionAdjustment;
  }

  void setForwardPrecisionToExpandedPrecision(
      Map<AbstractElement, Precision> pForwardPrecisionToExpandedPrecision) {
    forwardPrecisionToExpandedPrecision = pForwardPrecisionToExpandedPrecision;
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement, Precision pPrecision, UnmodifiableReachedSet pElements) throws CPAException {
    Triple<AbstractElement, Precision, Action> result = wrappedPrecisionAdjustment.prec(pElement, pPrecision, pElements);

    Precision newPrecision = forwardPrecisionToExpandedPrecision.get(pElement);
    if(newPrecision != null) {
      return Triple.of(result.getFirst(), newPrecision, result.getThird());
    } else {
      return result;
    }
  }

}