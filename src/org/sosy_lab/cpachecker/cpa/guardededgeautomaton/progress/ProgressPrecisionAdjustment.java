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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.progress;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ProgressPrecisionAdjustment implements PrecisionAdjustment {

  /*
   * (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet)
   *
   * This method does not depend on pElements.
   */
  @Override
  public Triple<AbstractElement, Precision, Action> prec(
      AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) throws CPAException {
    ProgressElement lElement = (ProgressElement)pElement;
    ProgressPrecision lPrecision = (ProgressPrecision)pPrecision;

    if (lPrecision.isProgress(lElement.getTransition())) {
      GuardedEdgeAutomatonElement lWrappedElement = lElement.getWrappedElement();

      if (!(lWrappedElement instanceof GuardedEdgeAutomatonStateElement)) {
        throw new RuntimeException();
      }

      GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lWrappedElement;

      Precision lAdjustedPrecision = lPrecision.remove(lElement.getTransition());

      return new Triple<AbstractElement, Precision, Action>(new AlternationElement(lStateElement), lAdjustedPrecision, Action.BREAK);
    }
    else {
      return new Triple<AbstractElement, Precision, Action>(lElement.getWrappedElement(), pPrecision, Action.CONTINUE);
    }
  }

}
