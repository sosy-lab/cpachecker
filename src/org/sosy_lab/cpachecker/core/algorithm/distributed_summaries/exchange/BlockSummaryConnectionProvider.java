// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.google.common.collect.ImmutableList;
import java.io.IOException;

public interface BlockSummaryConnectionProvider<T extends BlockSummaryConnection> {

  /**
   * Creates multiple distinct {@link BlockSummaryConnection Connections}.
   *
   * @param connections number of connections to create
   * @return list of created Connections
   * @throws IOException if an IOException occurs during Connection creation
   */
  ImmutableList<T> createConnections(int connections) throws IOException;
}
