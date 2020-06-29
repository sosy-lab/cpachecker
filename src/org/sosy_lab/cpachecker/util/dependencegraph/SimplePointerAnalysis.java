// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class SimplePointerAnalysis {

  private static final PointerTransferRelation POINTER_TRANSFER_RELATION =
      new PointerTransferRelation();
  private static final Precision PRECISION = new Precision() {};

  private static Collection<CFAEdge> getAllEdges(CFA pCfa) {

    List<CFAEdge> edges = new ArrayList<>();

    for (CFANode node : pCfa.getAllNodes()) {
      Iterables.addAll(edges, CFAUtils.leavingEdges(node));
    }

    return edges;
  }

  private static PointerState next(PointerState pPointerState, CFAEdge pEdge)
      throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> collection =
        POINTER_TRANSFER_RELATION.getAbstractSuccessorsForEdge(pPointerState, PRECISION, pEdge);

    Optional<? extends AbstractState> optState = collection.stream().findFirst();

    if (optState.isPresent()) {
      return (PointerState) optState.orElseThrow();
    }

    return pPointerState;
  }

  public static PointerState run(CFA pCfa) throws CPATransferException, InterruptedException {

    Collection<CFAEdge> edges = getAllEdges(pCfa);
    PointerState pointerState = PointerState.INITIAL_STATE;
    Map<MemoryLocation, LocationSet> pointsToMap = new HashMap<>();
    boolean changed = true;

    while (changed) {

      changed = false;

      for (CFAEdge edge : edges) {

        PointerState nextPointerState = next(pointerState, edge);

        for (Map.Entry<MemoryLocation, LocationSet> entry :
            nextPointerState.getPointsToMap().entrySet()) {

          LocationSet locationSet = pointsToMap.get(entry.getKey());

          if (locationSet == null) {

            pointsToMap.put(entry.getKey(), entry.getValue());
            pointerState = pointerState.addPointsToInformation(entry.getKey(), entry.getValue());

            changed = true;

          } else if (!locationSet.containsAll(entry.getValue())) {

            locationSet = locationSet.addElements(entry.getValue());
            pointsToMap.put(entry.getKey(), locationSet);
            pointerState = pointerState.addPointsToInformation(entry.getKey(), entry.getValue());

            changed = true;
          }
        }
      }
    }

    return pointerState;
  }
}
