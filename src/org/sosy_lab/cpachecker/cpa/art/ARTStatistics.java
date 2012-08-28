/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

import com.google.common.base.Optional;

@Options(prefix="cpa.art")
public class ARTStatistics implements Statistics {

  @Option(name="export", description="export final ART as .dot file")
  private boolean exportART = true;

  @Option(name="file",
      description="export final ART as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File artFile = new File("ART.dot");

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

  private final ARTCPA cpa;

  public ARTStatistics(Configuration config, ARTCPA cpa) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult,
      ReachedSet pReached) {

    if (   !exportErrorPath
        && (!exportART || (artFile == null))) {

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
        ARTElement lastElement = (ARTElement)pReached.getLastElement();
        if (lastElement != null && lastElement.isTarget()) {
          targetPath = ARTUtils.getOnePathTo(lastElement);
        }
      }

      if (targetPath != null) {

        if (exportErrorPath) {
          // the shrinked errorPath only includes the nodes,
          // that are important for the error, it is not a complete path,
          // only some nodes of the targetPath are part of it
          ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
          Path shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);

          ARTElement rootElement = targetPath.getFirst().getFirst();
          String pathProgram;
          if (counterexample != null && counterexample.getTargetPath() != null) {
            // precise error path
            pathProgram = PathToCTranslator.translateSinglePath(Optional.<CFA>absent(), targetPath);

          } else {
            // Imprecise error path.
            // For the text export, we have no other change, but for the C code
            // export we use all existing paths to avoid this problem.
            ARTElement lastElement = (ARTElement)pReached.getLastElement();
            Set<ARTElement> pathElements = ARTUtils.getAllElementsOnPathsTo(lastElement);
            pathProgram = PathToCTranslator.translatePaths(Optional.<CFA>absent(), rootElement, pathElements);
          }

          writeErrorPathFile(errorPathFile, targetPath);
          writeErrorPathFile(errorPathCoreFile, shrinkedErrorPath);
          writeErrorPathFile(errorPathSourceFile, pathProgram);
          writeErrorPathFile(errorPathJson, targetPath.toJSON());

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

    if (exportART && artFile != null) {
      try {
        Files.writeFile(artFile, ARTUtils.convertARTToDot(pReached, getEdgesOfPath(targetPath), pReached.getWaitlist()));
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write ART to file");
      }
    }
  }

  private void writeErrorPathFile(File file, Object content) {
    if (file != null) {
      try {
        Files.writeFile(file, content);
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e,
            "Could not information about the error path to file");
      }
    }
  }

  private static Set<Pair<ARTElement, ARTElement>> getEdgesOfPath(Path pPath) {
    if (pPath == null) {
      return Collections.emptySet();
    }

    Set<Pair<ARTElement, ARTElement>> result = new HashSet<Pair<ARTElement, ARTElement>>(pPath.size());
    Iterator<Pair<ARTElement, CFAEdge>> it = pPath.iterator();
    assert it.hasNext();
    ARTElement lastElement = it.next().getFirst();
    while (it.hasNext()) {
      ARTElement currentElement = it.next().getFirst();
      result.add(Pair.of(lastElement, currentElement));
      lastElement = currentElement;
    }
    return result;
  }
}
