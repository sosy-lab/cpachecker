/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.threadmodular;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisTM;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.CPAs;

public class ThreadModularCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysisTM {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ThreadModularCPA.class);
  }

  protected final ShutdownNotifier shutdownNotifier;
  protected final ThreadModularTransferRelation transfer;
  protected final ApplyOperator applyOperator;
  protected final ThreadModularStatistics stats;

  public ThreadModularCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa);

    if (!(pCpa instanceof ConfigurableProgramAnalysisTM)) {
      throw new InvalidConfigurationException(
          "ThreadModularCPA needs CPAs that are capable for thread-modular approach");
    }
    // pConfig.inject(this, ThreadModularCPA.class);

    shutdownNotifier = pShutdownNotifier;

    stats = new ThreadModularStatistics();
    applyOperator = ((ConfigurableProgramAnalysisTM) pCpa).getApplyOperator();
    transfer =
        new ThreadModularTransferRelation(
            pCpa.getTransferRelation(),
            stats,
            pShutdownNotifier,
            applyOperator,
            pConfig);

    PredicateCPA pcpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (pcpa != null) {
      pcpa.setPrecisionAdjustmentExternalCheck(applyOperator::canBeAnythingApplied);
    }
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }

  @Override
  public ApplyOperator getApplyOperator() {
    return applyOperator;
  }
}
