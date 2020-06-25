/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.splitter;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.splitter.SplitInfoState.SequenceSplitInfoState;

@Options(prefix = "program.splitter")
public class SplitterCPA extends AbstractCPA {

  @Option(secure = true, name = "max", description = "maximal number")
  @IntegerOption(min = 2)
  int maxSplits = 2;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SplitterCPA.class);
  }

  private final TransferRelation transfer;

  private SplitterCPA(
      final Configuration config, final LogManager logger, final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super("SEP", "SEP", null);

    config.inject(this);
    transfer = new SplitterTransferRelation(config, logger, pShutdownNotifier, maxSplits);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new SequenceSplitInfoState(maxSplits);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  public int getMaximalSplitNumber() {
    return maxSplits;
  }
}
