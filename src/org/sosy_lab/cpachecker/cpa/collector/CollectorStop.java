/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.collector;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CollectorStop implements StopOperator {

  private final StopOperator delegateStop;
  private final LogManager logger;

  public CollectorStop(final StopOperator pDelegateStop, LogManager pLogger) {
    delegateStop = pDelegateStop;
    logger = pLogger;
  }

  @Override
  public boolean stop(
      AbstractState state, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {

    assert state instanceof CollectorState;

    ARGState wrappedState = (ARGState) ((CollectorState)state).getWrappedState();
    //logger.log(Level.INFO, "sonja StopwrappedState:\n" + wrappedState);


    //logger.log(Level.INFO, "sonja Stop REACHED:\n" + reached);

    Collection<? extends AbstractState> stopcollection;
      stopcollection = reached;

      Collection<AbstractState> wrappedstop = new ArrayList<>();
      for (AbstractState absElement : stopcollection) {
        ARGState stopElem = new ARGState(absElement, null) {
        };
        wrappedstop.add(stopElem);
      }

    //logger.log(Level.INFO, "sonja Stop REACHEDstoparg:\n" + wrappedstop);

    boolean stop = delegateStop.stop(wrappedState, wrappedstop, precision);

    return stop;
  }
}
