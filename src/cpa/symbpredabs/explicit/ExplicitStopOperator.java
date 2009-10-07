/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs.explicit;

import java.util.Collection;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;


/**
 * Coverage check for explicit-state lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitStopOperator implements StopOperator {

  private final ExplicitAbstractDomain domain;

  public ExplicitStopOperator(ExplicitAbstractDomain d) {
    domain = d;
  }

  public <AE extends AbstractElement> boolean stop(AE element,
                                                   Collection<AE> reached, Precision prec) throws CPAException {
    if (domain.getBottomElement().equals(element)) {
      // stopping here is only correct if reached is not empty.
      // correct means there is an element in reached that
      // covers the bottom element.
      // if this notion of correct is to strict we can skip the
      // assert
      assert(reached.size() > 0);

      return true;
    }
  
    for (AbstractElement e : reached) {
      if (stop(element, e)) {
        return true;
      }
    }
    return false;
  }


  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    if (domain.getBottomElement().equals(element)) {
      return true;
    }

    if (domain.getBottomElement().equals(reachedElement)) {
      return false;
    }

    ExplicitAbstractElement e1 = (ExplicitAbstractElement)element;
    ExplicitAbstractElement e2 = (ExplicitAbstractElement)reachedElement;

    if (!e2.isMarked()) {
      return false;
    }

    if (e1.getLocation().equals(e2.getLocation())) {
      LazyLogger.log(LazyLogger.DEBUG_4,
          "Checking Coverage of element: ", element);

      if (!e1.sameContext(e2)) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "NO, not covered: context differs");
        return false;
      }

      ExplicitCPA cpa = domain.getCPA();
      ExplicitAbstractFormulaManager amgr =
        cpa.getAbstractFormulaManager();

      assert(e1.getAbstraction() != null);
      assert(e2.getAbstraction() != null);

      boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());

      if (ok) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "Element: ", element, " COVERED by: ", e2);
        cpa.setCovered(e1);
        e1.setCovered(true);
      } else {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "NO, not covered");
      }

      return ok;
    } else {
      return false;
    }
  }

}
