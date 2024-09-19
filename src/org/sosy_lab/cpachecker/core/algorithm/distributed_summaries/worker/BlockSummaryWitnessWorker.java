// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryResultMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class BlockSummaryWitnessWorker extends BlockSummaryWorker {

  private final BlockSummaryConnection connection;
  private final Map<String, BlockSummaryPostConditionMessage> messages = new HashMap<>();
  private final DeserializeOperator deserialize;
  private static FormulaManagerView view;
  private final CFA cfa;
  private final Path witnessOutput;
  private boolean shutdown;

  BlockSummaryWitnessWorker(
      String pId,
      BlockSummaryAnalysisOptions pOptions,
      BlockSummaryConnection pConnection,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      ShutdownManager pShutdownManager,
      LogManager pLogger)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    super("witness-worker-" + pId, pLogger);
    connection = pConnection;

    witnessOutput = pOptions.getWitnessOutput();

    Configuration forwardConfiguration =
        Configuration.builder().loadFromFile(pOptions.getForwardConfiguration()).build();

    DCPAAlgorithm alg =
        new DCPAAlgorithm(
            pLogger, pBlock, pCFA, pSpecification, forwardConfiguration, pShutdownManager);

    deserialize = alg.getDCPA().getDeserializeOperator();

    PredicateCPA pcpa =
        CPAs.retrieveCPAOrFail(alg.getDCPA().getCPA(), PredicateCPA.class, getClass());
    initializeView(pcpa);

    cfa = pCFA;
    shutdown = false;
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage)
      throws InterruptedException, IOException, SolverException, CPAException {

    // Determine the type of message and process accordingly
    switch (pMessage.getType()) {
      case BLOCK_POSTCONDITION -> {
        // Store the latest post-condition message for each block
        BlockSummaryPostConditionMessage postConditionMessage =
            (BlockSummaryPostConditionMessage) pMessage;
        messages.merge(postConditionMessage.getBlockId(), postConditionMessage,
            (a, b) -> a.getTimestamp().isBefore(b.getTimestamp()) ? b : a);
      }
      case FOUND_RESULT -> {
        // Process results from the analysis
        BlockSummaryResultMessage msg = (BlockSummaryResultMessage) pMessage;
        Result result = msg.getResult();
        switch (result) {
          case FALSE -> {
            // Extract the final node ID from metadata and generate violation witness
            String metadata = msg.getMetadata();
            int finalNodeId = extractFinalNodeIdFromMetadata(metadata);
            generateViolationWitnessFromPath(metadata, finalNodeId);
          }
          case TRUE -> {
            // Process and save invariants for true results
            List<Map<String, Object>> invariants = new ArrayList<>();
            Path sourceFilePath = null;
            for (BlockSummaryPostConditionMessage message : messages.values()) {
              AbstractState state = deserialize.deserialize(message);
              BooleanFormula formula = convert(state);

              String cCode = convertFormulaToCExpression(formula);
              List<Map<String, Object>> invariantList = createInvariants(cCode, state);
              invariants.addAll(invariantList);
              if (sourceFilePath == null) {
                sourceFilePath = Paths.get(getSourceFilePath(state));
              }
            }
            if (sourceFilePath != null) {
              saveInvariantsToYaml(invariants, sourceFilePath);
            }
          }
          default -> {
          }
        }
        shutdown = true;
      }
      case ERROR -> shutdown = true;
      default -> {}
    }

    return ImmutableSet.of();
  }

  // Method to extract the final node ID from metadata string
  private int extractFinalNodeIdFromMetadata(String metadata) {
    List<String> transitions = Splitter.on(',').splitToList(metadata);
    String lastTransition = transitions.get(transitions.size() - 1);
    List<String> nodeIds = Splitter.on("->").splitToList(lastTransition);
    return Integer.parseInt(nodeIds.get(1).trim()); // The target node of the last transition
  }

  // Method to generate a violation witness from the error path
  private void generateViolationWitnessFromPath(String errorPath, int finalNodeId)
      throws IOException {
    List<String> transitions = Splitter.on(',').splitToList(errorPath);
    List<Map<String, Object>> segments = new ArrayList<>();

    CFANode currentNode = null;
    Path sourceFilePath = null;
    boolean finalNodeAdded = false;

    for (String transition : transitions) {
      List<String> nodeIds = Splitter.on("->").splitToList(transition);

      int sourceNodeId = Integer.parseInt(nodeIds.get(0).trim());
      int targetNodeId = Integer.parseInt(nodeIds.get(1).trim());

      // Find the source node if not already found
      if (currentNode == null || currentNode.getNodeNumber() != sourceNodeId) {
        currentNode = findNodeById(sourceNodeId);
        if (currentNode == null) {
          continue;
        }
      }

      // Match and find the next node
      CFANode nextNode = findNextNode(currentNode, targetNodeId);
      if (nextNode != null) {
        boolean isFinalNode = (targetNodeId == finalNodeId);

        // Mark finalNodeAdded if it's the final node
        if (isFinalNode) {
          finalNodeAdded = true;
        }

        if (sourceFilePath == null) {
          sourceFilePath = Paths.get(getSourceFilePathFromNode(currentNode));
        }

        // Create the segment for the matched node
        Map<String, Object> segment =
            createSegmentForNode(currentNode, nextNode, sourceFilePath.toString(), isFinalNode);
        if (segment != null) {
          segments.add(segment);
        }

        currentNode = nextNode;
      }
    }

    // Ensure the final node is added as the last segment if not already added
    if (!finalNodeAdded) {
      CFANode finalNode = findNodeById(finalNodeId);
      if (finalNode != null) {
        Map<String, Object> finalSegment =
            createFinalSegmentForNode(finalNode, sourceFilePath.toString());
        if (finalSegment != null) {
          segments.add(finalSegment);
        }
      }
    }

    // Create the YAML structure for the violation witness
    List<Map<String, Object>> content = new ArrayList<>();
    for (Map<String, Object> segment : segments) {
      Map<String, Object> segmentWrapper = new LinkedHashMap<>();
      segmentWrapper.put("segment", List.of(segment)); // Nest each segment under 'segment'
      content.add(segmentWrapper);
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("content", content);

    if (sourceFilePath != null) {
      saveViolationWitnessToYaml(data, sourceFilePath);
    } else {
      throw new IOException("Could not determine the source file path.");
    }
  }

  // Method to create a segment for a node
  private Map<String, Object> createSegmentForNode(
      CFANode node, CFANode nextNode, String inputFileName, boolean isFinalNode) {
    Map<String, Object> location = getLocationFromNode(node, inputFileName);

    if (location.isEmpty() && !isFinalNode) {
      return null;
    }

    Map<String, Object> waypoint = createWaypointForNode(node, location, isFinalNode, nextNode);
    if (waypoint == null) {
      return null;
    }

    Map<String, Object> segment = new LinkedHashMap<>();
    segment.put("waypoint", waypoint);

    return segment;
}

  // Method to create a waypoint for a node
  private Map<String, Object> createWaypointForNode(
      CFANode node, Map<String, Object> location, boolean isFinalNode, CFANode nextNode) {
    if (location.isEmpty()) {
      return null;
    }

    location.remove("function");

    Map<String, Object> waypoint = new LinkedHashMap<>();
    waypoint.put("action", "follow");

    // If it's the final node, set type as "target", otherwise check for branching
    if (isFinalNode) {
      waypoint.put("type", "target");
    } else if (node.getNumLeavingEdges() > 1) {
      waypoint.put("type", "branching");
      boolean isTrueBranch = isTrueBranch(node, nextNode);
      Map<String, Object> constraint = new LinkedHashMap<>();
      constraint.put("value", isTrueBranch ? "true" : "false");
      waypoint.put("constraint", constraint);
    }

    waypoint.put("location", location);

    return waypoint;
  }

  // Method to create the final segment for a node
  private Map<String, Object> createFinalSegmentForNode(CFANode node, String inputFileName) {
    Map<String, Object> segment = new LinkedHashMap<>();
    Map<String, Object> location = getLocationFromNode(node, inputFileName);

    if (location.isEmpty()) {
      return null;
    }

    Map<String, Object> waypoint = new LinkedHashMap<>();
    waypoint.put("action", "follow");
    waypoint.put("type", "target");
    waypoint.put("location", location);

    segment.put("waypoint", waypoint);
    return segment;
  }

  // Method to find a node by its ID
  private CFANode findNodeById(int nodeId) {
    CFANode entryNode = cfa.getMainFunction();
    return traverseAndFindNode(entryNode, nodeId, new HashSet<>());
  }

  // Method to traverse and find a node by its ID
  private CFANode traverseAndFindNode(CFANode currentNode, int nodeId, Set<CFANode> visited) {
    if (currentNode.getNodeNumber() == nodeId) {
      return currentNode;
    }

    if (!visited.add(currentNode)) {
      return null;
    }

    for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
      CFANode successor = currentNode.getLeavingEdge(i).getSuccessor();
      CFANode result = traverseAndFindNode(successor, nodeId, visited);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  // Method to find the next node given a current node and target node ID
  private CFANode findNextNode(CFANode currentNode, int targetNodeId) {
    for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
      CFANode successor = currentNode.getLeavingEdge(i).getSuccessor();
      if (successor.getNodeNumber() == targetNodeId) {
        return successor;
      }
    }
    return null;
  }

  // Method to check if a branch is a "true" branch
  private boolean isTrueBranch(CFANode node, CFANode nextNode) {
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      if (edge.getSuccessor().equals(nextNode)) {
        // For branching, assume that "true" corresponds to the main branch and "false" to the other
        return edge.getDescription().contains("true");
      }
    }
    return false;
  }

  // Method to get the location information from a node
  private Map<String, Object> getLocationFromNode(CFANode node, String inputFileName) {
    Map<String, Object> location = new LinkedHashMap<>();

    if (node.getNumEnteringEdges() > 0) {
      CFAEdge edge = node.getEnteringEdge(0);
      FileLocation fileLocation = edge.getFileLocation();

      if (fileLocation != null && fileLocation.isRealLocation()) {
        location.put("file_name", inputFileName);
        location.put("line", fileLocation.getStartingLineNumber());
        location.put("function", edge.getPredecessor().getFunctionName());
      }
    }

    // If location is incomplete or unavailable, return an empty map
    if (location.isEmpty()) {
      location = new LinkedHashMap<>();
    }

    return location;
  }

  // Method to get the source file path from a node
  private String getSourceFilePathFromNode(CFANode node) {
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      FileLocation fileLocation = edge.getFileLocation();
      if (fileLocation != null && fileLocation.isRealLocation()) {
        return fileLocation.getFileName().toString();
      }
    }
    throw new IllegalStateException("Cannot determine source file path from the given node.");
  }

  // Method to save the violation witness to a YAML file
  private void saveViolationWitnessToYaml(Map<String, Object> data, Path sourceFilePath)
      throws IOException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);

    Map<String, Object> metadata = createMetadata(sourceFilePath);
    List<Map<String, Object>> document = new ArrayList<>();

    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("entry_type", "violation_sequence");
    entry.put("metadata", metadata);
    entry.put("content", data.get("content"));
    document.add(entry);
    try (Writer writer = Files.newBufferedWriter(witnessOutput, java.nio.charset.StandardCharsets.UTF_8)) {
      yaml.dump(document, writer);
    }
  }

  // Method to save invariants to a YAML file
  private void saveInvariantsToYaml(List<Map<String, Object>> invariants, Path fileName)
      throws IOException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);

    Map<String, Object> metadata = createMetadata(fileName);
    List<Map<String, Object>> invariantEntries = new ArrayList<>();

    for (Map<String, Object> invariant : invariants) {
      Map<String, Object> invariantEntry = new HashMap<>();
      invariantEntry.put("invariant", invariant);
      invariantEntries.add(invariantEntry);
    }

    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("entry_type", "invariant_set");
    entry.put("metadata", metadata);
    entry.put("content", invariantEntries);

    // Wrap entry in a list to ensure proper YAML structure
    List<Map<String, Object>> document = new ArrayList<>();
    document.add(entry);

    try (Writer writer = Files.newBufferedWriter(witnessOutput, java.nio.charset.StandardCharsets.UTF_8)) {
      yaml.dump(document, writer);
    }
  }

  // Method to create metadata for the YAML output
  private Map<String, Object> createMetadata(Path fileName) throws IOException {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("format_version", "2.0");
    metadata.put("uuid", UUID.randomUUID().toString());
    metadata.put(
        "creation_time",
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));
    metadata.put("producer", createProducer());
    metadata.put("task", createTask(fileName));
    return metadata;
}


  // Method to create producer metadata
  private Map<String, Object> createProducer() {
    Map<String, Object> producer = new HashMap<>();
    producer.put("name", "CPAchecker");
    producer.put("version", "3.0");
    return producer;
}

  // Method to create task metadata, including file hashes
  private Map<String, Object> createTask(Path fileName) throws IOException {
    Map<String, Object> task = new HashMap<>();
    task.put("input_files", List.of(fileName.toString()));
    task.put("input_file_hashes", Map.of(fileName.toString(), computeSHA256(fileName)));
    task.put("specification", "CHECK( init(main()), LTL(G ! call(reach_error())) )");
    task.put("data_model", "ILP32");
    task.put("language", "C");
    return task;
}

  // Method to compute SHA-256 hash of a file
  private String computeSHA256(Path fileName) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
    try (InputStream fis = Files.newInputStream(fileName)) {
      byte[] buffer = new byte[1024];
      int n;
      while ((n = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, n);
      }
    }
    byte[] hash = digest.digest();
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      hexString.append(String.format("%02x", b));
    }
    return hexString.toString();
}

  // Method to create invariants from a given state
  private List<Map<String, Object>> createInvariants(String cCode, AbstractState state)
      throws IOException {
    List<Map<String, Object>> invariants = new ArrayList<>();
    CFANode node = getNodeFromState(state);

    if (node != null) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        FileLocation fileLocation = edge.getFileLocation();
        if (fileLocation != null && fileLocation.isRealLocation()) {
          String fileNameStr = fileLocation.getFileName().toString();
          Path fileName = Paths.get(fileNameStr);
          int lineNumber = fileLocation.getStartingLineNumber();
          String functionName = node.getFunctionName();

          Map<String, Object> location = new HashMap<>();
          location.put("file_name", fileName.toString());
          location.put("line", lineNumber);
          location.put("function", functionName);

          Map<String, Object> invariant = new HashMap<>();
          invariant.put("type", determineInvariantType(fileName, lineNumber));
          invariant.put("location", location);
          invariant.put("value", cCode);
          invariant.put("format", "c_expression");

          invariants.add(invariant);
        }
      }
    }

    return invariants;
}

  // Method to determine the type of invariant (e.g., loop or location invariant)
  private String determineInvariantType(Path fileName, int lineNumber) throws IOException {
    try (BufferedReader reader =
        Files.newBufferedReader(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
      String line;
      int currentLine = 0;
      while ((line = reader.readLine()) != null) {
        currentLine++;
        if (currentLine == lineNumber) {
          if (line.contains("for") || line.contains("while") || line.contains("do")) {
            return "loop_invariant";
          } else {
            return "location_invariant";
          }
        }
      }
    }
    return "location_invariant"; // Default to location_invariant if not found
  }

  // Method to get the source file path from an abstract state
  private String getSourceFilePath(AbstractState state) {
    CFANode node = getNodeFromState(state);
    if (node != null) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        FileLocation fileLocation = edge.getFileLocation();
        if (fileLocation != null && fileLocation.isRealLocation()) {
          return fileLocation.getFileName().toString();
        }
      }
    }
    throw new IllegalStateException("Cannot determine source file path from the given state.");
}

  // Method to extract the node from an abstract state
  private CFANode getNodeFromState(AbstractState state) {
    return AbstractStates.extractLocation(state);
}

  // Method to convert an abstract state to a Boolean formula
  private BooleanFormula convert(AbstractState state) {
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(state, PredicateAbstractState.class);
    if (predicateState == null) {
      throw new IllegalArgumentException(
          "Provided state does not contain a PredicateAbstractState.");
    }
    return view.uninstantiate(predicateState.getPathFormula().getFormula());
}

  // Method to convert a Boolean formula to a C expression string
  private String convertFormulaToCExpression(BooleanFormula formula) throws InterruptedException {
    FormulaToCExpressionConverter converter = new FormulaToCExpressionConverter(view);
    return converter.formulaToCExpression(formula);
}

  private static void initializeView(PredicateCPA pcpa) {
    if (view == null) {
      view = pcpa.getSolver().getFormulaManager();
    }
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
}

  @Override
  public boolean shutdownRequested() {
    return shutdown;
}

}