// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.visualization;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

@Options
public class MessageLogger {

  @Option(description = "output file for visualizing message exchange")
  @FileOption(Type.OUTPUT_FILE)
  private Path reportFile = Path.of("block_analysis/block_analysis.json");

  @Option(description = "output file for visualizing the block graph")
  @FileOption(Type.OUTPUT_FILE)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  private final Map<String, Multimap<String, Object>> entries;
  private final BlockGraph tree;

  public MessageLogger(BlockGraph pTree, Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    // IO.openOutputFile(reportFile, StandardCharsets.US_ASCII, StandardOpenOption.CREATE);
    entries = new HashMap<>();
    tree = pTree;
    pTree.getDistinctNodes().forEach(n -> entries.put(n.getId(), createInitialMap(n)));
  }

  private Multimap<String, Object> createInitialMap(BlockNode pNode) {
    Multimap<String, Object> map = ArrayListMultimap.create();
    map.putAll("code", Splitter.on("\n").splitToList(pNode.getCode()));
    map.putAll(
        "predecessors", transformedImmutableSetCopy(pNode.getPredecessors(), p -> p.getId()));
    map.putAll("successors", transformedImmutableSetCopy(pNode.getSuccessors(), p -> p.getId()));
    return map;
  }

  public synchronized void logTree() throws IOException {
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

  public synchronized void log(BlockSummaryMessage pMessage) throws IOException {
    if (entries.get(pMessage.getUniqueBlockId()) == null) {
      return;
    }
    Map<String, Object> messageToJSON = new HashMap<>();
    messageToJSON.put("type", pMessage.getType().name());
    messageToJSON.put("timestamp", pMessage.getTimestamp().toString());
    messageToJSON.put("from", pMessage.getUniqueBlockId());
    if (pMessage.getAbstractStateString(PredicateCPA.class).isEmpty()) {
      pMessage = BlockSummaryMessage.addEntry(pMessage, PredicateCPA.class.getName(), "true");
    }
    messageToJSON.put("payload", pMessage.getPayloadJSON());
    entries.get(pMessage.getUniqueBlockId()).put("messages", messageToJSON);
    Map<String, Map<String, Collection<Object>>> converted = new HashMap<>();
    entries.forEach((k, v) -> converted.put(k, v.asMap()));
    JSON.writeJSONString(converted, reportFile);
  }
}
