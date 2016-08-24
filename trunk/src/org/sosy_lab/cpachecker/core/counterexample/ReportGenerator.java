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

@Options
public class ReportGenerator {

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();

  private static final String HTML_TEMPLATE = "report-template.html";

  private final Configuration config;
  private final LogManager logger;

  @Option(
    secure = true,
    name = "analysis.programNames",
    description = "A String, denoting the programs to be analyzed"
  )
  private String programs;

  @Option(
    secure = true,
    name = "report.export",
    description = "Generate HTML report with analysis result."
  )
  private boolean generateReport = true;

  @Option(secure = true, name = "report.file", description = "File name for analysis report in case no counterexample was found.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reportFile = Paths.get("Report.html");

  @Option(
    secure = true,
    name = "counterexample.export.report",
    description = "File name for analysis report in case a counterexample was found."
  )
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

    if (!generateReport) {
      return false;
    }

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

    try (BufferedReader template =
            Resources.asCharSource(Resources.getResource(getClass(), HTML_TEMPLATE), Charsets.UTF_8)
                .openBufferedStream();
        Writer report = MoreFiles.openOutputFile(reportPath, Charsets.UTF_8)) {

      String line;
      while (null != (line = template.readLine())) {
        if (line.contains("CONFIGURATION")) {
          insertConfiguration(report);
        } else if (line.contains("STATISTICS")) {
          insertStatistics(report, statistics);
        } else if (line.contains("SOURCE_CONTENT")) {
          insertSources(report);
        } else if (line.contains("LOG")) {
          insertLog(report);
        } else if (line.contains("ERRORPATH") && counterExample != null) {
          insertErrorPathData(counterExample, report);
        } else if (line.contains("FUNCTIONS")) {
          insertFunctionNames(report, cfa);
        } else if (line.contains("SOURCE_FILE_NAMES")) {
          insertSourceFileNames(report);
        } else if (line.contains("COMBINEDNODES")) {
          insertCombinedNodesData(report, dotBuilder);
        } else if (line.contains("CFAINFO")) {
          insertCfaInfoData(report, dotBuilder);
        } else if (line.contains("FCALLEDGES")) {
          insertFCallEdges(report, dotBuilder);
        } else if (line.contains("REPORT_NAME")) {
          insertReportName(counterExample, report);
        } else if (line.contains("METATAGS")) {
          insertMetaTags(report);
        } else if (line.contains("GENERATED")) {
          insertDateAndVersion(report);
        } else {
          report.write(line + "\n");
        }
      }

    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Processing of HTML template failed.");
    }
  }

  private void insertMetaTags(Writer report) throws IOException {
    report.write("<meta name='generator'"
        + " content='CPAchecker " + CPAchecker.getCPAcheckerVersion() + "'>");
  }

  private void insertDateAndVersion(Writer report) throws IOException {
    String generated =
        String.format(
            "Generated on %s by CPAchecker %s",
            new SimpleDateFormat(DATE_TIME_FORMAT).format(new Date()),
            CPAchecker.getCPAcheckerVersion());
    report.write(generated);
  }

  private void insertReportName(@Nullable CounterexampleInfo counterExample, Writer report) throws IOException {
    if (counterExample == null) {
      report.write(sourceFiles.get(0));

    } else {
      String title = String.format(
          "%s (Counterexample %s)",
          sourceFiles.get(0),
          counterExample.getUniqueId());
      report.write(title);
    }
  }

  private void insertStatistics(Writer report, String statistics) throws IOException {
    int iterator = 0;
    for (String line : LINE_SPLITTER.split(statistics)) {
      line = "<pre id=\"statistics-" + iterator + "\">" + htmlEscape(line) + "</pre>\n";
      report.write(line);
      iterator++;
    }
  }

  private void insertSources(Writer report) throws IOException {
    int index = 0;
    for (String sourceFile : sourceFiles) {
      insertSource(Paths.get(sourceFile), report, index);
      index++;
    }
  }

  private void insertSource(Path sourcePath, Writer report, int sourceFileNumber)
      throws IOException {

    if (isReadable(sourcePath)) {
      int lineNumber = 1;
      try (BufferedReader source =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(sourcePath.toFile()), Charset.defaultCharset()))) {

        report.write(
            "<div class=\"sourceContent content\" ng-show = \"sourceFileIsSet("
                + sourceFileNumber
                + ")\">\n<table>\n");

        String line;
        while (null != (line = source.readLine())) {
          line = "<td><pre class=\"prettyprint\">" + htmlEscape(line) + "  </pre></td>";
          report.write(
              "<tr id=\"source-"
                  + lineNumber
                  + "\"><td><pre>"
                  + lineNumber
                  + "</pre></td>"
                  + line
                  + "</tr>\n");
          lineNumber++;
        }

        report.write("</table></div>\n");

      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Inserting source code failed.");
      }

    } else {
      report.write("<p>No Source-File available</p>");
    }
  }


  private void insertConfiguration(Writer report) throws IOException {

    Iterable<String> lines = LINE_SPLITTER.split(config.asPropertiesString());

    int iterator = 0;
    for (String line : lines) {
      line = "<pre id=\"config-" + iterator + "\">" + htmlEscape(line) + "</pre>\n";
      report.write(line);
      iterator++;
    }
  }

  private void insertLog(Writer bufferedWriter) throws IOException {
    if (logFile != null && Files.isReadable(logFile)) {
      try (BufferedReader log = Files.newBufferedReader(logFile, Charset.defaultCharset())) {

        int iterator = 0;
        String line;
        while (null != (line = log.readLine())) {
          line = "<pre id=\"log-" + iterator + "\">" + htmlEscape(line) + "</pre>\n";
          bufferedWriter.write(line);
          iterator++;
        }

      } catch (IOException e) {
        logger.logUserException(WARNING, e, "Could not create report: Adding log failed.");
      }

    } else {
      bufferedWriter.write("<p>No Log-File available</p>");
    }
  }

  private void insertFCallEdges(Writer report, DOTBuilder2 dotBuilder) throws IOException {
    report.write("var fCallEdges = ");
    dotBuilder.writeFunctionCallEdges(report);
    report.write(";\n");
  }

  private void insertCombinedNodesData(Writer report, DOTBuilder2 dotBuilder) throws IOException {
    report.write("var combinedNodes = ");
    dotBuilder.writeCombinedNodes(report);
    report.write(";\n");
  }

  private void insertCfaInfoData(Writer report, DOTBuilder2 dotBuilder) throws IOException {
    report.write("var cfaInfo = ");
    dotBuilder.writeCfaInfo(report);
    report.write(";\n");
  }

  private void insertErrorPathData(CounterexampleInfo counterExample, Writer report)
      throws IOException {
    report.write("var errorPathData = ");
    counterExample.toJSON(report);
    report.write(";\n");
  }

  private void insertFunctionNames(Writer report, CFA cfa) {
    try {
      report.write("var functions = ");
      JSON.writeJSONString(cfa.getAllFunctionNames(), report);
      report.write(";\n");

    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Insertion of function names failed.");
    }
  }

  private void insertSourceFileNames(Writer report) {
    try{
      report.write("var sourceFiles = ");
      JSON.writeJSONString(sourceFiles, report);
      report.write(";\n");

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
