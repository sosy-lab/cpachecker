/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement;
import org.sosy_lab.cpachecker.util.AbstractElements;

@Options(prefix="cpa.art")
public class ARTStatistics implements Statistics {

  @Option(name="export")
  private boolean exportART = true;

  @Option(name="file", type=Option.Type.OUTPUT_FILE)
  private File artFile = new File("ART.dot");

  @Option(name="errorPath.export")
  private boolean exportErrorPath = true;

  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE)
  private File errorPathFile = new File("ErrorPath.txt");

  @Option(name="errorPath.core", type=Option.Type.OUTPUT_FILE)
  private File errorPathCoreFile = new File("ErrorPathCore.txt");
  
  @Option(name="errorPath.source", type=Option.Type.OUTPUT_FILE)
  private File errorPathSourceFile = new File("ErrorPath.c");

  @Option(name="errorPath.json", type=Option.Type.OUTPUT_FILE)
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
        assert targetPath.getLast().getFirst() == lastElement;
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
          cpa.getLogger().log(Level.WARNING,
              "Could not write error path to file (", e.getMessage(), ")");
        }
      }
    }

    if (exportART && artFile != null) {
      dumpARTToDotFile(pReached, getEdgesOfPath(targetPath));
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

  private void dumpARTToDotFile(ReachedSet pReached, Set<Pair<ARTElement, ARTElement>> pathEdges) {
    ARTElement firstElement = (ARTElement)pReached.getFirstElement();

    Deque<ARTElement> worklist = new LinkedList<ARTElement>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARTElement> processed = new HashSet<ARTElement>();
    StringBuilder sb = new StringBuilder();
    StringBuilder edges = new StringBuilder();

    sb.append("digraph ART {\n");
    sb.append("style=filled; fontsize=10.0; fontname=\"Courier New\"; \n");

    worklist.add(firstElement);

    while(worklist.size() != 0){
      ARTElement currentElement = worklist.removeLast();
      if(processed.contains(currentElement)){
        continue;
      }
      processed.add(currentElement);
      if(!nodesList.contains(currentElement.getElementId())){
        String color;
        if (currentElement.isCovered()) {
          color = "green";
        } else if (currentElement.isTarget()) {
          color = "red";
        } else {
          AbstractElement abselem = AbstractElements.extractElementByType(currentElement, PredicateAbstractElement.AbstractionElement.class);
          if (abselem != null) {
            color = "blue";
          } else {
            color = "white";
          }
        }

        CFANode loc = currentElement.retrieveLocationElement().getLocationNode();
        String label = (loc==null ? 0 : loc.getNodeNumber()) + "000" + currentElement.getElementId();

        sb.append("node [shape = diamond, color = " + color + ", style = filled, label=" + label +" id=\"" + currentElement.getElementId() + "\"] " + currentElement.getElementId() + ";\n");

        nodesList.add(currentElement.getElementId());
      }

      for (ARTElement covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getElementId());
        edges.append(" -> ");
        edges.append(currentElement.getElementId());
        edges.append(" [style = dashed, label = \"covered by\"];\n");
      }

      for (ARTElement child : currentElement.getChildren()) {
        boolean colored = pathEdges.contains(Pair.of(currentElement, child));
        CFAEdge edge = currentElement.getEdgeToChild(child);
        edges.append(currentElement.getElementId());
        edges.append(" -> ");
        edges.append(child.getElementId());
        edges.append(" [");
        if (colored) {
          edges.append("color = red");
        }
        if (edge != null) {
          edges.append(" label = \"");
          edges.append(edge.toString().replace('"', '\''));
          edges.append("\"");
          edges.append(" id=\"");
          edges.append(currentElement.getElementId());
          edges.append("->");
          edges.append(child.getElementId());
          edges.append("\"");
        }
        edges.append("];\n");
        if(!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }
    sb.append(edges);
    sb.append("}\n");

    try {
      Files.writeFile(artFile, sb);
    } catch (IOException e) {
      cpa.getLogger().log(Level.WARNING,
          "Could not write ART to file (", e.getMessage(), ")");
    }
  }
}
