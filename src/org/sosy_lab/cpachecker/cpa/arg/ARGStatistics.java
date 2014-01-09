/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.Model;
import org.sosy_lab.cpachecker.core.Model.AssignableTerm;
import org.sosy_lab.cpachecker.core.Model.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

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

  @Option(name="errorPath.file",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathFile = Paths.get("ErrorPath.%d.txt");

  @Option(name="errorPath.core",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathCoreFile = Paths.get("ErrorPath.%d.core.txt");

  @Option(name="errorPath.source",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathSourceFile = Paths.get("ErrorPath.%d.c");

  @Option(name="errorPath.exportAsSource",
      description="translate error path to C program")
  private boolean exportSource = true;

  @Option(name="errorPath.json",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathJson = Paths.get("ErrorPath.%d.json");

  @Option(name="errorPath.assignment",
      description="export one variable assignment for error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathAssignment = Paths.get("ErrorPath.%d.assignment.txt");

  @Option(name="errorPath.graph",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathGraphFile = Paths.get("ErrorPath.%d.dot");

  @Option(name="errorPath.automaton",
      description="export error path to file as an automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathAutomatonFile = Paths.get("ErrorPath.%d.spc");

  @Option(name="errorPath.graphml",
      description="export error path to file as an automaton to a graphml file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathAutomatonGraphmlFile = Paths.get("ErrorPath.%d.graphml");

  @Option(name="errorPath.graphml.pathUntilNonAssumeToSink",
      description="include path branches to the first non-assume edge to identify sinks")
  private boolean pathUntilNonAssumeToSink = false;

  private final ARGCPA cpa;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;

  public ARGStatistics(Configuration config, ARGCPA cpa) throws InvalidConfigurationException {
    config.inject(this);

    this.cpa = cpa;

    if (argFile == null && simplifiedArgFile == null && refinementGraphFile == null) {
      exportARG = false;
    }
    if (!exportSource) {
      errorPathSourceFile = null;
    }
    if (errorPathAssignment == null && errorPathCoreFile == null && errorPathFile == null
        && errorPathGraphFile == null && errorPathJson == null && errorPathSourceFile == null
        && errorPathAutomatonFile == null) {
      exportErrorPath = false;
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

    final ARGState rootState = (ARGState)pReached.getFirstState();
    final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
    int cexIndex = 0;

    for (Map.Entry<ARGState, CounterexampleInfo> cex : getAllCounterexamples(pReached).entrySet()) {
      final ARGState targetState = checkNotNull(cex.getKey());
      @Nullable final CounterexampleInfo counterexample = cex.getValue();
      final ARGPath targetPath = checkNotNull(getTargetPath(targetState, counterexample));

      final Set<Pair<ARGState, ARGState>> targetPathEdges = getEdgesOfPath(targetPath);
      allTargetPathEdges.addAll(targetPathEdges);

      if (exportErrorPath && counterexample != null) {
        exportCounterexample(pReached, rootState, cexIndex++, counterexample,
            targetPath, Predicates.in(targetPathEdges));
      }
    }

    if (exportARG) {
      exportARG(rootState, Predicates.in(allTargetPathEdges));
    }
  }

  private void exportCounterexample(ReachedSet pReached,
      final ARGState rootState,
      final int cexIndex,
      final CounterexampleInfo counterexample,
      final ARGPath targetPath,
      final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {

    writeErrorPathFile(errorPathFile, cexIndex,
        createErrorPathWithVariableAssignmentInformation(targetPath, counterexample));

    if (errorPathCoreFile != null) {
      // the shrinked errorPath only includes the nodes,
      // that are important for the error, it is not a complete path,
      // only some nodes of the targetPath are part of it
      ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
      ARGPath shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);
      writeErrorPathFile(errorPathCoreFile, cexIndex,
          createErrorPathWithVariableAssignmentInformation(shrinkedErrorPath, counterexample));
    }

    writeErrorPathFile(errorPathJson, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        targetPath.toJSON(pAppendable);
      }
    });

    final Set<ARGState> pathElements;
    Appender pathProgram = null;
    if (counterexample != null && counterexample.getTargetPath() != null) {
      // precise error path
      pathElements = targetPath.getStateSet();

      if (errorPathSourceFile != null) {
        pathProgram = PathToCTranslator.translateSinglePath(targetPath);
      }

    } else {
      // Imprecise error path.
      // For the text export, we have no other chance,
      // but for the C code and graph export we use all existing paths
      // to avoid this problem.
      ARGState lastElement = (ARGState)pReached.getLastState();
      pathElements = ARGUtils.getAllStatesOnPathsTo(lastElement);

      if (errorPathSourceFile != null) {
        pathProgram = PathToCTranslator.translatePaths(rootState, pathElements);
      }
    }

    if (pathProgram != null) {
      writeErrorPathFile(errorPathSourceFile, cexIndex, pathProgram);
    }

    writeErrorPathFile(errorPathGraphFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        ARGToDotWriter.write(pAppendable, rootState,
            ARGUtils.CHILDREN_OF_STATE,
            Predicates.in(pathElements),
            isTargetPathEdge);
      }
    });

    writeErrorPathFile(errorPathAutomatonFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        ARGUtils.producePathAutomaton(pAppendable, rootState, pathElements,
                                      "ErrorPath" + cexIndex,
                                      counterexample);
      }
    });

    writeErrorPathFile(errorPathAutomatonGraphmlFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        ARGPathExport exporter = new ARGPathExport();
        exporter.producePathAutomatonGraphMl(pAppendable, rootState, pathElements,
                                      "ErrorPath" + cexIndex, pathUntilNonAssumeToSink);
      }
    });

    if (counterexample != null) {
      if (counterexample.getTargetPathModel() != null) {
        writeErrorPathFile(errorPathAssignment, cexIndex, counterexample.getTargetPathModel());
      }

      for (Pair<Object, Path> info : counterexample.getAllFurtherInformation()) {
        if (info.getSecond() != null) {
          writeErrorPathFile(info.getSecond(), cexIndex, info.getFirst());
        }
      }
    }
  }

  private Appender createErrorPathWithVariableAssignmentInformation(
      final ARGPath targetPath, final CounterexampleInfo counterexample) {
    final Model model = counterexample.getTargetPathModel();
    return new Appender() {
      @Override
      public void appendTo(Appendable out) throws IOException {
        // Write edges mixed with assigned values.
        List<CFAEdge> edgePath = targetPath.asEdgesList();
        List<Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>>> exactValuePath;
        exactValuePath = null;

        if (model != null) {
          exactValuePath = model.getExactVariableValuePath(edgePath);
        }

        if(exactValuePath != null) {
          printPreciseValues(out, exactValuePath);
        } else {
          printAllValues(out, edgePath);
        }
      }

      private void printAllValues(Appendable out, List<CFAEdge> pEdgePath) throws IOException {
        for (CFAEdge edge : from(pEdgePath).filter(notNull())) {
          out.append(edge.toString());
          out.append(System.lineSeparator());
          if (model != null) {
            for (Model.AssignableTerm term : model.getAssignedTermsPerEdge().getAllAssignedTerms(edge)) {
              out.append('\t');
              out.append(term.toString());
              out.append(": ");
              out.append(model.get(term).toString());
              out.append(System.lineSeparator());
            }
          }
        }
      }

      private void printPreciseValues(Appendable out,
          List<Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>>> pExactValuePath) throws IOException {

        for (Pair<CFAEdge, Collection<Pair<AssignableTerm, Object>>> edgeValuePair : from(pExactValuePath).filter(notNull())) {

          out.append(edgeValuePair.getFirst().toString());
          out.append(System.lineSeparator());

          String cCode = CFAPathWithAssignments.getAsCode(edgeValuePair.getSecond(), edgeValuePair.getFirst());
          if (cCode != null) {
            out.append('\t');
            out.append(cCode);
            out.append(System.lineSeparator());
          }
        }
      }
    };
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
        cex = CounterexampleInfo.feasible(path, Model.empty());
      }
      counterexamples.put(s, cex);
    }

    return counterexamples;
  }

  private ARGPath getTargetPath(final ARGState targetState,
      @Nullable final CounterexampleInfo counterexample) {
    ARGPath targetPath = null;

    if (counterexample != null) {
      targetPath = counterexample.getTargetPath();
    }

    if (targetPath == null) {
      // try to find one
      // This is imprecise if there are several paths in the ARG,
      // because we randomly select one existing path,
      // but this path may actually be infeasible.
      targetPath = ARGUtils.getOnePathTo(targetState);
    }
    return targetPath;
  }

  private void writeErrorPathFile(Path file, int cexIndex, Object content) {
    if (file != null) {
      // fill in index in file name
      file = Paths.get(String.format(file.toString(), cexIndex));

      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not write information about the error path to file");
      }
    }
  }

  private static Set<Pair<ARGState, ARGState>> getEdgesOfPath(ARGPath pPath) {
    Set<Pair<ARGState, ARGState>> result = new HashSet<>(pPath.size());
    Iterator<Pair<ARGState, CFAEdge>> it = pPath.iterator();
    assert it.hasNext();
    ARGState lastElement = it.next().getFirst();
    while (it.hasNext()) {
      ARGState currentElement = it.next().getFirst();
      result.add(Pair.of(lastElement, currentElement));
      lastElement = currentElement;
    }
    return result;
  }
}
