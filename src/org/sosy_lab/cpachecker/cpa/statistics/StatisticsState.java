/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.statistics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Represents a state along the currently analysed path within the StatisticsCPA domain.
 */
public class StatisticsState implements AbstractStateWithLocation, AbstractQueryableState, Partitionable, Serializable {

  /**
   * This class handles the logic of creating new states and merging them.
   */
  public static class StatisticsStateFactory {
    private Set<StatisticsProvider> propertyProviders = new HashSet<>();
    private boolean fixed = false;
    private boolean isAnalysis = false;
    private StatisticsData analysisData = null;
    private Map<CFAEdge, Boolean> analysisTrack = null;

    public static enum FactoryAnalysisType {
      Analysis, MetricsQuery
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
      if (fixed) {
        throw new IllegalStateException("providers are already fixed");
      }
      if (!propertyProviders.add(provider)){
        throw new IllegalStateException("the requested provider was already added!");
      }
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
      assert state1.getLocationNode() == state2.getLocationNode() : "Locations have to match!";
      return
          new StatisticsState(
              state1.data.mergeState(state2.data),
              this,
              state1.locationNode);
    }

    public boolean containsPrevious(StatisticsState state1, StatisticsState state2) {
      if (!isAnalysis) {
        throw new  UnsupportedOperationException("Not implemented jet. Figure out if this is already covered (see also mergedState)");
      }

      return state1.locationNode.equals(state2.locationNode);
    }

    public StatisticsData getGlobalAnalysis() {
      return analysisData;
    }
  }
  private static final long serialVersionUID = -801176497691618779L;


  private transient CFANode locationNode;
  private final StatisticsStateFactory factory;
  private final StatisticsData data;

  /**
   * Should only be used by StatisticsStateFactory
   */
  private StatisticsState(StatisticsData data, StatisticsStateFactory factory, CFANode locationNode) {
      this.locationNode = locationNode;
      this.factory = factory;
      this.data = data;
  }

  public StatisticsData getStatistics() {
    if (data == null) {
      return null;
    }
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
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    throw new InvalidQueryException("The Query \"" + pProperty
        + "\" currently not supported.");
  }

  @Override
  public void modifyProperty(String pModification)
      throws InvalidQueryException {
    throw new InvalidQueryException("The statistics CPA does not support modification.");
  }

  @Override
  public String getCPAName() {
    return "statistics";
  }

  @Override
  public Object evaluateProperty(String pProperty)
      throws InvalidQueryException {
    if (data == null) {
      throw new InvalidQueryException("The statistics CPA initialized for analysis does not support queries.");
    }
    // TODO: find property (the following code is invalid)...
    StatisticsDataProvider prov = data.getProperty(pProperty);
    return prov.getPropertyValue();
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
      //hash = hash * 17 + covered.hashCode();
      hash = hash * 31 + locationNode.hashCode();
      return hash;
  }

  @Override
  public boolean equals(Object pArg0) {
    if(super.equals(pArg0)) {
      return true;
    }
    StatisticsState other = (StatisticsState)pArg0;
    if (other == null) {
      return false;
    }
    if (locationNode.equals(other.locationNode)
        /*&& covered.equals(other.covered)*/){
      return true;
    }
    return false;
  }
}
