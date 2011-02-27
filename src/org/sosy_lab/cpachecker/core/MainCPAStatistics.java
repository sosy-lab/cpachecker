/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

@Options
class MainCPAStatistics implements Statistics {
  
  private class MemoryStatistics extends Thread {
    
    private static final long MEMORY_CHECK_INTERVAL = 100; // milliseconds
    
    private long maxHeap = 0;
    private long sumHeap = 0;
    private long maxNonHeap = 0;
    private long sumNonHeap = 0;
    private long count = 0;
    
    @Override
    public void run() {
      MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
      while (monitorMemoryUsage) {
        count++;
        long currentHeapUsed = mxBean.getHeapMemoryUsage().getUsed();
        maxHeap = Math.max(maxHeap, currentHeapUsed);
        sumHeap += currentHeapUsed;
        
        long currentNonHeapUsed = mxBean.getNonHeapMemoryUsage().getUsed();
        maxNonHeap = Math.max(maxNonHeap, currentNonHeapUsed);
        sumNonHeap += currentNonHeapUsed;
        
        try {
          sleep(MEMORY_CHECK_INTERVAL);
        } catch (InterruptedException e) {
          this.interrupt();
          monitorMemoryUsage = false;
        }
      }
    }
    
  }

    @Option(name="reachedSet.export")
    private boolean exportReachedSet = true;

    @Option(name="reachedSet.file", type=Option.Type.OUTPUT_FILE)
    private File outputFile = new File("reached.txt");
    
    @Option(name="statistics.memory")
    private volatile boolean monitorMemoryUsage = true;

    private final LogManager logger;
    private final Collection<Statistics> subStats;
    private final MemoryStatistics memStats = new MemoryStatistics();
    
    final Timer programTime = new Timer();
    final Timer cfaCreationTime = new Timer();
    final Timer cpaCreationTime = new Timer();
    final Timer analysisTime = new Timer();

    private CFACreator cfaCreator;

    public MainCPAStatistics(Configuration config, LogManager logger) throws InvalidConfigurationException {
        config.inject(this);

        this.logger = logger;
        subStats = new ArrayList<Statistics>();
        memStats.setDaemon(true);
        memStats.start();
        programTime.start();
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
  }

    @Override
    public String getName() {
        return "CPAchecker";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
        // call stop again in case CPAchecker was terminated abnormally
        analysisTime.stop();
        programTime.stop();
        monitorMemoryUsage = false;

        if (exportReachedSet && outputFile != null) {
          try {
            Files.writeFile(outputFile, Joiner.on('\n').join(reached));
          } catch (IOException e) {
            logger.log(Level.WARNING,
                "Could not write reached set to file (", e.getMessage(), ")");
          } catch (OutOfMemoryError e) {
            logger.log(Level.WARNING,
                "Could not write reached set to file due to memory problems (", e.getMessage(), ")");
          }
        }

        for (Statistics s : subStats) {
            String name = s.getName();
            if (name != null && !name.isEmpty()) {
              name = name + " statistics";
              out.println("");
              out.println(name);
              out.println(Strings.repeat("-", name.length()));
            }
            s.printStatistics(out, result, reached);
        }

        Set<CFANode> allLocations = ImmutableSet.copyOf(AbstractElements.extractLocations(reached));
        
        out.println("\nCPAchecker general statistics");
        out.println("-----------------------------");
        out.println("Size of reached set:          " + reached.size());
        out.println("  Number of locations:        " + allLocations.size());
        if (reached instanceof PartitionedReachedSet) {
          PartitionedReachedSet p = (PartitionedReachedSet)reached;
          out.println("  Number of partitions:       " + p.getNumberOfPartitions());
        }
        out.println("Time for CFA construction:    " + cfaCreationTime);
        if (cfaCreator != null) {
          out.println("  Time for loading C parser:  " + cfaCreator.parserInstantiationTime);
          out.println("  Time for parsing C file:    " + cfaCreator.parsingTime);
          out.println("  Time for AST to CFA:        " + cfaCreator.conversionTime);
          out.println("  Time for CFA sanity checks: " + cfaCreator.checkTime);
          out.println("  Time for post-processing:   " + cfaCreator.processingTime);
          if (cfaCreator.pruningTime.getNumberOfIntervals() > 0) {
            out.println("  Time for CFA pruning:       " + cfaCreator.pruningTime);
          }
          if (cfaCreator.exportTime.getNumberOfIntervals() > 0) {
            out.println("  Time for export:            " + cfaCreator.exportTime);
          }
        }
        out.println("Time for CPA instantiaton:    " + cpaCreationTime);
        out.println("Time for Analysis:            " + analysisTime);
        out.println("Total time for CPAchecker:    " + programTime);
        
        out.println("");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcTime = 0;
        int gcCount = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
          gcTime += gcBean.getCollectionTime();
          gcCount += gcBean.getCollectionCount();
        }
        out.println("Time for Garbage Collector:   " + Timer.formatTime(gcTime) + " (in " + gcCount + " runs)");
        try {
          memStats.join(); // thread should have terminated already,
                           // but wait for it to ensure memory visibility
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        if (memStats.count > 0) {
          out.println("Heap memory usage:            " + formatMem(memStats.maxHeap) + " max (" + formatMem(memStats.sumHeap/memStats.count) + " avg)");
          out.println("Non-Heap memory usage:        " + formatMem(memStats.maxNonHeap) + " max (" + formatMem(memStats.sumNonHeap/memStats.count) + " avg)");
        }
          
        out.println("");
        out.print("Given specification violated? ");
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
    
    private static String formatMem(long mem) {
      return String.format("%,9dMB", mem >> 20);
    }

    public void setCFACreator(CFACreator pCfaCreator) {
      Preconditions.checkState(cfaCreator == null);
      cfaCreator = pCfaCreator;
    }
}
