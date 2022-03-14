// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.splitter.heuristics;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

public class SplitAtAssumes implements SplitHeuristic {

  public SplitAtAssumes() {}

  @Override
  public boolean removeSplitIndices(final CFAEdge pCfaEdge) {
    return false;
  }

  @Override
  public Collection<Integer> getIndicesToRemove(final CFAEdge pCfaEdge) {
    return ImmutableList.of();
  }

  @Override
  public boolean divideSplitIndices(CFAEdge pCfaEdge) {
    return pCfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge;
  }

  @Override
  public int divideIntoHowManyParts(CFAEdge pCfaEdge) {
    return 2;
  }
}
