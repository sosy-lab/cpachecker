// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class PathRestrictionCPA extends AbstractCPA {

  private SegmentedPaths pathCollection;

  private PathRestrictionCPA() {
    super("sep", "sep", new FlatLatticeDomain(), new PathRestrictionTransferRelation());
  }

  public void init(SegmentedPaths pPathCollection) {
    Preconditions.checkState(pathCollection == null);
    pathCollection = Preconditions.checkNotNull(pPathCollection);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PathRestrictionCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return PathRestrictionState.initialState(pathCollection);
  }
}
