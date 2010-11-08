/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.assumptions.collector;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/**
 * Stop operator for the assumption collector CPA. Stops if the stop flag is
 * true.
 *
 * @author g.theoduloz
 */
public class AssumptionCollectorStop implements StopOperator {

  @Override
  public boolean stop(AbstractElement element, Collection<AbstractElement> reached, Precision precision) {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;

    // if stop, then do not stop to make sure the state is
    // added to the reached set
    return !assumptionElement.isStop();
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement) {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;

    // if stop, then do not stop to make sure the state is
    // added to the reached set
    return !assumptionElement.isStop();
  }
}