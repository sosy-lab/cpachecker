// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.globalinfo;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

public class CFAInfo {
  private final ImmutableMap<Integer, CFANode> nodeNumberToNode;
  private LocationStateFactory locationStateFactory;

  CFAInfo(CFA cfa) {
    ImmutableMap.Builder<Integer, CFANode> nodeNumberToNode0 = ImmutableMap.builder();
    for (CFANode node : cfa.getAllNodes()) {
      nodeNumberToNode0.put(node.getNodeNumber(), node);
    }
    nodeNumberToNode = nodeNumberToNode0.buildOrThrow();
  }

  public CFANode getNodeByNodeNumber(int nodeNumber) {
    return nodeNumberToNode.get(nodeNumber);
  }

  public void storeLocationStateFactory(LocationStateFactory pElementFactory) {
    locationStateFactory = pElementFactory;
  }

  public LocationStateFactory getLocationStateFactory() {
    return locationStateFactory;
  }
}
