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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockTree;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message.MessageType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class MessageLogger {

  private static final Path reportFile = Path.of("./output/block_analysis.json");

  private final Map<String, Multimap<String, Object>> entries;
  private final Solver solver;

  public MessageLogger(BlockTree pTree, Solver pSolver) {
    // IO.openOutputFile(reportFile, StandardCharsets.US_ASCII, StandardOpenOption.CREATE);
    entries = new HashMap<>();
    solver = pSolver;
    pTree.getDistinctNodes().forEach(n -> entries.put(n.getId(), createInitialMap(n)));
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

  public synchronized void log(Message pMessage)
      throws IOException, SolverException, InterruptedException {
    Map<String, Object> messageToJSON = new HashMap<>();
    messageToJSON.put("type", pMessage.getType().name());
    messageToJSON.put("timestamp", pMessage.getTimestamp());
    messageToJSON.put("from", pMessage.getUniqueBlockId());
    String payload = pMessage.getPayload();
    if (pMessage.getType() == MessageType.ERROR_CONDITION || pMessage.getType() == MessageType.BLOCK_POSTCONDITION) {
      BooleanFormula formula = solver.getFormulaManager().parse(payload);
      payload = BooleanFormulaParser.parse(formula) + "(sat: " +
          !solver.isUnsat(formula)  + ")";
    }
    messageToJSON.put("payload", payload);
    messageToJSON.put("additionalInformation", pMessage.getAdditionalInformation());
    entries.get(pMessage.getUniqueBlockId()).put("messages", messageToJSON);
    Map<String, Map<String, Collection<Object>>> converted = new HashMap<>();
    entries.forEach((k, v) -> converted.put(k, v.asMap()));
    JSON.writeJSONString(converted, reportFile);
  }

}
