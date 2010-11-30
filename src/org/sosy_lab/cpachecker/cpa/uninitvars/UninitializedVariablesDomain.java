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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * @author Philipp Wendler
 */
public class UninitializedVariablesDomain implements AbstractDomain {

  private static class UninitializedVariablesJoinOperator {
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      UninitializedVariablesElement uninitVarsElement1 = (UninitializedVariablesElement)element1;
      UninitializedVariablesElement uninitVarsElement2 = (UninitializedVariablesElement)element2;

      UninitializedVariablesElement newElement = uninitVarsElement1.clone();

      newElement.getGlobalVariables().addAll(uninitVarsElement2.getGlobalVariables());
      newElement.getLocalVariables().addAll(uninitVarsElement2.getLocalVariables());
      // only the local variables of the current context need to be joined,
      // the others are already identical (were joined before calling the last function)

      return newElement;
    }
  }

  private static class UninitializedVariablesPartialOrder {
    // returns true if element1 < element2 on lattice
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2)
                                         throws CPAException {

      UninitializedVariablesElement uninitVarsElement1 = (UninitializedVariablesElement)element1;
      UninitializedVariablesElement uninitVarsElement2 = (UninitializedVariablesElement)element2;

      if (!uninitVarsElement1.getGlobalVariables().containsAll(
                              uninitVarsElement2.getGlobalVariables())) {
        return false;
      }

      // need to check all function contexts
      Iterator<Pair<String, Collection<String>>> it1 = uninitVarsElement1.getallLocalVariables().iterator();
      Iterator<Pair<String, Collection<String>>> it2 = uninitVarsElement2.getallLocalVariables().iterator();

      while (it1.hasNext()) {
        assert it2.hasNext();

        Pair<String, Collection<String>> stackframe1 = it1.next();
        Pair<String, Collection<String>> stackframe2   = it2.next();

        assert stackframe1.getFirst().equals(stackframe1.getFirst());

        if (!stackframe1.getSecond().containsAll(stackframe2.getSecond())) {
          return false;
        }
      }
      assert !it2.hasNext(); // ensure same length

      return true;
    }
  }

  private static final UninitializedVariablesJoinOperator joinOperator = new UninitializedVariablesJoinOperator();
  private static final UninitializedVariablesPartialOrder partialOrder = new UninitializedVariablesPartialOrder();

  @Override
  public AbstractElement join(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return joinOperator.join(pElement1, pElement2);
  }

  @Override
  public boolean isLessOrEqual(AbstractElement pElement1,
      AbstractElement pElement2) throws CPAException {
    return partialOrder.satisfiesPartialOrder(pElement1, pElement2);
  }
}