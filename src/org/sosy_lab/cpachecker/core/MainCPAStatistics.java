/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.MemoryStatistics;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

@Options
class MainCPAStatistics implements Statistics {

    @Option(name="reachedSet.export",
        description="print reached set to text file")
    private boolean exportReachedSet = true;

    @Option(name="reachedSet.file",
        description="print reached set to text file")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path outputFile = Paths.get("reached.txt");

    @Option(name="statistics.memory",
      description="track memory usage of JVM during runtime")
    private boolean monitorMemoryUsage = true;

    private final LogManager logger;
    private final Collection<Statistics> subStats;
    private final MemoryStatistics memStats;

    final Timer programTime = new Timer();
    final Timer creationTime = new Timer();
    final Timer cpaCreationTime = new Timer();
    final Timer analysisTime = new Timer();

    private CFACreator cfaCreator;

    public MainCPAStatistics(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
        logger = pLogger;
        config.inject(this);

        subStats = new ArrayList<>();

        if (monitorMemoryUsage) {
          memStats = new MemoryStatistics(pLogger);
          memStats.setDaemon(true);
          memStats.start();
        } else {
          memStats = null;
        }

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
        checkNotNull(out);
        checkNotNull(result);
        checkArgument(result == Result.NOT_YET_STARTED || reached != null);

        // call stop again in case CPAchecker was terminated abnormally
        if (analysisTime.isRunning()) {
          analysisTime.stop();
        }
        if (programTime.isRunning()) {
          programTime.stop();
        }
        if (memStats != null) {
          memStats.interrupt(); // stop memory statistics collection
        }

        if (result != Result.NOT_YET_STARTED) {
          dumpReachedSet(reached);

          printSubStatistics(out, result, reached);
        }

        out.println("CPAchecker general statistics");
        out.println("-----------------------------");

        if (result != Result.NOT_YET_STARTED) {
          try {
            printReachedSetStatistics(reached, out);
          } catch (OutOfMemoryError e) {
            logger.logUserException(Level.WARNING, e,
                "Out of memory while generating statistics about final reached set");
          }
        }

        printTimeStatistics(out);

        out.println("");

        printMemoryStatistics(out);
    }

    private void dumpReachedSet(ReachedSet reached) {
      assert reached != null : "ReachedSet may be null only if analysis not yet started";

      if (exportReachedSet && outputFile != null) {
        try (Writer w = Files.openOutputFile(outputFile)) {
          Joiner.on('\n').appendTo(w, reached);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write reached set to file");
        } catch (OutOfMemoryError e) {
          logger.logUserException(Level.WARNING, e,
              "Could not write reached set to file due to memory problems");
        }
      }
    }

    private void printSubStatistics(PrintStream out, Result result, ReachedSet reached) {
      assert reached != null : "ReachedSet may be null only if analysis not yet started";

      for (Statistics s : subStats) {
        String name = s.getName();
        if (!Strings.isNullOrEmpty(name)) {
          name = name + " statistics";
          out.println(name);
          out.println(Strings.repeat("-", name.length()));
        }

        try {
          s.printStatistics(out, result, reached);
        } catch (OutOfMemoryError e) {
          logger.logUserException(Level.WARNING, e,
              "Out of memory while generating statistics and writing output files");
        }

        if (!Strings.isNullOrEmpty(name)) {
          out.println();
        }
      }
    }

    private void printReachedSetStatistics(ReachedSet reached, PrintStream out) {
      assert reached != null : "ReachedSet may be null only if analysis not yet started";

      if (reached instanceof ForwardingReachedSet) {
        reached = ((ForwardingReachedSet)reached).getDelegate();
      }
      int reachedSize = reached.size();

      out.println("Size of reached set:          " + reachedSize);

      if (reached instanceof LocationMappedReachedSet) {
        LocationMappedReachedSet l = (LocationMappedReachedSet)reached;
        int locs = l.getNumberOfPartitions();
        if (locs > 0) {
          out.println("  Number of locations:        " + locs);
          out.println("    Avg states per loc.:      " + reachedSize / locs);
          Map.Entry<Object, Collection<AbstractState>> maxPartition = l.getMaxPartition();
          out.println("    Max states per loc.:      " + maxPartition.getValue().size() + " (at node " + maxPartition.getKey() + ")");
        }

      } else {
        HashMultiset<CFANode> allLocations = HashMultiset.create(from(reached)
                                                                      .transform(EXTRACT_LOCATION)
                                                                      .filter(notNull()));

        int locs = allLocations.entrySet().size();
        if (locs > 0) {
          out.println("  Number of locations:        " + locs);
          out.println("    Avg states per loc.:      " + reachedSize / locs);

          int max = 0;
          CFANode maxLoc = null;
          for (Multiset.Entry<CFANode> location : allLocations.entrySet()) {
            int size = location.getCount();
            if (size > max) {
              max = size;
              maxLoc = location.getElement();
            }
          }
          out.println("    Max states per loc.:      " + max + " (at node " + maxLoc + ")");
        }
      }

      if (reached instanceof PartitionedReachedSet) {
        PartitionedReachedSet p = (PartitionedReachedSet)reached;
        int partitions = p.getNumberOfPartitions();
        out.println("  Number of partitions:       " + partitions);
        out.println("    Avg size of partitions:   " + reachedSize / partitions);
        Map.Entry<Object, Collection<AbstractState>> maxPartition = p.getMaxPartition();
        out.print  ("    Max size of partitions:   " + maxPartition.getValue().size());
        if (maxPartition.getValue().size() > 1) {
          out.println(" (with key " + maxPartition.getKey() + ")");
        } else {
          out.println();
        }
      }
      out.println("  Number of target states:    " + from(reached).filter(IS_TARGET_STATE).size());
      if (reached.hasWaitingState()) {
        out.println("  Size of final wait list     " + reached.getWaitlistSize());
      }
    }

    private void printTimeStatistics(PrintStream out) {
      out.println("Time for analysis setup:      " + creationTime);
      out.println("  Time for loading CPAs:      " + cpaCreationTime);
      if (cfaCreator != null) {
        cfaCreator.printCfaCreationStatistics(out);
      }
      out.println("Time for Analysis:            " + analysisTime);
      out.println("Total time for CPAchecker:    " + programTime);
    }

    private void printMemoryStatistics(PrintStream out) {
      MemoryStatistics.printGcStatistics(out);

      if (memStats != null) {
        try {
          memStats.join(); // thread should have terminated already,
                           // but wait for it to ensure memory visibility
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        memStats.printStatistics(out);
      }
    }

    public void setCFACreator(CFACreator pCfaCreator) {
      Preconditions.checkState(cfaCreator == null);
      cfaCreator = pCfaCreator;
    }
}
