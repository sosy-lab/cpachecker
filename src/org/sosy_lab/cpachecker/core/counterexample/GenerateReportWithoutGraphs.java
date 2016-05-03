package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

  private static final Path INPUT_ROOT = Paths.get("scripts/generate-report-with-graphs/");
  private static final Path REPORT = Paths.get("report");
  private static final String HTML_TEMPLATE = "report_template.html";
  private static final String JS_TEMPLATE = "app/app_template.js";
  private static final String OUT_HTML = "report_withoutGraphs_%d.html";
  private static final String NO_PATHS_OUT_HTML = "report_withoutGraphs.html";
  private static final String OUT_JS = "app/app_%d.js";
  private static final String NO_PATHS_OUT_JS = "app/app.js";

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;

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

  @Nullable private final Path outputPath;
  @Nullable private final Path reportDir;
  @Nullable private final Path combinedNodesPath;
  @Nullable private final Path cfaInfoPath;
  @Nullable private final Path fCallEdgesPath;

  private final List<Path> errorPathFiles = new ArrayList<>();
  private final List<String> sourceFiles = new ArrayList<>();

  public GenerateReportWithoutGraphs(Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    config = checkNotNull(pConfig);
    logger = checkNotNull(pLogger);
    cfa = checkNotNull(pCfa);

    config.inject(this);
    if (statisticsFile != null) {
      outputPath = statisticsFile.getParent();
    } else if (logFile != null) {
      outputPath = logFile.getParent();
    } else {
      outputPath = null;
    }

    if (outputPath != null) {
      combinedNodesPath = outputPath.resolve(DOTBuilder2.COMBINED_NODES);
      fCallEdgesPath = outputPath.resolve(DOTBuilder2.F_CALL_EDGES);
      cfaInfoPath = outputPath.resolve(DOTBuilder2.CFA_INFO);

      reportDir = outputPath.resolve(REPORT);

      sourceFiles.addAll(COMMA_SPLITTER.splitToList(programs));
      errorPathFiles.addAll(getErrorPathFiles());

    } else {
      combinedNodesPath = null;
      fCallEdgesPath = null;
      cfaInfoPath = null;
      reportDir = null;
    }
  }

  private List<Path> getErrorPathFiles() {
    List<Path> errorPaths = Lists.newArrayList();
    PathTemplate errorPathTemplate = PathTemplate.ofFormatString(outputPath + "ErrorPath.%d.json");

    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      Path errorPath = errorPathTemplate.getPath(i);
      if (errorPath.exists()) {
        errorPaths.add(errorPath);

      } else {
        break;
      }
    }

    return errorPaths;
  }

  public void generate() {
    if (outputPath == null) {
      return; // output is disabled
    }

    if (errorPathFiles.isEmpty()) {
      fillOutHTMLTemplate(-1);
      fillOutJSTemplate(-1);

    } else {
      for (int i = 0; i < errorPathFiles.size(); i++) {
        fillOutHTMLTemplate(i);
        fillOutJSTemplate(i);
      }

    }
  }

  private void fillOutHTMLTemplate(int round) {
    File templateFile = INPUT_ROOT.resolve(HTML_TEMPLATE).toFile();
    final String outFileName;
    if (round == -1) {
      outFileName = NO_PATHS_OUT_HTML;
    } else {
      outFileName = String.format(OUT_HTML, round);
    }

    Path reportPath = reportDir.resolve(outFileName);
    try {
      Files.createParentDirs(reportPath);
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report.");
      return;
    }

    try (BufferedReader template =
            new BufferedReader(
                new InputStreamReader(new FileInputStream(templateFile), Charset.defaultCharset()));
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
        } else if (line.contains("SOURCE")) {
          insertSources(report);
        } else if (line.contains("LOG")) {
          insertLog(report);
        } else if (line.contains("SCRIPT") && round != -1) {
          report.write(
              "<script type =\"text/javascript\" src=\"app/app_" + round + ".js\"></script>\n");
        } else if (line.contains("SCRIPT")) {
          report.write("<script type =\"text/javascript\" src=\"app/app.js\"></script>\n");
        } else {
          report.write(line + "\n");
        }
      }

    } catch (IOException e) {
      logger.logUserException(
          WARNING, e, "Could not create report: Procesing of HTML template failed.");
    }
  }

  private void insertStatistics(BufferedWriter bufferedWriter) throws IOException {
    if (statisticsFile.exists() && statisticsFile.isFile()) {
      int iterator = 0;
      try (BufferedReader statistics =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(statisticsFile.toFile()), Charset.defaultCharset()))) {

        String line;
        while (null != (line = statistics.readLine())) {
          line = "<pre id=\"statistics-" + iterator + "\">" + line + "</pre>\n";
          bufferedWriter.write(line);
          iterator++;
        }

      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Writing of statistics failed.");
      }

    } else {
      bufferedWriter.write("<p>No Statistics-File available</p>");
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

  private void fillOutJSTemplate(int round) {
    File inputFile = INPUT_ROOT.resolve(JS_TEMPLATE).toFile();
    final String outFileName;
    if (round == -1) {
      outFileName = NO_PATHS_OUT_JS;
    } else {
      outFileName = String.format(OUT_JS, round);
    }
    File outputFile = reportDir.resolve(outFileName).toFile();
    try (BufferedReader bufferedTemplateReader =
            new BufferedReader(
                new InputStreamReader(new FileInputStream(inputFile), Charset.defaultCharset()));
        BufferedWriter bufferedWriter =
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(outputFile), Charset.defaultCharset()))) {
      String line;
      while (null != (line = bufferedTemplateReader.readLine())) {
        if (line.contains("ERRORPATH") && round != -1) {
          insertErrorPathData(errorPathFiles.get(round), bufferedWriter);
        } else if (line.contains("FUNCTIONS")) {
          insertFunctionNames(bufferedWriter);
        } else if (line.contains("SOURCEFILES")) {
          insertSourceFileNames(bufferedWriter);
        } else if (line.contains("COMBINEDNODES")) {
          insertCombinedNodesData(bufferedWriter);
        } else if (line.contains("CFAINFO")) {
          insertCfaInfoData(bufferedWriter);
        } else if (line.contains("FCALLEDGES")) {
          insertFCallEdges(bufferedWriter);
        } else {
          bufferedWriter.write(line + "\n");
        }
      }
    } catch (IOException e) {
      logger.logUserException(WARNING, e, "Could not create report.");
    }
  }


  private void insertFCallEdges(BufferedWriter bufferedWriter) {
    if (fCallEdgesPath != null && fCallEdgesPath.exists()) {
      try (BufferedReader bufferedReader =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(fCallEdgesPath.toFile()), Charset.defaultCharset()))) {
        String line;
        bufferedWriter.write("var fCallEdges = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Insertion of function call edges failed.");
      }
    }
  }

  private void insertCombinedNodesData(Writer report) {

    if (combinedNodesPath != null && combinedNodesPath.exists()) {
      try (BufferedReader combinedNodes =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(combinedNodesPath.toFile()), Charset.defaultCharset()))) {

        String line;
        report.write("var combinedNodes = ");
        while (null != (line = combinedNodes.readLine())) {
          report.write(line);
        }
        report.write(";\n");

      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Insertion of combindes nodes failed.");
      }
    }
  }

  private void insertCfaInfoData(Writer report) {
    if (cfaInfoPath != null && cfaInfoPath.exists()) {
      try (BufferedReader bufferedReader =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(cfaInfoPath.toFile()), Charset.defaultCharset()))) {

        String line;
        report.write("var cfaInfo = ");
        while (null != (line = bufferedReader.readLine())) {
          report.write(line);
        }
        report.write(";\n");

      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Insertion of CFA info failed.");
      }
    }
  }

  private void insertErrorPathData(Path errorPatData, BufferedWriter bufferedWriter) {
    if (errorPatData.exists()) {
      try (BufferedReader bufferedReader =
          new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(errorPatData.toFile()), Charset.defaultCharset()))) {

        String line;
        bufferedWriter.write("var errorPathData = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");

      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "Could not create report: Insertion of error path data failed.");
      }
    }
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

  private void insertSourceFileNames(BufferedWriter report) {
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
