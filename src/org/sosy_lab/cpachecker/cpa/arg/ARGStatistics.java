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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithConcreteCex;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA.PartitionState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix="cpa.arg")
public class ARGStatistics implements Statistics {

  @Option(secure=true, name="dumpAfterIteration", description="Dump all ARG related statistics files after each iteration of the CPA algorithm? (for debugging and demonstration)")
  private boolean dumpArgInEachCpaIteration = false;

  @Option(secure=true, name="export", description="export final ARG as .dot file")
  private boolean exportARG = true;

  @Option(secure=true, name="file",
      description="export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("ARG.dot");

  @Option(secure=true, name="proofWitness",
      description="export a proof as .graphml file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path proofWitness = null;

  @Option(secure=true, name="simplifiedARG.file",
      description="export final ARG as .dot file, showing only loop heads and function entries/exits")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Paths.get("ARGSimplified.dot");

  @Option(secure=true, name="refinements.file",
      description="export simplified ARG that shows all refinements to .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path refinementGraphFile = Paths.get("ARGRefinements.dot");

  protected final ConfigurableProgramAnalysis cpa;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;
  private final @Nullable CEXExporter cexExporter;
  private final ARGPathExporter argPathExporter;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private final LogManager logger;

  public ARGStatistics(
      Configuration config, LogManager pLogger, ConfigurableProgramAnalysis pCpa, CFA cfa)
      throws InvalidConfigurationException {
    config.inject(this, ARGStatistics.class); // needed for sub-classes

    logger = pLogger;
    cpa = pCpa;
    assumptionToEdgeAllocator =
        new AssumptionToEdgeAllocator(config, logger, cfa.getMachineModel());
    cexExporter = new CEXExporter(config, logger, cfa, cpa);
    argPathExporter = new ARGPathExporter(config, logger, cfa);

    if (argFile == null && simplifiedArgFile == null && refinementGraphFile == null && proofWitness == null) {
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
        refinementGraphUnderlyingWriter =
            MoreFiles.openOutputFile(refinementGraphFile, Charset.defaultCharset());
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
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (cexExporter.dumpErrorPathImmediately() && !exportARG) {
      return;
    }

    final Map<ARGState, CounterexampleInfo> counterexamples = getAllCounterexamples(pReached);

    if (!cexExporter.dumpErrorPathImmediately()) {
      for (Map.Entry<ARGState, CounterexampleInfo> cex : counterexamples.entrySet()) {
        cexExporter.exportCounterexample(cex.getKey(), cex.getValue());
      }
    }

    if (exportARG) {
      exportARG(pReached, counterexamples);
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

    String path = pPath.toString();
    int sepIx = path.lastIndexOf(".");
    String prefix = path.substring(0, sepIx);
    String extension = path.substring(sepIx, path.length());
    return Paths.get(prefix + "-" + partitionKey + extension);
  }

  private void exportARG(UnmodifiableReachedSet pReached,
      final Map<ARGState, CounterexampleInfo> counterexamples) {
    final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
    for (CounterexampleInfo cex : counterexamples.values()) {
      allTargetPathEdges.addAll(cex.getTargetPath().getStatePairs());
    }

    // The state space might be partitioned ...
    // ... so we would export a separate ARG for each partition ...
    boolean partitionedArg =
        pReached.isEmpty()
            || AbstractStates.extractStateByType(pReached.getFirstState(), PartitionState.class)
                != null;

    final Set<ARGState> rootStates = partitionedArg
        ? ARGUtils.getRootStates(pReached)
        : Collections.singleton(AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class));

    for (ARGState rootState: rootStates) {
      exportARG0(rootState, Predicates.in(allTargetPathEdges));
    }
  }

  @SuppressWarnings("try")
  private void exportARG0(final ARGState rootState, final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {
    SetMultimap<ARGState, ARGState> relevantSuccessorRelation =
        ARGUtils.projectARG(rootState, ARGState::getChildren, ARGUtils.RELEVANT_STATE);
    Function<ARGState, Collection<ARGState>> relevantSuccessorFunction = Functions.forMap(relevantSuccessorRelation.asMap(), ImmutableSet.<ARGState>of());

    if (proofWitness != null) {
      try (Writer w =
          MoreFiles.openOutputFile(
              adjustPathNameForPartitioning(rootState, proofWitness), StandardCharsets.UTF_8)) {
        argPathExporter.writeProofWitness(w, rootState,
            Predicates.alwaysTrue(),
            Predicates.alwaysTrue());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (argFile != null) {
      try (Writer w =
          MoreFiles.openOutputFile(
              adjustPathNameForPartitioning(rootState, argFile), Charset.defaultCharset())) {
        ARGToDotWriter.write(
            w, rootState, ARGState::getChildren, Predicates.alwaysTrue(), isTargetPathEdge);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (simplifiedArgFile != null) {
      try (Writer w =
          MoreFiles.openOutputFile(
              adjustPathNameForPartitioning(rootState, simplifiedArgFile),
              Charset.defaultCharset())) {
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

  private Map<ARGState, CounterexampleInfo> getAllCounterexamples(final UnmodifiableReachedSet pReached) {
    Map<ARGState, CounterexampleInfo> counterexamples = new HashMap<>();

    for (AbstractState targetState : from(pReached).filter(IS_TARGET_STATE)) {
      ARGState s = (ARGState)targetState;
      CounterexampleInfo cex = s.getCounterexampleInformation().orElse(null);
      if (cex == null) {
        ARGPath path = ARGUtils.getOnePathTo(s);
        if (path.getFullPath().isEmpty()) {
          // path is invalid,
          // this might be a partial path in BAM, from an intermediate TargetState to root of its ReachedSet.
          // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
        } else {

          CFAPathWithAssumptions assignments = createAssignmentsForPath(path);
          // we use the imprecise version of the CounterexampleInfo, due to the possible
          // merges which are done in the used CPAs, but if we can compute a path with assignments,
          // it is probably precise
          if (!assignments.isEmpty()) {
            cex = CounterexampleInfo.feasiblePrecise(path, assignments);
          } else {
            cex = CounterexampleInfo.feasibleImprecise(path);
          }
        }
      }
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    return counterexamples;
  }

  private CFAPathWithAssumptions createAssignmentsForPath(ARGPath pPath) {

    FluentIterable<ConfigurableProgramAnalysisWithConcreteCex> cpas =
        CPAs.asIterable(cpa).filter(ConfigurableProgramAnalysisWithConcreteCex.class);

    CFAPathWithAssumptions result = null;

    // TODO Merge different paths
    for (ConfigurableProgramAnalysisWithConcreteCex wrappedCpa : cpas) {
      ConcreteStatePath path = wrappedCpa.createConcreteStatePath(pPath);
      CFAPathWithAssumptions cexPath = CFAPathWithAssumptions.of(path, assumptionToEdgeAllocator);

      if (result != null) {
        result = result.mergePaths(cexPath);
      } else {
        result = cexPath;
      }
    }

    if (result == null) {
      return CFAPathWithAssumptions.empty();
    } else {
      return result;
    }
  }

  public void exportCounterexampleOnTheFly(
      ARGState pTargetState, CounterexampleInfo pCounterexampleInfo) throws InterruptedException {
    if (cexExporter.dumpErrorPathImmediately()) {
      cexExporter.exportCounterexampleIfRelevant(pTargetState, pCounterexampleInfo);
    }
  }

  public void printIterationStatistics(UnmodifiableReachedSet pReached) {
    if (dumpArgInEachCpaIteration) {
      exportARG(pReached, getAllCounterexamples(pReached));
    }
  }
}
