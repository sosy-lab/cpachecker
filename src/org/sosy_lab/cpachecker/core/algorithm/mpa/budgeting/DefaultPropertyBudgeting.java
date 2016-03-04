/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa.budgeting;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

@Options(prefix="analysis.mpa.budget")
public class DefaultPropertyBudgeting implements PropertyBudgeting {

  @Option(secure=true, name="limit.numRefinements",
      description="Disable a property after a specific number of refinements was exhausted.")
  private int refinementsLimit = -1;

  @Option(secure=true, name="limit.loopRelatedPrecisionElements",
      description="Disable a property after a specific number of loop-related precision elements is required.")
  private int loopRelatedPrecisionElementsLimit = -1;

  @Option(secure=true, name="limit.refinementsTimesMore",
      description="Disable a property after it has x times more refinements compared to another property (that has at least one element).")
  private int refinementsTimesMoreLimit = -1;

  @Option(secure=true, name="limit.automataStateExplosionPercent",
      description="Disable a property after x times more specification automata states cannot be merged/are not covered.")
  private int automataStateExplosionPercent = -1;

  @Option(secure=true, name="limit.avgRefineTime",
      description="Disable a property after the avg. time for refinements was exhausted.")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
    defaultUserUnit=TimeUnit.MILLISECONDS, min=-1)
  private TimeSpan avgRefineTimeLimit = TimeSpan.ofNanos(-1);

  @Option(secure=true, name="limit.totalRefineTime",
      description="Disable a property after a specific time (total) for refinements was exhausted.")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
    defaultUserUnit=TimeUnit.MILLISECONDS, min=-1)
  private TimeSpan totalRefineTimeLimit = TimeSpan.ofNanos(-1);

  private final LogManager logger;

  private final int budgetFactor;

  public DefaultPropertyBudgeting(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    logger = pLogger;
    budgetFactor = 1;
  }

  private DefaultPropertyBudgeting(LogManager pLogger, TimeSpan pTotalRefineTimeLimit,
      TimeSpan pAvgRefineTimeLimit, int pRefinementsLimit, int pBudgetFactor) {
    logger = pLogger;
    totalRefineTimeLimit = pTotalRefineTimeLimit;
    avgRefineTimeLimit = pAvgRefineTimeLimit;
    refinementsLimit = pRefinementsLimit;
    budgetFactor = pBudgetFactor;
  }

  private Pair<Integer, Integer> maxInfeasibleCexFor(Set<? extends Property> pProperties) {
    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
    return argCpa.getCexSummary().getMaxInfeasibleCexCountFor(pProperties);
  }

  @Override
  public boolean isTargetBudgedExhausted(Property pProperty) {

    Pair<Integer, Integer> maxInfeasibleCexs = maxInfeasibleCexFor(ImmutableSet.of(pProperty));

    if (refinementsTimesMoreLimit > 0) {
      if (maxInfeasibleCexs.getSecond() > 0) {
        if (maxInfeasibleCexs.getFirst() > 0) {
          if (refinementsLimit > maxInfeasibleCexs.getFirst()) {
            float factor = (float) maxInfeasibleCexs.getFirst() / (float) maxInfeasibleCexs.getSecond();
            if (factor > refinementsTimesMoreLimit) {
              return true;
            }
          }
        }
      }
    }

    if (automataStateExplosionPercent > 0) {
      double percent = PropertyStats.INSTANCE.getExplosionPercent(pProperty);
      if ((100 * percent) > automataStateExplosionPercent) {
        return true;
      }
    }

    if (refinementsLimit > 0
        && maxInfeasibleCexs.getFirst() >= (refinementsLimit * budgetFactor)) {
      return true;
    }

    if (loopRelatedPrecisionElementsLimit > 0) {
      Optional<Integer> related = PropertyStats.INSTANCE.getLoopRelatedPredicates(pProperty);
      if (related.isPresent()) {
        if (related.get() > (loopRelatedPrecisionElementsLimit * budgetFactor)) {
          logger.logf(Level.INFO, "Loop related precision limit exhausted for %s", pProperty.toString());
          return true;
        }
      }
    }

    if (avgRefineTimeLimit.asMillis() > 0
     || totalRefineTimeLimit.asMillis() > 0) {
      try {
        Optional<StatCpuTime> t = PropertyStats.INSTANCE.getRefinementTime(pProperty);
        if (t.isPresent()) {
          StatCpuTime s = t.get();
          if (s.getIntervals() > 0) {
            final long avgMsec = s.getCpuTimeSum().asMillis()  / s.getIntervals();
            logger.logf(Level.INFO, "Precision refinement time (msec) for %s: %d avg, %d total",
                pProperty.toString(), avgMsec, s.getCpuTimeSum().asMillis());

            if (avgRefineTimeLimit.asMillis() > 0
                && avgMsec > avgRefineTimeLimit.asMillis() * budgetFactor) {
              logger.log(Level.INFO, "Exhausted avg. refine. time of property " + pProperty.toString());
              return true;
            }

            if (totalRefineTimeLimit.asMillis() > 0
                && s.getCpuTimeSum().asMillis() > totalRefineTimeLimit.asMillis() * budgetFactor) {
              logger.log(Level.INFO, "Exhausted total refine. time of property " + pProperty.toString());
              return true;
            }
          }
        }
      } catch (NoTimeMeasurement e) {
      }
    }

    return false;
  }

  @Override
  public PropertyBudgeting getBudgetTimesTwo() {
    return new DefaultPropertyBudgeting(logger, totalRefineTimeLimit, avgRefineTimeLimit,
        refinementsLimit, budgetFactor * 2);
  }

  @Override
  public boolean isTransitionBudgedExhausted(Property pForProperty) {

    if (automataStateExplosionPercent > 0) {
      double percent = 100 * PropertyStats.INSTANCE.getExplosionPercent(pForProperty);
      if (percent > automataStateExplosionPercent) {
        logger.logf(Level.INFO, "Exhausted automaton explosion percent of property %s (%f)", pForProperty.toString(), percent);
        return true;
      }
    }

    return false;
  }

}
