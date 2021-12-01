// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.backward;

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
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.CPACreatingRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFull;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "concurrent.task.backward")
public class BackwardAnalysisRequest extends CPACreatingRequest implements TaskRequest {
  private final Configuration taskConfiguration;
  
  private final Block target;
  private final Block source;
  private final ErrorOrigin origin;
  private final CFANode start;
  private final ShareableBooleanFormula errorCondition;
  private final FormulaManagerView fMgr;
  private final PathFormulaManager pfMgr;
  
  @SuppressWarnings("FieldMayBeFinal")
  @Option(description 
      = "Optional configuration file for forward analysis during concurrent analysis."
      + "Relative paths get interpreted starting from the location of the configuration"
      + "file which sets this value, i.e. usually"
      + "concurrent-task-partitioning.properties in config/includes/."
      + "If no value is set, the analysis uses the file predicateForward.properties in"
      + "the package core.algorithm.concurrent.task.forward.", secure=true)
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configFile = null;

  public BackwardAnalysisRequest(
      final Block pTarget,
      final ErrorOrigin pOrigin,
      final CFANode pStart,
      @Nullable final Block pSource,
      @Nullable final ShareableBooleanFormula pErrorCondition,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCFA,
      final MessageFactory pMessageFactory)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(pMessageFactory, pLogger, pShutdownNotifier);
    
    pConfig.inject(this);
    taskConfiguration = BackwardAnalysisFull.getConfiguration(pLogger, configFile, pConfig);

    prepareCPA(taskConfiguration, pCFA, Specification.alwaysSatisfied(), pTarget);
    
    target = pTarget;
    origin = pOrigin;
    start = pStart;
    source = pSource;
    
    PredicateCPA predicateCPA = argcpa.retrieveWrappedCpa(PredicateCPA.class);
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
        : "Only " + Scheduler.getThreadName() + " may call process() on task";

    if (source == null) {
      CFANode location = origin.getLocation();
      if (pAlreadyPropagated.contains(location)) {
        throw new RequestInvalidatedException();
      } else {
        pAlreadyPropagated.add(location);
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
        origin,
        start,
        errorCondition,
        blockSummary,
        reached,
        algorithm,
        argcpa,
        messageFactory,
        logManager,
        shutdownNotifier);
  }
}
