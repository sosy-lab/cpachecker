// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

public class MessageLogger {

  private static final Path reportFile = Path.of("./output/block_analysis/block_analysis.json");
  private static final Path blockCFAFile = Path.of("./output/block_analysis/blocks.json");

  private final Map<String, Multimap<String, Object>> entries;
  private final BlockTree tree;
  private FormulaManagerView fmgr;

  public MessageLogger(BlockTree pTree) {
    // IO.openOutputFile(reportFile, StandardCharsets.US_ASCII, StandardOpenOption.CREATE);
    entries = new HashMap<>();
    tree = pTree;
    pTree.getDistinctNodes().forEach(n -> entries.put(n.getId(), createInitialMap(n)));
    try {
      fmgr = Solver.create(Configuration.defaultConfiguration(), LogManager.createNullLogManager(), ShutdownNotifier.createDummy()).getFormulaManager();
    } catch (InvalidConfigurationException pE) {
      fmgr = null;
    }
  }

  private Multimap<String, Object> createInitialMap(BlockNode pNode) {
    Multimap<String, Object> map = ArrayListMultimap.create();
    map.putAll("code", Splitter.on("\n").splitToList(pNode.getCode()));
    map.putAll("predecessors", pNode.getPredecessors().stream().map(p -> p.getId()).collect(
        Collectors.toSet()));
    map.putAll("successors", pNode.getSuccessors().stream().map(p -> p.getId()).collect(
        Collectors.toSet()));
    return map;
  }

  public synchronized void logTree() throws IOException {
    Map<String, Map<String, List<String>>> treeMap = new HashMap<>();
    tree.getDistinctNodes().forEach(n -> {
      Map<String, List<String>> attributes = new HashMap<>();
      attributes.put("code", Splitter.on("\n").splitToList(n.getCode()));
      attributes.put("predecessors", n.getPredecessors().stream().map(p -> p.getId()).collect(
          Collectors.toList()));
      attributes.put("successors", n.getSuccessors().stream().map(p -> p.getId()).collect(
          Collectors.toList()));
      treeMap.put(n.getId(), attributes);
    });
    JSON.writeJSONString(treeMap, blockCFAFile);
  }

  public synchronized void log(Message pMessage)
      throws IOException, SolverException, InterruptedException {
    if (entries.get(pMessage.getUniqueBlockId()) == null) {
      return;
    }
    Map<String, Object> messageToJSON = new HashMap<>();
    messageToJSON.put("type", pMessage.getType().name());
    messageToJSON.put("timestamp", pMessage.getTimestamp());
    messageToJSON.put("from", pMessage.getUniqueBlockId());
    Payload p = pMessage.getPayload();
    String message = p.get(PredicateCPA.class.getName());
    if (message != null) {
      p = new Payload(p);
      p.put(PredicateCPA.class.getName(), BooleanFormulaParser.parse(fmgr.parse(message)).toString());
    }
    messageToJSON.put("payload", p.toJSONString());
    entries.get(pMessage.getUniqueBlockId()).put("messages", messageToJSON);
    Map<String, Map<String, Collection<Object>>> converted = new HashMap<>();
    entries.forEach((k, v) -> converted.put(k, v.asMap()));
    JSON.writeJSONString(converted, reportFile);
  }

}
