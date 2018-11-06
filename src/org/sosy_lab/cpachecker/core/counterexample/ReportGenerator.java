/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static java.nio.file.Files.isReadable;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options
public class ReportGenerator {

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');

  private static final String HTML_TEMPLATE = "report.html";
  private static final String CSS_TEMPLATE = "report.css";
  private static final String JS_TEMPLATE = "report.js";

  private final Configuration config;
  private final LogManager logger;

  @Option(
    secure = true,
    name = "report.export",
    description = "Generate HTML report with analysis result.")
  private boolean generateReport = true;

  @Option(
    secure = true,
    name = "report.file",
    description = "File name for analysis report in case no counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reportFile = Paths.get("Report.html");

  @Option(
    secure = true,
    name = "counterexample.export.report",
    description = "File name for analysis report in case a counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate counterExampleFiles = PathTemplate.ofFormatString("Counterexample.%d.html");

  private final @Nullable Path logFile;
  private final ImmutableList<String> sourceFiles;
  private final Map<Integer, Object> argNodes;
  private final Map<String, Object> argEdges;
  private final Map<String, Object> argRelevantEdges;
  private final Map<Integer, Object> argRelevantNodes;

  public ReportGenerator(
      Configuration pConfig,
      LogManager pLogger,
      @Nullable Path pLogFile,
      ImmutableList<String> pSourceFiles)
      throws InvalidConfigurationException {
    config = checkNotNull(pConfig);
    logger = checkNotNull(pLogger);
    logFile = pLogFile;
    config.inject(this);
    sourceFiles = pSourceFiles;
    argNodes = new HashMap<>();
    argEdges = new HashMap<>();
    argRelevantEdges = new HashMap<>();
    argRelevantNodes = new HashMap<>();
  }

  public void generate(CFA pCfa, UnmodifiableReachedSet pReached, String pStatistics) {
    checkNotNull(pCfa);
    checkNotNull(pReached);
    checkNotNull(pStatistics);

    if (!generateReport || (reportFile == null && counterExampleFiles == null)) {
      return;
    }

    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(pReached).filter(IS_TARGET_STATE)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    if (counterExamples.isEmpty() ? (reportFile == null) : (counterExampleFiles == null)) {
      return;
    }

    // we cannot export the graph for some special analyses, e.g., termination analysis
    if (!pReached.isEmpty() && pReached.getFirstState() instanceof ARGState) {
      buildArgGraphData(pReached);
      buildRelevantArgGraphData(pReached);
    }

    DOTBuilder2 dotBuilder = new DOTBuilder2(pCfa);
    PrintStream console = System.out;
    if (counterExamples.isEmpty()) {
      if (reportFile != null) {
        fillOutTemplate(null, reportFile, pCfa, dotBuilder, pStatistics);
        console.println("Graphical representation included in the file \"" + reportFile + "\".");
      }

    } else {
      for (CounterexampleInfo counterExample : counterExamples) {
        fillOutTemplate(
            counterExample,
            counterExampleFiles.getPath(counterExample.getUniqueId()),
            pCfa,
            dotBuilder,
            pStatistics);
      }

      StringBuilder counterExFiles = new StringBuilder();
      counterExFiles.append("Graphical representation included in the file");
      if (counterExamples.size() > 1) {
        counterExFiles.append('s');
      }
      counterExFiles.append(" \"");
      Joiner.on("\", \"")
          .appendTo(
              counterExFiles,
              counterExamples.transform(cex -> counterExampleFiles.getPath(cex.getUniqueId())));
      counterExFiles.append("\".");
      console.println(counterExFiles.toString());
    }
  }

  private void fillOutTemplate(
      @Nullable CounterexampleInfo counterExample,
      Path reportPath,
      CFA cfa,
      DOTBuilder2 dotBuilder,
      String statistics) {

    try (
        BufferedReader reader =
            Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
                .openBufferedStream();
        Writer writer = IO.openOutputFile(reportPath, Charsets.UTF_8)) {

      String line;
      while (null != (line = reader.readLine())) {
        if (line.contains("CONFIGURATION")) {
          insertConfiguration(writer);
        } else if (line.contains("REPORT_CSS")) {
          insertCss(writer);
        } else if (line.contains("REPORT_JS")) {
          insertJs(writer, cfa, dotBuilder, counterExample);
        } else if (line.contains("STATISTICS")) {
          insertStatistics(writer, statistics);
        } else if (line.contains("SOURCE_CONTENT")) {
          insertSources(writer);
        } else if (line.contains("LOG")) {
          insertLog(writer);
        } else if (line.contains("REPORT_NAME")) {
          insertReportName(counterExample, writer);
        } else if (line.contains("METATAGS")) {
          insertMetaTags(writer);
        } else if (line.contains("GENERATED")) {
          insertDateAndVersion(writer);
        } else {
          writer.write(line + "\n");
        }
      }
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Processing of HTML template failed.");
    }
  }

  private void insertJs(
      Writer writer,
      CFA cfa,
      DOTBuilder2 dotBuilder,
      @Nullable CounterexampleInfo counterExample)
      throws IOException {
    try (BufferedReader reader =
        Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
            .openBufferedStream();) {
      String line;
      while (null != (line = reader.readLine())) {
        if (line.contains("CFA_JSON_INPUT")) {
          insertCfaJson(writer, cfa, dotBuilder, counterExample);
        } else if (line.contains("ARG_JSON_INPUT")) {
          insertArgJson(writer);
        } else if (line.contains("SOURCE_FILES")) {
          insertSourceFileNames(writer);
        } else {
          writer.write(line + "\n");
        }
      }
    }
  }

  private void insertCfaJson(
      Writer writer,
      CFA cfa,
      DOTBuilder2 dotBuilder,
      @Nullable CounterexampleInfo counterExample) {
    try {
      writer.write("var cfaJson = {\n");
      insertFunctionNames(writer, cfa);
      writer.write(",\n");
      insertFCallEdges(writer, dotBuilder);
      writer.write(",\n");
      insertCombinedNodesData(writer, dotBuilder);
      writer.write(",\n");
      insertCombinedNodesLabelsData(writer, dotBuilder);
      writer.write(",\n");
      insertMergedNodesListData(writer, dotBuilder);
      writer.write(",\n");
      if (counterExample != null) {
        insertErrorPathData(counterExample, writer);
      }
      dotBuilder.writeCfaInfo(writer);
      writer.write("\n}\n");
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Inserting CFA Json failed.");
    }
  }

  private void insertArgJson(Writer writer) {
    try {
      writer.write("var argJson = {");
      if (!argNodes.isEmpty() && !argEdges.isEmpty()) {
        writer.write("\n\"nodes\":");
        JSON.writeJSONString(argNodes.values(), writer);
        writer.write(",\n\"edges\":");
        JSON.writeJSONString(argEdges.values(), writer);
        writer.write("\n");
      }
      if(!argRelevantEdges.isEmpty() && !argRelevantNodes.isEmpty()){
        writer.write(",\n\"relevantnodes\":");
        JSON.writeJSONString(argRelevantNodes.values(), writer);
        writer.write(",\n\"relevantedges\":");
        JSON.writeJSONString(argRelevantEdges.values(), writer);
        writer.write("\n");
      }
      writer.write("}\n");
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Inserting ARG Json failed.");
    }
  }

  private void insertCss(Writer writer) throws IOException {
    writer.write("<style>" + "\n");
    Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
        .copyTo(writer);
    writer.write("</style>");
  }

  private void insertMetaTags(Writer writer) {
    try {
      writer.write(
          "<meta name='generator'"
              + " content='CPAchecker "
              + CPAchecker.getCPAcheckerVersion()
              + "'>\n");
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Inserting metatags failed.");
    }
  }

  private void insertDateAndVersion(Writer writer) {
    try {
      String generated =
          String.format(
              "Generated on %s by CPAchecker %s",
              new SimpleDateFormat(DATE_TIME_FORMAT).format(new Date()),
              CPAchecker.getCPAcheckerVersion());
      writer.write(generated);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Inserting date and version failed.");
    }
  }

  private void insertReportName(@Nullable CounterexampleInfo counterExample, Writer writer) {
    try {
      if (counterExample == null) {
        writer.write(sourceFiles.get(0));
      } else {
        String title =
            String
                .format("%s (Counterexample %s)", sourceFiles.get(0), counterExample.getUniqueId());
        writer.write(title);
      }
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Inserting report name failed.");
    }
  }

  private void insertStatistics(Writer writer, String statistics) throws IOException {
    int counter = 0;
    String insertTableLine =
        "<table  id=\"statistics_table\" class=\"display\" style=\"width:100%;padding: 10px\" class=\"table table-bordered\"><thead class=\"thead-light\"><tr><th scope=\"col\">Statistics Name</th><th scope=\"col\">Statistics Value</th scope=\"col\"><th>Additional Value</th></tr></thead><tbody>\n";
    writer.write(insertTableLine);
    for (String line : LINE_SPLITTER.split(statistics)) {
      if (!line.contains(":") && !(line.trim().isEmpty()) && !line.contains("----------")) {
        String insertTableHead =
            "<tr class=\"table_head\" id=\"statistics-"
                + counter
                + "\"><th>"
                + htmlEscaper().escape(line)
                + "</th><th></th><th></th></tr>";
        writer.write(insertTableHead);
      } else {
        int count = line.indexOf(line.trim());
        for (int i = 0; i < count / 2; i++) {
          line = "\t" + line;
        }
        List<String> splitLine = Splitter.on(":").limit(2).splitToList(line);
        if (splitLine.size() == 2) {
          if (!splitLine.get(1).contains(";") && splitLine.get(1).contains("(")) {
            List<String> splitLineAnotherValue =
                Splitter.on("(").limit(2).splitToList(splitLine.get(1));
            line =
                "<tr id=\"statistics-"
                    + counter
                    + "\"><td>"
                    + htmlEscaper().escape(splitLine.get(0))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLineAnotherValue.get(0))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLineAnotherValue.get(1).replaceAll("[()]", ""))
                    + "</td></tr>\n";
            writer.write(line);
          } else {
            line =
                "<tr id=\"statistics-"
                    + counter
                    + "\"><td>"
                    + htmlEscaper().escape(splitLine.get(0))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLine.get(1))
                    + "</td><td></td></tr>\n";
            writer.write(line);
          }
          counter++;
        }
      }
      counter++;
    }
    String exitTableLine = "</tbody></table>\n";
    writer.write(exitTableLine);
  }

  private void insertSources(Writer report) throws IOException {
    int index = 0;
    for (String sourceFile : sourceFiles) {
      insertSource(Paths.get(sourceFile), report, index);
      index++;
    }
  }

  private void insertSource(Path sourcePath, Writer writer, int sourceFileNumber)
      throws IOException {
    if (isReadable(sourcePath)) {
      int lineNumber = 1;
      try (BufferedReader source =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(sourcePath.toFile()),
                  Charset.defaultCharset()))) {
        writer.write(
            "<div class=\"sourceContent\" ng-show = \"sourceFileIsSet("
                + sourceFileNumber
                + ")\">\n<table>\n");
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

  private void insertConfiguration(Writer writer) throws IOException {
    Iterable<String> lines = LINE_SPLITTER.split(config.asPropertiesString());
    int iterator = 0;
    String insertTableLine =
        "<table  id=\"config_table\" class=\"display\" style=\"width:100%;padding: 10px\" class=\"table table-bordered\"><thead class=\"thead-light\"><tr><th scope=\"col\">#</th><th scope=\"col\">Configuration Name</th><th scope=\"col\">Configuration Value</th></tr></thead><tbody>\n";
    writer.write(insertTableLine);
    for (String line : lines) {
      List<String> splitLine = Splitter.on('=').limit(2).splitToList(line);
      if (splitLine.size() == 2) {
        int countLineNumber = iterator + 1;
        line =
            "<tr id=\"config-"
                + iterator
                + "\"><th scope=\"row\">"
                + countLineNumber
                + "</th><td>"
                + htmlEscaper().escape(splitLine.get(0))
                + "</td><td>"
                + htmlEscaper().escape(splitLine.get(1))
                + "</td></tr>\n";
        writer.write(line);
        iterator++;
      }
    }
    String exitTableLine = "</tbody></table>\n";
    writer.write(exitTableLine);
  }

  private void insertLog(Writer writer) throws IOException {
    if (logFile != null && Files.isReadable(logFile)) {
      String insertTableLine =
          "<table  id=\"log_table\" class=\"display\" style=\"width:100%;padding: 10px\" class=\"table table-bordered\"><thead class=\"thead-light\"><tr><th scope=\"col\">Date</th><th scope=\"col\">Time</th><th scope=\"col\">Log Level</th><th scope=\"col\">Log Info</th><th scope=\"col\">Log Message</th></tr></thead><tbody>\n";
      writer.write(insertTableLine);
      try (BufferedReader log = Files.newBufferedReader(logFile, Charset.defaultCharset())) {
        int counter = 0;
        String line;
        while (null != (line = log.readLine())) {
          String getDate = line.replaceFirst("\\s", "-i-");
          String getLogLevel = getDate.replaceFirst("\\s", "-i-");
          String getLogInfo = getLogLevel.replaceFirst("\\s", "-i-");
          String getLogMessage = getLogInfo.replaceFirst("\\s", "-i-");
          List<String> splitLine = Splitter.onPattern("-i-").limit(5).splitToList(getLogMessage);
          if (splitLine.size() == 5) {
            line =
                "<tr id=\"log-"
                    + counter
                    + "\"><th scope=\"row\">"
                    + htmlEscaper().escape(splitLine.get(0))
                    + "</th><td>"
                    + htmlEscaper().escape(splitLine.get(1))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLine.get(2))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLine.get(3))
                    + "</td><td>"
                    + htmlEscaper().escape(splitLine.get(4))
                    + "</td></tr>\n";
            writer.write(line);
          }
          counter++;
        }
        String exitTableLine = "</tbody></table>\n";
        writer.write(exitTableLine);
      } catch (IOException e) {
        logger.logUserException(WARNING, e, "Could not create report: Adding log failed.");
      }
    } else {
      writer.write("<p>Log not available</p>");
    }
  }

  private void insertFCallEdges(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"functionCallEdges\":");
      dotBuilder.writeFunctionCallEdges(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of function call edges failed.");
    }
  }

  private void insertCombinedNodesData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"combinedNodes\":");
      dotBuilder.writeCombinedNodes(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of combined nodes failed.");
    }
  }

  private void insertCombinedNodesLabelsData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"combinedNodesLabels\":");
      dotBuilder.writeCombinedNodesLabels(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of combined nodes labels failed.");
    }
  }

  private void insertMergedNodesListData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"mergedNodes\":");
      dotBuilder.writeMergedNodesList(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of merged nodes failed.");
    }
  }

  private void insertErrorPathData(CounterexampleInfo counterExample, Writer writer) {
    try {
      writer.write("\"errorPath\":");
      counterExample.toJSON(writer);
      writer.write(",\n");
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of counter example failed.");
    }
  }

  // Program entry function at first place is important for the graph generation
  private void insertFunctionNames(Writer writer, CFA cfa) {
    try {
      writer.write("\"functionNames\":");
      Set<String> allFunctionsEntryFirst = Sets.newLinkedHashSet();
      allFunctionsEntryFirst.add(cfa.getMainFunction().getFunctionName());
      allFunctionsEntryFirst.addAll(cfa.getAllFunctionNames());
      JSON.writeJSONString(allFunctionsEntryFirst, writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of function names failed.");
    }
  }

  private void insertSourceFileNames(Writer writer) {
    try {
      writer.write("var sourceFiles = ");
      JSON.writeJSONString(sourceFiles, writer);
      writer.write(";\n");
    } catch (IOException e) {
      logger.logUserException(
          WARNING,
          e,
          "Could not create report: Insertion of source file names failed.");
    }
  }

  /** Build ARG data for all ARG states in the reached set. */
  private void buildArgGraphData(UnmodifiableReachedSet reached) {
    for (AbstractState entry : reached.asCollection()) {
      int parentStateId = ((ARGState) entry).getStateId();
      for (CFANode node : AbstractStates.extractLocations(entry)) {
        if (!argNodes.containsKey(parentStateId)) {
          argNodes.put(parentStateId, createArgNode(parentStateId, node, (ARGState) entry));
        }
        if (!((ARGState) entry).getChildren().isEmpty()) {
          for (ARGState child : ((ARGState) entry).getChildren()) {
            int childStateId = child.getStateId();
            // Covered state is not contained in the reached set
            if (child.isCovered()) {
              String label = child.toDOTLabel();
              label = label.length() > 2 ? label.substring(0, label.length() - 2) : "";
              createCoveredArgNode(childStateId, child, label);
              createCoveredArgEdge(childStateId, child.getCoveringState().getStateId());
            }
            argEdges.put(
                parentStateId + "->" + childStateId,
                createArgEdge(
                    parentStateId, childStateId, ((ARGState) entry).getEdgesToChild(child)));
          }
        }
      }
    }
  }

  /** Build ARG data for all relevant/important ARG states in the reached set. */
  private void buildRelevantArgGraphData(UnmodifiableReachedSet reached) {
    SetMultimap<ARGState, ARGState> relevantSetMultimap =
        ARGUtils.projectARG(
            (ARGState) reached.getFirstState(), ARGState::getChildren, ARGUtils.RELEVANT_STATE);

    for (Entry<ARGState, Collection<ARGState>> entry : relevantSetMultimap.asMap().entrySet()) {
      ARGState parent = entry.getKey();
      Collection<ARGState> children = entry.getValue();
      int parentStateId = parent.getStateId();
      for (CFANode node : AbstractStates.extractLocations(parent)) {
        if (!argRelevantNodes.containsKey(parentStateId)) {
          argRelevantNodes.put(parentStateId, createArgNode(parentStateId, node, parent));
        }
      }

      for (ARGState child : children) {
        int childStateId = child.getStateId();
        for (CFANode node : AbstractStates.extractLocations(child)) {
          if (!argRelevantNodes.containsKey(childStateId)) {
            argRelevantNodes.put(childStateId, createArgNode(childStateId, node, child));
          }
          argRelevantEdges.put(
              parentStateId + "->" + childStateId,
              createArgEdge(parentStateId, childStateId, parent.getEdgesToChild(child)));
        }
      }
    }
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

  private void createCoveredArgNode(int childStateId, ARGState child, String dotLabel) {
    Map<String, Object> nodeData = new HashMap<>();
    for (CFANode coveredNode : AbstractStates.extractLocations(child)) {
      if (!argNodes.containsKey(childStateId)) {
        nodeData.put("index", childStateId);
        nodeData.put("func", coveredNode.getFunctionName());
        nodeData.put(
            "label",
            childStateId
                + " @ "
                + coveredNode
                + "\n"
                + coveredNode.getFunctionName()
                + nodeTypeInNodeLabel(coveredNode)
                + dotLabel);
        nodeData.put("type", "covered");
        argNodes.put(childStateId, nodeData);
      }
    }
  }

  private void createCoveredArgEdge(int parentStateId, int coveringStateId) {
    Map<String, Object> coveredEdge = new HashMap<>();
    coveredEdge.put("source", parentStateId);
    coveredEdge.put("target", coveringStateId);
    coveredEdge.put("label", "covered by");
    coveredEdge.put("type", "covered");
    argEdges.put("" + coveringStateId + "->" + parentStateId, coveredEdge);
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

  // Add the node type (if it is entry or exit) to the node label
  private String nodeTypeInNodeLabel(CFANode node) {
    if (node instanceof FunctionEntryNode) {
      return " entry";
    } else if (node instanceof FunctionExitNode) {
      return " exit";
    }
    return "";
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
