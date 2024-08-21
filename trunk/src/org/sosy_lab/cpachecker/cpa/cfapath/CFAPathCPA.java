// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cfapath;

import org.sosy_lab.common.annotations.Unmaintained;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

@Unmaintained
public class CFAPathCPA extends AbstractCPA {

  private static final CFAPathCPA sInstance = new CFAPathCPA();

  public static CFAPathCPA getInstance() {
    return sInstance;
  }

  public CFAPathCPA() {
    super("SEP", "NEVER", CFAPathDomain.getInstance(), new CFAPathTransferRelation());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return CFAPathStandardState.getEmptyPath();
  }
}
