// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;

  @SuppressWarnings("unused")
  private final Optional<SummaryInformation> maybeSummaryInformation;

  public LocationTransferRelation(LocationStateFactory pFactory, CFA pCFA) {
    factory = pFactory;
    maybeSummaryInformation = pCFA.getSummaryInformation();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {

    CFANode node = ((LocationState) element).getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {
      LocationPrecision lPrec = (LocationPrecision) prec;
      if (lPrec.getCurrentStrategy().isEmpty()
          && !lPrec.isStartOfSomeStrategy(cfaEdge.getSuccessor())) {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      } else {
        if (lPrec.getCurrentStrategy().isPresent()) {
          CFANode expectedNextNode =
              lPrec.getCurrentStrategy().orElseThrow().getStartGhostCfaNode();
          if (expectedNextNode == cfaEdge.getSuccessor()) {
            return Collections.singleton(factory.getState(expectedNextNode));
          }
        }
      }
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec)
      throws CPATransferException {

    CFANode node = ((LocationState) element).getLocationNode();

    if (maybeSummaryInformation.isEmpty()) {
      return CFAUtils.successorsOf(node).transform(n -> factory.getState(n)).toList();
    } else {
      return FluentIterable.from(node.getLeavingEdges())
          .filter(
              e ->
                  e.getSuccessor()
                      == ((LocationPrecision) prec)
                          .getCurrentStrategy()
                          .orElseThrow()
                          .getStartGhostCfaNode())
          .transform(e -> factory.getState(e.getSuccessor()))
          .toList();
    }
  }
}
