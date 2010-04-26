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
package org.sosy_lab.cpachecker.cpa.predicateabstraction;

import java.util.Collection;

import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;


/**
 * Coverage check for explicit-state lazy abstraction
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class PredicateAbstractionStopOperator implements StopOperator {

  private final PredicateAbstractionAbstractDomain domain;

  public PredicateAbstractionStopOperator(PredicateAbstractionAbstractDomain d) {
    domain = d;
  }

  public boolean stop(AbstractElement element,
      Collection<AbstractElement> reached, Precision prec) throws CPAException {
    
    for (AbstractElement e : reached) {
      if (stop(element, e)) {
        return true;
      }
    }
    return false;
  }


  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    // transfer relation does not produce bottom, so no need to check for bottom here

    PredicateAbstractionAbstractElement e1 = (PredicateAbstractionAbstractElement)element;
    PredicateAbstractionAbstractElement e2 = (PredicateAbstractionAbstractElement)reachedElement;

//    if (!e2.isMarked()) {
//      return false;
//    }

    PredicateAbstractionCPA cpa = domain.getCPA();
    AbstractFormulaManager amgr =
      cpa.getAbstractFormulaManager();

    assert(e1.getAbstraction() != null);
    assert(e2.getAbstraction() != null);

    boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());

//    if (ok) {
//      CPAMain.logManager.log(Level.FINEST,
//          "Element: ", element, " COVERED by: ", e2);
//      cpa.setCovered(e1);
//      e1.setCovered(true);
//    } else {
//      CPAMain.logManager.log(Level.FINEST,
//          "NO, not covered");
//    }

    return ok;
  }

}
