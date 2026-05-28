// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class PathCPA extends AbstractCPA {

  private ImmutableList<CFAEdge> path;

  public PathCPA() {
    super("sep", "sep", new FlatLatticeDomain(), new PathTransferRelation());
  }

  public void init(ImmutableList<CFAEdge> pPath) {
    assert pPath != null;
    assert path == null;
    assert !pPath.isEmpty();
    path = pPath;
  }

  public static CPAFactory factory() {
    return new PathCPAFactory();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    if (path != null && path.getFirst().getPredecessor().equals(node)) {
      return new PathState(path);
    } else {
      return PathState.INVALID;
    }
  }

  public static PathCPA create() {
    return new PathCPA();
  }
}
