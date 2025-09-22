// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.infrastructure.DssConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;

public class DssVisualizationWorker extends DssWorker {

  private final DssConnection connection;
  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final Path reportFiles;
  private boolean shutdown = false;
  private final int identifier;

  private final int numberOfBlocks;
  private int statisticMessageCount = 0;

  DssVisualizationWorker(
      String id,
      BlockGraph pBlockGraph,
      DssConnection pConnection,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      LogManager pLogger) {
    super(id, pMessageFactory, pLogger);
    identifier = Instant.now().hashCode();
    connection = pConnection;
    reportFiles = pOptions.getReportFiles();
    numberOfBlocks = pBlockGraph.getNodes().size();
    try {
      if (pOptions.getBlockCFAFile() != null) {
        pBlockGraph.export(pOptions.getBlockCFAFile());
      }
    } catch (IOException e) {
      pLogger.logException(
          Level.WARNING,
          e,
          "VisualizationWorker failed to log the BlockGraph. "
              + "The visualization might contain old data or will not work. "
              + "However, the analysis continues normally.");
    }
  }

  private void log(DssMessage pMessage) throws IOException {
    if (reportFiles != null) {
      JSON.writeJSONString(
          pMessage.asJsonWithIdentifier(identifier),
          reportFiles.resolve("M" + idGenerator.getFreshId() + ".json"));
    }
  }

  @Override
  public Collection<DssMessage> processMessage(DssMessage pMessage)
      throws InterruptedException, IOException {
    log(pMessage);
    boolean stop = false;
    while (connection.hasPendingMessages()) {
      DssMessage m = connection.read();
      log(m);
      statisticMessageCount += m.getType() == DssMessageType.STATISTIC ? 1 : 0;
      stop |= m.getType() == DssMessageType.EXCEPTION || statisticMessageCount == numberOfBlocks;
    }
    if (stop) {
      shutdown = true;
    }
    return ImmutableSet.of();
  }

  @Override
  public DssConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
