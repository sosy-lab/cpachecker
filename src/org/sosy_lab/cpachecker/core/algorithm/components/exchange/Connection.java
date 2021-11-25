// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import java.io.Closeable;
import java.io.IOException;

public interface Connection extends Closeable {

  /**
   * Wait for an incoming Message and return it.
   * @return current Message to process
   * @throws InterruptedException if thread is interrupted.
   */
  Message read() throws InterruptedException;

  /**
   * Write and broadcast a message to all connections
   * @param message Message to broadcast
   * @throws IOException if write operation fails
   * @throws InterruptedException if thread is interrupted
   */
  void write(Message message) throws IOException, InterruptedException;

}
