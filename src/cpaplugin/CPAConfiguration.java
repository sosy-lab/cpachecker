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
package cpaplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 * CPA Checker properties file. Processes the properties file and save them as strings.
 * If -config is not among program arguments, reads properties file from a default
 * location. If properties are modified via command line arguments, they are processed
 * and related properties keys are modified by this class.
 * @author erkan
 *
 */
public class CPAConfiguration extends Properties{
  
    public static class InvalidCmdlineArgumentException extends Exception {

      private static final long serialVersionUID = -6526968677815416436L;

      private InvalidCmdlineArgumentException(String msg) {
        super(msg);
      }
    }
  
    private static final long serialVersionUID = -5910186668866464153L;
    /** Delimiters to create string arrays */
    private static final String DELIMS = "[;, ]+";

    // TODO use arguments later to change config values dynamically
    /**
     * Class constructor to process arguments and load file.
     * @param args arguments to change values dynamically
     */
    public CPAConfiguration(String[] args) throws InvalidCmdlineArgumentException, IOException {

        // get the file name
        String fileName = getConfigFileName(args);
        // load the file
        loadFile(fileName);
        // if there are some commandline arguments, process them
        if (args != null) {
          processArgs(args);                
        }
        //normalizeValues();
    }

    public CPAConfiguration(String fileName) throws IOException {
      // load the file
      loadFile(fileName);
  }
    
    /**
     * if -config is specified in arguments, loads this properties file,
     * otherwise loads the file from a default location. Default properties file is
     * $CPACheckerMain/default.properties
     * @param args commandline arguments
     */
    private String getConfigFileName(String[] args) throws InvalidCmdlineArgumentException{
        Iterator<String> argsIt = Arrays.asList(args).iterator();
  
        while (argsIt.hasNext()) {
          if (argsIt.next().equals("-config")) {
            if (argsIt.hasNext()) {
              return argsIt.next();
            } else {
              throw new InvalidCmdlineArgumentException("-config argument missing!");
            }
          }
        }
        return null;
    }
    
    private String getDefaultConfigFileName() {
      // TODO use resources for this?
      URL binDir = getClass().getProtectionDomain().getCodeSource().getLocation();
      String binDirString = binDir.getPath();
      int index = binDirString.lastIndexOf(File.separatorChar);
      binDirString = binDirString.substring(0, index);
      return binDirString + ".." + File.separatorChar + "default.properties";
    }

    /**
     * Handle a command line argument with one value.
     */
    private boolean handleArgument1(String arg, String option, String currentArg, Iterator<String> args)
          throws InvalidCmdlineArgumentException {
      if (currentArg.equals(arg)) {
        if (args.hasNext()) {
          this.setProperty(option, args.next());
        } else {
          throw new InvalidCmdlineArgumentException(currentArg + " argument missing!");
        }
        return true;
      } else {
        return false;
      }
    }
    
    /**
     * Reads the arguments and process them. If a corresponding key is found, the property
     * is updated
     * @param args commandline arguments
     * @throws Exception if an option is set but no value for the option is found
     */
    private void processArgs(String[] args) throws InvalidCmdlineArgumentException {
        List<String> ret = new ArrayList<String>();
        
        Iterator<String> argsIt = Arrays.asList(args).iterator();

        while (argsIt.hasNext()) {
            String arg = argsIt.next();
            if (   handleArgument1("-outputpath", "output.path", arg, argsIt)
                || handleArgument1("-logfile", "log.file", arg, argsIt)
                || handleArgument1("-cfafile", "cfa.file", arg, argsIt)
                || handleArgument1("-predlistpath", "predicates.path", arg, argsIt)
                || handleArgument1("-entryfunction", "analysis.entryFunction", arg, argsIt)
               ) { 
              // nothing left to do 
            
            } else if (arg.equals("-dfs")) {
                this.setProperty("analysis.traversal", "dfs");
            } else if (arg.equals("-bfs")) {
                this.setProperty("analysis.traversal", "bfs");
            } else if (arg.equals("-topsort")) {
              this.setProperty("analysis.traversal", "topsort");
            } else if (arg.equals("-nolog")) {
                this.setProperty("log.level", "off");
                this.setProperty("log.consoleLevel", "off");
            } else if (arg.equals("-setprop")) {
                if (argsIt.hasNext()) {
                    String[] bits = argsIt.next().split("=");
                    if (bits.length != 2) {
                        throw new InvalidCmdlineArgumentException(
                                "-setprop argument must be a key=value pair!");
                    }
                    this.setProperty(bits[0], bits[1]);
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
                ret.add(arg);
            }
        }

        // arguments with non-specified options are considered as file names
        if (!ret.isEmpty()) {
            Iterator<String> it = ret.iterator();
            String programNames = it.next();
            while (it.hasNext()) {
                programNames = programNames + ", " + it.next();
            }
            this.setProperty("analysis.programNames", programNames);
        }
    }

    /**
     * Load the file as property file see {@link Properties}
     * @param fileName name of the property file
     * @return true if file is loaded successfully
     */
    private void loadFile(String fileName) throws IOException {
      if (fileName == null || fileName.isEmpty()) {
        fileName = getDefaultConfigFileName();
      }
      
      load(new FileInputStream(fileName));
    }

    // TODO implement this when you get really bored
    //	void normalizeValues() {
    //	for (Enumeration<?> keys = propertyNames(); keys.hasMoreElements();) {
    //	String k = (String) keys.nextElement();
    //	String v = getProperty(k);

    //	// trim heading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
    //	String v0 = v;
    //	v = v.trim();
    //	if (!v.equals(v0)) {
    //	put(k, v);
    //	}

    //	if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
    //	|| "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)) {
    //	put(k, "true");
    //	} else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
    //	|| "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)) {
    //	put(k, "false");
    //	}
    //	}
    //	}


    /**
     * If there are a number of properties for a given key, this method will split them
     * using {@link CPAConfiguration#DELIMS} and return the array of properties
     * @param key the key for the property
     * @return array of properties
     */
    public String[] getPropertiesArray(String key){
        String s = getProperty(key);
        return (s != null) ? s.split(DELIMS) : null;
    }


    /**
     * A shortcut for properties which has only true, false value
     * @param key the key for the property
     * @return the boolean value of the property, if the key is not present in
     * the properties file false
     */
    public boolean getBooleanValue(String key){
        return Boolean.valueOf(getProperty(key));
    }
}

