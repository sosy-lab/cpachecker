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
import static java.nio.file.Files.isReadable;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

@Options
public class ReportGenerator {

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();

  private static final String HTML_TEMPLATE = "report.html";
  private static final String CSS_TEMPLATE = "report.css";
  private static final String JS_TEMPLATE = "report.js";

  private final Configuration config;
  private final LogManager logger;

  @Option(
      secure = true,
      name = "analysis.programNames",
      description = "A String, denoting the programs to be analyzed")
  private String programs;

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
  private final List<String> sourceFiles;

  public ReportGenerator(Configuration pConfig, LogManager pLogger, @Nullable Path pLogFile)
      throws InvalidConfigurationException {
    config = checkNotNull(pConfig);
    logger = checkNotNull(pLogger);
    logFile = pLogFile;
    config.inject(this);
    sourceFiles = COMMA_SPLITTER.splitToList(programs);
  }

  public boolean generate(CFA pCfa, UnmodifiableReachedSet pReached, String pStatistics) {
    checkNotNull(pCfa);
    checkNotNull(pReached);
    checkNotNull(pStatistics);

    if (!generateReport) { return false; }

    Iterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(pReached)
                .filter(IS_TARGET_STATE)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

    if (!counterExamples.iterator().hasNext()) {
      if (reportFile != null) {
        DOTBuilder2 dotBuilder = new DOTBuilder2(pCfa);
        fillOutTemplate(null, reportFile, pCfa, dotBuilder, pStatistics);
        return true;
      } else {
        return false;
      }

    } else if (counterExampleFiles != null) {
      DOTBuilder2 dotBuilder = new DOTBuilder2(pCfa);
      for (CounterexampleInfo counterExample : counterExamples) {
        fillOutTemplate(
            counterExample,
            counterExampleFiles.getPath(counterExample.getUniqueId()),
            pCfa,
            dotBuilder,
            pStatistics);
      }
      return true;
    } else {
      return false;
    }
  }

  private void fillOutTemplate(
      @Nullable CounterexampleInfo counterExample,
      Path reportPath,
      CFA cfa,
      DOTBuilder2 dotBuilder,
      String statistics) {

    try (BufferedReader reader =
        Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
            .openBufferedStream();
        Writer writer = MoreFiles.openOutputFile(reportPath, Charsets.UTF_8)) {

      String line;
      while (null != (line = reader.readLine())) {
        if (line.contains("CONFIGURATION")) {
          insertConfiguration(writer);
        } else if (line.contains("REPORT_CSS")) {
          insertCss(writer);
        } else if (line.contains("REPORT_JS")) { // inserts the JSON info
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
      reader.close();
      writer.close();
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Processing of HTML template failed.");
    }
  }

  private void insertJs(Writer writer, CFA cfa, DOTBuilder2 dotBuilder,
      @Nullable CounterexampleInfo counterExample) throws IOException {
    try (BufferedReader reader =
        Resources.asCharSource(Resources.getResource(getClass(), JS_TEMPLATE), Charsets.UTF_8)
            .openBufferedStream();) {
      String line;
      while (null != (line = reader.readLine())) {
        if (line.contains("CFA_JSON_INPUT")) {
          insertJson(writer, cfa, dotBuilder, counterExample);
        } else if (line.contains("SOURCE_FILES")) {
          insertSourceFileNames(writer);
        } else {
          writer.write(line + "\n");
        }
      }
      reader.close();
    }
  }

  private void insertJson(Writer writer, CFA cfa, DOTBuilder2 dotBuilder,
      @Nullable CounterexampleInfo counterExample) {
    try {
      writer.write("var json = {\n");
      insertFunctionNames(writer, cfa);
      writer.write(",\n");
      insertFCallEdges(writer, dotBuilder);
      writer.write(",\n");
      insertCombinedNodesData(writer, dotBuilder);
      writer.write(",\n");
      insertInversedCombinedNodesData(writer, dotBuilder);
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
      logger.logUserException(
          WARNING, e, "Could not create report: generall IO Exception occured.");
    }

  }

  private void insertCss(Writer writer) throws IOException {
    try (BufferedReader reader =
        Resources.asCharSource(Resources.getResource(getClass(), CSS_TEMPLATE), Charsets.UTF_8)
            .openBufferedStream();) {
      writer.write("<style>" + "\n");
      String line;
      while (null != (line = reader.readLine())) {
        writer.write(line + "\n");
      }
      writer.write("</style>");
      reader.close();
    }
  }

  private void insertMetaTags(Writer writer) {
    try {
      writer.write("<meta name='generator'"
          + " content='CPAchecker " + CPAchecker.getCPAcheckerVersion() + "'>\n");
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Inserting metatags failed.");
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
          WARNING, e, "Could not create report: Inserting date and version failed.");
    }
  }

  private void insertReportName(@Nullable CounterexampleInfo counterExample, Writer writer) {
    try {
      if (counterExample == null) {
        writer.write(sourceFiles.get(0));
      } else {
        String title = String.format(
            "%s (Counterexample %s)",
            sourceFiles.get(0),
            counterExample.getUniqueId());
        writer.write(title);
      }
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Inserting report name failed.");
    }
  }

  private void insertStatistics(Writer writer, String statistics) throws IOException {
    int counter = 0;
    for (String line : LINE_SPLITTER.split(statistics)) {
      line = "<pre id=\"statistics-" + counter + "\">" + htmlEscape(line) + "</pre>\n";
      writer.write(line);
      counter++;
    }
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
                  new FileInputStream(sourcePath.toFile()), Charset.defaultCharset()))) {
        writer.write(
            "<div class=\"sourceContent content\" ng-show = \"sourceFileIsSet("
                + sourceFileNumber
                + ")\">\n<table>\n");
        String line;
        while (null != (line = source.readLine())) {
          line = "<td><pre class=\"prettyprint\">" + htmlEscape(line) + "  </pre></td>";
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
        logger.logUserException(
            WARNING, e, "Could not create report: Inserting source code failed.");
      }
    } else {
      writer.write("<p>No Source-File available</p>");
    }
  }


  private void insertConfiguration(Writer writer) throws IOException {
    Iterable<String> lines = LINE_SPLITTER.split(config.asPropertiesString());
    int iterator = 0;
    for (String line : lines) {
      line = "<pre id=\"config-" + iterator + "\">" + htmlEscape(line) + "</pre>\n";
      writer.write(line);
      iterator++;
    }
  }

  private void insertLog(Writer writer) {
    try (BufferedReader log = Files.newBufferedReader(logFile, Charset.defaultCharset())) {
      if (logFile != null && Files.isReadable(logFile)) {
        int counter = 0;
        String line;
        while (null != (line = log.readLine())) {
          line = "<pre id=\"log-" + counter + "\">" + htmlEscape(line) + "</pre>\n";
          writer.write(line);
          counter++;
        }
    } else {
      writer.write("<p>No Log-File available</p>");
    }
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report: Adding log failed.");
    }
  }

  private void insertFCallEdges(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"functionCallEdges\":");
      dotBuilder.writeFunctionCallEdges(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of function call edges failed.");
    }
  }

  private void insertCombinedNodesData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"combinedNodes\":");
      dotBuilder.writeCombinedNodes(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of combined nodes failed.");
    }
  }

  private void insertInversedCombinedNodesData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"inversedCombinedNodes\":");
      dotBuilder.writeInversedComboNodes(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of inversed combined nodes failed.");
    }
  }

  private void insertCombinedNodesLabelsData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"combinedNodesLabels\":");
      dotBuilder.writeCombinedNodesLabels(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of combined nodes labels failed.");
    }
  }

  private void insertMergedNodesListData(Writer writer, DOTBuilder2 dotBuilder) {
    try {
      writer.write("\"mergedNodes\":");
      dotBuilder.writeMergedNodesList(writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of merged nodes failed.");
    }
  }

  private void insertErrorPathData(CounterexampleInfo counterExample, Writer writer) {
    try {
      writer.write("\"errorPath\":");
      counterExample.toJSON(writer);
      writer.write("," + "//CounterExample\n");
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of counter example failed.");
    }
  }

  private void insertFunctionNames(Writer writer, CFA cfa) {
    try {
      writer.write("\"functionNames\":");
      JSON.writeJSONString(cfa.getAllFunctionNames(), writer);
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of function names failed.");
    }
  }

  private void insertSourceFileNames(Writer writer) {
    try {
      writer.write("var sourceFiles = [");
      JSON.writeJSONString(sourceFiles, writer);
      writer.write("];\n");
    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of source file names failed.");
    }
  }

  private static String htmlEscape(String text) {

    Map<String, String> htmlReplacements = new ImmutableMap.Builder<String, String>()
        .put("&", "&amp;")
        .put("<", "&lt;")
        .put(">", "&gt;")
        .build();

    String regexp = Joiner.on('|').join(htmlReplacements.keySet());

    StringBuffer sb = new StringBuffer();
    Pattern p = Pattern.compile(regexp);
    Matcher m = p.matcher(text);

    while (m.find()) {
      m.appendReplacement(sb, htmlReplacements.get(m.group()));
    }

    m.appendTail(sb);
    return sb.toString();
  }
}
