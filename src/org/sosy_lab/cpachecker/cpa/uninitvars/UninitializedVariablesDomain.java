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
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * @author Philipp Wendler
 */
public class UninitializedVariablesDomain implements AbstractDomain {

  private static class UninitializedVariablesTopElement extends UninitializedVariablesElement {

    public UninitializedVariablesTopElement() {
      super("<TOP>");
    }

    @Override
    public String toString() {
      return "<UninitializedVariables TOP>";
    }
  }

  private static class UninitializedVariablesJoinOperator implements JoinOperator {
    @Override
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

  private static class UninitializedVariablesPartialOrder implements PartialOrder {
    @Override
    // returns true if element1 < element2 on lattice
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2)
                                         throws CPAException {

      UninitializedVariablesElement uninitVarsElement1 = (UninitializedVariablesElement)element1;
      UninitializedVariablesElement uninitVarsElement2 = (UninitializedVariablesElement)element2;

      if (element2 == topElement) {
        return true;
      }
      if (element1 == topElement) {
        return false;
      }

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

  private static final JoinOperator joinOperator = new UninitializedVariablesJoinOperator();
  private static final PartialOrder partialOrder = new UninitializedVariablesPartialOrder();
  private static final AbstractElement topElement = new UninitializedVariablesTopElement();

  @Override
  public JoinOperator getJoinOperator() {
    return joinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return partialOrder;
  }

  @Override
  public AbstractElement getTopElement() {
    return topElement;
  }
}