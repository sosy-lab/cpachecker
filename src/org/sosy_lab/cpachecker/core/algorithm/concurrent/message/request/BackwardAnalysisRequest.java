// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request;

import com.google.common.collect.Table;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFull;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPABackwards;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "concurrent.task.backward")
public class BackwardAnalysisRequest implements TaskRequest {
  private final Configuration backward;
  
  private final Block target;
  private final Block source;
  private final CFANode start;
  private final ShareableBooleanFormula errorCondition;
  private final Algorithm algorithm;
  private final ReachedSet reached;
  private final BlockAwareCompositeCPA cpa;
  private final MessageFactory messageFactory;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;
  private final FormulaManagerView fMgr;
  private final PathFormulaManager pfMgr;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Configuration file for backward analysis during concurrent analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  public BackwardAnalysisRequest(
      final Block pTarget,
      final CFANode pStart,
      @Nullable final Block pSource,
      @Nullable final ShareableBooleanFormula pErrorCondition,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final MessageFactory pMessageFactory)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    backward = BackwardAnalysisFull.getConfiguration(pLogger, configFile);

    target = pTarget;
    start = pStart;
    source = pSource;
    messageFactory = pMessageFactory;
    logManager = pLogger;
    shutdownNotifier = pShutdownNotifier;

    CoreComponentsFactory factory =
        new CoreComponentsFactory(
            backward, logManager, pShutdownNotifier, AggregatedReachedSets.empty());

    Specification emptySpec = Specification.alwaysSatisfied();
    CompositeCPA compositeCpa = (CompositeCPA) factory.createCPA(pCFA, emptySpec);
    reached = factory.createReachedSet(compositeCpa);
    
    if (compositeCpa.retrieveWrappedCpa(PredicateCPA.class) == null) {
      throw new InvalidConfigurationException(
          "Backward analysis requires a composite CPA with predicateCPA as component CPA.");
    }
    if (compositeCpa.retrieveWrappedCpa(LocationCPA.class) == null
        && compositeCpa.retrieveWrappedCpa(LocationCPABackwards.class) == null) {
      throw new InvalidConfigurationException(
          "Backward analysis requires a composite CPA with locationCPA as component CPA.");
    }

    cpa =
        (BlockAwareCompositeCPA)
            BlockAwareCompositeCPA.factory()
                .setConfiguration(backward)
                .setLogger(pLogger)
                .setShutdownNotifier(pShutdownNotifier)
                .set(pCFA, CFA.class)
                .set(target, Block.class)
                .set(compositeCpa, CompositeCPA.class)
                .createInstance();

    algorithm = factory.createAlgorithm(cpa, pCFA, emptySpec);

    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;

    fMgr = predicateCPA.getSolver().getFormulaManager();
    pfMgr = predicateCPA.getPathFormulaManager();
    
    if (pErrorCondition == null) {
      PathFormula condition = pfMgr.makeEmptyPathFormula();
      errorCondition = new ShareableBooleanFormula(fMgr, condition);
    } else {
      errorCondition = pErrorCondition;
    }
  }

  /**
   * {@inheritDoc}
   * <hr/>
   * <p>For {@link BackwardAnalysisRequest}, this method obtains and stores the block summary for
   * the inter-block edge along which the error condition enters the target block. As soon as the
   * task then actually executes, it first checks whether the conjunction of this summary and the
   * error condition is satisfiable. If this is not the case, the condition need not propagate
   * further and the {@link BackwardAnalysisFull} terminates early.
   *
   * @return The immutable {@link BackwardAnalysisFull} which actually implements the {@link Task}.
   * @throws RequestInvalidatedException The {@link BackwardAnalysisRequest} has become invalidated by
   *                                  a more recent one and the {@link BackwardAnalysisFull} must not
   *                                  execute.
   * @see BackwardAnalysisFull
   */
  @Override
  public Task process(
      Table<Block, Block, ShareableBooleanFormula> pSummaries, Map<Block, Integer> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated)
      throws RequestInvalidatedException {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only " + Scheduler.getThreadName() + " may call finalize() on task";

    if (source == null) {
      if (pAlreadyPropagated.contains(start)) {
        throw new RequestInvalidatedException();
      } else {
        pAlreadyPropagated.add(start);
      }
    }

    ShareableBooleanFormula blockSummary = null;
    if (source != null) {
      blockSummary = pSummaries.get(target, source);
    }

    if (blockSummary == null) {
      PathFormula emptyFormula = pfMgr.makeEmptyPathFormula();
      blockSummary = new ShareableBooleanFormula(fMgr, emptyFormula);
    }

    return new BackwardAnalysisFull(
        target,
        start,
        errorCondition,
        blockSummary,
        reached,
        algorithm,
        cpa,
        messageFactory,
        logManager,
        shutdownNotifier);
  }
}
