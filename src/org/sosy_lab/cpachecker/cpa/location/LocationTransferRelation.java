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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

@Options
public class LocationTransferRelation implements TransferRelation {

  private final LocationStateFactory factory;
  private final CFA pCFA;

  @Option(
      secure = true,
      name = "summaries.transfer",
      description = "Dependencies between the Different Strategies")
  private StrategyDependencyEnum transferStrategy = StrategyDependencyEnum.BASESTRATEGYDEPENDENCY;

  public LocationTransferRelation(LocationStateFactory pFactory, CFA pCFA, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    factory = pFactory;
    this.pCFA = pCFA;
    if (this.pCFA.getSummaryInformation().isPresent()) {
      SummaryInformation summaryInformation = pCFA.getSummaryInformation().orElseThrow();
      StrategyDependency summaryTransferStrategy =
          new StrategyDependencyFactory().createStrategy(this.transferStrategy);
      summaryInformation.setTransferStrategy(summaryTransferStrategy);
    }
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
        List<StrategiesEnum> allowedStrategies =
            summaryInformation.getAllowedStrategies(cfaEdge, node);

        List<CFANode> successors = new ArrayList<>();
        successors.add(cfaEdge.getSuccessor());

        return FluentIterable.from(successors)
            .filter(n -> allowedStrategies.contains(summaryInformation.getStrategyForNode(n)))
            .transform(n -> factory.getState(n))
            .toList();
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
      List<StrategiesEnum> availableStrategies =
          CFAUtils.successorsOf(node)
              .transform(n -> summaryInformation.getStrategyForNode(n))
              .toList();
      Set<StrategiesEnum> allowedStrategies = new HashSet<>(availableStrategies);
      allowedStrategies.removeAll(summaryInformation.getUnallowedStrategiesForNode(node));
      Set<StrategiesEnum> chosenStrategies =
          new HashSet<>(
              summaryInformation
                  .getTransferSummaryStrategy()
                  .filter(new ArrayList<>(allowedStrategies)));

      return CFAUtils.successorsOf(node)
          .filter(n -> chosenStrategies.contains(summaryInformation.getStrategyForNode(n)))
          .transform(n -> factory.getState(n))
          .toList();
    }
  }
}
