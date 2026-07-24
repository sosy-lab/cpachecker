// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

class PathTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<PathState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    PathState pathState = (PathState) element;

    if (pathState.isInitial) {
      // We can only return one single state as initial state -> create multiple states here
      return FluentIterable.from(PathState.initialStates(pathState.paths))
          .transformAndConcat(s -> getAbstractSuccessorsForEdge(s, prec, cfaEdge))
          .toList();
    }

    if (pathState.isAtEndOfPath() || !SegmentedPaths.isDecisionEdge(cfaEdge)) {
      return ImmutableList.of(pathState);
    }

    if (!SegmentedPaths.edgeToString(cfaEdge)
        .equals(pathState.activePath.get(pathState.pathIndex))) {
      return ImmutableList.of();
    }

    int nextPathIndex = pathState.pathIndex + 1;

    if (nextPathIndex == pathState.activePath.size()) {
      return PathState.startSegment(pathState.paths, pathState.segmentIndex + 1).toList();
    } else {
      return ImmutableList.of(
          new PathState(
              pathState.paths, pathState.segmentIndex, pathState.activePath, nextPathIndex));
    }
  }
}
