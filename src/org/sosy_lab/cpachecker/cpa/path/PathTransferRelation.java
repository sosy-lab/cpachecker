// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PathTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<PathState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    PathState pathState = (PathState) element;

    if (pathState.isInvalid()) {
      return ImmutableList.of();
    }

    PathState newState = pathState.followEdge(cfaEdge);

    if (newState.isInvalid()) {
      return ImmutableList.of();
    }

    return ImmutableList.of(newState);
  }
}
