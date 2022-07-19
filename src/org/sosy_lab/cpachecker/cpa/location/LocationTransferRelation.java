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
import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryInformation;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;
  private final CFA pCFA;

  public LocationTransferRelation(LocationStateFactory pFactory, CFA pCFA) {
    factory = pFactory;
    this.pCFA = pCFA;
  }

  @Override
  public Collection<LocationState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {

    CFANode node = ((LocationState) element).getLocationNode();

    if (CFAUtils.allLeavingEdges(node).contains(cfaEdge)) {
      if (this.pCFA.getSummaryInformation().isEmpty()) {
        return Collections.singleton(factory.getState(cfaEdge.getSuccessor()));
      } else {
        SummaryInformation summaryInformation = pCFA.getSummaryInformation().orElseThrow();
        List<StrategiesEnum> chosenStrategies =
            summaryInformation.getChosenStrategies(node, (LocationPrecision) prec);

        ArrayList<LocationState> successors = new ArrayList<>();

        if (chosenStrategies.contains(SummaryUtils.getStrategyForEdge(cfaEdge))) {
          successors.add(factory.getState(cfaEdge.getSuccessor()));
        }

        return new ImmutableList<>(successors);
      }
    }

    return ImmutableSet.of();
  }

  @Override
  public Collection<LocationState> getAbstractSuccessors(AbstractState element, Precision prec)
      throws CPATransferException {

    CFANode node = ((LocationState) element).getLocationNode();

    if (this.pCFA.getSummaryInformation().isEmpty()) {
      return CFAUtils.successorsOf(node).transform(n -> factory.getState(n)).toList();
    } else {
      SummaryInformation summaryInformation = pCFA.getSummaryInformation().orElseThrow();
      List<StrategiesEnum> chosenStrategies =
          summaryInformation.getChosenStrategies(node, (LocationPrecision) prec);

      return FluentIterable.from(node.getLeavingEdges())
          .filter(e -> chosenStrategies.contains(SummaryUtils.getStrategyForEdge(e)))
          .transform(e -> factory.getState(e.getSuccessor()))
          .toList();
    }
  }
}
