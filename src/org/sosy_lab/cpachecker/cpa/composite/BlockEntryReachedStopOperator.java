// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static org.sosy_lab.cpachecker.util.AbstractStates.filterLocation;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockEntryReachedStopOperator implements StopOperator {
  private final CompositeStopOperator stop;

  private final CFANode blockEntry;

  public BlockEntryReachedStopOperator(CompositeStopOperator pStopOperator, Block pTarget) {
    stop = pStopOperator;
    blockEntry = pTarget.getEntry();
  }

  @Override
  public boolean stop(AbstractState state, Collection<AbstractState> reached, Precision precision)
      throws
      CPAException, InterruptedException {
    Iterable<AbstractState> entryStates = filterLocation(reached, blockEntry);

    if (!Iterables.isEmpty(entryStates)) {
      return true;
    } else {
      return stop.stop(state, reached, precision);
    }
  }
}
