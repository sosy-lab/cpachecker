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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import cpa.common.ReachedElements;

import cmdline.CPAMain.Result;

public class MainCPAStatistics implements CPAStatistics {
    private Collection<CPAStatistics> subStats;
    private long programStartingTime;
    private long analysisStartingTime;
    private long analysisEndingTime;

    public MainCPAStatistics() {
        subStats = new LinkedList<CPAStatistics>();
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

    public void addSubStatistics(CPAStatistics s) {
        subStats.add(s);
    }
    
    public Collection<CPAStatistics> getSubStatistics() {
      return subStats;
  }

    @Override
    public String getName() {
        return "CPAChecker";
    }

    @Override
    public void printStatistics(PrintWriter out, Result result, ReachedElements reached) {
        long totalTimeInMillis = analysisEndingTime - analysisStartingTime;
        long totalAbsoluteTimeMillis = analysisEndingTime - programStartingTime;

        out.println("\nCPAChecker general statistics:");
        out.println("------------------------------");
        out.println("Total Time Elapsed: " + toTime(totalTimeInMillis));
        out.println("Total Time Elapsed including CFA construction: " +
                toTime(totalAbsoluteTimeMillis));

        if (!subStats.isEmpty()) {
            out.println("\nAnalysis-specific statistics:");
            out.println("-----------------------------\n");
            for (CPAStatistics s : subStats) {
                String name = s.getName();
                out.println(name);
                char[] c = new char[name.length()];
                Arrays.fill(c, '-');
                out.println(String.copyValueOf(c));
                s.printStatistics(out, result, reached);
                out.println("");
            }
        } else {
            out.println("");
        }
        out.print("Error location(s) reached? ");
        switch (result) {
        case UNKNOWN:
          out.println("UNKNOWN, analysis has not completed");
          break;
        case UNSAFE:
          out.println("YES, there is a BUG!");
          break;
        case SAFE:
          out.println("NO, the system is safe");
        }
        out.flush();
    }

    private String toTime(long timeMillis) {
        return String.format("% 5d.%03ds", timeMillis/1000, timeMillis%1000);
    }
}
