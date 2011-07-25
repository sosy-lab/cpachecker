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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

@Options(prefix="cpa.art")
public class ARTStatistics implements Statistics {

  @Option(name="export", description="export final ART as .dot file")
  private boolean exportART = true;

  @Option(name="file", type=Option.Type.OUTPUT_FILE,
      description="export final ART as .dot file")
  private File artFile = new File("ART.dot");

  @Option(name="errorPath.export",
      description="export error path to file, if one is found")
  private boolean exportErrorPath = true;

  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE,
      description="export error path to file, if one is found")
  private File errorPathFile = new File("ErrorPath.txt");

  @Option(name="errorPath.core", type=Option.Type.OUTPUT_FILE,
      description="export error path to file, if one is found")
  private File errorPathCoreFile = new File("ErrorPathCore.txt");

  @Option(name="errorPath.source", type=Option.Type.OUTPUT_FILE,
      description="export error path to file, if one is found")
  private File errorPathSourceFile = new File("ErrorPath.c");

  @Option(name="errorPath.json", type=Option.Type.OUTPUT_FILE,
      description="export error path to file, if one is found")
  private File errorPathJson = new File("ErrorPath.json");

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

    if (   (!exportErrorPath || (errorPathFile == null))
        && (!exportART       || (artFile == null))) {

      // shortcut, avoid unnecessary creation of path etc.
      return;
    }

    Path targetPath = null;
    ARTElement lastElement = (ARTElement)pReached.getLastElement();
    if (lastElement != null && lastElement.isTarget()) {

      // use target path stored at CPA if present
      targetPath = cpa.getTargetPath();
      if (targetPath != null) {
        // target path has to be the path to the current target element
       // assert targetPath.getLast().getFirst() == lastElement;
      } else {
        // otherwise create one
        targetPath = ARTUtils.getOnePathTo(lastElement);
      }

      if (exportErrorPath && errorPathFile != null) {

        // the shrinked errorPath only includes the nodes,
        // that are important for the error, it is not a complete path,
        // only some nodes of the targetPath are part of it
        ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
        Path shrinkedErrorPath = pathShrinker.shrinkErrorPath(targetPath);

        try {
          Files.writeFile(errorPathFile, targetPath);
          Files.writeFile(errorPathCoreFile, shrinkedErrorPath);
          Files.writeFile(errorPathSourceFile, targetPath.toSourceCode());
          Files.writeFile(errorPathJson, targetPath.toJSON());

        } catch (IOException e) {
          cpa.getLogger().logUserException(Level.WARNING, e,
              "Could not write error path to file");
        }
      }
    }

    if (exportART && artFile != null) {
      try {
        Files.writeFile(artFile, ARTUtils.convertARTToDot(pReached, getEdgesOfPath(targetPath)));
      } catch (IOException e) {
        cpa.getLogger().logUserException(Level.WARNING, e, "Could not write ART to file");
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
