/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.NoTimeMeasurement;
import org.sosy_lab.cpachecker.util.statistics.StatCpuTime.StatCpuTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public enum PropertyStats implements Statistics {
  INSTANCE;

  private Set<Property> relevantProperties = Sets.newHashSet();
  private Map<Property, StatCpuTime> refinementTime = Maps.newHashMap();
  private Map<Property, StatCounter> refinementCount = Maps.newHashMap();
  private Map<Property, StatCounter> coverageCount = Maps.newHashMap();
  private Map<Property, StatCounter> noCoverageCount = Maps.newHashMap();
  private Multimap<Property, AbstractionPredicate> loopRelatedPredicates = HashMultimap.create();
  private Multimap<Property, AbstractionPredicate> loopUnrelatedPredicates = HashMultimap.create();

  public synchronized void clear() {
    refinementCount.clear();
    refinementTime.clear();
    coverageCount.clear();
    noCoverageCount.clear();
    relevantProperties.clear();
    loopRelatedPredicates.clear();
    loopUnrelatedPredicates.clear();
  }

  private final Comparator<Property> propertyRefinementComparator = new Comparator<Property>() {

    @Override
    public int compare(Property p1, Property p2) {
      Optional<StatCounter> p1refCount = getRefinementCount(p1);
      Optional<StatCounter> p2refCount = getRefinementCount(p2);
      Optional<StatCpuTime> p1refTime = getRefinementTime(p1);
      Optional<StatCpuTime> p2refTime = getRefinementTime(p2);

      // -1 : P1 is cheaper
      // +1 : P1 is more expensive

      if (p1refTime.isPresent()) {
        if (!p2refTime.isPresent()) {
          return 1;
        }

        try {
          if (p1refTime.get().getCpuTimeSum().asMillis() < p2refTime.get().getCpuTimeSum().asMillis()) {
            return -1;
          } else if (p1refTime.get().getCpuTimeSum().asMillis() > p2refTime.get().getCpuTimeSum().asMillis()) {
            return 1;
          }
        } catch (NoTimeMeasurement e) {
          return 0;
        }
      }

      if (p1refCount.isPresent()) {
        if (!p2refCount.isPresent()) {
          return 1;
        }

        if (p1refCount.get().getValue() < p2refCount.get().getValue()) {
          return -1;
        } else if (p1refCount.get().getValue() > p2refCount.get().getValue()) {
          return 1;
        }
      }

      return 0;
    }
  };

  private final Comparator<Property> propertyExplosionComparator = new Comparator<Property>() {
    @Override
    public int compare(Property p1, Property p2) {
      final double p1ExplosionFactor = PropertyStats.INSTANCE.getExplosionFactor(p1);
      final double p2ExplosionFactor = PropertyStats.INSTANCE.getExplosionFactor(p1);

      // -1 : P1 is cheaper
      // +1 : P1 is more expensive
      if (p1ExplosionFactor < p2ExplosionFactor) {
        return -1;
      } else if (p1ExplosionFactor > p2ExplosionFactor) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  public Comparator<Property> getPropertyExplosionComparator() {
    return propertyExplosionComparator;
  }

  public Comparator<Property> getPropertyRefinementComparator() {
    return propertyRefinementComparator;
  }

  public interface StatHandle extends AutoCloseable{
    @Override
    public void close();
  }

  public synchronized Optional<StatCpuTime> getRefinementTime(Property pProperty) {
    StatCpuTime result = refinementTime.get(pProperty);
    if (result == null) {
      return Optional.absent();
    }
    return Optional.of(result);
  }

  public synchronized Optional<StatCounter> getRefinementCount(Property pProperty) {
    StatCounter result = refinementCount.get(pProperty);
    if (result == null) {
      return Optional.absent();
    }
    return Optional.of(result);
  }

  public synchronized Optional<Integer> getLoopRelatedPredicates(Property pProperty) {
    return Optional.of(loopRelatedPredicates.get(pProperty).size());
  }

  public Set<Property> getRelevantProperties() {
    return relevantProperties;
  }

  public synchronized double getExplosionFactor(Property pProperty) {
    final StatCounter covCount = coverageCount.get(pProperty);
    final StatCounter noCovCount = noCoverageCount.get(pProperty);

    if (covCount == null || noCovCount == null) {
      return 0;
    }

    int divBy = covCount.getValue() + noCovCount.getValue();
    if (divBy == 0) {
      return 0;
    }

    return (double) noCovCount.getValue() / (double) divBy;
  }

  public synchronized void signalStopOperatorResult(final Set<? extends Property> pProperties, boolean pCoverage) {
    for (Property p: pProperties) {
      StatCounter counter;
      if (pCoverage) {
        counter = coverageCount.get(p);
        if (counter == null) {
          counter = new StatCounter(p.toString());
          coverageCount.put(p, counter);
        }
      } else {
        counter = noCoverageCount.get(p);
        if (counter == null) {
          counter = new StatCounter(p.toString());
          noCoverageCount.put(p, counter);
        }
      }
      counter.inc();
    }
  }

  public synchronized StatHandle startRefinement(final Set<Property> pProperties) {

    final List<StatCpuTimer> timers = Lists.newArrayList();

    for (Property p: pProperties) {
      StatCounter counter = refinementCount.get(p);
      if (counter == null) {
        counter = new StatCounter(p.toString());
        refinementCount.put(p, counter);
      }
      counter.inc();

      StatCpuTime time = refinementTime.get(p);
      if (time == null) {
        time = new StatCpuTime();
        refinementTime.put(p, time);
      }
      timers.add(time.start());
    }

    return new StatHandle() {
      @Override
      public void close() {
        for (StatCpuTimer t: timers) {
          t.stop();
        }
      }
    };
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    TreeSet<Property> properties = Sets.newTreeSet(new Comparator<Property>() {
      @Override
      public int compare(Property pO1, Property pO2) {
        return pO1.toString().compareTo(pO2.toString());
      }
    });

    for (Property p: properties) {
      StatisticsUtils.write(pOut, 1, 50, p.toString(), "");

      StatisticsUtils.write(pOut, 2, 50, "Coverage Ratio", getExplosionFactor(p));
    }
  }

  @Override
  public String getName() {
    return "Property Statistics";
  }

  public void signalRelevancesOfProperties(ImmutableSet<? extends SafetyProperty> pRelevant) {
    relevantProperties.addAll(pRelevant);
  }

  public void trackPropertyPredicate(AbstractionPredicate pPredicate, Pair<CFANode, Integer> pKey,
      boolean pLoopRelated, Set<Property> pPropertiesAtTarget) {

    for (Property prop: pPropertiesAtTarget) {
      if (pLoopRelated) {
        loopRelatedPredicates.put(prop, pPredicate);
      } else {
        loopUnrelatedPredicates.put(prop, pPredicate);
      }
    }
  }

}
