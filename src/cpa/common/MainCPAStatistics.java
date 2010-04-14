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
package cpa.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.CPAcheckerResult.Result;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Statistics;
import exceptions.InvalidConfigurationException;

@Options
public class MainCPAStatistics implements Statistics {
  
    @Option(name="reachedSet.export")
    private boolean exportReachedSet = true;
    
    @Option(name="output.path")
    private String outputDirectory = "test/output/";
  
    @Option(name="reachedSet.file")
    private String outputFile = "reached.txt";
  
    private final LogManager logger;
    private final Collection<Statistics> subStats;
    private long programStartingTime;
    private long analysisStartingTime;
    private long analysisEndingTime;

    public MainCPAStatistics(Configuration config, LogManager logger) throws InvalidConfigurationException {
        config.inject(this);
        
        this.logger = logger;
        subStats = new ArrayList<Statistics>();
        programStartingTime = 0;
        analysisStartingTime = 0;
        analysisEndingTime = 0;
    }

    public void startProgramTimer() {
        programStartingTime = System.currentTimeMillis();
    }

    public void startAnalysisTimer() {
        analysisStartingTime = System.currentTimeMillis();
    }

    public void stopAnalysisTimer() {
        analysisEndingTime = System.currentTimeMillis();
    }
    
    public Collection<Statistics> getSubStatistics() {
      return subStats;
  }

    @Override
    public String getName() {
        return "CPAchecker";
    }

    @Override
    public void printStatistics(PrintWriter out, Result result, ReachedElements reached) {
        if (analysisEndingTime == 0) {
          stopAnalysisTimer();
        }

        if (exportReachedSet) {
          File reachedFile = new File(outputDirectory, outputFile);
          try {
            PrintWriter file = new PrintWriter(reachedFile);
            for (AbstractElement e : reached) {
              file.println(e);
            }
            file.close();
          } catch (FileNotFoundException e) {
            logger.log(Level.WARNING,
                "Could not write reached set to file ", reachedFile.getAbsolutePath(),
                ", (", e.getMessage(), ")");
          }
        }
        
        long totalTimeInMillis = analysisEndingTime - analysisStartingTime;
        long totalAbsoluteTimeMillis = analysisEndingTime - programStartingTime;

        out.println("\nCPAchecker general statistics:");
        out.println("------------------------------");
        out.println("Size of reached set: " + reached.size());
        out.println("Total Time Elapsed: " + toTime(totalTimeInMillis));
        out.println("Total Time Elapsed including CFA construction: " +
                toTime(totalAbsoluteTimeMillis));

        for (Statistics s : subStats) {
            String name = s.getName();
            if (name != null && !name.isEmpty()) {
              out.println("");
              out.println(name);
              char[] c = new char[name.length()];
              Arrays.fill(c, '-');
              out.println(String.copyValueOf(c));
            }
            s.printStatistics(out, result, reached);
        }

        out.println("");
        out.print("Error location(s) reached? ");
        switch (result) {
        case UNKNOWN:
          out.println("UNKNOWN, analysis has not completed\n\n" +
              "***********************************************************************\n" +
              "* WARNING: Analysis interrupted!! The statistics might be unreliable! *\n" +
              "***********************************************************************"
            );
          break;
        case UNSAFE:
          out.println("YES, there is a BUG!");
          break;
        case SAFE:
          out.println("NO, the system is considered safe by the chosen CPAs");
          break;
        default:
          out.println("UNKNOWN result: " + result);
        }
        out.flush();
    }

    private String toTime(long timeMillis) {
        return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }
}
