// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraphBuilder;

/**
 * Factory class for creating {@link Slicer} objects. The concrete <code>Slicer</code> that is
 * created by this classes {@link #create(LogManager, ShutdownNotifier, Configuration, CFA) create}
 * method depends on the given configuration.
 */
public class SlicerFactory implements StatisticsProvider {

  private enum ExtractorType {
    ALL,
    REDUCER,
    SYNTAX;
  }

  private enum SlicingType {
    /**
     * Use static program slicing based on dependence graph of CFA
     *
     * @see org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraphBuilder
     * @see StaticSlicer
     */
    STATIC,
    /**
     * Use identity function as slicer.
     *
     * @see IdentitySlicer
     */
    IDENTITY
  }

  @Options(prefix = "slicing")
  public static class SlicerOptions {
    @Option(
        name = "extractor",
        secure = true,
        description = "which type of extractor for slicing criteria to use")
    private ExtractorType extractorType = ExtractorType.ALL;

    @Option(secure = true, name = "type", description = "what kind of slicing to use")
    private SlicingType slicingType = SlicingType.STATIC;

    @Option(
        secure = true,
        name = "partiallyRelevantEdges",
        description =
            "Whether to allow edges in the resulting slice that are only partially relevant (e.g."
                + " function calls where not every parameter is relevant). Setting this parameter"
                + " to true can decrease the size of the resulting slice.")
    private boolean partiallyRelevantEdges = true;

    public SlicerOptions(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    public ExtractorType getExtractorType() {
      return extractorType;
    }

    public SlicingType getSlicingType() {
      return slicingType;
    }
  }

  private final Collection<Statistics> stats;

  public SlicerFactory() {
    stats = new ArrayList<>();
  }

  private CSystemDependenceGraph createDependenceGraph(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, Configuration pConfig, CFA pCfa)
      throws CPAException, InvalidConfigurationException, InterruptedException {

    final CSystemDependenceGraphBuilder depGraphBuilder =
        new CSystemDependenceGraphBuilder(pCfa, pConfig, pLogger, pShutdownNotifier);
    try {
      return depGraphBuilder.build();
    } finally {
      depGraphBuilder.collectStatistics(stats);
    }
  }

  /**
   * Creates a new {@link Slicer} object according to the given {@link Configuration}. All other
   * arguments of this method are passed to the created slicer and its components, if required by
   * them.
   */
  public Slicer create(
      LogManager pLogger, ShutdownNotifier pShutdownNotifier, Configuration pConfig, CFA pCfa)
      throws CPAException, InvalidConfigurationException, InterruptedException {
    SlicerOptions options = new SlicerOptions(pConfig);

    final SlicingCriteriaExtractor extractor;
    final ExtractorType extractorType = options.getExtractorType();
    switch (extractorType) {
      case ALL:
        extractor = new AllTargetsExtractor();
        break;
      case REDUCER:
        extractor = new ReducerExtractor(pConfig);
        break;
      case SYNTAX:
        extractor = new SyntaxExtractor(pConfig, pCfa, pLogger, pShutdownNotifier);
        break;
      default:
        throw new AssertionError("Unhandled criterion extractor type " + extractorType);
    }

    final SlicingType slicingType = options.getSlicingType();
    switch (slicingType) {
      case STATIC:
        CSystemDependenceGraph dependenceGraph =
            createDependenceGraph(pLogger, pShutdownNotifier, pConfig, pCfa);
        return new StaticSlicer(
            extractor,
            pLogger,
            pShutdownNotifier,
            pConfig,
            dependenceGraph,
            options.partiallyRelevantEdges);
      case IDENTITY:
        return new IdentitySlicer(extractor, pLogger, pShutdownNotifier, pConfig);
      default:
        throw new AssertionError("Unhandled slicing type " + slicingType);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.addAll(stats);
  }
}
