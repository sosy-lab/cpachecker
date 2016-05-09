package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;

import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder2;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@Options
public class GenerateReportWithoutGraphs {

  private static final Splitter LINE_SPLITTER = Splitter.on('\n');
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();

  private static final Path SCRIPTS = Paths.get("scripts");
  private static final Path HTML_TEMPLATE = SCRIPTS.resolve("report_template.html");

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final UnmodifiableReachedSet reached;
  private final String statistics;
  private final DOTBuilder2 dotBuilder;

  @Option(
    secure = true,
    name = "analysis.programNames",
    description = "A String, denoting the programs to be analyzed"
  )
  private String programs;

  @Option(secure = true, name = "log.file", description = "name of the log file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path logFile = Paths.get("CPALog.txt");

  @Option(secure = true, name = "statistics.file", description = "write some statistics to disk")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path statisticsFile = Paths.get("Statistics.txt");

  @Option(secure = true, name = "report.file.", description = "export report as HTML")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reportFile = Paths.get("Report.html");

  @Option(secure = true, name = "counterexample.report.file", description = "export counterexample as HTML")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate counterExampleFiles = PathTemplate.ofFormatString("Counterexample.%d.html");

  private final List<String> sourceFiles = new ArrayList<>();

  public GenerateReportWithoutGraphs(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      UnmodifiableReachedSet pReached,
      String pStatistics)
      throws InvalidConfigurationException {
    config = checkNotNull(pConfig);
    logger = checkNotNull(pLogger);
    cfa = checkNotNull(pCfa);
    reached = checkNotNull(pReached);
    statistics = checkNotNull(pStatistics);
    dotBuilder = new DOTBuilder2(pCfa);
    config.inject(this);
    sourceFiles.addAll(COMMA_SPLITTER.splitToList(programs));
  }

  public void generate() {
    if (reportFile == null || counterExampleFiles == null) {
      return; // output is disabled
    }

    Iterable<CounterexampleInfo> counterExamples =
        Optional.presentInstances(
            from(reached.asCollection())
                .filter(IS_TARGET_STATE)
                .filter(ARGState.class)
                .transform(new ExtractCounterExampleInfo()));

    if (!counterExamples.iterator().hasNext()) {
      fillOutTemplate(null, reportFile);

    } else {
      int index = 0;
      for (CounterexampleInfo counterExample : counterExamples) {
        fillOutTemplate(counterExample, counterExampleFiles.getPath(index));
        index++;
      }
    }
  }

  private static class ExtractCounterExampleInfo
      implements Function<ARGState, Optional<CounterexampleInfo>> {

    @Override
    public Optional<CounterexampleInfo> apply(ARGState state) {
      return state.getCounterexampleInformation();
    }
  }

  private void fillOutTemplate(@Nullable CounterexampleInfo counterExample, Path reportPath) {
    try {
      Files.createParentDirs(reportPath);
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report.");
      return;
    }

    try (BufferedReader template =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(HTML_TEMPLATE.toFile()), Charset.defaultCharset()));
        BufferedWriter report =
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(reportPath.toFile()), Charset.defaultCharset()))) {

      String line;
      while (null != (line = template.readLine())) {
        if (line.contains("CONFIGURATION")) {
          insertConfiguration(report);
        } else if (line.contains("STATISTICS")) {
          insertStatistics(report);
        } else if (line.contains("SOURCE_CONTENT")) {
          insertSources(report);
        } else if (line.contains("LOG")) {
          insertLog(report);
        } else if (line.contains("ERRORPATH") && counterExample != null) {
          insertErrorPathData(counterExample, report);
        } else if (line.contains("FUNCTIONS")) {
          insertFunctionNames(report);
        } else if (line.contains("SOURCE_FILE_NAMES")) {
          insertSourceFileNames(report);
        } else if (line.contains("COMBINEDNODES")) {
          insertCombinedNodesData(report);
        } else if (line.contains("CFAINFO")) {
          insertCfaInfoData(report);
        } else if (line.contains("FCALLEDGES")) {
          insertFCallEdges(report);
        } else {
          report.write(line + "\n");
        }
      }

    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Procesing of HTML template failed.");
    }
  }

  private void insertStatistics(Writer report) throws IOException {
    int iterator = 0;
    for (String line : LINE_SPLITTER.split(statistics)) {
      line = "<pre id=\"statistics-" + iterator + "\">" + line + "</pre>\n";
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

    if (sourcePath.exists()) {

      int iterator = 0;
      try (BufferedReader source =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(sourcePath.toFile()), Charset.defaultCharset()))) {

        report.write(
            "<table class=\"sourceContent\" ng-show = \"sourceFileIsSet("
                + sourceFileNumber
                + ")\">\n");

        String line;
        while (null != (line = source.readLine())) {
          line = "<td><pre class=\"prettyprint\">" + line + "  </pre></td>";
          report.write(
              "<tr id=\"source-"
                  + iterator
                  + "\"><td><pre>"
                  + iterator
                  + "</pre></td>"
                  + line
                  + "</tr>\n");
          iterator++;
        }

        report.write("</table>\n");

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
      line = "<pre id=\"config-" + iterator + "\">" + line + "</pre>\n";
      report.write(line);
      iterator++;
    }
  }

  private void insertLog(Writer bufferedWriter) throws IOException {
    if (logFile != null && logFile.exists()) {
      try (BufferedReader log =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(logFile.toFile()), Charset.defaultCharset()))) {

        int iterator = 0;
        String line;
        while (null != (line = log.readLine())) {
          line = "<pre id=\"log-" + iterator + "\">" + line + "</pre>\n";
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

  private void insertFCallEdges(Writer report) throws IOException {
    report.write("var fCallEdges = ");
    dotBuilder.writeFunctionCallEdges(report);
    report.write(";\n");
  }

  private void insertCombinedNodesData(Writer report) throws IOException {
    report.write("var combinedNodes = ");
    dotBuilder.writeCombinedNodes(report);
    report.write(";\n");
  }

  private void insertCfaInfoData(Writer report) throws IOException {
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

  private void insertFunctionNames(BufferedWriter report) {
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
}
