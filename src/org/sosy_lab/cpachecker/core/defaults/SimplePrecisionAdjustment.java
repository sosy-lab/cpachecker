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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Base implementation for precision adjustment implementations which fulfill
 * these three requirements:
 * - prec does not change the state
 * - prec does not change the precision
 * - prec does not need access to the reached set
 *
 * By inheriting from this class, implementations give callers the opportunity
 * to directly call {@link #prec(AbstractState, Precision)}, which is faster.
 */
public abstract class SimplePrecisionAdjustment implements PrecisionAdjustment {

  @Override
  public PrecisionAdjustmentResult prec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates) throws CPAException {

    Action action = prec(pState, pPrecision);

    return PrecisionAdjustmentResult.create(pState, pPrecision, action);
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
