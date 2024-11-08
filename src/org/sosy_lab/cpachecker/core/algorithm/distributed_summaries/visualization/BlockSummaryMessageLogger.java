// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Predicate;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

@Options
public class BlockSummaryMessageLogger {

  @Option(
      name = "dss.logging.reportFiles",
      description = "output file for visualizing message exchange")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path reportFiles = Path.of("block_analysis/block_analysis");

  @Option(description = "output file for visualizing the block graph")
  @FileOption(Type.OUTPUT_FILE)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  private final BlockGraph tree;
  private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

  private final int hashCode = Instant.now().hashCode();

  public BlockSummaryMessageLogger(BlockGraph pTree, Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    tree = pTree;
  }

  public void logBlockGraph() throws IOException {
    tree.export(blockCFAFile);
  }

  // suppress warnings is fine here because error-prone does not recognize that we call
  // getEpochSeconds before accessing nanos.
  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public synchronized void log(BlockSummaryMessage pMessage, Predicate<String> pFilter)
      throws IOException {
    Map<String, Object> messageToJSON = new HashMap<>();
    messageToJSON.put("type", pMessage.getType().name());
    Optional<Instant> maybeTimestamp = pMessage.getTimestamp();
    checkState(
        !maybeTimestamp.isEmpty(),
        "Trying to log message, but timestamp in message is missing. Try turning on debug mode"
            + " for distributedSummaries.");
    Instant timestamp = maybeTimestamp.orElseThrow();
    BigInteger secondsToNano =
        BigInteger.valueOf(timestamp.getEpochSecond())
            .multiply(BigInteger.valueOf(1000000000))
            .add(BigInteger.valueOf(timestamp.getNano()));
    messageToJSON.put("timestamp", secondsToNano.toString());
    messageToJSON.put("hashCode", hashCode);
    messageToJSON.put("from", pMessage.getUniqueBlockId());
    messageToJSON.put("payload", pMessage.getPayloadJSON(pFilter));
    JSON.writeJSONString(
        messageToJSON, reportFiles.resolve("M" + ID_GENERATOR.getFreshId() + ".json"));
  }

  public synchronized void log(BlockSummaryMessage pMessage) throws IOException {
    log(pMessage, s -> s.contains("readable"));
  }
}
