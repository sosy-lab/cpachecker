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
import java.io.PrintWriter;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsAbstractElement;

@Options
public class ARTStatistics implements Statistics {

  @Option(name="ART.export")
  private boolean exportART = true;

  @Option(name="ART.file", type=Option.Type.OUTPUT_FILE)
  private File artFile = new File("ART.dot");

  @Option(name="cpas.art.errorPath.export")
  private boolean exportErrorPath = true;

  @Option(name="cpas.art.errorPath.file", type=Option.Type.OUTPUT_FILE)
  private File errorPathFile = new File("ErrorPath.txt");

  private final LogManager logger;

  public ARTStatistics(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.logger = logger;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintWriter pOut, Result pResult,
      ReachedSet pReached) {
    if (exportART) {
      dumpARTToDotFile(pReached);
    }

    if (exportErrorPath) {
      ARTElement lastElement = (ARTElement)pReached.getLastElement();
      if (lastElement != null && lastElement.isTarget()) {
        try {
          Files.writeFile(errorPathFile,
              AbstractARTBasedRefiner.buildPath(lastElement), false);
        } catch (IOException e) {
          logger.log(Level.WARNING,
              "Could not write error path to file (", e.getMessage(), ")");
        }
      }
    }
  }

  private void dumpARTToDotFile(ReachedSet pReached) {
    ARTElement firstElement = (ARTElement)pReached.getFirstElement();

    Deque<ARTElement> worklist = new LinkedList<ARTElement>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARTElement> processed = new HashSet<ARTElement>();
    StringBuffer sb = new StringBuffer();
    StringBuffer edges = new StringBuffer();

    sb.append("digraph ART {\n");
    sb.append("style=filled; color=lightgrey; \n");

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
          SymbPredAbsAbstractElement symbpredabselem = currentElement.retrieveWrappedElement(SymbPredAbsAbstractElement.class);
          if (symbpredabselem != null && symbpredabselem.isAbstractionNode()) {
            color = "blue";
          } else {
            color = "white";
          }
        }

        CFANode loc = currentElement.retrieveLocationElement().getLocationNode();
        String label = (loc==null ? 0 : loc.getNodeNumber()) + "000" + currentElement.getElementId();
        sb.append("node [shape = diamond, color = " + color + ", style = filled, label=" + label +"] " + currentElement.getElementId() + ";\n");

        nodesList.add(currentElement.getElementId());
      }

      for (ARTElement covered : currentElement.getCoveredByThis()) {
        edges.append(covered.getElementId());
        edges.append(" -> ");
        edges.append(currentElement.getElementId());
        edges.append(" [style = dashed, label = \"covered by\"];\n");
      }

      for(ARTElement child : currentElement.getChildren()){
        CFAEdge edge = currentElement.getEdgeToChild(child);
        edges.append(currentElement.getElementId());
        edges.append(" -> ");
        edges.append(child.getElementId());
        edges.append(" [label = \"");
        edges.append(edge != null ? edge.toString().replace('"', '\'') : "");
        edges.append("\"];\n");
        if(!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }
    sb.append(edges);
    sb.append("}\n");

    try {
      Files.writeFile(artFile, sb, false);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Could not write ART to file (", e.getMessage(), ")");
    }
  }
}
