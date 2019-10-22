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

import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;

/**
 * Abstract implementation of {@link Slicer} that takes care of mapping the specification to slicing
 * criteria.
 *
 * <p>Implements {@link #getRelevantEdges(CFA, Specification)} by mapping the specification to a set
 * of target edges that are handed to {@link #getRelevantEdges(CFA, Collection)} as slicing
 * criteria.
 */
@Options(prefix = "slicing")
public abstract class AbstractSlicer implements Slicer {

  private enum ExtractorType {
    ALL, REDUCER, SYNTAX;
  }

  @Option(name="extractor", secure=true, description="which type of extractor for slicing criteria to use")
  private ExtractorType extractorType = ExtractorType.ALL;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final SlicingCriteriaExtractor extractor;

  public AbstractSlicer(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    pConfig.inject(this, AbstractSlicer.class);

    switch (extractorType) {
      case ALL:
        extractor = new AllTargetsExtractor();
        break;
      case REDUCER:
        extractor = new ReducerExtractor(pConfig);
        break;
      case SYNTAX:
        extractor = new SyntaxExtractor(pConfig, pCfa, logger, pShutdownNotifier);
        break;
      default:
        throw new AssertionError("Unknown criterion extractor type");
    }
  }

  @Override
  public Set<CFAEdge> getRelevantEdges(CFA pCfa, Specification pSpecification)
      throws InterruptedException {

    Set<CFAEdge> slicingCriteria =
        extractor.getSlicingCriteria(pCfa, pSpecification, shutdownNotifier, logger);

    return getRelevantEdges(pCfa, slicingCriteria);
  }
}
