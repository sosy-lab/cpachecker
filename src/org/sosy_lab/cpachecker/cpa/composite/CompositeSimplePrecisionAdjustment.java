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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.defaults.BreakOnTargetsPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Alternative to {@link CompositeSimplePrecisionAdjustment} which is faster
 * but can only be used if all child prec operators are either
 * {@link BreakOnTargetsPrecisionAdjustment} or {@link StaticPrecisionAdjustment}.
 *
 * It works by storing a list of indices and then just checks the elements at
 * these indexes if they are targets.
 * It does never call other precision adjustment operators.
 */
class CompositeSimplePrecisionAdjustment extends SimplePrecisionAdjustment {

  private final ImmutableList<SimplePrecisionAdjustment> precisionAdjustments;

  CompositeSimplePrecisionAdjustment(
      ImmutableList<SimplePrecisionAdjustment> precisionAdjustments) {
    this.precisionAdjustments = precisionAdjustments;
  }

  @Override
  public Action prec(AbstractState pElement, Precision pPrecision) throws CPAException {
    CompositeState comp = (CompositeState) pElement;
    CompositePrecision prec = (CompositePrecision) pPrecision;
    assert (comp.getWrappedStates().size() == prec.getWrappedPrecisions().size());
    int dim = comp.getNumberOfStates();

    for (int i = 0; i < dim; ++i) {
      SimplePrecisionAdjustment precisionAdjustment = precisionAdjustments.get(i);
      AbstractState oldElement = comp.get(i);
      Precision oldPrecision = prec.get(i);
      Action action = precisionAdjustment.prec(oldElement, oldPrecision);

      if (action == Action.BREAK) {
        return Action.BREAK;
      }
    }

    return Action.CONTINUE;
  }
}