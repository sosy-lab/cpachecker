// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class PathCPA extends AbstractCPA {

  private SegmentedPaths pathCollection;

  private PathCPA() {
    super("sep", "sep", new FlatLatticeDomain(), new PathTransferRelation());
  }

  public void init(SegmentedPaths pPathCollection) {
    Preconditions.checkNotNull(pPathCollection);
    Preconditions.checkState(pathCollection == null);
    pathCollection = pPathCollection;
  }

  public static CPAFactory factory() {
    return new PathCPAFactory();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return PathState.initialState(pathCollection);
  }

  static PathCPA create() {
    return new PathCPA();
  }
}
