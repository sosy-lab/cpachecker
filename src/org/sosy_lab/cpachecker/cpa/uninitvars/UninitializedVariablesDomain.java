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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class UninitializedVariablesDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
      UninitializedVariablesState uninitVarsElement1 = (UninitializedVariablesState)element1;
      UninitializedVariablesState uninitVarsElement2 = (UninitializedVariablesState)element2;

      UninitializedVariablesState newElement = uninitVarsElement1.clone();

      newElement.getGlobalVariables().addAll(uninitVarsElement2.getGlobalVariables());
      newElement.getLocalVariables().addAll(uninitVarsElement2.getLocalVariables());
      // only the local variables of the current context need to be joined,
      // the others are already identical (were joined before calling the last function)

      return newElement;
  }

  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {
      // returns true if element1 < element2 on lattice
      UninitializedVariablesState uninitVarsElement1 = (UninitializedVariablesState)element1;
      UninitializedVariablesState uninitVarsElement2 = (UninitializedVariablesState)element2;

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