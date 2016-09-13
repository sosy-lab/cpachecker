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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

import java.util.Collection;

@Options(prefix = "cpa.uninitvars")
public class UninitializedVariablesCPA extends AbstractCPA implements StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(UninitializedVariablesCPA.class);
  }

  @Option(secure=true, description="print warnings during analysis when uninitialized variables are used")
  private String printWarnings = "true";
  @Option(secure=true, name="merge", values={"sep", "join"},
      description="which merge operator to use for UninitializedVariablesCPA?")
  private String mergeType = "sep";
  @Option(secure=true, name="stop", values={"sep", "join"},
      description="which stop operator to use for UninitializedVariablesCPA?")
  private String stopType = "sep";

  private final UninitializedVariablesStatistics statistics;

  private UninitializedVariablesCPA(Configuration config) throws InvalidConfigurationException {
    super("irrelevant", "irrelevant", new UninitializedVariablesDomain(), null);
    config.inject(this);
    statistics = new UninitializedVariablesStatistics(printWarnings);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new UninitializedVariablesState(pNode.getFunctionName());
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new UninitializedVariablesTransferRelation(printWarnings);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

}
