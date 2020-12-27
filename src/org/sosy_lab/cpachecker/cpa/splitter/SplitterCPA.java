// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
