// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import java.io.IOException;
import java.util.List;

public interface ConnectionProvider<T extends Connection> {

  /**
   * Create {@code connections} different Connections
   *
   * @param connections number of connections to generate
   * @return List with {@code connections} Connections
   * @throws IOException if creating the connections fails
   */
  List<T> createConnections(int connections) throws IOException;
}
