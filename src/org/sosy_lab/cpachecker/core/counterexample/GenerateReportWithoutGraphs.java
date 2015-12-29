package org.sosy_lab.cpachecker.core.counterexample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import org.sosy_lab.common.log.LogManager;

public class GenerateReportWithoutGraphs {
  private static LogManager logger;
  static String cpaOutDir = "";
  static String configFile = "";
  static String statisticsFile = "";
  static String logFile = "";
  static List<String> errorPathFiles = new ArrayList();
  static List <String> sourceFiles = new ArrayList();
  static String combinedNodesFile ="";
  static String cfaInfoFile ="";
  static String fCallEdgesFile = "";
  
  public GenerateReportWithoutGraphs(LogManager plogger) { 
    logger = plogger;
  }
  
  public static int getErrorpathFiles() {
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
    return errorPathFiles.size();
  }

  public static void getFiles(String configPath) {
    BufferedReader bufferedConfigReader = null;
    File inputFile = new File(configPath);
    Map<String, String> config = new HashMap<String, String>();
    String line;
    try {
      bufferedConfigReader = new BufferedReader(new FileReader(inputFile));
      while (null != (line = bufferedConfigReader.readLine())) {
        String[] keyValue = line.split("=", 2); 
        config.put(keyValue[0].trim(), keyValue[1].trim());
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "getSourceFiles: configFile could not have been reached");
    } finally {
      if (null != bufferedConfigReader) {
        try { 
          bufferedConfigReader.close();
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "getSourceFiles: BufferdReader could not have been closed");
        }
      }
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
    configFile = configPath;
    combinedNodesFile = cpaOutDir + "combinednodes.json";
    cfaInfoFile = cpaOutDir + "cfainfo.json";
    fCallEdgesFile = "output/fcalledges.json";
  }


  public static void fillOutHTMLTemplate(String inputPath, String outputPath) {
    BufferedReader bufferedTemplateReader = null;
    BufferedWriter bufferedWriter = null;
    File inputFile = new File(inputPath);
    File outputFile = new File(outputPath);
    try {
      bufferedTemplateReader = new BufferedReader(new FileReader(inputFile));
      bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
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
        } else { 
          bufferedWriter.write(line + "\n");
        }
      }
    } catch (IOException e) { 
      logger.logUserException(Level.WARNING, e, "fillOutHTMLTemplate: inputFile or outputFile couldn't have been reached");
    } finally { 
      if (null != bufferedTemplateReader) { 
        try { 
          bufferedTemplateReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "fillOutHTMLTemplate: BufferdReader couldn't have been closed");
        }
      }
      if (null != bufferedWriter) { 
        try { 
          bufferedWriter.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "fillOutHTMLTemplate: BufferdWriter couldn't have been closed");
        }
      }
    }
    
  }

  private static void insertStatistics(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File file = new File(filePath);
    int iterator = 0;
    try { 
      if (file.exists()) { 
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        while (null != (line = bufferedReader.readLine())) { 
          line = "<pre id=\"statistics-" + iterator + "\">" + line + "</pre>\n";
          bufferedWriter.write(line);
          iterator++;
        }
      } else { 
        bufferedWriter.write("<p>No Statistics-File available</p>");
      }
    } catch (IOException e) { 
      logger.logUserException(Level.WARNING, e, "insertStatistics: file couldn't have been reached");
    } finally { 
      if (null != bufferedReader) { 
        try { 
          bufferedReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "insertStatistics: BufferdReader couldn't have been closed");
        }
      }
    }
  }

  private static void insertSource(String filePath, BufferedWriter bufferedWriter, int sourceFileNumber) {
    BufferedReader bufferedReader = null;
    File file = new File(filePath);
    int iterator = 0;
    try { 
      if (file.exists()) { 
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        bufferedWriter.write("<table class=\"sourceContent\" ng-show = \"report.sourceFileIsSet(" + sourceFileNumber + ")\">\n");
        while (null != (line = bufferedReader.readLine())) { 
          line = "<td><pre class=\"prettyprint\">" + line + "</pre></td>";
          bufferedWriter.write("<tr id=\"source-" + iterator + "\"><td><pre>" + iterator + "</pre></td>" + line + "</tr>\n");
          iterator++;
        }
        bufferedWriter.write("</table>\n");
      } else { 
        bufferedWriter.write("<p>No Source-File available</p>");
      }
    } catch (IOException e) { 
      logger.logUserException(Level.WARNING, e, "insertSource: file couldn't have been reached");
    } finally { 
      if (null != bufferedReader) { 
        try { 
          bufferedReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "insertSource: BufferdReader couldn't have been closed");
        }
      }
    }
  }


  private static void insertConfiguration(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File file = new File(filePath);
    try { 
      if (file.exists()) { 
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        int iterator = 0;
        while (null != (line = bufferedReader.readLine())) { 
          line = "<pre id=\"config-" + iterator + "\">" + line + "</pre>\n";
          bufferedWriter.write(line);
          iterator++;
        }
      } else { 
        bufferedWriter.write("<p>No Configuration-File available</p>");
      }
    } catch (IOException e) { 
      logger.logUserException(Level.WARNING, e, "insertConfiguration: file couldn't have been reached");
    } finally { 
      if (null != bufferedReader) { 
        try { 
          bufferedReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "insertConfiguration: BufferdReader couldn't have been closed");
        }
      }
    }
  }

  private static void insertLog(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File file = new File(filePath);
    int iterator = 0;
    try { 
      if (file.exists()) { 
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        while (null != (line = bufferedReader.readLine())) { 
          line = "<pre id=\"log-" + iterator + "\">" + line + "</pre>\n";
          bufferedWriter.write(line);
          iterator++;
        }
      } else { 
        bufferedWriter.write("<p>No Log-File available</p>");
      }
    } catch (IOException e) { 
      logger.logUserException(Level.WARNING, e, "insertLog: file couldn't have been reached");
    } finally { 
      if (null != bufferedReader) { 
        try { 
          bufferedReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "insertLog: BufferdReader couldn't have been closed");
        }
      }
    }
  }

  public static void fillOutJSTemplate(String inputPath, String outputPath, int round) {
    BufferedReader bufferedTemplateReader = null;
    BufferedWriter bufferedWriter = null;
    File inputFile = new File(inputPath);
    File outputFile = new File(outputPath);
    try { 
      bufferedTemplateReader = new BufferedReader(new FileReader(inputFile));
      bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
      String line;
      while (null != (line = bufferedTemplateReader.readLine())) { 
        if (line.contains("ERRORPATH") && round != -1) { 
          insertErrorPathData(errorPathFiles.get(round), bufferedWriter);
        } else if (line.contains("ERRORPATH") && round == -1) {
          //TODO: Set the left panel to width 0 and disable evtl search functionality
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
      logger.logUserException(Level.WARNING, e, "fillOutJSTemplate: inputFile or outputFile couldn't have been reached");
    } finally { 
      if (null != bufferedTemplateReader) { 
        try { 
          bufferedTemplateReader.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "fillOutJSTemplate: BufferedReader couldn't have been closed");
        }
      }
      if (null != bufferedWriter) { 
        try { 
          bufferedWriter.close();
        } catch (IOException e) { 
          logger.logUserException(Level.WARNING, e, "fillOutJSTemplate: BufferedWriter couldn't have been closed");
        }
      }
    }
  }
  

  private static void insertFCallEdges(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File inputFile = new File(filePath);
    if (inputFile.exists()) { 
      try { 
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line;
        bufferedWriter.write("var fCallEdges = ");
        while (null != (line = bufferedReader.readLine())) { 
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) { 
        logger.logUserException(Level.WARNING, e, "insertFCallEdges: file couldn't have been reached");
      } finally { 
        if (null != bufferedReader) { 
          try { 
            bufferedReader.close();
          } catch (IOException e) { 
            logger.logUserException(Level.WARNING, e, "insertFCallEdges: BufferdReader couldn't have been closed");
          }
        }
      }
    }
  }

  private static void insertCombinedNodesData(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File inputFile = new File(filePath);
    if (inputFile.exists()) { 
      try { 
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line;
        bufferedWriter.write("var combinedNodes = ");
        while (null != (line = bufferedReader.readLine())) { 
          bufferedWriter.write(line);
        } 
        bufferedWriter.write(";\n");
      } catch (IOException e) { 
        logger.logUserException(Level.WARNING, e, "insertCombinedNodesData: file couldn't have been reached");
      } finally { 
        if (null != bufferedReader) { 
          try { 
            bufferedReader.close();
          } catch (IOException e) { 
            logger.logUserException(Level.WARNING, e, "insertCombinedNodesData: BufferdReader couldn't have been closed");
          }
        }
      }
    }
  }

  private static void insertCfaInfoData(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File inputFile = new File(filePath);
    if (inputFile.exists()) { 
      try { 
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line;
        bufferedWriter.write("var cfaInfo = ");
        while (null != (line = bufferedReader.readLine())) { 
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) { 
        logger.logUserException(Level.WARNING, e, "insertCfaInfoData: file couldn't have been reached");
      } finally { 
        if (null != bufferedReader) { 
          try { 
            bufferedReader.close();
          } catch (IOException e) { 
            logger.logUserException(Level.WARNING, e, "insertCfaInfoData: BufferdReader couldn't have been closed");
          }
        }
      }
    }
  }

  private static void insertErrorPathData(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File inputFile = new File(filePath);
    if (inputFile.exists()) { 
      try { 
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line;
        bufferedWriter.write("var errorPathData = ");
        while (null != (line = bufferedReader.readLine())) { 
          bufferedWriter.write(line);
        }
        bufferedWriter.write(";\n");
      } catch (IOException e) { 
        logger.logUserException(Level.WARNING, e, "insertErrorPathData: file couldn't have been reached");
      } finally { 
        if (null != bufferedReader) { 
          try { 
            bufferedReader.close();
          } catch (IOException e) { 
            logger.logUserException(Level.WARNING, e, "insertErrorPathData: BufferdReader couldn't have been closed");
          }
        }
      }
    }
  }

  private static void insertFunctionNames(String directoryPath, BufferedWriter bufferedWriter) { 
    File dir = new File(directoryPath);
    if (dir.exists()) { 
      String[] files = dir.list();
      Arrays.sort(files);
      String line;
      boolean firstFunction = true;
      try { 
        bufferedWriter.write("var functions = [");
        for (int i = 0; i < files.length; i++) { 
          if (files[i].contains("cfa__") && files[i].contains(".svg")) { 
            line = files[i].substring(5, (files[i].length() - 4));
            if (firstFunction) { 
              bufferedWriter.write("\"" + line + "\"");
              firstFunction = false;
            } else { 
              bufferedWriter.write(", \"" + line + "\"");
            }
          }
        }
        bufferedWriter.write("];\n");
      } catch (IOException e) { 
        logger.logUserException(Level.WARNING, e, "insertFunctionNames: BufferdReader couldn't have been closed");
      }
    }
  }
  
  private static void insertSourceFileNames(List<String> filePaths, BufferedWriter bufferedWriter){
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
      logger.logUserException(Level.WARNING, e, "insertSourceFileNames: file couldn't have been reached");
    }
  }
}
