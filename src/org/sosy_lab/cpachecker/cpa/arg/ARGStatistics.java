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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
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
  private boolean exportART = true;

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
  private Path errorPathFile = Paths.get("ErrorPath.txt");

  @Option(name="errorPath.core",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathCoreFile = Paths.get("ErrorPathCore.txt");

  @Option(name="errorPath.source",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathSourceFile = Paths.get("ErrorPath.c");

  @Option(name="errorPath.exportAsSource",
      description="translate error path to C program")
  private boolean exportSource = true;

  @Option(name="errorPath.json",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathJson = Paths.get("ErrorPath.json");

  @Option(name="errorPath.assignment",
      description="export one variable assignment for error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathAssignment = Paths.get("ErrorPathAssignment.txt");

  @Option(name="errorPath.graph",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path errorPathGraphFile = Paths.get("ErrorPath.dot");

  private final ARGCPA cpa;

  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;

  public ARGStatistics(Configuration config, ARGCPA cpa) throws InvalidConfigurationException {
    config.inject(this);

    this.cpa = cpa;

    if (argFile == null && simplifiedArgFile == null && refinementGraphFile == null) {
      exportART = false;
    }
    if (!exportSource) {
      errorPathSourceFile = null;
    }
    if (errorPathAssignment == null && errorPathCoreFile == null && errorPathFile == null
        && errorPathGraphFile == null && errorPathJson == null && errorPathSourceFile == null) {
      exportErrorPath = false;
    }
  }

  ARGToDotWriter getRefinementGraphWriter() {
    if (!exportART || refinementGraphFile == null) {
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

    if (!exportART && !exportErrorPath) {
      // shortcut, avoid unnecessary creation of path etc.
      assert refinementGraphWriter == null;
      return;
    }

    final ARGState rootState = (ARGState)pReached.getFirstState();
    @Nullable final CounterexampleInfo counterexample = getCounterexample(pReached);
    @Nullable final ARGPath targetPath = getTargetPath(counterexample, pReached);
    final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge = getEdgesOfPath(targetPath);

    if (exportErrorPath && targetPath != null) {

      writeErrorPathFile(errorPathFile, targetPath);

      if (errorPathCoreFile != null) {
        // the shrinked errorPath only includes the nodes,
        // that are important for the error, it is not a complete path,
        // only some nodes of the targetPath are part of it
        ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
        ARGPath shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);
        writeErrorPathFile(errorPathCoreFile, shrinkedErrorPath);
      }

      writeErrorPathFile(errorPathJson, new Appender() {
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
        writeErrorPathFile(errorPathSourceFile, pathProgram);
      }

      writeErrorPathFile(errorPathGraphFile, new Appender() {
        @Override
        public void appendTo(Appendable pAppendable) throws IOException {
          ARGToDotWriter.write(pAppendable, rootState,
              ARGUtils.CHILDREN_OF_STATE,
              Predicates.in(pathElements),
              isTargetPathEdge);
        }
      });

      if (counterexample != null) {
        if (counterexample.getTargetPathAssignment() != null) {
          writeErrorPathFile(errorPathAssignment, counterexample.getTargetPathAssignment());
        }

        for (Pair<Object, File> info : counterexample.getAllFurtherInformation()) {
          if(info.getSecond() != null) {
            writeErrorPathFile(info.getSecond().toPath(), info.getFirst());
          }
        }
      }
    }

    if (exportART) {
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
  }

  private CounterexampleInfo getCounterexample(ReachedSet pReached) {
    CounterexampleInfo counterexample = cpa.getLastCounterexample();

    if (counterexample != null) {
      ARGState targetState = counterexample.getTargetPath().getLast().getFirst();
      if (!pReached.contains(targetState)) {
        // counterexample is outdated
        return null;
      }
    }
    return counterexample;
  }

  private ARGPath getTargetPath(@Nullable final CounterexampleInfo counterexample, final ReachedSet pReached) {
    ARGPath targetPath = null;

    if (counterexample != null) {
      targetPath = counterexample.getTargetPath();
    }

    if (targetPath == null) {
      // try to find one
      // This is imprecise if there are several paths in the ARG,
      // because we randomly select one existing path,
      // but this path may actually be infeasible.
      ARGState lastElement = (ARGState)pReached.getLastState();
      if (lastElement != null && lastElement.isTarget()) {
        targetPath = ARGUtils.getOnePathTo(lastElement);
      }
    }
    return targetPath;
  }

  private void writeErrorPathFile(Path file, Object content) {
    if (file != null) {
      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not write information about the error path to file");
      }
    }
  }

  private static Predicate<Pair<ARGState, ARGState>> getEdgesOfPath(@Nullable ARGPath pPath) {
    if (pPath == null) {
      return Predicates.alwaysFalse();
    }

    Set<Pair<ARGState, ARGState>> result = new HashSet<>(pPath.size());
    Iterator<Pair<ARGState, CFAEdge>> it = pPath.iterator();
    assert it.hasNext();
    ARGState lastElement = it.next().getFirst();
    while (it.hasNext()) {
      ARGState currentElement = it.next().getFirst();
      result.add(Pair.of(lastElement, currentElement));
      lastElement = currentElement;
    }
    return Predicates.in(result);
  }
}
