// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.defuse;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DefUseDomain implements AbstractDomain {
  @Override
  public boolean isLessOrEqual(AbstractState element1, AbstractState element2) {
    DefUseState defUseState1 = (DefUseState) element1;
    DefUseState defUseState2 = (DefUseState) element2;

    return defUseState2.containsAllOf(defUseState1);
  }

  @Override
  public AbstractState join(AbstractState element1, AbstractState element2) {
    // Useless code, but helps to catch bugs by causing cast exceptions
    DefUseState defUseState1 = (DefUseState) element1;
    DefUseState defUseState2 = (DefUseState) element2;

    Set<DefUseDefinition> joined = new HashSet<>();
    Iterables.addAll(joined, defUseState1);

    Iterables.addAll(joined, defUseState2);

    return new DefUseState(joined);
  }
}
