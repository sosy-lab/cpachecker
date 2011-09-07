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
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Standard stop-join operator that first joins all elements
 * of the reached set into a single element, and then checks the
 * partial order relation.
 */
public class StopJoinOperator implements StopOperator {

 private final AbstractDomain domain;

  public StopJoinOperator(AbstractDomain domain) {
    this.domain = domain;
  }

  @Override
  public boolean stop(AbstractElement element, Collection<AbstractElement> reached,
                      Precision precision) throws CPAException {
    Iterator<AbstractElement> it = reached.iterator();
    AbstractElement joinedElement = it.next();
    while (it.hasNext()) {
      joinedElement = domain.join(it.next(), joinedElement);
    }

    return domain.isLessOrEqual(element, joinedElement);
  }
}
