// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;

public class AllTargetsExtractor implements SlicingCriteriaExtractor {

  @Override
  public Set<CFAEdge> getSlicingCriteria(
      final CFA pCfa,
      final Specification pError,
      final ShutdownNotifier shutdownNotifier,
      LogManager logger)
      throws InterruptedException {
    TargetLocationProvider targetProvider =
        new CachingTargetLocationProvider(shutdownNotifier, logger, pCfa);

    ImmutableSet<CFANode> targetLocations =
        targetProvider.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pError);

    return targetLocations
        .stream()
        .flatMap(x -> CFAUtils.allEnteringEdges(x).stream())
        .collect(Collectors.toSet());
  }
}
