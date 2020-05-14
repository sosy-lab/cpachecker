/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.collector;

import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static java.nio.file.Files.isReadable;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;


@Options(prefix = "cpa.collector")
public class CollectorStatistics implements Statistics {


  private static final String HTML_TEMPLATE = "collector.html";
  private static final String CSS_TEMPLATE = "collector.css";
  private static final String JS_TEMPLATE = "collector.js";
  private final LogManager logger;
  private final HashMap<Integer, Object> cNodes = new HashMap<>();
  private final Multimap<Integer, Object> cEdges;
  private final HashMap<Map<Integer, Object>, Multimap<Integer, Object>>
      collectorlinkedNodesAndEdges = new HashMap<>();
  @Option(secure = true, name = "export", description = "export collector as .dot file")

  ImmutableList<String> sourceFiles;

  public CollectorStatistics(Configuration config, @Nullable
                             String pathSourceFile, LogManager pLogger)
      throws InvalidConfigurationException {
    this.logger = pLogger;

    config.inject(this, CollectorStatistics.class);

    cEdges = ArrayListMultimap.create();

    assert pathSourceFile != null;
    sourceFiles = ImmutableList.of(pathSourceFile);
  }

  // Similar to the getEdgeText method in DOTBuilder2
  private static String getEdgeText(CFAEdge edge) {
    return edge.getDescription()
        .replaceAll("\\\"", "\\\\\\\"")
        .replaceAll("\n", " ")
        .replaceAll("\\s+", " ")
        .replaceAll(" ;", ";");
  }

  @Override
  public String getName() {
    return "CollectorCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    if (!reached.isEmpty() && reached.getFirstState() instanceof CollectorState) {
      build(reached);
    }

    /*StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put("Verification result", result);
    //writer.put("Reached States", reached.toString());**/
  }

  private void makeHTMLFile() {
    try {
      BufferedReader reader =
          Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
              .openBufferedStream();

      Writer writerhtml = IO.openOutputFile(Paths.get("./output/ComputationSteps.html"),
          Charsets.UTF_8);
      BufferedWriter bwhtml = new BufferedWriter(writerhtml);
      String line2;
      while (null != (line2 = reader.readLine())) {
        if (line2.contains("COLLECTOR_CSS")) {
          bwhtml.write("<style>" + "\n");
          Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
              .copyTo(bwhtml);
          bwhtml.write("</style>");
        } else if (line2.contains("COLLECTOR_JS")) {
          insertJs(bwhtml);
        } else if (line2.contains("SOURCE_CONTENT")) {
          insertSources(bwhtml);
        }

        else {
          bwhtml.write(line2 + "\n");
        }
      }
      bwhtml.close();

    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create HTMLFile.");
    }
  }

  private void insertSources(Writer report) throws IOException {
    for (String sourceFile : sourceFiles) {
      insertSource(Paths.get(sourceFile), report);
    }
  }

  //Method insertSource is similar to insertSource in ReportGenerator
  private void insertSource(Path sourcePath, Writer writer)
      throws IOException {
    if (isReadable(sourcePath)) {
      int lineNumber = 1;
      try (BufferedReader source =
               new BufferedReader(
                   new InputStreamReader(
                       new FileInputStream(sourcePath.toFile()),
                       Charset.defaultCharset()))) {
        writer.write(
            "<div class=\"sourceContent\">\n<table>\n");
        String line;
        while (null != (line = source.readLine())) {
          line = "<td><pre class=\"prettyprint\">" + htmlEscaper().escape(line) + "  </pre></td>";
          writer.write(
              "<tr id=\"source-"
                  + lineNumber
                  + "\"><td><pre>"
                  + lineNumber
                  + "</pre></td>"
                  + line
                  + "</tr>\n");
          lineNumber++;
        }
        writer.write("</table></div>\n");
      } catch (IOException e) {
        logger
            .logUserException(WARNING, e, "Could not create report: Inserting source code failed.");
      }
    } else {
      writer.write("<p>No Source-File available</p>");
    }
  }

  private void insertJs(
      Writer writer)
      throws IOException {
    try (BufferedReader reader =
             Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
                 .openBufferedStream()) {
      String line;
      while (null != (line = reader.readLine())) {
        if (line.contains("COLLECTOR_JSON_INPUT")) {
          makeArgJson(writer);
        } else {
          writer.write(line + "\n");
        }
      }
    }
  }

  private void makeLinkedCollectorData() {

    //sort Nodes by key(myID)
    Map<Integer, Object> sortedcNodes = new TreeMap<>(cNodes);

    ImmutableMap<Integer, Object> immutableMapNodesC =
        ImmutableMap.<Integer, Object>builder().putAll(sortedcNodes)
            .build();

    ImmutableMultimap<Integer, Object> immutableMapEdgesC =
        ImmutableMultimap.<Integer, Object>builder().putAll(cEdges)
            .build();

    collectorlinkedNodesAndEdges.put(immutableMapNodesC, immutableMapEdgesC);
  }

  private void makeArgJson(Writer writer) {

    if (!collectorlinkedNodesAndEdges.isEmpty()) {
      collectorlinkedNodesAndEdges.forEach((key, value) -> {
        try {
          writer.write("var myData = { ");
          writer.write("\n\"nodes\":");
          JSON.writeJSONString(key.values(), writer);
          writer.write(",\n\"edges\":");
          JSON.writeJSONString(value.values(), writer);
          writer.write("}\n");
        } catch (IOException e) {
          logger.logUserException(WARNING, e, "Inserting ARG Json failed.");
        }
      });
    }
  }

  private void build(UnmodifiableReachedSet reached) {
    //String destroyed = "destroyed";
    //String notDestroyed = "";

    for (AbstractState entry : reached.asCollection()) {
      boolean merged = ((CollectorState) entry).ismerged();
      boolean stopped = ((CollectorState) entry).isStopped();

      if (merged) {
        String type = "toMerge";
        String typeM = "merged";
        String typeC = "mergeChild";

        int ID = ((CollectorState) entry).getStateId();

        ARGStateView myA1 = ((CollectorState) entry).getmyARG1();
        int ID1 = myA1.getStateId();
        int myID1 = myA1.getMyStateId();
        ARGState entryARG1 = myA1.getARGState();
        boolean destroyed1 = entryARG1.isDestroyed();

        ARGStateView myA2 = ((CollectorState) entry).getMyARG2();
        int ID2 = myA2.getStateId();
        int myID2 = myA2.getMyStateId();
        ARGState entryARG2 = myA2.getARGState();
        boolean destroyed2 = entryARG2.isDestroyed();

        ARGStateView myAM = ((CollectorState) entry).getMyARGmerged();
        int ID3 = myAM.getStateId();
        int myID3 = myAM.getMyStateId();
        ARGState entryARG = ((CollectorState) entry).getARGState();

        if (!destroyed1) {
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes
                .put(myID1, createNEWNode(myA1.getCount(),ID1, myA1.getCount(), node, entryARG1, type,
                    stopped));
          }

          Collection<ARGState> children = entryARG1.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID1, childID, entryARG.getEdgesToChild(child), type);
            cEdges.put(myID1, edgeValue);
          }
        } else {
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID1, createNEWNode(myA1.getCount(),ID1, myAM.getCount(), node, entryARG1, type,
                stopped));
          }

          Collection<ARGState> children = ((CollectorState) entry).getChildrenTomerge1();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue1 = createChildEdge(
                ID1, childID, typeC);
            cEdges.put(myID1, edgeValue1);
          }

          Collection<ARGState> parents1 = myA1.getParentslist();
          if (parents1 != null) {
            int parentID = getParentsId(parents1);
            Map<String, Object> edgeValue1 = createExtraEdge(
                parentID, ID1, type);
            cEdges.put(myID1, edgeValue1);
          }
        }

        if (!destroyed2) {
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes
                .put(myID2, createNEWNode(myA2.getCount(),ID2, myA2.getCount(), node, entryARG2, type,
                    stopped));
          }
          Collection<ARGState> children = entryARG2.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID2, childID, entryARG.getEdgesToChild(child), type);
            cEdges.put(myID2, edgeValue);
          }
        } else {
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID2, createNEWNode(myA2.getCount(),ID2, myAM.getCount(), node, entryARG2, type,
                stopped));
          }

          Collection<ARGState> children = ((CollectorState) entry).getChildrenTomerge2();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue2 = createChildEdge(
                ID2, childID, typeC);
            cEdges.put(myID1, edgeValue2);
          }

          Collection<ARGState> parents2 = myA2.getParentslist();
          if (parents2 != null) {
            int parentID = getParentsId(parents2);
            Map<String, Object> edgeValue2 = createExtraEdge(
                parentID, ID2, type);
            cEdges.put(myID2, edgeValue2);
          }
        }

        for (CFANode node : AbstractStates.extractLocations(entry)) {
          cNodes.put(myID3, createNEWNode(myAM.getCount(),ID3, myAM.getCount(), node, entryARG, typeM,
              stopped));
          }

        Collection<ARGState> children = entryARG.getChildren();
        for (ARGState child : children) {
          int childID = child.getStateId();
          Map<String, Object> edgeValue = createStandardEdge(
              ID, childID, entryARG.getEdgesToChild(child), typeM);
          cEdges.put(myID3, edgeValue);
        }
      } else {
        int ID = ((CollectorState) entry).getStateId();
        ARGStateView myA = ((CollectorState) entry).getMyARGTransferRelation();
        if (myA != null) {
          int myIDtr = myA.getMyStateId();
          String type = determineType((CollectorState) entry);
          ARGState entryARG = ((CollectorState) entry).getARGState();
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes
                .put(myIDtr, createNEWNode(myA.getCount(),ID, myA.getCount(), node, entryARG, type,
                    stopped));
          }

          Collection<ARGState> children = entryARG.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID, childID, entryARG.getEdgesToChild(child), type);
            cEdges.put(myIDtr, edgeValue);
          }
        }
        // Start node
        else {
          String type = determineType((CollectorState) entry);
          ARGState entryARG = ((CollectorState) entry).getARGState();


          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(-1, createFirstNode(ID, node, entryARG, type));
          }
          Collection<ARGState> children = entryARG.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID, childID, entryARG.getEdgesToChild(child), type);
            cEdges.put(-1, edgeValue);
          }
        }
      }
    }
    makeLinkedCollectorData();
    makeHTMLFile();
  }

  private int getParentsId(Collection<ARGState> pParents) {
    int parentID = 0;
    for (ARGState parent : pParents) {
      parentID = parent.getStateId();
    }
    return parentID;
  }

  // Nodes and Edges
  private Map<String, Object> createNEWNode(
      int step,
      int parentStateId,
      int interval,
      CFANode node,
      ARGState argState,
      String type,
      boolean stopped) {
    String dotLabel =
        argState.toDOTLabel().length() > 2
        ? argState.toDOTLabel().substring(0, argState.toDOTLabel().length() - 2)
        : "";
    Map<String, Object> argNode = new HashMap<>();
    if (!(interval <= step)) {
      argNode.put("intervalStop", interval);
    } else {
      argNode.put("intervalStop", "");
    }
    argNode.put("intervalStart", step);
    argNode.put("analysisStop", stopped);
    argNode.put("index", parentStateId);//ARGState-ID
    argNode.put("func", node.getFunctionName());
    argNode.put(
        "label",
        parentStateId
            + " @ "
            + node
            + "\n"
            + node.getFunctionName()
            + nodeTypeInNodeLabel(node)
            + "\n"
            + dotLabel);
    argNode.put("type", type);
    return argNode;
  }

  private Map<String, Object> createFirstNode(
      int parentStateId,
      CFANode node,
      ARGState argState,
      String type) {
    String dotLabel =
        argState.toDOTLabel().length() > 2
        ? argState.toDOTLabel().substring(0, argState.toDOTLabel().length() - 2)
        : "";
    Map<String, Object> argNode = new HashMap<>();

    argNode.put("analysisStop", false);
    argNode.put("intervalStop", "");
    argNode.put("intervalStart", 0);
    argNode.put("index", parentStateId);
    argNode.put("func", node.getFunctionName());
    argNode.put(
        "label",
        parentStateId
            + " @ "
            + node
            + "\n"
            + node.getFunctionName()
            + nodeTypeInNodeLabel(node)
            + "\n"
            + dotLabel);
    argNode.put("type", type);
    return argNode;
  }

  private String determineType(CollectorState state) {

    if (state.ismerged()) {
      return "merged";
    }
    return "none";
  }

  // Add the node type (if it is entry or exit) to the node label
  private String nodeTypeInNodeLabel(CFANode node) {
    if (node instanceof FunctionEntryNode) {
      return " entry";
    } else if (node instanceof FunctionExitNode) {
      return " exit";
    }
    return "";
  }

  private Map<String, Object> createExtraEdge(
      int parentStateId,
      int childStateId,
      String type) {
    Map<String, Object> argEdge = new HashMap<>();

    argEdge.put("source", parentStateId);
    argEdge.put("target", childStateId);
    argEdge.put("mergetype", type);

    argEdge.put("type", "dummy type");
    argEdge.put("label", "merge edge");
    return argEdge;
  }

  private Map<String, Object> createChildEdge(
      int parentStateId,
      int childStateId,
      String type) {
    Map<String, Object> argEdge = new HashMap<>();

    argEdge.put("source", parentStateId);
    argEdge.put("target", childStateId);
    argEdge.put("mergetype", type);

    argEdge.put("type", "dummy type");
    argEdge.put("label", "Child merge edge");
    return argEdge;
  }

  //Method createStandardEdge is similar to createArgEdge in ReportGenerator
  private Map<String, Object> createStandardEdge(
      int parentStateId, int childStateId, List<CFAEdge> edges, String mergetype) {
    Map<String, Object> argEdge = new HashMap<>();
    argEdge.put("mergetype", mergetype);
    argEdge.put("source", parentStateId);
    argEdge.put("target", childStateId);
    StringBuilder edgeLabel = new StringBuilder();
    if (edges.isEmpty()) {
      edgeLabel.append("dummy edge");
      argEdge.put("type", "dummy type");
    } else {
      argEdge.put("type", edges.get(0).getEdgeType().toString());
      if (edges.size() > 1) {
        edgeLabel.append("Lines ");
        edgeLabel.append(edges.get(0).getFileLocation().getStartingLineInOrigin());
        edgeLabel.append(" - ");
        edgeLabel.append(edges.get(edges.size() - 1).getFileLocation().getStartingLineInOrigin());
        edgeLabel.append(":");
        argEdge.put("lines", edgeLabel.substring(6));
      } else {
        edgeLabel.append("Line ");
        edgeLabel.append(edges.get(0).getFileLocation().getStartingLineInOrigin());
        argEdge.put("line", edgeLabel.substring(5));
      }
      for (CFAEdge edge : edges) {
        if (edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          edgeLabel.append("\n");
          List<String> edgeText = Splitter.on(':').limit(2).splitToList(getEdgeText(edge));
          edgeLabel.append(edgeText.get(0));
          if (edgeText.size() > 1) {
            edgeLabel.append("\n");
            edgeLabel.append(edgeText.get(1));
          }
        } else {
          edgeLabel.append("\n");
          edgeLabel.append(getEdgeText(edge));
        }
      }
      argEdge.put("file", edges.get(0).getFileLocation().getFileName());
    }
    argEdge.put("label", edgeLabel.toString());
    return argEdge;
  }
}

