// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph.PathPosition;

public class PathRestrictionCPA extends AbstractCPA {

  private @Nullable DecisionGraph paths;

  private PathRestrictionCPA() {
    super("sep", "sep", new FlatLatticeDomain(), new PathRestrictionTransferRelation());
  }

  /** Restricts the analysis to the paths permitted by {@code pPaths}. */
  public void init(DecisionGraph pPaths) {
    Preconditions.checkState(paths == null, "The paths can only be set once");
    paths = Preconditions.checkNotNull(pPaths);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PathRestrictionCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    // CPAchecker creates an initial state before the paths are known. It must not be explored.
    return paths == null ? PathPosition.UNINITIALIZED : paths.start();
  }
}
