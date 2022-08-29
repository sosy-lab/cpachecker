// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import java.util.function.BiPredicate;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

public class BlockOperatorPredicate implements BiPredicate<CFANode, Integer> {

  private final BlockOperator blockOperator;

  public BlockOperatorPredicate(CFA pCfa, Configuration  pConfiguration)
      throws InvalidConfigurationException {
    blockOperator = new BlockOperator();
    pConfiguration.inject(blockOperator);
    blockOperator.setCFA(pCfa);
  }

  @Override
  public boolean test(CFANode pNode, Integer pInteger) {
    return blockOperator.isBlockEnd(pNode, pInteger);
  }
}
