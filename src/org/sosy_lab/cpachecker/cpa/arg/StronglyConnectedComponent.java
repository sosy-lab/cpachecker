// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class StronglyConnectedComponent
    extends de.uni_freiburg.informatik.ultimate.util.scc.StronglyConnectedComponent<ARGState> {

  public ImmutableList<ARGState> nodesAsList() {
    return ImmutableList.copyOf(mNodes);
  }

  public boolean hasTargetStates() {
    return mNodes.stream().anyMatch(ARGState::isTarget);
  }

  @Override
  public String toString() {
    return String.format(
        "[%s]",
        mNodes.stream()
            .map(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .collect(Collectors.joining(", ")));
  }
}
