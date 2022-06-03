// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;

/** Represents a state along the currently analysed path within the StatisticsCPA domain. */
public class StatisticsState implements AbstractStateWithLocation, Partitionable {

  /** This class handles the logic of creating new states and merging them. */
  public static class StatisticsStateFactory {
    private Set<StatisticsProvider> propertyProviders = new HashSet<>();
    private boolean fixed = false;
    private boolean isAnalysis = false;
    private StatisticsData analysisData = null;
    private Map<CFAEdge, Boolean> analysisTrack = null;

    public enum FactoryAnalysisType {
      Analysis,
      MetricsQuery
    }

    public StatisticsStateFactory(FactoryAnalysisType analysisType) {
      setAnalysisType(analysisType);
    }

    public void setAnalysisType(FactoryAnalysisType analysisType) {
      switch (analysisType) {
        case Analysis:
          isAnalysis = true;
          break;
        case MetricsQuery:
          isAnalysis = false;
          break;
        default:
          throw new IllegalStateException("unknown analysisType");
      }
    }

    public void addProvider(StatisticsProvider provider) {
      checkState(!fixed, "providers are already fixed");
      checkState(propertyProviders.add(provider), "the requested provider was already added!");
    }

    private StatisticsData createInitialDataProvider() {
      fixed = true;
      return new StatisticsData(propertyProviders);
    }

    public StatisticsState createNew(CFANode node) {
      StatisticsData initialState = createInitialDataProvider();
      if (isAnalysis) {
        analysisData = initialState;
        analysisTrack = new HashMap<>();
        initialState = null;
      }
      return new StatisticsState(initialState, this, node);
    }

    public StatisticsState nextState(StatisticsState state, CFAEdge successor) {
      StatisticsData nextState = null;
      if (!isAnalysis) {
        nextState = state.data.getNextState(successor);
      } else {
        if (!analysisTrack.containsKey(successor)) {
          analysisData = analysisData.getNextState(successor);
          analysisTrack.put(successor, true);
        }
      }
      return new StatisticsState(nextState, this, successor.getSuccessor());
    }

    public StatisticsState mergedState(StatisticsState state1, StatisticsState state2) {
      assert Objects.equals(state1.getLocationNode(), state2.getLocationNode())
          : "Locations have to match!";
      return new StatisticsState(state1.data.mergeState(state2.data), this, state1.locationNode);
    }

    public boolean containsPrevious(StatisticsState state1, StatisticsState state2) {
      if (!isAnalysis) {
        throw new UnsupportedOperationException(
            "Not implemented jet. Figure out if this is already covered (see also mergedState)");
      }

      return state1.locationNode.equals(state2.locationNode);
    }

    public StatisticsData getGlobalAnalysis() {
      return analysisData;
    }
  }

  private final CFANode locationNode;
  private final StatisticsStateFactory factory;
  private final StatisticsData data;

  /** Should only be used by StatisticsStateFactory */
  private StatisticsState(
      StatisticsData data, StatisticsStateFactory factory, CFANode locationNode) {
    this.locationNode = locationNode;
    this.factory = factory;
    this.data = data;
  }

  public StatisticsData getStatistics() {
    return data;
  }

  @Override
  public CFANode getLocationNode() {
    return locationNode;
  }

  @Override
  public String toString() {
    return locationNode.toString();
  }

  @Override
  public Object getPartitionKey() {
    return this;
  }

  public StatisticsState nextState(CFAEdge pSuccessor) {
    return factory.nextState(this, pSuccessor);
  }

  public StatisticsState mergeState(StatisticsState state2) {
    return factory.mergedState(this, state2);
  }

  public boolean containsPrevious(StatisticsState state2) {
    return factory.containsPrevious(this, state2);
  }

  @Override
  public int hashCode() {
    int hash = 1;
    // hash = hash * 17 + covered.hashCode();
    hash = hash * 31 + locationNode.hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object pOther) {
    if (super.equals(pOther)) {
      return true;
    }
    StatisticsState other = (StatisticsState) pOther;
    if (other == null) {
      return false;
    }
    if (locationNode.equals(other.locationNode)
    /*&& covered.equals(other.covered)*/ ) {
      return true;
    }
    return false;
  }
}
