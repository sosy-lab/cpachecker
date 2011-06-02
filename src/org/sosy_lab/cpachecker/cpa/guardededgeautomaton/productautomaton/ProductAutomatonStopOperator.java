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
package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton;

import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeStopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class ProductAutomatonStopOperator extends CompositeStopOperator {

  public ProductAutomatonStopOperator(ImmutableList<StopOperator> pStopOperators) {
    super(pStopOperators);
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    ProductAutomatonElement compositeElement1 = (ProductAutomatonElement)element;
    ProductAutomatonElement compositeElement2 = (ProductAutomatonElement)reachedElement;

    List<AbstractElement> compositeElements1 = compositeElement1.getElements();
    List<AbstractElement> compositeElements2 = compositeElement2.getElements();

    for (int idx = 0; idx < compositeElements1.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractElement absElem1 = compositeElements1.get(idx);
      AbstractElement absElem2 = compositeElements2.get(idx);

      if (!stopOp.stop(absElem1, absElem2)){
        return false;
      }
    }

    return true;
  }

}
