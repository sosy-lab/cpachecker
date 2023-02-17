// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

@Options
public class MessageLogger {

  @Option(description = "output file for visualizing message exchange")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path reportFiles = Path.of("block_analysis/block_analysis");

  @Option(description = "output file for visualizing the block graph")
  @FileOption(Type.OUTPUT_FILE)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  private final BlockGraph tree;
  private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

  private final int hashCode = Instant.now().hashCode();

  public MessageLogger(BlockGraph pTree, Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    tree = pTree;
  }

  public synchronized void logBlockGraph() throws IOException {
    Map<String, Map<String, List<String>>> treeMap = new HashMap<>();
    tree.getDistinctNodes()
        .forEach(
            n -> {
              Map<String, List<String>> attributes = new HashMap<>();
              attributes.put("code", Splitter.on("\n").splitToList(n.getCode()));
              attributes.put(
                  "predecessors",
                  transformedImmutableListCopy(n.getPredecessors(), p -> p.getId()));
              attributes.put(
                  "successors", transformedImmutableListCopy(n.getSuccessors(), p -> p.getId()));
              treeMap.put(n.getId(), attributes);
            });
    JSON.writeJSONString(treeMap, blockCFAFile);
  }

  // suppress warnings is fine here because error-prone does not recognize that we call
  // getEpochSeconds before accessing nanos.
  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public synchronized void log(BlockSummaryMessage pMessage) throws IOException {
    Map<String, Object> messageToJSON = new HashMap<>();
    messageToJSON.put("type", pMessage.getType().name());
    BigInteger secondsToNano =
        BigInteger.valueOf(pMessage.getTimestamp().getEpochSecond())
            .multiply(BigInteger.valueOf(1000000000))
            .add(BigInteger.valueOf(pMessage.getTimestamp().getNano()));
    messageToJSON.put("timestamp", secondsToNano.toString());
    messageToJSON.put("hashCode", hashCode);
    messageToJSON.put("from", pMessage.getUniqueBlockId());
    if (pMessage.getAbstractState(PredicateCPA.class).isEmpty()) {
      pMessage = BlockSummaryMessage.addEntry(pMessage, PredicateCPA.class.getName(), "true");
    }
    messageToJSON.put("payload", pMessage.getPayloadJSON(s -> s.contains("PredicateCPA")));
    JSON.writeJSONString(
        messageToJSON, reportFiles.resolve("M" + ID_GENERATOR.getFreshId() + ".json"));
  }
}
