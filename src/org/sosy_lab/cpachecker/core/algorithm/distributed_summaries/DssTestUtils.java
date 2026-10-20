// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import java.io.IOException;
import java.util.function.Predicate;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Helper class for Distributed Summary Synthesis tests. */
public class DssTestUtils {

  private static final String DSS_CONFIGURATION_FILE = "config/dss.properties";

  /** Create a {@link BlockOperator} and return the block-end predicate. */
  public static Predicate<CFANode> createBlockOperator(CFA cfa)
      throws InvalidConfigurationException, IOException {
    BlockOperator blockOperator = new BlockOperator();
    Configuration config =
        TestUtils.configurationForTest().loadFromFile(DSS_CONFIGURATION_FILE).build();
    config.inject(blockOperator);
    try {
      blockOperator.setCFA(cfa);
    } catch (CPAException e) {
      // if blockOperator.setCFA throws a CPAException, this is because of an invalid configuration
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }

    return n -> blockOperator.isBlockEnd(n, -1);
  }
}
