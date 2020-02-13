/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.print.DocFlavor.STRING;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;


@Options(prefix="cpa.collector")
public class CollectorStatistics implements Statistics {



  @Option(secure=true, name="export", description="export collector as .dot file")
  private boolean exportARG = true;

  @Option(secure=true, name="file",
      description="export collector as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("collector.dot");


  private final CollectorCPA cpa;
  private final LogManager logger;
  private static final String HTML_TEMPLATE = "collector.html";
  private static final String CSS_TEMPLATE = "collector.css";
  private static final String JS_TEMPLATE = "collector3.js";
  private Collection<ARGState> reachedcollectionARG = new ArrayList<ARGState>();
  private myARGState myARGState1;
  private myARGState myARGState2;
  private final LinkedHashMap<ARGState,ARGState> linkedparents = new LinkedHashMap<>();
  private LinkedHashMap<ARGState, Boolean> linkedDestroyer = new LinkedHashMap<>();
  private ARGState newarg1;
  private ARGState newarg2;
  private ARGState newarg;
  private myARGState myARGStatetransfer;
  private ARGState convertedARGStatetransfer;
  private ARGState convertedparenttransfer;
  private ARGState newarg3;
  private final Map<Integer, Object> argNodes;
  private final Multimap<Integer, Object> argEdges;
  //private final Map<Integer, Object> cNodes;
  private final LinkedHashMap<Integer, Object> cNodes= new LinkedHashMap<>();;
  private final Multimap<Integer, Object> cEdges;
  private LinkedHashMap<String,Integer> linkedIndex = new LinkedHashMap<>();
  private final Multimap<Integer, Object> cNodesMulti;
  private final Multimap<Integer, CollectorState> cStates;
  private int count;
  private LinkedHashMap<Map<Integer, Object>,Multimap<Integer, Object>> linkedNodesAndEdges = new LinkedHashMap<>();
  private LinkedHashMap<Map<Integer, Object>,Multimap<Integer, Object>> collectorlinkedNodesAndEdges = new LinkedHashMap<>();
  private LinkedHashMap<Multimap<Integer, Object>,Multimap<Integer, Object>> multicollectorlinkedNodesAndEdges = new LinkedHashMap<>();
  private Collection<CollectorState> reachedcollectionCollector = new ArrayList<CollectorState>();
  private int cARGID2;
  private int cARGID1;
  private int myparentID1;
  private int myparentID2;
  private LinkedHashMap<Integer,Integer> linkedIds = new LinkedHashMap<>();


  public CollectorStatistics(CollectorCPA ccpa, Configuration config,LogManager pLogger) throws InvalidConfigurationException {
    this.cpa = ccpa;
    this.logger=pLogger;

    config.inject(this, CollectorStatistics.class);

    argNodes = new HashMap<>();
    argEdges = ArrayListMultimap.create();
    //cNodes = new HashMap<>();
    cEdges = ArrayListMultimap.create();
    cNodesMulti = ArrayListMultimap.create();
    cStates = ArrayListMultimap.create();
  }

  @Override
  public String getName() {
    return "CollectorCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {


    if (!reached.isEmpty() && reached.getFirstState() instanceof CollectorState) {
      //reconstructARG(reached);
      //buildFromCollector(reached);
      build(reached);

    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put("Verification result", result);
    writer.put("Reached States", reached.toString()) ;
    }


  public ARGState getFirst(Collection<ARGState> collection){
      return collection.iterator().next();
    }

  private void makeDotFile(Collection<ARGState> pReachedcollectionARG) {
    try{
      int i = 0;
      String filenamepart1 = "./output/CollectorDotFiles/etape_";
      String filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
      File file = new File(filenamefinal);
      while (file.exists()) {
        filenamefinal = filenamepart1 + Integer.toString(i) + ".dot";
        file = new File(filenamefinal);
        i++;
      }
      file.createNewFile();
      Writer writer = new FileWriter(file, false);
      BufferedWriter bw = new BufferedWriter(writer);

      ARGToDotWriter.write(bw, pReachedcollectionARG, "Reconstruction of ARGstates");

      bw.close();
    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create DotFiles.");
    }
  }

  private void makeHTMLFile(){
    try {
      BufferedReader  reader = Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
          .openBufferedStream();

    Writer writerhtml = IO.openOutputFile(Paths.get("./output/ComputationSteps.html"),
        Charsets.UTF_8);
    BufferedWriter bwhtml = new BufferedWriter(writerhtml);
    String line2;
    while (null != (line2 = reader.readLine())){
      if (line2.contains("REPORT_CSS")) {
        bwhtml.write("<style>" + "\n");
        Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
            .copyTo(bwhtml);
        bwhtml.write("</style>");
      } else if (line2.contains("REPORT_JS")){
        insertJs(bwhtml);
      } else {
        bwhtml.write(line2 + "\n");
      }
    }
    bwhtml.close();

  }catch (IOException e) {
    logger.logUserException(
        WARNING, e, "Could not create HTMLFile.");
    }
  }

  private void makeAFileDirectory () {

    String directoryName = "./output/CollectorDotFiles";
    File directory = new File(directoryName);
   if (! directory.exists()){
     try {
       directory.mkdir();
     }catch(SecurityException se){
       logger.logUserException(
           WARNING, se, "Could not create directory.");
     }
    }
}

  private void insertJs(
      Writer writer)
      throws IOException {
    try (BufferedReader reader =
             Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
                 .openBufferedStream();) {
      String line;
      while (null != (line = reader.readLine())) {
         if (line.contains("ARG_JSON_INPUT")) {
          makeArgJson(writer);
        }  else {
          writer.write(line + "\n");
        }
      }
    }
  }


  private void makeLinkedCollectorData(){

    //sort Nodes by key(myID)
    Map<Integer, Object> sortedcNodes = new TreeMap<Integer, Object>(cNodes);

    ImmutableMap<Integer, Object> immutableMapNodesC = ImmutableMap.<Integer, Object>builder().putAll(sortedcNodes)
        .build();

    ImmutableMultimap<Integer, Object> immutableMapEdgesC = ImmutableMultimap.<Integer, Object>builder().putAll(cEdges)
        .build();

    collectorlinkedNodesAndEdges.put(immutableMapNodesC,immutableMapEdgesC);
  }

  private void makeArgJson(Writer writer) {

    if (!collectorlinkedNodesAndEdges.isEmpty()) {
      collectorlinkedNodesAndEdges.forEach((key, value) -> {
        try {
          writer.write("var myData" + count + " = { ");
            writer.write("\n\"nodes\":");
            JSON.writeJSONString(key.values(), writer);
            writer.write(",\n\"edges\":");
            JSON.writeJSONString(value.values(), writer);
            writer.write("\n");
          writer.write("}\n");
        } catch (IOException e) {
          logger.logUserException(WARNING, e, "Inserting ARG Json failed.");
        }
      });
    }
  }

  private void build(UnmodifiableReachedSet reached){
    String destroyed = "destroyed";
    String notDestroyed = "";

    for (AbstractState entry : reached.asCollection()) {
      boolean merged = ((CollectorState) entry).ismerged();

      if (merged){
        String type = "toMerge";
        String typeM = "merged";
        String typeC = "mergeChild";

        int ID = ((CollectorState) entry).getStateId();

        myARGState myA1 = ((CollectorState) entry).getmyARG1();
        int ID1 = myA1.getStateId();
        int myID1 = myA1.getMyStateId();
        ARGState entryARG1 = myA1.getARGState();
        boolean destroyed1 = entryARG1.isDestroyed();

        myARGState myA2 = ((CollectorState) entry).getMyARG2();
        int ID2 = myA2.getStateId();
        int myID2 = myA2.getMyStateId();
        ARGState entryARG2 = myA2.getARGState();
        boolean destroyed2 = entryARG2.isDestroyed();

        myARGState myAM = ((CollectorState) entry).getMyARGmerged();
        int ID3 = myAM.getStateId();
        int myID3 = myAM.getMyStateId();
        ARGState entryARG = ((CollectorState) entry).getARGState();

        if(!destroyed1){
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID1, createNEWNode(ID1, myID1, ID1, node, entryARG1, type, notDestroyed));
          }

          Collection<ARGState> children = entryARG1.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID1, childID, entryARG.getEdgesToChild(child), myID1, type);
            cEdges.put(myID1, edgeValue);
          }
        }else{
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID1, createNEWNode(ID1, myID1, ID3, node, entryARG1, type, destroyed));
          }

          Collection<ARGState> children = ((CollectorState) entry).getChildrenTomerge1();
          for (ARGState child : children){
            int childID = child.getStateId();
            //logger.log(Level.INFO, "node1: " + ID1 + " myID:" + myID1 + " childID" + childID);
            Map<String, Object> edgeValue1 = createChildEdge(
                ID1, childID, typeC, myID1, destroyed);
            cEdges.put(myID1, edgeValue1);
          }


          ImmutableList<ARGState> parents1 = myA1.getParentslist();
          if(parents1 != null) {
            for (ARGState parent : parents1) {
              int parentID = parent.getStateId();
              //logger.log(Level.INFO, "node1: " + ID1 + " myID:" + myID1 + " parentID" + parentID);
              Map<String, Object> edgeValue1 = createExtraEdge(
                  parentID, ID1, type, myID1, destroyed);
              cEdges.put(myID1, edgeValue1);
            }
          }
        }

        if(!destroyed2){
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID2, createNEWNode(ID2, myID2, ID2, node, entryARG2, type, notDestroyed));
          }
          Collection<ARGState> children = entryARG2.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            //logger.log(Level.INFO, "node2 notdest: " + ID2 + " myID:" + myID2 + " childID" + childID);
            Map<String, Object> edgeValue = createStandardEdge(
                ID2, childID, entryARG.getEdgesToChild(child), myID2, type);
            cEdges.put(myID2, edgeValue);
          }
        }else {
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myID2, createNEWNode(ID2, myID2, ID3, node, entryARG2, type, destroyed));
          }

          Collection<ARGState> children = ((CollectorState) entry).getChildrenTomerge2();
          for (ARGState child : children){
            int childID = child.getStateId();
            //logger.log(Level.INFO, "node2: " + ID2 + " myID:" + myID2 + " childID" + childID);
            Map<String, Object> edgeValue2 = createChildEdge(
                ID2, childID, typeC, myID2, destroyed);
            cEdges.put(myID1, edgeValue2);
          }

          ImmutableList<ARGState> parents2 = myA2.getParentslist();
          if (parents2 != null) {
            for (ARGState parent : parents2) {
              int parentID = parent.getStateId();
              //logger.log(Level.INFO, "node2: " + ID2 + " myID:" + myID2 + " parentID" + parentID);
              Map<String, Object> edgeValue2 = createExtraEdge(
                  parentID, ID2, type, myID2, destroyed);
              cEdges.put(myID2, edgeValue2);
            }
          }
        }

        for (CFANode node : AbstractStates.extractLocations(entry)) {
          cNodes.put(myID3, createNEWNode(ID3, myID3, ID3 ,node, entryARG, typeM,notDestroyed));
        }
       Collection<ARGState> children = entryARG.getChildren();
        for (ARGState child : children) {
          int childID = child.getStateId();
          Map<String, Object> edgeValue = createStandardEdge(
              ID, childID, entryARG.getEdgesToChild(child), myID3, typeM);
          cEdges.put(myID3, edgeValue);
        }
      }
      else {
        int ID = ((CollectorState) entry).getStateId();
        myARGState myA = ((CollectorState) entry).getMyARGTransferRelation();
        if (myA != null) {
          int IDtr = myA.getStateId();
          int myIDtr = myA.getMyStateId();
          String type = determineType((CollectorState) entry);
          ARGState entryARG = ((CollectorState) entry).getARGState();
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(myIDtr, createNEWNode(ID, myIDtr, IDtr,node, entryARG, type, notDestroyed));
          }
         Collection<ARGState> children = entryARG.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            //logger.log(Level.INFO, "node: " + IDtr + " myID:" + myIDtr + " childID" + childID);
            Map<String, Object> edgeValue = createStandardEdge(
                ID, childID, entryARG.getEdgesToChild(child), myIDtr, type);
            cEdges.put(myIDtr, edgeValue);
          }
        }
        // Start node
        else{
          String type = determineType((CollectorState) entry);
          String destroyedType = "";
          ARGState entryARG = ((CollectorState) entry).getARGState();
          boolean destroyedFirst = entryARG.isDestroyed();
          if(destroyedFirst){
            switch (destroyedType = "destroyed") {
            }
          }
          for (CFANode node : AbstractStates.extractLocations(entry)) {
            cNodes.put(-1, createFirstNode(ID, node, entryARG, type, destroyedType));
          }
          Collection<ARGState> children = entryARG.getChildren();
          for (ARGState child : children) {
            int childID = child.getStateId();
            Map<String, Object> edgeValue = createStandardEdge(
                ID, childID, entryARG.getEdgesToChild(child), -1, type);
            cEdges.put(-1, edgeValue);
          }
        }
      }
    }
    //logger.log(Level.INFO, "cnodes:\n" + cNodes.size());
    //logger.log(Level.INFO, "cnodes:\n" + cNodes);
    //logger.log(Level.INFO, "cedges:\n" + cEdges.size());
    makeLinkedCollectorData();
    makeHTMLFile();
  }


// ReportGenerator methods
  private Map<String, Object> createNEWNode(int parentStateId, int parentStateId2,int interval, CFANode node, ARGState argState, String type, String destroyed) {
    String dotLabel =
        argState.toDOTLabel().length() > 2
        ? argState.toDOTLabel().substring(0, argState.toDOTLabel().length() - 2)
        : "";
    Map<String, Object> argNode = new HashMap<>();
    argNode.put("destroyed", destroyed);
    if(!(interval <= parentStateId)){
      argNode.put("intervalStop", interval);
    }
    else{
      argNode.put("intervalStop", "");
    }
    argNode.put("intervalStart", parentStateId);
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
  private Map<String, Object> createFirstNode(int parentStateId, CFANode node, ARGState argState, String type, String destroyed) {
    String dotLabel =
        argState.toDOTLabel().length() > 2
        ? argState.toDOTLabel().substring(0, argState.toDOTLabel().length() - 2)
        : "";
    Map<String, Object> argNode = new HashMap<>();

    argNode.put("destroyed", destroyed);
    argNode.put("intervalStop", "");
    argNode.put("intervalStart", parentStateId);
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

  private Map<String, Object> createExtraEdge(int parentStateId, int childStateId, String type, int myID, String destroyed){
    Map<String, Object> argEdge = new HashMap<>();

    argEdge.put("source", parentStateId);
    argEdge.put("target", childStateId);
    argEdge.put("mergetype",type);
    argEdge.put("destroyed",destroyed);
    StringBuilder edgeLabel = new StringBuilder();

    edgeLabel.append("merge edge");
    argEdge.put("type", "dummy type");
    argEdge.put("label", edgeLabel.toString());
    return argEdge;
  }
  private Map<String, Object> createChildEdge(int parentStateId, int childStateId, String type, int myID, String destroyed){
    Map<String, Object> argEdge = new HashMap<>();

    argEdge.put("source", parentStateId);
    argEdge.put("target", childStateId);
    argEdge.put("mergetype",type);
    argEdge.put("destroyed",destroyed);
    StringBuilder edgeLabel = new StringBuilder();

    edgeLabel.append("Child merge edge");
    argEdge.put("type", "dummy type");
    argEdge.put("label", edgeLabel.toString());
    return argEdge;
  }
  private Map<String, Object> createStandardEdge(
      int parentStateId, int childStateId, List<CFAEdge> edges, int myID, String mergetype) {
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
        edgeLabel.append("");
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

  // Similar to the getEdgeText method in DOTBuilder2
  private static String getEdgeText(CFAEdge edge) {
    return edge.getDescription()
        .replaceAll("\\\"", "\\\\\\\"")
        .replaceAll("\n", " ")
        .replaceAll("\\s+", " ")
        .replaceAll(" ;", ";");
  }
}
