// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.path;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;

/**
 * CPA for path conditions ({@link PathCondition}). It can be configured to work with any condition
 * that implements this interface.
 */
@Options(prefix = "cpa.conditions.path")
public class PathConditionsCPA
    implements ConfigurableProgramAnalysisWithBAM, AdjustableConditionCPA, StatisticsProvider {

  @Option(secure = true, description = "The condition", name = "condition", required = true)
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cpa.conditions.path")
  private PathCondition.Factory conditionClass;

  @Option(
      secure = true,
      description =
          "Number of times the path condition may be adjusted, i.e., the path condition threshold"
              + " may be increased (-1 to always adjust)",
      name = "adjustment.threshold")
  @IntegerOption(min = -1)
  private int adjustmentThreshold = -1;

  private int performedAdjustments = 0;

  private final PathCondition condition;

  private final AbstractDomain domain = new FlatLatticeDomain();
  private final TransferRelation transfer =
      new SingleEdgeTransferRelation() {
        @Override
        public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
            AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
          return Collections.singleton(condition.getAbstractSuccessor(pState, pCfaEdge));
        }
      };

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PathConditionsCPA.class);
  }

  private PathConditionsCPA(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    condition = conditionClass.create(config);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (condition instanceof StatisticsProvider) {
      ((StatisticsProvider) condition).collectStatistics(pStatsCollection);

    } else if (condition instanceof Statistics) {
      pStatsCollection.add((Statistics) condition);
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return condition.getInitialState(pNode);
  }

  @Override
  public boolean adjustPrecision() {
    if (adjustmentThreshold == -1 || performedAdjustments < adjustmentThreshold) {
      performedAdjustments++;
      return condition.adjustPrecision();
    }
    return false;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return StopAlwaysOperator.getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public Reducer getReducer() {
    return condition.getReducer();
  }
}
