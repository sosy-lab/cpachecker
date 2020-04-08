/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

@Options(prefix = "cpa.bdd")
final class BDDStatistics implements Statistics {

  @Option(secure = true, name = "logfile", description = "Dump tracked variables to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  Path dumpfile = Paths.get("BDDCPA_tracked_variables.log");

  @Option(secure = true, name = "variablesFile", description = "Dump tracked variables to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path variablesFile = Paths.get("BDDCPA_ordered_variables.txt");

  private final NamedRegionManager manager;
  private final PredicateManager predmgr;
  private final LogManager logger;
  private final CFA cfa;

  BDDStatistics(
      Configuration pConfig,
      CFA pCfa,
      LogManager pLogger,
      NamedRegionManager pManager,
      PredicateManager pPredMgr)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    cfa = pCfa;
    logger = pLogger;
    manager = pManager;
    predmgr = pPredMgr;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    VariableClassification varClass = cfa.getVarClassification().orElseThrow();
    final Set<Partition> intBool = varClass.getIntBoolPartitions();
    int numOfBooleans = varClass.getIntBoolVars().size();

    int numOfIntEquals = 0;
    final Set<Partition> intEq = varClass.getIntEqualPartitions();
    for (Partition p : intEq) {
      numOfIntEquals += p.getVars().size();
    }

    int numOfIntAdds = 0;
    final Set<Partition> intAdd = varClass.getIntAddPartitions();
    for (Partition p : intAdd) {
      numOfIntAdds += p.getVars().size();
    }

    Collection<String> trackedIntBool =
        new TreeSet<>(); // TreeSet for nicer output through ordering
    Collection<String> trackedIntEq = new TreeSet<>();
    Collection<String> trackedIntAdd = new TreeSet<>();
    for (String var : predmgr.getTrackedVars()) {
      if (varClass.getIntBoolVars().contains(var)) {
        trackedIntBool.add(var);
      } else if (varClass.getIntEqualVars().contains(var)) {
        trackedIntEq.add(var);
      } else if (varClass.getIntAddVars().contains(var)) {
        trackedIntAdd.add(var);
      } else {
        // ignore other vars, they are either function_return_vars or tmp_vars
      }
    }

    if (dumpfile != null) { // option -noout
      try (Writer w = IO.openOutputFile(dumpfile, Charset.defaultCharset())) {
        w.append("Boolean\n\n");
        w.append(trackedIntBool.toString());
        w.append("\n\nIntEq\n\n");
        w.append(trackedIntEq.toString());
        w.append("\n\nIntAdd\n\n");
        w.append(trackedIntAdd.toString());
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write tracked variables for BDDCPA to file");
      }
    }

    out.println(
        String.format(
            "Number of boolean vars:           %d (of %d)", trackedIntBool.size(), numOfBooleans));
    out.println(
        String.format(
            "Number of intEqual vars:          %d (of %d)", trackedIntEq.size(), numOfIntEquals));
    out.println(
        String.format(
            "Number of intAdd vars:            %d (of %d)", trackedIntAdd.size(), numOfIntAdds));
    out.println(
        String.format(
            "Number of all vars:               %d",
            trackedIntBool.size() + trackedIntEq.size() + trackedIntAdd.size()));
    out.println("Number of intBool partitions:     " + intBool.size());
    out.println("Number of intEq partitions:       " + intEq.size());
    out.println("Number of intAdd partitions:      " + intAdd.size());
    out.println("Number of all partitions:         " + varClass.getPartitions().size());
    manager.printStatistics(out);
  }

  @Override
  public String getName() {
    return "BDDCPA";
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (variablesFile != null) {
      try {
        IO.writeFile(
            variablesFile,
            Charset.defaultCharset(),
            Joiner.on("\n").join(manager.getOrderedPredicates()));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ordered variables to file");
      }
    }
  }
}
