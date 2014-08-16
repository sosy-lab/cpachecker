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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Class responsible for extracting coverage information from ReachedSet and CFA
 * and writing it into a file.
 */
public class CoverageInformation {

  public static void writeCoverageInfo(PathTemplate outputFile, ReachedSet reached, CFA cfa,
      LogManager logger) {

    Set<CFANode> reachedLocations = getAllLocationsFromReached(reached);

    Map<String, CoveragePrinter> printers = new HashMap<>();

    //Add information about visited locations
    for (CFANode node : cfa.getAllNodes()) {
       //This part adds lines, which are only on edges, such as "return" or "goto"
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        boolean visited = reachedLocations.contains(edge.getPredecessor())
            && reachedLocations.contains(edge.getSuccessor());

        if (edge instanceof MultiEdge) {
          for (CFAEdge innerEdge : ((MultiEdge)edge).getEdges()) {
            handleEdgeCoverage(innerEdge, visited, printers);
          }
        } else {
          handleEdgeCoverage(edge, visited, printers);
        }
      }
    }

    // Add information about visited functions
    for (FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      final FileLocation loc = entryNode.getFileLocation();
      if (loc.getStartingLineNumber() == 0) {
        // dummy location
        continue;
      }
      final String functionName = entryNode.getFunctionName();
      final CoveragePrinter printer = getPrinter(loc, printers);

      final int startingLine = loc.getStartingLineInOrigin();
      final int endingLine = loc.getEndingLineNumber() - loc.getStartingLineNumber() + loc.getStartingLineInOrigin();

      printer.addExistingFunction(functionName, startingLine, endingLine);

      if (reachedLocations.contains(entryNode)) {
        printer.addVisitedFunction(functionName);
      }
    }

    for (Map.Entry<String, CoveragePrinter> entry : printers.entrySet()) {
      Path p = outputFile.getPath(entry.getKey().replace(File.separator, "--"));
      try (Writer out = Files.openOutputFile(p)) {
        entry.getValue().print(out, entry.getKey());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
      }
    }
  }

  private static void handleEdgeCoverage(final CFAEdge edge, final boolean visited,
      final Map<String, CoveragePrinter> printers) {
    final FileLocation loc = edge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return;
    }
    if (edge instanceof ADeclarationEdge
        && (((ADeclarationEdge)edge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return;
    }

    final CoveragePrinter printer = getPrinter(loc, printers);

    final int startingLine = loc.getStartingLineInOrigin();
    final int endingLine = loc.getEndingLineNumber() - loc.getStartingLineNumber() + loc.getStartingLineInOrigin();

    for (int line = startingLine; line <= endingLine; line++) {
      printer.addExistingLine(line);
    }

    if (visited) {
      for (int line = startingLine; line <= endingLine; line++) {
        printer.addVisitedLine(line);
      }
    }
  }

  private static CoveragePrinter getPrinter(FileLocation loc, Map<String, CoveragePrinter> printers) {
    assert loc.getStartingLineNumber() != 0; // Cannot produce coverage info for dummy file location

    String file = loc.getFileName();
    CoveragePrinter printer = printers.get(file);
    if (printer == null) {
      printer = new CoveragePrinterGcov();
      printers.put(file, printer);
    }
    return printer;
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
