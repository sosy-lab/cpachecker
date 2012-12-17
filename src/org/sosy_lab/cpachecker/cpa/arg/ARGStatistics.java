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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

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

@Options(prefix="cpa.arg")
public class ARGStatistics implements Statistics {

  @Option(name="export", description="export final ARG as .dot file")
  private boolean exportART = true;

  @Option(name="file",
      description="export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File argFile = new File("ARG.dot");

  @Option(name="errorPath.export",
      description="export error path to file, if one is found")
  private boolean exportErrorPath = true;

  @Option(name="errorPath.file",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathFile = new File("ErrorPath.txt");

  @Option(name="errorPath.core",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathCoreFile = new File("ErrorPathCore.txt");

  @Option(name="errorPath.source",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathSourceFile = new File("ErrorPath.c");

  @Option(name="errorPath.json",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathJson = new File("ErrorPath.json");

  @Option(name="errorPath.assignment",
      description="export one variable assignment for error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathAssignment = new File("ErrorPathAssignment.txt");

  @Option(name="errorPath.graph",
      description="export error path to file, if one is found")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File errorPathGraphFile = new File("ErrorPath.dot");

  @Option(name ="errorPath.toCFile",
      description="translate Error Path to C File ")
  private boolean toCFile = true;

  private final ARGCPA cpa;

  public ARGStatistics(Configuration config, ARGCPA cpa) throws InvalidConfigurationException {
    config.inject(this);

    toCFile = Boolean.parseBoolean(config.getProperty("errorPath.toCFile"));

    this.cpa = cpa;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult,
      ReachedSet pReached) {


    if (!(   (exportErrorPath && (errorPathFile != null))
          || (exportART       && (argFile != null      ))
       )) {

      // do nothing, if !(exportErrorPath || exportART)
      // shortcut, avoid unnecessary creation of path etc.
      return;
    }

    Path targetPath = null;

    if (pResult != Result.SAFE) {
      CounterexampleInfo counterexample = cpa.getLastCounterexample();
      Object assignment = null;

      if (counterexample != null) {
        targetPath = counterexample.getTargetPath();
        assignment = counterexample.getTargetPathAssignment();
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

      if (targetPath != null) {

        if (exportErrorPath && errorPathFile != null) {
          // the shrinked errorPath only includes the nodes,
          // that are important for the error, it is not a complete path,
          // only some nodes of the targetPath are part of it
          ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
          Path shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);

          ARGState rootState = targetPath.getFirst().getFirst();
          Set<ARGState> pathElements;
          String pathProgram = null;
          if (counterexample != null && counterexample.getTargetPath() != null) {
            // precise error path
            pathElements = targetPath.getStateSet();

            if (toCFile)
              pathProgram = PathToCTranslator.translateSinglePath(targetPath);

          } else {
            // Imprecise error path.
            // For the text export, we have no other chance,
            // but for the C code and graph export we use all existing paths
            // to avoid this problem.
            ARGState lastElement = (ARGState)pReached.getLastState();
            pathElements = ARGUtils.getAllStatesOnPathsTo(lastElement);

            if (toCFile)
              pathProgram =
                  PathToCTranslator.translatePaths(rootState, pathElements);
          }

          writeErrorPathFile(errorPathFile, targetPath);
          writeErrorPathFile(errorPathCoreFile, shrinkedErrorPath);

          if(toCFile)
            writeErrorPathFile(errorPathSourceFile, pathProgram);

          writeErrorPathFile(errorPathJson, targetPath.toJSON());
          writeErrorPathFile(errorPathGraphFile, ARGUtils.convertARTToDot(rootState, pathElements, getEdgesOfPath(targetPath)));

          if (assignment != null) {
            writeErrorPathFile(errorPathAssignment, assignment);
          }

          if (counterexample != null) {
            for (Pair<Object, File> info : counterexample.getAllFurtherInformation()) {
              writeErrorPathFile(info.getSecond(), info.getFirst());
            }
          }
        }
      }
    }

    if (exportART && argFile != null) {
      try {
        ARGState rootState = (ARGState)pReached.getFirstState();
        Files.writeFile(argFile, ARGUtils.convertARTToDot(rootState, null, getEdgesOfPath(targetPath)));
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write ARG to file.");
      }
    }
  }

  private void writeErrorPathFile(File file, Object content) {
    if (file != null) {
      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not write information about the error path to file.");
      }
    }
  }

  private static Set<Pair<ARGState, ARGState>> getEdgesOfPath(Path pPath) {
    if (pPath == null) {
      return Collections.emptySet();
    }

    Set<Pair<ARGState, ARGState>> result = new HashSet<Pair<ARGState, ARGState>>(pPath.size());
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