/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;

/**
 * Abstract implementation of {@link Slicer} that takes care of mapping the specification to slicing
 * criteria.
 *
 * <p>Implements {@link #getRelevantEdges(CFA, Specification)} by mapping the specification to a set
 * of target edges that are handed to {@link #getRelevantEdges(CFA, Collection)} as slicing
 * criteria.
 */
public abstract class AbstractSlicer implements Slicer {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public AbstractSlicer(LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Set<CFAEdge> getRelevantEdges(CFA pCfa, Specification pSpecification)
      throws InterruptedException {
    TargetLocationProvider targetProvider =
        new CachingTargetLocationProvider(shutdownNotifier, logger, pCfa);

    ImmutableSet<CFANode> targetLocations =
        targetProvider.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpecification);

    Set<CFAEdge> slicingCriteria =
        targetLocations
            .stream()
            .flatMap(x -> CFAUtils.allEnteringEdges(x).stream())
            .collect(Collectors.toSet());

    return getRelevantEdges(pCfa, slicingCriteria);
  }
}
