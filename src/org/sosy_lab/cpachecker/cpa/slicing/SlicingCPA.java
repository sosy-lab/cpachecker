/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.slicing;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;
import org.sosy_lab.cpachecker.util.slicing.Slicer;
import org.sosy_lab.cpachecker.util.slicing.StaticSlicer;

/**
 * CPA that performs program slicing during analysis. The Slicing CPA wraps another CPA. If a CFA
 * edge <code>g = (l, op, l')</code> is not relevant, the program operation <code>op</code> is not
 * considered - the wrapped CPA will handle the edge as if it was <code>(l, noop, l')</code>.
 *
 * <p>The set of relevant edges for the slicing criteria is stored in the {@link SlicingPrecision}.
 * This set can be iteratively created through the {@link
 * org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm CEGAR} approach. Initially, it is empty.
 */
@Options(prefix = "cpa.slicing")
public class SlicingCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  @Option(
      secure = true,
      name = "exportSlice",
      description = "Whether to export the final slice to a file")
  private boolean exportSlice = true;

  @Option(secure = true, name = "exportSliceFile", description = "File to export final slice to")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportSliceFile = Paths.get("ProgramSlice.txt");

  @Option(
      secure = true,
      name = "refinableSlice",
      description =
          "Whether to use a refinable slicing precision that starts with an empty slice, or a statically computed, fixed slicing precision")
  private boolean useRefinableSlice = false;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final Specification spec;

  private final Slicer slicer;

  private TransferRelation transferRelation;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private PrecisionAdjustment precisionAdjustment;


  /**
   * Returns the factory for creating this CPA.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SlicingCPA.class);
  }

  public SlicingCPA(
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Configuration pConfig,
      final CFA pCfa,
      final Specification pSpec)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    if (exportSlice && exportSliceFile == null) {
      throw new InvalidConfigurationException("File to export slice to is 'null'.");
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    config = pConfig;
    cfa = pCfa;
    spec = pSpec;

    transferRelation = new SlicingTransferRelation(pCpa.getTransferRelation());
    mergeOperator = new PrecisionDelegatingMerge(pCpa.getMergeOperator());
    stopOperator = new PrecisionDelegatingStop(pCpa.getStopOperator());
    precisionAdjustment = new PrecisionDelegatingPrecisionAdjustment(pCpa.getPrecisionAdjustment());

    final DependenceGraph dependenceGraph =
        pCfa.getDependenceGraph()
            .orElseThrow(
                () -> new InvalidConfigurationException("SlicingCPA requires dependence graph"));
    slicer = new StaticSlicer(logger, shutdownNotifier, config, dependenceGraph, pCfa);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return super.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    Precision wrappedPrec = getWrappedCpa().getInitialPrecision(pNode, pPartition);

    Set<CFAEdge> relevantEdges;
    if (useRefinableSlice) {
      relevantEdges = ImmutableSet.of();
    } else {
      relevantEdges = computeSlice(cfa, spec);
    }

    return new SlicingPrecision(wrappedPrec, relevantEdges);
  }

  private Set<CFAEdge> computeSlice(CFA pCfa, Specification pSpec) throws InterruptedException {
    return slicer.getRelevantEdges(pCfa, pSpec);
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public Configuration getConfig() {
    return config;
  }

  public CFA getCfa() {
    return cfa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream out, Result result, UnmodifiableReachedSet reached) {

            if (exportSlice) {
              try (Writer sliceFile =
                  IO.openOutputFile(exportSliceFile, Charset.defaultCharset())) {
                SlicingPrecision fullPrec = null;
                for (Precision p : reached.getPrecisions()) {
                  SlicingPrecision slicingPrec =
                      Precisions.extractPrecisionByType(p, SlicingPrecision.class);
                  if (fullPrec == null) {
                    fullPrec = slicingPrec;
                  } else {
                    fullPrec =
                        fullPrec.getNew(slicingPrec.getWrappedPrec(), slicingPrec.getRelevant());
                  }
                }
                ImmutableList<String> edges =
                    ImmutableList.sortedCopyOf(
                        Collections2.transform(fullPrec.getRelevant(), Object::toString));

                for (String e : edges) {
                  sliceFile.write(e);
                  sliceFile.write('\n');
                }
              } catch (IOException pE) {
                logger.logException(Level.INFO, pE, "Writing slice failed");
              }
            }
          }

          @Nullable
          @Override
          public String getName() {
            return SlicingCPA.class.getSimpleName();
          }
        });

    if (slicer instanceof StatisticsProvider) {
      ((StatisticsProvider) slicer).collectStatistics(pStatsCollection);
    }

    super.collectStatistics(pStatsCollection);
  }
}
