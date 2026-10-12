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
import org.sosy_lab.cpachecker.util.graph.StronglyConnectedComponent;

public final class ARGStronglyConnectedComponent extends StronglyConnectedComponent<ARGState> {

  public ARGStronglyConnectedComponent(ARGState pRoot) {
    super(pRoot);
  }

  public ImmutableList<ARGState> nodesAsList() {
    return ImmutableList.copyOf(getNodes());
  }

  public boolean hasTargetStates() {
    return getNodes().stream().anyMatch(ARGState::isTarget);
  }

  @Override
  public String toString() {
    return String.format(
        "[%s]",
        getNodes().stream()
            .map(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
            .collect(Collectors.joining(", ")));
  }
}
