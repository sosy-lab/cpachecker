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
package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Class responsible for extracting coverage information from ReachedSet and CFA
 * and writing it into a file.
 */
public class CoverageInformation {

  public static void writeCoverageInfo(Path outputFile, ReachedSet reached, CFA cfa,
      LogManager logger, String programNames) {

    Set<CFANode> locations = getAllLocationsFromReached(reached);

    CoveragePrinter printer = new CoveragePrinterGcov();

    //Add information about visited locations and functions
    for (CFANode node : locations) {
      printer.addVisitedLine(node.getLineNumber());
      printer.addVisitedFunction(node.getFunctionName());
    }

    for (CFANode node : cfa.getAllNodes()) {
      if (node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0) instanceof CDeclarationEdge) {
        //We don't mark all global definitions
        CDeclarationEdge declEdge = (CDeclarationEdge) node.getLeavingEdge(0);
        if (declEdge.getDeclaration().isGlobal()) {
          continue;
        }
      }

      //We don't mark last line - "}"
      if (node instanceof FunctionExitNode) {
        continue;
      }

      printer.addExistingLine(node.getLineNumber());

      //This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge pEdge : CFAUtils.leavingEdges(node)) {
        if (pEdge instanceof CDeclarationEdge) {
          continue;
        }
        int line = pEdge.getLineNumber();
        CFANode predessor = pEdge.getPredecessor();
        CFANode successor = pEdge.getSuccessor();

        if (pEdge instanceof AStatementEdge) {
          FileLocation location = ((AStatementEdge)pEdge).getStatement().getFileLocation();
          if (location.getStartingLineNumber() != location.getEndingLineNumber()) {
            for (int j = location.getStartingLineNumber(); j <= location.getEndingLineNumber(); j++) {
              printer.addExistingLine(j);
              if (locations.contains(predessor) && locations.contains(successor)) {
                printer.addVisitedLine(j);
              }
            }
          }
        }

        printer.addExistingLine(line);

        if (locations.contains(predessor) && locations.contains(successor)) {
          printer.addVisitedLine(line);
        }
        if (pEdge instanceof MultiEdge) {
          for (CFAEdge singleEdge : ((MultiEdge)pEdge).getEdges()) {
            if (singleEdge instanceof CDeclarationEdge) {
              continue;
            }
            line = singleEdge.getLineNumber();
            printer.addExistingLine(line);
            if (locations.contains(predessor) && locations.contains(successor)) {
              printer.addVisitedLine(line);
            }
          }
        }
      }
    }

    //Now collect information about all functions
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      printer.addExistingFunction(entryNode.getFunctionName(), entryNode.getLineNumber()
          , entryNode.getExitNode().getLineNumber());
    }

    try (Writer out = Files.openOutputFile(outputFile)) {
      printer.print(out, programNames);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
    }
  }

  private static Set<CFANode> getAllLocationsFromReached(ReachedSet reached) {
    if (reached instanceof ForwardingReachedSet) {
      reached = ((ForwardingReachedSet)reached).getDelegate();
    }
    if (reached instanceof LocationMappedReachedSet) {
      return ((LocationMappedReachedSet)reached).getLocations();

    } else {
      return from(reached)
                  .transform(EXTRACT_LOCATION)
                  .filter(notNull())
                  .toSet();
    }
  }
}
