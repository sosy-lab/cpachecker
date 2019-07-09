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

import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
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
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
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
  //private static final String HTML_TEMPLATE = "collectorreport.html";
  private static final String HTML_TEMPLATE = "collectortable.html";
  private static final String CSS_TEMPLATE = "collectortable.css";
  private static final String JS_TEMPLATE = "collectortable.js";
  private Collection<ARGState> reachedcollectionARG = new ArrayList<ARGState>();
  private Collection<ARGState> reconstructedCollection;
  private LinkedHashMap<ARGState, Boolean> destroyedStates;
  private Boolean aftermerge;
  private LinkedHashMap<ARGState, Boolean> linkedmergepartner;
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
  private final Map<String, Object> argEdges;


  public CollectorStatistics(CollectorCPA ccpa, Configuration config,LogManager pLogger) throws InvalidConfigurationException {
    this.cpa = ccpa;
    this.logger=pLogger;

    config.inject(this, CollectorStatistics.class);

    argNodes = new HashMap<>();
    argEdges = new HashMap<>();
  }

  @Override
  public String getName() {
    return "CollectorCPA";
  } //return null wenn ich eh keine Statistiken will

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    if (!reached.isEmpty() && reached.getFirstState() instanceof CollectorState) {
      reconstructARG(reached);
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put("Sonja", 42);//hier können statistics gedruckt werden, siehe andere Klassen
    writer.put("sonja result", result);
    writer.put("Sonja reached", reached.toString()) ;
    writer.put("Sonja reconstructed", reachedcollectionARG.toString());
    }

    private void reconstructARG(UnmodifiableReachedSet reached){

    makeASonjaFileDirectory();

    for (AbstractState entry : reached.asCollection()) {

      myARGStatetransfer = ((CollectorState) entry).getMyARGTransferRelation();

      if (myARGStatetransfer != null) {
        convertedARGStatetransfer = myARGStatetransfer.getARGState();
        convertedparenttransfer = myARGStatetransfer.getparentARGState();
        AbstractState wrappedmyARG = ((CollectorState) entry).getMyARGTransferRelation().getwrappedState();
        AbstractState parentwrappedmyARG = ((CollectorState) entry).getMyARGTransferRelation().getwrappedParentState();

        if (reachedcollectionARG.size() == 0) {
          newarg = new ARGState(parentwrappedmyARG, null);
          newarg.markExpanded();
          linkedparents.put(convertedARGStatetransfer, newarg);
        } else {

          if (linkedparents.containsKey(convertedparenttransfer)) {
            ARGState current = linkedparents.get(convertedparenttransfer);
            newarg = new ARGState(wrappedmyARG, current);
            newarg.markExpanded();
            linkedparents.put(convertedARGStatetransfer, newarg);
          } else {
            newarg = new ARGState(wrappedmyARG, null);
            linkedparents.put(convertedARGStatetransfer, newarg);
            logger.log(Level.INFO, "ARGState without parents:\n" + newarg);
          }
        }
        reachedcollectionARG.add(newarg);
        //makeFiles(reachedcollectionARG);

      }

      Boolean merged = ((CollectorState) entry).ismerged();
      if (merged) {
        myARGState1 = ((CollectorState) entry).getmyARG1();
        myARGState2 = ((CollectorState) entry).getMyARG2();
        if (myARGState1 != null && myARGState2 != null) {
          ARGState convertedARGState1 = myARGState1.getARGState();
          ARGState convertedparent1 = myARGState1.getparentARGState();

          ARGState convertedARGState2 = myARGState2.getARGState();
          ARGState convertedparent2 = myARGState2.getparentARGState();

          AbstractState wrappedmyARG1 = ((CollectorState) entry).getmyARG1().getwrappedState();
          AbstractState wrappedmyARG2 = ((CollectorState) entry).getMyARG2().getwrappedState();

          ARGState mergedstate = ((CollectorState) entry).getARGState();

          if (linkedparents.containsKey(convertedparent1) && linkedparents
              .containsKey(convertedparent2)) {

            AbstractState c = mergedstate.getWrappedState();
            final ARGState current1 = linkedparents.get(convertedparent1);
            final ARGState current2 = linkedparents.get(convertedparent2);

            newarg1 = new ARGState(wrappedmyARG1, current1);
            reachedcollectionARG.add(newarg1);
            //makeFiles(reachedcollectionARG);
            newarg2 = new ARGState(wrappedmyARG2, current2);
            reachedcollectionARG.add(newarg2);
            //makeFiles(reachedcollectionARG);

            boolean destroyed1 = convertedARGState1.isDestroyed();
            boolean destroyed2 = convertedARGState2.isDestroyed();
            linkedparents.put(convertedARGState1, newarg1);
            linkedDestroyer.put(newarg1, destroyed1);
            linkedparents.put(convertedARGState2, newarg2);
            linkedDestroyer.put(newarg2, destroyed2);

            reachedcollectionARG.remove(newarg2);
            reachedcollectionARG.remove(newarg1);
            newarg1.removeFromARG();
            newarg2.removeFromARG();

            newarg3 = new ARGState(c, current2);
            newarg3.addParent(current1);


            linkedparents.put(mergedstate, newarg3);

            reachedcollectionARG.add(newarg3);
            //makeFiles(reachedcollectionARG);

          }
        }
      }
  }

      buildArgGraphData(reachedcollectionARG);
      makeFiles(reachedcollectionARG);
  }

  private void insertCss(BufferedWriter pWriter) {
  }

  public ARGState getFirst(Collection<ARGState> collection){
      return collection.iterator().next();
    }


  private void makeFiles(Collection<ARGState> pReachedcollectionARG) {
    try{
      int i = 0;
      String filenamepart1 = "./output/SonjasDotFiles/etape_";
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

      ARGToDotWriter.write(bw, pReachedcollectionARG, "Test Reconstruction Sonja");

      bw.close();
      //####################################
      BufferedReader reader =
          Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
              .openBufferedStream();
      Writer writerhtml = IO.openOutputFile(Paths.get("./output/SonjasFile.html"),
          Charsets.UTF_8);
      BufferedWriter bwhtml = new BufferedWriter(writerhtml);
      String line2;
      while (null != (line2 = reader.readLine())){
       // logger.log(Level.INFO, "sonja will lesen " + line2);
        //bwhtml.write(line2);
        if (line2.contains("REPORT_CSS")) {
          //insertCss(writer);
          bwhtml.write("<style>" + "\n");
          Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
              .copyTo(bwhtml);
          bwhtml.write("</style>");
        } else if (line2.contains("REPORT_JS")){
          logger.log(Level.INFO, "sonja will javascript " );
          //insertArgJson(bwhtml);
          /**Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
              .copyTo(bwhtml);**/
          insertJs(bwhtml);
        } else {
          bwhtml.write(line2 + "\n");
        }
      }
      bwhtml.close();
      //####################################



    }catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create Sonjas file.");
    }
  }
private void makeASonjaFileDirectory () {

    String directoryName = "./output/SonjasDotFiles";
    File directory = new File(directoryName);
   if (! directory.exists()){
     try {
       directory.mkdir();
     }catch(SecurityException se){
       logger.logUserException(
           WARNING, se, "Could not create Sonjas directory.");
     }
    }
}
//!!!!!Ab hier code aus ReportGenerator:


  private void insertJs(
      Writer writer)
      throws IOException {
    try (BufferedReader reader =
             Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
                 .openBufferedStream();) {
      String line;
      while (null != (line = reader.readLine())) {
         if (line.contains("ARG_JSON_INPUT")) {
          insertArgJson(writer);
        }  else {
          writer.write(line + "\n");
        }
      }
    }
  }

  private void insertArgJson(Writer writer) {
    try {
      // Achtung für die HTML-Tabelle Klammern {} weg und write "nodes" auskommentieren
      writer.write("var myData = { ");
      if (!argNodes.isEmpty() && !argEdges.isEmpty()) {
        writer.write("\n\"nodes\":");
        JSON.writeJSONString(argNodes.values(), writer);
        writer.write(",\n\"edges\":");
        JSON.writeJSONString(argEdges.values(), writer);
        writer.write("\n");
      }
      writer.write("}\n");
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Inserting ARG Json failed.");
    }
  }


  private void buildArgGraphData(Collection<ARGState> reached) {
    for (AbstractState entry : reached) {
      int parentStateId = ((ARGState) entry).getStateId();
      for (CFANode node : AbstractStates.extractLocations(entry)) {
        if (!argNodes.containsKey(parentStateId)) {
          argNodes.put(parentStateId, createArgNode(parentStateId, node, (ARGState) entry));
          if (!((ARGState) entry).getChildren().isEmpty()) {
            for (ARGState child : ((ARGState) entry).getChildren()) {
              int childStateId = child.getStateId();
              // Covered state is not contained in the reached set
              if (child.isCovered()) {
                String label = child.toDOTLabel();
                label = label.length() > 2 ? label.substring(0, label.length() - 2) : "";
                //createCoveredArgNode(childStateId, child, label);
                //createCoveredArgEdge(childStateId, child.getCoveringState().getStateId());
              }
              argEdges.put(
                  parentStateId + "->" + childStateId,
                  createArgEdge(
                      parentStateId, childStateId, ((ARGState) entry).getEdgesToChild(child)));
            }
          }
        }}}
    logger.log(Level.INFO, "sonja argnodes " +"\n" + argNodes.toString());
    logger.log(Level.INFO, "sonja argedges " +"\n" + argEdges.toString());
  }

  private Map<String, Object> createArgNode(int parentStateId, CFANode node, ARGState argState) {
    String dotLabel =
        argState.toDOTLabel().length() > 2
        ? argState.toDOTLabel().substring(0, argState.toDOTLabel().length() - 2)
        : "";
    Map<String, Object> argNode = new HashMap<>();
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
    argNode.put("type", determineNodeType(argState));
    return argNode;
  }
  private String determineNodeType(ARGState argState) {
    if (argState.isTarget()) {
      return "target";
    }
    if (!argState.wasExpanded()) {
      return "not-expanded";
    }
    if (argState.shouldBeHighlighted()) {
      return "highlighted";
    }
    return "";
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
  private Map<String, Object> createArgEdge(
      int parentStateId, int childStateId, List<CFAEdge> edges) {
    Map<String, Object> argEdge = new HashMap<>();
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
