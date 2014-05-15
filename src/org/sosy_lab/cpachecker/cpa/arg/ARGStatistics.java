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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.arg")
public class ARGStatistics implements Statistics {

  @Option(name="export", description="export final ARG as .dot file")
  private boolean exportARG = true;

  @Option(name="file",
      description="export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("ARG.dot");

  @Option(name="simplifiedARG.file",
      description="export final ARG as .dot file, showing only loop heads and function entries/exits")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Paths.get("ARGSimplified.dot");

  @Option(name="refinements.file",
      description="export simplified ARG that shows all refinements to .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path refinementGraphFile = Paths.get("ARGRefinements.dot");

  @Option(name="errorPath.export",
      description="export error path to file, if one is found")
  private boolean exportErrorPath = true;

  private final ARGCPA cpa;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;
  private final ARGPathExport witnessExporter;
  private final CEXExporter cexExporter;

  public ARGStatistics(Configuration config, ARGCPA cpa) throws InvalidConfigurationException {
    this.cpa = cpa;

    config.inject(this);

    witnessExporter = new ARGPathExport(config);
    cexExporter = new CEXExporter(config, cpa.getLogger());

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

        cpa.getLogger().logUserException(Level.WARNING, e,
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

    if (!exportARG && !exportErrorPath) {
      // shortcut, avoid unnecessary creation of path etc.
      assert refinementGraphWriter == null;
      return;
    }

    final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
    int cexIndex = 0;

    for (Map.Entry<ARGState, CounterexampleInfo> cex : getAllCounterexamples(pReached).entrySet()) {
      cexExporter.exportCounterexample(cex.getKey(), cex.getValue(), cexIndex++, allTargetPathEdges,
          !cexExporter.shouldDumpErrorPathImmediately());
    }

    if (exportARG) {
      final ARGState rootState = (ARGState)pReached.getFirstState();
      exportARG(rootState, Predicates.in(allTargetPathEdges));
    }
  }

  private void exportARG(final ARGState rootState, final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {
    SetMultimap<ARGState, ARGState> relevantSuccessorRelation = ARGUtils.projectARG(rootState, ARGUtils.CHILDREN_OF_STATE, ARGUtils.RELEVANT_STATE);
    Function<ARGState, Collection<ARGState>> relevantSuccessorFunction = Functions.forMap(relevantSuccessorRelation.asMap(), ImmutableSet.<ARGState>of());

    if (argFile != null) {
      try (Writer w = Files.openOutputFile(argFile)) {
        ARGToDotWriter.write(w, rootState,
            ARGUtils.CHILDREN_OF_STATE,
            Predicates.alwaysTrue(),
            isTargetPathEdge);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (simplifiedArgFile != null) {
      try (Writer w = Files.openOutputFile(simplifiedArgFile)) {
        ARGToDotWriter.write(w, rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    if (refinementGraphUnderlyingWriter != null) {
      try (Writer w = refinementGraphUnderlyingWriter) { // for auto-closing
        refinementGraphWriter.writeSubgraph(rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            Predicates.alwaysFalse());
        refinementGraphWriter.finish();

      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write refinement graph to file");
      }
    }
  }

  private Map<ARGState, CounterexampleInfo> getAllCounterexamples(final ReachedSet pReached) {
    Map<ARGState, CounterexampleInfo> probableCounterexample = cpa.getCounterexamples();
    // This map may contain too many counterexamples
    // (for target states that were in the mean time removed from the ReachedSet),
    // as well as too feww counterexamples
    // (for target states where we don't have a CounterexampleInfo
    // because we did no refinement).
    // So we create a map with all target states,
    // adding the CounterexampleInfo where we have it (null otherwise).

    Map<ARGState, CounterexampleInfo> counterexamples = new HashMap<>();

    for (AbstractState targetState : from(pReached).filter(IS_TARGET_STATE)) {
      ARGState s = (ARGState)targetState;
      CounterexampleInfo cex = probableCounterexample.get(s);
      if (cex == null) {
        ARGPath path = ARGUtils.getOnePathTo(s);
        if (Lists.transform(path, Pair.getProjectionToSecond()).contains(null)) {
          // path is invalid,
          // this might be a partial path in BAM, from an intermediate TargetState to root of its ReachedSet.
          // TODO this check does not avoid dummy-paths in BAM, that might exist in main-reachedSet.
        } else {
          cex = CounterexampleInfo.feasible(path, Model.empty());
        }
      }
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    return counterexamples;
  }
}
