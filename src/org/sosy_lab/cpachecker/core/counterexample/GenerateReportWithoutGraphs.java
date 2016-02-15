package org.sosy_lab.cpachecker.core.counterexample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

public class GenerateReportWithoutGraphs {
  private LogManager logger;
  private static final Path INPUT_ROOT = Paths.get("scripts/generate-report-with-graphs/");
  private static final Path OUTPUT_ROOT = Paths.get("output/report/");
  private static final Path USED_CONFIGURATION_PATH = Paths.get("output/UsedConfiguration.properties");
  private static final String HTML_TEMPLATE = "report_template.html";
  private static final String JS_TEMPLATE = "app/app_template.js";
  private static final String OUT_HTML = "report_withoutGraphs_%d.html";
  private static final String NO_PATHS_OUT_HTML = "report_withoutGraphs.html";
  private static final String OUT_JS = "app/app_%d.js";
  private static final String NO_PATHS_OUT_JS = "app/app.js";
  private String cpaOutDir = "";
  private String configFile = "";
  private String statisticsFile = "";
  private String logFile = "";
  private List<String> errorPathFiles = new ArrayList<>();
  private List <String> sourceFiles = new ArrayList<>();
  private String combinedNodesFile = "";
  private String cfaInfoFile = "";
  private String fCallEdgesFile = "";
  private final CFA cfa;

  public GenerateReportWithoutGraphs(LogManager pLogger, CFA pCfa) {
    if (pLogger == null) {
      throw new IllegalArgumentException("Logger can not be null.");
    }
    logger = pLogger;
    cfa = pCfa;
  }

  public void generate() {
    if (!setupOutputEnvironment()) {
      return;
    }
    getFiles();
    int amountOfErrorPaths = getErrorpathFiles();
    if(amountOfErrorPaths != 0) {
      for(int i = 0; i < amountOfErrorPaths; i++) {
        fillOutHTMLTemplate(i);
        fillOutJSTemplate(i);
      }
    } else {
      fillOutHTMLTemplate(-1);
      fillOutJSTemplate(-1);
    }
  }

  @SuppressWarnings("CheckReturnValue") // TODO should check return value of mkdirs()
  private boolean setupOutputEnvironment() {
    OUTPUT_ROOT.resolve("app").mkdirs();
    try {
      copyFile(INPUT_ROOT.resolve("app/generic.css"),
               OUTPUT_ROOT.resolve("app/generic.css"));
    } catch (IOException | IllegalArgumentException e) {
      logger.logUserException(Level.WARNING, e, "setupOutputEnvironment: generic.css couldn't be"
          + " copied to output directory");
      return false;
    }
    return true;
  }

  private void copyFile(Path source, Path target) throws IOException {
    if (source == null) {
      throw new IllegalArgumentException("Source can not be null.");
    }
    if (!source.exists()) {
      throw new IllegalArgumentException("Source (" + source + ") doesn't exist");
    }
    if (target == null) {
      throw new IllegalArgumentException("Target can not be null.");
    }
    try (InputStream in = source.asByteSource().openBufferedStream();
         OutputStream out = target.asByteSink().openBufferedStream()) {
      byte[] buf = new byte[1024];
      int length;
      while ((length = in.read(buf)) > 0) {
        out.write(buf, 0, length);
      }
    }
  }

  private int getErrorpathFiles() {
    int i = 0;
    while(true){
      File errPath = new File(cpaOutDir + "ErrorPath." + i + ".json");
      if(errPath.exists()) {
        errorPathFiles.add(cpaOutDir + "ErrorPath." + i + ".json");
      } else {
        break;
      }
    i++;
    }
    return i;
  }

  private void getFiles() {
    if (!USED_CONFIGURATION_PATH.exists()) {
      return;
    }
    File inputFile = USED_CONFIGURATION_PATH.toFile();
    Map<String, String> config = new HashMap<>();
    String line;
    try (BufferedReader bufferedConfigReader = new BufferedReader(new FileReader(inputFile))) {
      while (null != (line = bufferedConfigReader.readLine())) {
        String[] keyValue = line.split("=", 2);
        config.put(keyValue[0].trim(), keyValue[1].trim());
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "getSourceFiles: configFile could not have been"
          + " reached");
    }
    if(config.containsKey("output.path")){
      cpaOutDir = config.get("output.path").toString();
    } else {
      cpaOutDir = "output/";
    }
    if(config.containsKey("analysis.programNames")){
      String[] sources = config.get("analysis.programNames").toString().split(",");
      for(int i = 0; i < sources.length; i++){
        sourceFiles.add(sources[i].trim());
      }
    }
    if(config.containsKey("statistics.file")){
      statisticsFile = config.get("statistics.file");
    } else {
      statisticsFile = cpaOutDir + "Statistics.txt";
    }
    if(config.containsKey("log.file")){
      logFile = config.get("log.file");
    } else {
      logFile = cpaOutDir + "CPALog.txt";
    }
    configFile = USED_CONFIGURATION_PATH.getAbsolutePath();

    combinedNodesFile = cpaOutDir + "combinednodes.json";
    cfaInfoFile = cpaOutDir + "cfainfo.json";
    fCallEdgesFile = "output/fcalledges.json";
  }


  private void fillOutHTMLTemplate(int round) {
    File inputFile = INPUT_ROOT.resolve(HTML_TEMPLATE).toFile();
    final String outFileName;
    if (round == -1) {
      outFileName = NO_PATHS_OUT_HTML;
    } else {
      outFileName = String.format(OUT_HTML, round);
    }
    File outputFile = OUTPUT_ROOT.resolve(outFileName).toFile();
    try (BufferedReader bufferedTemplateReader = new BufferedReader(new FileReader(inputFile));
         BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile))) {
      String line;
      while (null != (line = bufferedTemplateReader.readLine())) {
        if (line.contains("CONFIGURATION")) {
          insertConfiguration(configFile, bufferedWriter);
        } else if (line.contains("STATISTICS")) {
          insertStatistics(statisticsFile, bufferedWriter);
        } else if (line.contains("SOURCE")) {
          for(int j = 0; j < sourceFiles.size(); j++){
            insertSource(sourceFiles.get(j), bufferedWriter, j);
          }
        } else if (line.contains("LOG")) {
          insertLog(logFile, bufferedWriter);
        } else if (line.contains("SCRIPT") && round != -1) {
          bufferedWriter.write("<script type =\"text/javascript\" src=\"app/app_" + round
              + ".js\"></script>\n");
        } else if (line.contains("SCRIPT")) {
          bufferedWriter.write("<script type =\"text/javascript\" src=\"app/app.js\"></script>\n");
        } else {
          bufferedWriter.write(line + "\n");
        }
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "fillOutHTMLTemplate: inputFile or outputFile"
          + " couldn't have been reached");
    }
  }

  private void insertStatistics(String filePath, BufferedWriter bufferedWriter) throws IOException {
    File file = new File(filePath);
    int iterator = 0;
    if (file.exists() && file.isFile()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
          String line;
          while (null != (line = bufferedReader.readLine())) {
            line = "<pre id=\"statistics-" + iterator + "\">" + line + "</pre>\n";
            bufferedWriter.write(line);
            iterator++;
          }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertStatistics: file (" + filePath
            + ") couldn't have been reached");
      }
    } else {
      bufferedWriter.write("<p>No Statistics-File available</p>");
    }
  }

  private void insertSource(String filePath, BufferedWriter bufferedWriter, int sourceFileNumber)
      throws IOException {
    File file = new File(filePath);
    int iterator = 0;
    if (file.exists()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
          String line;
          bufferedWriter.write("<table class=\"sourceContent\" ng-show = \"sourceFileIsSet("
              + sourceFileNumber + ")\">\n");
          while (null != (line = bufferedReader.readLine())) {
            line = "<td><pre class=\"prettyprint\">" + line + "  </pre></td>";
            bufferedWriter.write("<tr id=\"source-" + iterator + "\"><td><pre>" + iterator
                + "</pre></td>" + line + "</tr>\n");
            iterator++;
          }
          bufferedWriter.write("</table>\n");
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertSource: file couldn't have been reached");
      }
    } else {
      bufferedWriter.write("<p>No Source-File available</p>");
    }
  }


  private void insertConfiguration(String filePath, BufferedWriter bufferedWriter)
      throws IOException {
    File file = new File(filePath);
    if (file.exists() && file.isFile()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
          String line;
          int iterator = 0;
          while (null != (line = bufferedReader.readLine())) {
            line = "<pre id=\"config-" + iterator + "\">" + line + "</pre>\n";
            bufferedWriter.write(line);
            iterator++;
          }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertConfiguration: file (" + filePath
            + ") couldn't have been reached");
      }
    } else {
      bufferedWriter.write("<p>No Configuration-File available</p>");
    }
  }

  private void insertLog(String filePath, BufferedWriter bufferedWriter) throws IOException {
    File file = new File(filePath);
    int iterator = 0;
    if (file.exists() && file.isFile()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
          String line;
          while (null != (line = bufferedReader.readLine())) {
            line = "<pre id=\"log-" + iterator + "\">" + line + "</pre>\n";
            bufferedWriter.write(line);
            iterator++;
          }

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertLog: file (" + filePath
            + ") couldn't have been reached");
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
    File outputFile = OUTPUT_ROOT.resolve(outFileName).toFile();
    try (BufferedReader bufferedTemplateReader = new BufferedReader(new FileReader(inputFile));
         BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile))) {
      String line;
      while (null != (line = bufferedTemplateReader.readLine())) {
        if (line.contains("ERRORPATH") && round != -1) {
          insertErrorPathData(errorPathFiles.get(round), bufferedWriter);
        } else if (line.contains("FUNCTIONS")) {
          insertFunctionNames(cpaOutDir, bufferedWriter);
        } else if (line.contains("SOURCEFILES")) {
          insertSourceFileNames(sourceFiles, bufferedWriter);
        } else if (line.contains("COMBINEDNODES")) {
          insertCombinedNodesData(combinedNodesFile, bufferedWriter);
        } else if (line.contains("CFAINFO")) {
          insertCfaInfoData(cfaInfoFile, bufferedWriter);
        } else if (line.contains("FCALLEDGES")) {
          insertFCallEdges(fCallEdgesFile, bufferedWriter);
        } else {
          bufferedWriter.write(line + "\n");
        }
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "fillOutJSTemplate: inputFile or outputFile"
          + " couldn't have been reached");
    }
  }


  private void insertFCallEdges(String filePath, BufferedWriter bufferedWriter) {
    File inputFile = new File(filePath);
    if (inputFile.exists()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
        String line;
        bufferedWriter.write("var fCallEdges = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertFCallEdges: file couldn't have been"
            + " reached");
      }
    }
  }

  private void insertCombinedNodesData(String filePath, BufferedWriter bufferedWriter) {
    File inputFile = new File(filePath);
    if (inputFile.exists()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
        String line;
        bufferedWriter.write("var combinedNodes = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertCombinedNodesData: file couldn't have"
            + " been reached");
      }
    }
  }

  private void insertCfaInfoData(String filePath, BufferedWriter bufferedWriter) {
    File inputFile = new File(filePath);
    if (inputFile.exists()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
        String line;
        bufferedWriter.write("var cfaInfo = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertCfaInfoData: file couldn't have been"
            + " reached");
      }
    }
  }

  private void insertErrorPathData(String filePath, BufferedWriter bufferedWriter) {
    File inputFile = new File(filePath);
    if (inputFile.exists()) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
        String line;
        bufferedWriter.write("var errorPathData = ");
        while (null != (line = bufferedReader.readLine())) {
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "insertErrorPathData: file couldn't have been"
            + " reached");
      }
    }
  }

  private void insertFunctionNames(String directoryPath, BufferedWriter bufferedWriter) {
    File dir = new File(directoryPath);
    if (dir.exists()) {
      String[] files = dir.list();
      if (files != null) {
        Arrays.sort(files);
        try {
          bufferedWriter.write("var functions = [");
          bufferedWriter.write(Joiner.on(',').join(FluentIterable.from(cfa.getAllFunctionNames())
              .transform(new Function<String, String>() {
            @Override
            public String apply(String pArg0) {
              return "\"" + pArg0 + "\"";
            }
          })));
          bufferedWriter.write("];\n");
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "insertFunctionNames: BufferdReader"
              + " couldn't have been closed");
        }
      }
    }
  }

  private void insertSourceFileNames(List<String> filePaths, BufferedWriter bufferedWriter){
    boolean firstFile = true;
    try{
      bufferedWriter.write("var sourceFiles = [");
      for(int i = 0; i < filePaths.size(); i++){
        if(firstFile){
          bufferedWriter.write("\""  + filePaths.get(i) + "\"");
          firstFile = false;
        } else {
          bufferedWriter.write(", \""  + filePaths.get(i) + "\"");
        }
      }
      bufferedWriter.write("];\n");
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "insertSourceFileNames: file couldn't have"
          + " been reached");
    }
  }
}
