/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.IterationStatistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CounterexamplesSummary;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA.PartitionState;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

@Options(prefix="cpa.arg")
public class ARGStatistics implements IterationStatistics {

  @Option(secure=true, name="dumpAfterIteration", description="Dump all ARG related statistics files after each iteration of the CPA algorithm? (for debugging and demonstration)")
  private boolean dumpArgInEachCpaIteration = false;

  @Option(secure=true, name="export", description="export final ARG as .dot file")
  private boolean exportARG = true;

  @Option(secure=true, name="file",
      description="export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("ARG.dot");

  @Option(secure=true, name="simplifiedARG.file",
      description="export final ARG as .dot file, showing only loop heads and function entries/exits")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Paths.get("ARGSimplified.dot");

  @Option(secure=true, name="refinements.file",
      description="export simplified ARG that shows all refinements to .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path refinementGraphFile = Paths.get("ARGRefinements.dot");

  @Option(secure=true, name="argLevelStatisticsFile",
      description="Export statistics on the number of states per level of the ARG to a .csv file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path levelStatisticsFile = null;

  private final LogManager logger;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;

  private final @Nullable CEXExporter cexExporter;
  private final CounterexamplesSummary cexSummary;

  public ARGStatistics(Configuration config, LogManager pLogger, ARGCPA pCpa,
      MachineModel pMachineModel, Language pLanguage,
      @Nullable CEXExporter pCexExporter, CounterexamplesSummary pCexSummary)
          throws InvalidConfigurationException {


    config.inject(this);

    logger = pLogger;
    cexSummary = pCexSummary;
    cexExporter = pCexExporter;

    if (argFile == null && simplifiedArgFile == null && refinementGraphFile == null) {
      exportARG = false;
    }
  }

  ARGToDotWriter getRefinementGraphWriter() {
    if (!exportARG || refinementGraphFile == null) {
      return null;
    }

    if (refinementGraphWriter == null) {
      // Open output file for refinement graph,
      // we continuously write into this file during analysis.
      // We do this lazily so that the file is written only if there are refinements.
      try {
        refinementGraphUnderlyingWriter = Files.openOutputFile(refinementGraphFile);
        refinementGraphWriter = new ARGToDotWriter(refinementGraphUnderlyingWriter);
      } catch (IOException e) {
        if (refinementGraphUnderlyingWriter != null) {
          try {
            refinementGraphUnderlyingWriter.close();
          } catch (IOException innerException) {
            e.addSuppressed(innerException);
          }
        }

        logger.logUserException(Level.WARNING, e,
            "Could not write refinement graph to file");

        refinementGraphFile = null; // ensure we won't try again
        refinementGraphUnderlyingWriter = null;
        refinementGraphWriter = null;
      }
    }

    // either both are null or none
    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    return refinementGraphWriter;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult,
      ReachedSet pReached) {
    if (cexExporter == null && !exportARG) {
      return;
    }

    final Map<ARGState, CounterexampleInfo> counterexamples = cexSummary.getAllCounterexamples(pReached);

    if (cexExporter != null) {
      int cexIndex = 0;
      for (Map.Entry<ARGState, CounterexampleInfo> cex : counterexamples.entrySet()) {
        cexExporter.exportCounterexample(cex.getKey(), cex.getValue(), cexIndex++);
      }
    }

    if (levelStatisticsFile != null) {
      try (Writer w = Files.openOutputFile(levelStatisticsFile)) {
        writeLevelStatisticsCsv(pReached, w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write statistics on the ARG levels to a file");
      }
    }

    if (exportARG) {
      final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
      for (CounterexampleInfo cex : counterexamples.values()) {
        allTargetPathEdges.addAll(cex.getTargetPath().getStatePairs());
      }

      // The state space might be partitioned ...
      // ... so we would export a separate ARG for each partition ...
      boolean partitionedArg = AbstractStates.extractStateByType(
          pReached.getFirstState(), PartitionState.class) != null;

      final Set<ARGState> rootStates = partitionedArg
          ? ARGUtils.getRootStates(pReached)
          : Collections.singleton(AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class));

      for (ARGState rootState: rootStates) {
        exportARG(rootState, Predicates.in(allTargetPathEdges));
      }
    }
  }

  private Path adjustPathNameForPartitioning(ARGState rootState, Path pPath) {
    if (pPath == null) {
      return null;
    }

    PartitionState partyState = AbstractStates.extractStateByType(rootState, PartitionState.class);
    if (partyState == null) {
      return pPath;
    }

    final String partitionKey = partyState.getStateSpacePartition().getPartitionKey().toString();

    int sepIx = pPath.getPath().lastIndexOf(".");
    String prefix = pPath.getPath().substring(0, sepIx);
    String extension = pPath.getPath().substring(sepIx, pPath.getPath().length());
    return Paths.get(prefix + "-" + partitionKey + extension);
  }

  private void exportARG(final ARGState rootState, final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {
    SetMultimap<ARGState, ARGState> relevantSuccessorRelation = ARGUtils.projectARG(rootState, ARGUtils.CHILDREN_OF_STATE, ARGUtils.RELEVANT_STATE);
    Function<ARGState, Collection<ARGState>> relevantSuccessorFunction = Functions.forMap(relevantSuccessorRelation.asMap(), ImmutableSet.<ARGState>of());

    if (argFile != null) {
      try (Writer w = Files.openOutputFile(adjustPathNameForPartitioning(rootState, argFile))) {
        ARGToDotWriter.write(w, rootState,
            ARGUtils.CHILDREN_OF_STATE,
            Predicates.alwaysTrue(),
            isTargetPathEdge);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (simplifiedArgFile != null) {
      try (Writer w = Files.openOutputFile(adjustPathNameForPartitioning(rootState, simplifiedArgFile))) {
        ARGToDotWriter.write(w, rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    if (refinementGraphUnderlyingWriter != null) {
      try (Writer w = refinementGraphUnderlyingWriter) { // for auto-closing
        // TODO: Support for partitioned state spaces
        refinementGraphWriter.writeSubgraph(rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
        refinementGraphWriter.finish();

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write refinement graph to file");
      }
    }
  }

  @Override
  public void printIterationStatistics(PrintStream pOut, ReachedSet pReached) {
    if (dumpArgInEachCpaIteration) {
      printStatistics(pOut, Result.UNKNOWN, pReached);
    }
  }

  /**
   * Write a statistic on the number of ARG states per level of the ARG.
   *
   * Format:  Level (integer) \t number of states on this level (integer)
   *
   * @param pReached  The set of reached states.
   * @param pTarget   Where to write the CSV
   */
  void writeLevelStatisticsCsv(ReachedSet pReached, Appendable pTarget) {
    Preconditions.checkNotNull(pReached);
    Preconditions.checkNotNull(pTarget);

    // 1. Compute the statistics
    List<Integer> statesPerLevel = computeNumberOfStatesPerArgLevel(pReached);

    // 2. Dump the statistics to the target
    try {
      for (int level=0; level <statesPerLevel.size(); level++) {
        int statesAtLevel = statesPerLevel.get(level);

        pTarget.append(Integer.toString(level));
        pTarget.append('\t');
        pTarget.append(Integer.toString(statesAtLevel));
        pTarget.append('\n');
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Wrinting statistics on states per level failed!", e);
    }
  }

  /**
   * Compute statistics on the number of states per level of the ARG.
   *
   * @param pReached
   *
   * @return  List where the index is the level, and the value is the number of states in this level.
   */
  private List<Integer> computeNumberOfStatesPerArgLevel(ReachedSet pReached) {
    Preconditions.checkNotNull(pReached);
    Preconditions.checkNotNull(pReached.getFirstState() != null);
    Preconditions.checkArgument(pReached.getFirstState() instanceof ARGState);

    List<Integer> statesPerLevel = Lists.newArrayList();
    Deque<ARGState> worklist = Lists.newLinkedList();
    worklist.add((ARGState) pReached.getFirstState());

    Set<ARGState> visited = Sets.newHashSet();

    while (!worklist.isEmpty()) {
      ARGState e = worklist.pop();

      if (!visited.add(e)) {
        continue;
      }

      int level = e.getStateLevel();

      while (statesPerLevel.size() <= level) {
        statesPerLevel.add(Integer.valueOf(0));
      }

      int statesAtLevel = statesPerLevel.get(level);
      statesPerLevel.set(level, statesAtLevel + 1);

      worklist.addAll(e.getChildren()); // The ARG is acyclic!
    }

    return statesPerLevel;
  }
}
