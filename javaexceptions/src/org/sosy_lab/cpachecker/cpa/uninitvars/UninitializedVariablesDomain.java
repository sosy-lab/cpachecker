// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Iterator;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Pair;

public class UninitializedVariablesDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    UninitializedVariablesState uninitVarsElement1 = (UninitializedVariablesState) element1;
    UninitializedVariablesState uninitVarsElement2 = (UninitializedVariablesState) element2;

    if (uninitVarsElement2.getGlobalVariables().containsAll(uninitVarsElement1.getGlobalVariables())
        && uninitVarsElement2
            .getLocalVariables()
            .containsAll(uninitVarsElement1.getLocalVariables())) {
      return uninitVarsElement2;
    }

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
    UninitializedVariablesState uninitVarsElement1 = (UninitializedVariablesState) element1;
    UninitializedVariablesState uninitVarsElement2 = (UninitializedVariablesState) element2;

    if (!uninitVarsElement1
        .getGlobalVariables()
        .containsAll(uninitVarsElement2.getGlobalVariables())) {
      return false;
    }

    // need to check all function contexts
    Iterator<Pair<String, List<String>>> it1 = uninitVarsElement1.getallLocalVariables().iterator();
    Iterator<Pair<String, List<String>>> it2 = uninitVarsElement2.getallLocalVariables().iterator();

    while (it1.hasNext()) {
      assert it2.hasNext();

      Pair<String, List<String>> stackframe1 = it1.next();
      Pair<String, List<String>> stackframe2 = it2.next();

      assert stackframe1.getFirst().equals(stackframe1.getFirst());

      if (!stackframe1.getSecond().containsAll(stackframe2.getSecond())) {
        return false;
      }
    }
    assert !it2.hasNext(); // ensure same length

    return true;
  }
}
