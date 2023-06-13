// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageReachedSet;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageContainer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.usage")
public class IdentifierIterator
    extends WrappedConfigurableRefinementBlock<ReachedSet, SingleIdentifier> implements Refiner {

  private class Stats implements Statistics {

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
      IdentifierIterator.this.printStatistics(writer);
    }

    @Override
    public String getName() {
      return "UsageStatisticsRefiner";
    }
  }

  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;

  @Option(
      name = "precisionReset",
      description = "The value of marked unsafes, after which the precision should be cleaned",
      secure = true)
  private int precisionReset = Integer.MAX_VALUE;

  // TODO Option is broken!!
  @Option(
      name = "totalARGCleaning",
      description = "clean all ARG or try to reuse some parts of it (memory consuming)",
      secure = true)
  private boolean totalARGCleaning = true;

  @Option(
      name = "hideFilteredUnsafes",
      description = "filtered unsafes, which can not be removed using precision, may be hidden",
      secure = true)
  private boolean hideFilteredUnsafes = false;

  private final BAMTransferRelation transfer;

  int i = 0;
  int lastFalseUnsafeSize = -1;
  int lastTrueUnsafes = 0;

  private final Map<SingleIdentifier, AdjustablePrecision> precisionMap = new HashMap<>();

  public IdentifierIterator(
      ConfigurableRefinementBlock<SingleIdentifier> pWrapper,
      Configuration config,
      ConfigurableProgramAnalysis pCpa,
      BAMTransferRelation pTransfer)
      throws InvalidConfigurationException {
    super(pWrapper);
    config.inject(this);
    cpa = pCpa;
    UsageCPA uCpa = CPAs.retrieveCPA(pCpa, UsageCPA.class);
    uCpa.getStats().setBAMCPA((BAMCPA) cpa);
    logger = uCpa.getLogger();
    transfer = pTransfer;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(
          BAMPredicateRefiner.class.getSimpleName() + " could not find the PredicateCPA");
    }

    BAMPredicateCPA predicateCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(BAMPredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          BAMPredicateRefiner.class.getSimpleName() + " needs an BAMPredicateCPA");
    }

    return new RefinementBlockFactory(pCpa, predicateCpa.getConfiguration()).create();
  }

  @Override
  public RefinementResult performBlockRefinement(ReachedSet pReached)
      throws CPAException, InterruptedException {

    UsageReachedSet uReached = (UsageReachedSet) pReached;
    UsageContainer container = uReached.getUsageContainer();
    Set<SingleIdentifier> processedUnsafes = new HashSet<>();

    logger.log(Level.INFO, ("Perform US refinement: " + i++));
    int originUnsafeSize = container.getTotalUnsafeSize();
    if (lastFalseUnsafeSize == -1) {
      lastFalseUnsafeSize = originUnsafeSize;
    }
    int counter = lastFalseUnsafeSize - originUnsafeSize;
    boolean newPrecisionFound = false;

    sendUpdateSignal(PredicateRefinerAdapter.class, pReached);
    sendUpdateSignal(PointIterator.class, container);

    Iterator<SingleIdentifier> iterator = container.getUnrefinedUnsafeIterator();
    boolean isPrecisionChanged = false;
    AbstractState firstState = pReached.getFirstState();
    AdjustablePrecision finalPrecision = (AdjustablePrecision) pReached.getPrecision(firstState);

    while (iterator.hasNext()) {
      SingleIdentifier currentId = iterator.next();

      RefinementResult result = wrappedRefiner.performBlockRefinement(currentId);
      newPrecisionFound |= result.isFalse();

      AdjustablePrecision info = result.getPrecision();

      if (!info.isEmpty()) {
        AdjustablePrecision updatedPrecision;
        if (precisionMap.containsKey(currentId)) {
          updatedPrecision = precisionMap.get(currentId).add(info);
        } else {
          updatedPrecision = info;
        }
        precisionMap.put(currentId, updatedPrecision);
        finalPrecision = finalPrecision.add(updatedPrecision);
        isPrecisionChanged = true;
      }

      if (result.isTrue()) {
        container.setAsRefined(currentId, result);
        processedUnsafes.add(currentId);
      } else if (hideFilteredUnsafes && result.isFalse() && !isPrecisionChanged) {
        // We do not add a precision, but consider the unsafe as false
        // set it as false now, because it will occur again, as precision is not changed
        // We can not look at precision size here - the result can be false due to heuristics
        container.setAsFalseUnsafe(currentId);
        processedUnsafes.add(currentId);
      }
    }
    int newTrueUnsafeSize = container.getProcessedUnsafeSize();
    counter += (newTrueUnsafeSize - lastTrueUnsafes);
    if (counter >= precisionReset) {
      Precision p = pReached.getPrecision(pReached.getFirstState());
      pReached.updatePrecision(
          pReached.getFirstState(),
          Precisions.replaceByType(
              p, PredicatePrecision.empty(), Predicates.instanceOf(PredicatePrecision.class)));

      // TODO will we need other finish signal?
      // wrappedRefiner.finish(getClass());
      lastFalseUnsafeSize = originUnsafeSize;
      lastTrueUnsafes = newTrueUnsafeSize;
    }
    if (newPrecisionFound) {
      BAMPredicateCPA bamcpa = CPAs.retrieveCPA(cpa, BAMPredicateCPA.class);
      assert bamcpa != null;
      bamcpa.clearAllCaches();
      // ARGState.clearIdGenerator();
      if (totalARGCleaning) {
        transfer.cleanCaches();
      } else {
        /* MultipleARGSubtreeRemover subtreesRemover = transfer.getMultipleARGSubtreeRemover();
        subtreesRemover.cleanCaches();*/
      }
      pReached.clear();

      processedUnsafes.addAll(
          Sets.intersection(precisionMap.keySet(), container.getFalseUnsafes()));
      for (AdjustablePrecision prec :
          from(processedUnsafes).transform(precisionMap::remove).filter(Predicates.notNull())) {
        finalPrecision = finalPrecision.subtract(prec);
      }

      CFANode firstNode = AbstractStates.extractLocation(firstState);
      // Get new state to remove all links to the old ARG
      pReached.add(
          cpa.getInitialState(firstNode, StateSpacePartition.getDefaultPartition()),
          finalPrecision);

      // TODO should we signal about removed ids?

      sendFinishSignal();
    }
    // pStat.UnsafeCheck.stopIfRunning();
    if (newPrecisionFound) {
      return RefinementResult.createTrue();
    } else {
      return RefinementResult.createFalse();
    }
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    wrappedRefiner.printStatistics(pOut);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(new Stats());
    super.collectStatistics(statsCollection);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    return performBlockRefinement(pReached).isTrue();
  }
}
