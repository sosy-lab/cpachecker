// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dominator;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class DominatorCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DominatorCPA.class);
  }

  public DominatorCPA() {
    super("join", "join", DelegateAbstractDomain.getInstance(), new DominatorTransferRelation());
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    return new DominatorState(ImmutableSet.of(pNode));
  }
}
