// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph.PathPosition;

class PathRestrictionTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<PathPosition> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    PathPosition next = ((PathPosition) element).advance(cfaEdge);
    return next.isEmpty() ? ImmutableList.of() : ImmutableList.of(next);
  }
}
