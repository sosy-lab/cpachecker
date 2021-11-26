// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.worker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class VisualizationWorker extends Worker {

  private final Multimap<String, Message> messages;

  protected VisualizationWorker(LogManager pLogger) {
    super(pLogger);
    messages = ArrayListMultimap.create();
  }

  @Override
  public Optional<Message> processMessage(Message pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {
    messages.put(pMessage.getUniqueBlockId(), pMessage);
    return noResponse;
  }
}
