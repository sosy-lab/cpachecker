/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cmdline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import cmdline.stubs.StubFile;

import com.google.common.base.Joiner;
import common.configuration.Configuration;

import cpa.common.CPAchecker;
import cpa.common.LogManager;
import exceptions.InvalidConfigurationException;

public class CPAMain {

  public static class InvalidCmdlineArgumentException extends Exception {

    private static final long serialVersionUID = -6526968677815416436L;

    private InvalidCmdlineArgumentException(String msg) {
      super(msg);
    }
  }
  
  public static void main(String[] args) {
    // initialize various components
    Configuration cpaConfig = null;
    try {
      cpaConfig = createConfiguration(args);
    } catch (InvalidCmdlineArgumentException e) {
      System.err.println("Could not parse command line arguments: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Could not read config file " + e.getMessage());
      System.exit(1);
    }
    
    LogManager logManager = null;
    try {
      logManager = new LogManager(cpaConfig);
    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
    }
    
    // get code file name
    String[] names = cpaConfig.getPropertiesArray("analysis.programNames");
    if (names == null) {
      logManager.log(Level.SEVERE, "No code file given!");
      System.exit(1);
    }
    
    if (names.length != 1) {
      logManager.log(Level.SEVERE, 
              "Support for multiple code files is currently not implemented!");
      System.exit(1);
    }
    
    File sourceFile = new File(names[0]);
    if (!sourceFile.exists()) {
      logManager.log(Level.SEVERE, "File", names[0], "does not exist!");
      System.exit(1);
    }
    
    if (!sourceFile.isFile()) {
      logManager.log(Level.SEVERE, "File", names[0], "is not a normal file!");
      System.exit(1);
    }
    
    if (!sourceFile.canRead()) {
      logManager.log(Level.SEVERE, "File", names[0], "is not readable!");
      System.exit(1);
    }

    // run analysis
    CPAchecker cpachecker = null;
    try {
      cpachecker = new CPAchecker(cpaConfig, logManager);
    } catch (InvalidConfigurationException e) {
      logManager.log(Level.SEVERE, "Invalid configuration:", e.getMessage());
      System.exit(1);
    }
    cpachecker.run(new StubFile(names[0]));
    
    //ensure all logs are written to the outfile
    logManager.flush();
  }

  public static Configuration createConfiguration(String[] args)
          throws InvalidCmdlineArgumentException, IOException {
    // get the file name
    String fileName = getConfigFileName(args);
    
    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = Collections.emptyMap();
    if (args != null) {
       cmdLineOptions = processArguments(args);                
    }

    Configuration config = new Configuration(fileName, cmdLineOptions);

    //normalizeValues();
    return config;
  }

  /**
   * if -config is specified in arguments, loads this properties file,
   * otherwise loads the file from a default location. Default properties file is
   * $CPACheckerMain/default.properties
   * @param args commandline arguments
   */
  private static String getConfigFileName(String[] args) throws InvalidCmdlineArgumentException {
    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      if (argsIt.next().equals("-config")) {
        if (argsIt.hasNext()) {
          return argsIt.next();
        } else {
          throw new InvalidCmdlineArgumentException("Argument to -config parameter missing!");
        }
      }
    }
    throw new InvalidCmdlineArgumentException("No -config parameter specified!");
  }

  /**
   * Reads the arguments and process them.
   * @param args commandline arguments
   * @return a map with all options found in the command line
   * @throws InvalidCmdlineArgumentException if there is an error in the command line
   */
  private static Map<String, String> processArguments(String[] args) throws InvalidCmdlineArgumentException {
    Map<String, String> properties = new HashMap<String, String>();
    List<String> programs = new ArrayList<String>();

    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      String arg = argsIt.next();
      if (   handleArgument1("-outputpath", "output.path", arg, argsIt, properties)
          || handleArgument1("-logfile", "log.file", arg, argsIt, properties)
          || handleArgument1("-cfafile", "cfa.file", arg, argsIt, properties)
          || handleArgument1("-predlistpath", "predicates.path", arg, argsIt, properties)
          || handleArgument1("-entryfunction", "analysis.entryFunction", arg, argsIt, properties)
      ) { 
        // nothing left to do 

      } else if (arg.equals("-dfs")) {
        properties.put("analysis.traversal", "dfs");
      } else if (arg.equals("-bfs")) {
        properties.put("analysis.traversal", "bfs");
      } else if (arg.equals("-topsort")) {
        properties.put("analysis.traversal", "topsort");
      } else if (arg.equals("-nolog")) {
        properties.put("log.level", "off");
        properties.put("log.consoleLevel", "off");
      } else if (arg.equals("-setprop")) {
        if (argsIt.hasNext()) {
          String[] bits = argsIt.next().split("=");
          if (bits.length != 2) {
            throw new InvalidCmdlineArgumentException(
                "-setprop argument must be a key=value pair!");
          }
          properties.put(bits[0], bits[1]);
        } else {
          throw new InvalidCmdlineArgumentException("-setprop argument missing!");
        }
      } else if (arg.equals("-help")) {
        System.out.println("OPTIONS:");
        System.out.println(" -outputpath");
        System.out.println(" -logfile");
        System.out.println(" -cfafile");
        System.out.println(" -predlistpath");
        System.out.println(" -entryfunction");
        System.out.println(" -dfs");
        System.out.println(" -bfs");
        System.out.println(" -nolog");
        System.out.println(" -setprop");
        System.out.println(" -help");
        System.exit(0);
      } else if (arg.equals("-config")) {
        // this has been processed earlier, in loadFileName
        argsIt.next(); // ignore config file name argument
      } else {
        programs.add(arg);
      }
    }

    // arguments with non-specified options are considered as file names
    if (!programs.isEmpty()) {
      properties.put("analysis.programNames", Joiner.on(", ").join(programs));
    }
    return properties;
  }

  /**
   * Handle a command line argument with one value.
   */
  private static boolean handleArgument1(String arg, String option, String currentArg,
        Iterator<String> args, Map<String, String> properties)
        throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {
        properties.put(option, args.next());
      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing!");
      }
      return true;
    } else {
      return false;
    }
  }

// TODO implement this when you get really bored
//  private void normalizeValues() {
//    for (Enumeration<?> keys = propertyNames(); keys.hasMoreElements();) {
//      String k = (String) keys.nextElement();
//      String v = getProperty(k);
//    
//      // trim heading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
//      String v0 = v;
//      v = v.trim();
//      if (!v.equals(v0)) {
//        put(k, v);
//      }
//    
//      if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
//            || "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)) {
//        put(k, "true");
//      } else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
//            || "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)) {
//        put(k, "false");
//      }
//    }
//  }
}
