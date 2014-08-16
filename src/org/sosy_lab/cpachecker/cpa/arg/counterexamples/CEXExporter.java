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
package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;
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
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.CFAMultiEdgeWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssignments;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPathExport;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

@Options(prefix="cpa.arg.errorPath")
public class CEXExporter {


  @Option(name="enabled",
      description="export error path to file, if one is found")
  private boolean exportErrorPath = true;

  @Option(name="file",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathFile = PathTemplate.ofFormatString("ErrorPath.%d.txt");

  @Option(name="core",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathCoreFile = PathTemplate.ofFormatString("ErrorPath.%d.core.txt");

  @Option(name="source",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathSourceFile = PathTemplate.ofFormatString("ErrorPath.%d.c");

  @Option(name="exportAsSource",
      description="translate error path to C program")
  private boolean exportSource = true;

  @Option(name="json",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathJson = PathTemplate.ofFormatString("ErrorPath.%d.json");

  @Option(name="assignment",
      description="export one variable assignment for error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAssignment = PathTemplate.ofFormatString("ErrorPath.%d.assignment.txt");

  @Option(name="graph",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathGraphFile = PathTemplate.ofFormatString("ErrorPath.%d.dot");

  @Option(name="automaton",
      description="export error path to file as an automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAutomatonFile = PathTemplate.ofFormatString("ErrorPath.%d.spc");

  @Option(name="graphml",
      description="export error path to file as an automaton to a graphml file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAutomatonGraphmlFile = null;

  @Option(name="exportImmediately",
          description="export error paths to files immediately after they were found")
  private boolean dumpErrorPathImmediately = false;

  private final LogManager logger;
  private final ARGPathExport witnessExporter;


  public CEXExporter(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
    this.witnessExporter = new ARGPathExport(config);

    if (!exportSource) {
      errorPathSourceFile = null;
    }
    if (errorPathAssignment == null && errorPathCoreFile == null && errorPathFile == null
        && errorPathGraphFile == null && errorPathJson == null && errorPathSourceFile == null
        && errorPathAutomatonFile == null && errorPathAutomatonGraphmlFile == null) {
      exportErrorPath = false;
    }
  }

  /**
   * Export an Error Trace in different formats, for example as C-file, dot-file or automaton.
   *
   * @param pTargetState state of an ARG, used as fallback, if pCounterexampleInfo contains no targetPath.
   * @param pCounterexampleInfo contains further information and the (optional) targetPath.
   *                            If the targetPath is available, it will be used for the output.
   *                            Otherwise we use backwards reachable states from pTargetState.
   * @param cexIndex should be a unique index for the CEX and will be used to enumerate files.
   * @param allTargetPathEdges can be used to collect edges. All targetPath-edges are added to it.
   * @param reallyWriteToDisk enable/disable output to files.
   */
  public void exportCounterexample(final ARGState pTargetState,
      @Nullable final CounterexampleInfo pCounterexampleInfo,
      int cexIndex, @Nullable final Set<Pair<ARGState, ARGState>> allTargetPathEdges,
      boolean reallyWriteToDisk) {
    checkNotNull(pTargetState);

    final ARGPath targetPath = checkNotNull(getTargetPath(pTargetState, pCounterexampleInfo));

    final Set<Pair<ARGState, ARGState>> targetPathEdges = getEdgesOfPath(targetPath);
    if (allTargetPathEdges != null) {
      allTargetPathEdges.addAll(targetPathEdges);
    }

    if (reallyWriteToDisk && exportErrorPath && pCounterexampleInfo != null) {
      exportCounterexample(pTargetState, cexIndex, pCounterexampleInfo,
          targetPath, Predicates.in(targetPathEdges));
    }
  }

  private void exportCounterexample(final ARGState lastState,
                                    final int cexIndex,
                                    @Nullable final CounterexampleInfo counterexample,
                                    @Nonnull final ARGPath targetPath,
                                    final Predicate<Pair<ARGState, ARGState>> isTargetPathEdge) {

    final ARGState rootState = targetPath.getFirstState();

    writeErrorPathFile(errorPathFile, cexIndex,
            createErrorPathWithVariableAssignmentInformation(targetPath.getInnerEdges(), counterexample));

    if (errorPathCoreFile != null) {
      // the shrinked errorPath only includes the nodes,
      // that are important for the error, it is not a complete path,
      // only some nodes of the targetPath are part of it
      ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
      List<CFAEdge> shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);
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
      pathElements = ARGUtils.getAllStatesOnPathsTo(lastState);

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

    if (counterexample != null) {
      if (counterexample.getTargetPathModel() != null) {
        writeErrorPathFile(errorPathAssignment, cexIndex, counterexample.getTargetPathModel());
      }

      for (Pair<Object, PathTemplate> info : counterexample.getAllFurtherInformation()) {
        if (info.getSecond() != null) {
          writeErrorPathFile(info.getSecond(), cexIndex, info.getFirst());
        }
      }
    }

    writeErrorPathFile(errorPathAutomatonGraphmlFile, cexIndex, new Appender() {
      @Override
      public void appendTo(Appendable pAppendable) throws IOException {
        witnessExporter.writePath(pAppendable, rootState,
                ARGUtils.CHILDREN_OF_STATE,
                Predicates.in(pathElements),
                isTargetPathEdge,
                counterexample);
      }
    });
  }

  private Appender createErrorPathWithVariableAssignmentInformation(
          final List<CFAEdge> edgePath, final CounterexampleInfo counterexample) {
    final Model model = counterexample == null ? null : counterexample.getTargetPathModel();
    return new Appender() {
      @Override
      public void appendTo(Appendable out) throws IOException {
        // Write edges mixed with assigned values.
        CFAPathWithAssignments exactValuePath;
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
            //TODO Erase, counterexample is supposed to be independent of Assignable terms
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
                                      CFAPathWithAssignments pExactValuePath) throws IOException {

        for (CFAEdgeWithAssignments edgeWithAssignments : from(pExactValuePath).filter(notNull())) {

          if (edgeWithAssignments instanceof CFAMultiEdgeWithAssignments) {
            for (CFAEdgeWithAssignments singleEdge : (CFAMultiEdgeWithAssignments) edgeWithAssignments) {
              printPreciseValues(out, singleEdge);
            }
          } else {
            printPreciseValues(out, edgeWithAssignments);
          }
        }
      }

      private void printPreciseValues(Appendable out, CFAEdgeWithAssignments edgeWithAssignments) throws IOException {
        out.append(edgeWithAssignments.getCFAEdge().toString());
        out.append(System.lineSeparator());

        String cCode = edgeWithAssignments.prettyPrintCode(1);
        if (cCode != null) {
          out.append(cCode);
        }

        String comment = edgeWithAssignments.getComment();

        if (comment != null) {
          out.append('\t');
          out.append(comment);
          out.append(System.lineSeparator());
        }
      }
    };
  }

  private ARGPath getTargetPath(final ARGState targetState, @Nullable final CounterexampleInfo counterexample) {
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

  private void writeErrorPathFile(PathTemplate template, int cexIndex, Object content) {
    if (template != null) {
      // fill in index in file name
      Path file = template.getPath(cexIndex);

      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
                "Could not write information about the error path to file");
      }
    }
  }

  private static Set<Pair<ARGState, ARGState>> getEdgesOfPath(ARGPath pPath) {
    Set<Pair<ARGState, ARGState>> result = new HashSet<>(pPath.size());
    Iterator<ARGState> it = pPath.asStatesList().iterator();
    assert it.hasNext();
    ARGState lastElement = it.next();
    while (it.hasNext()) {
      ARGState currentElement = it.next();
      result.add(Pair.of(lastElement, currentElement));
      lastElement = currentElement;
    }
    return result;
  }

  public boolean shouldDumpErrorPathImmediately() {
    return dumpErrorPathImmediately;
  }
}
