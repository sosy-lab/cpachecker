package org.sosy_lab.cpachecker.core.counterexample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.List;
import org.sosy_lab.common.log.LogManager;

public class GenerateReportWithoutGraphs {
  private static LogManager logger;
  static String sourceFile = "";
  static String configFile = "output/UsedConfiguration.properties";
  static String statisticsFile = "output/Statistics.txt";
  static String logFile = "output/CPALog.txt";
  static String errorPathFile = "output/ErrorPath.0.json";
  static String combinedNodesFile ="output/combinednodes.json";
  static String cfaInfoFile ="output/cfainfo.json";
  static String fCallEdgesFile = "output/fcalledges.json";
  static String outputDir = "output";
  
  public GenerateReportWithoutGraphs(LogManager plogger) { 
    logger = plogger;
  }

  public static void writeSource(String source) {
    sourceFile = source;
  }
  public static void writeSources(List<String> sources){
    //TODO: handle more than one sourcefile
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
          insertSource(sourceFile, bufferedWriter);
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

  private static void insertSource(String filePath, BufferedWriter bufferedWriter) { 
    BufferedReader bufferedReader = null;
    File file = new File(filePath);
    int iterator = 0;
    try { 
      if (file.exists()) { 
        bufferedReader = new BufferedReader(new FileReader(file));
        String line;
        while (null != (line = bufferedReader.readLine())) { 
          line = "<td><pre class=\"prettyprint\">" + line + "</pre></td>";
          bufferedWriter.write("<tr id=\"source-" + iterator + "\"><td><pre>" + iterator + "</pre></td>" + line + "</tr>\n");
          iterator++;
        }
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

  public static void fillOutJSTemplate(String inputPath, String outputPath) { 
    BufferedReader bufferedTemplateReader = null;
    BufferedWriter bufferedWriter = null;
    File inputFile = new File(inputPath);
    File outputFile = new File(outputPath);
    try { 
      bufferedTemplateReader = new BufferedReader(new FileReader(inputFile));
      bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
      String line;
      while (null != (line = bufferedTemplateReader.readLine())) { 
        if (line.contains("ERRORPATH")) { 
          insertErrorPathData(errorPathFile, bufferedWriter);
        } else if (line.contains("FUNCTIONS")) { 
          insertFunctionNames(outputDir, bufferedWriter);
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
}
