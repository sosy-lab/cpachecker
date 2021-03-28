// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopInformation;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * The Method getSmallestIf will look for if's in the loop and return the smallest lineNumber, to be
 * able to differentiate between them and the condition, since they aren't any different in the cfa
 * to the loop condition, so this class will look for it in the file itself. The other methods are
 * to help out getSmallestIf.
 */
public final class UtilIfAfterLoopCondition {

  private final int NO_IF = -1;

  protected UtilIfAfterLoopCondition() {}

  private List<Integer> readFile(
      FileLocation fileLocation, int smallestLineNumber, int biggestLineNumber, LogManager logger) {
    List<Integer> allLinesWithIf = new ArrayList<>();
    try (Reader freader =
        Files.newBufferedReader(Paths.get(fileLocation.getFileName()), Charset.defaultCharset())) {
      try (BufferedReader reader = new BufferedReader(freader)) {
        String line = "";
        int lineNumber = 1;
        while (line != null) {
          line = reader.readLine();
          if (lineNumber >= smallestLineNumber && lineNumber <= biggestLineNumber && line != null) {
            int temp = findIf(line, lineNumber);
            if (temp != NO_IF) {
              allLinesWithIf.add(temp);
            }

            ;
          }
          lineNumber++;
        }
      }
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Something is not working with the file you try to import");
    }

    return allLinesWithIf;
  }

  private int findSmallestIfLineNumber(List<Integer> ifLines) {
    if (!ifLines.isEmpty()) {
      int small = ifLines.get(0);
      for (Integer v : ifLines) {
        if (v < small) {
          small = v;
        }
      }
      return small;
    } else {
      return -1;
    }
  }

  private int findIf(String text, int lineNumber) {
    int lineNumberTemp = lineNumber;
    String temp = Iterables.get(Splitter.on('(').split(text), 0);
    if (temp.contains("if")) {
      return lineNumberTemp;
    } else {
      lineNumberTemp = NO_IF;
      return lineNumberTemp;
    }
  }

  private int getSmallestLineNumber(List<CFANode> nodes) {
    int small = -1;
    for (CFANode node : nodes) {
      if (node.getNumLeavingEdges() > 0) {
        small = node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
        break;
      }
    }
    if (small != -1) {
      for (CFANode node : nodes) {
        if (node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin() < small) {
          small = node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
        }
      }
    }
    return small;
  }

  private int getBiggestLineNumber(List<CFANode> nodes) {
    int big = -1;
    for (CFANode node : nodes) {
      if (node.getNumLeavingEdges() > 0) {
        big = node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
        break;
      }
    }
    if (big != -1) {
      for (CFANode node : nodes) {
        if (node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin() > big) {
          big = node.getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
        }
      }
    }
    return big;
  }

  /**
   * Method that finds out if there is a if-case in the loop and in which line it starts, only the
   * if-case with the lowest line number will be returned if there are multiple
   *
   * @param nodes loop nodes that have to be analyzed
   * @param pLogger logger that logs exceptions
   * @return returns the line-number the if case starts, there are no if cases if it returns -1
   */
  public int getSmallestIf(List<CFANode> nodes, LogManager pLogger) {
    FileLocation fileLocation = null;
    LogManager logger = pLogger;

    for (CFANode n : nodes) {
      if (n.getNumLeavingEdges() > 0) {
        fileLocation = n.getLeavingEdge(0).getFileLocation();
        break;
      }
    }
    int smallestLineNumber = getSmallestLineNumber(nodes);
    int biggestLineNumber = getBiggestLineNumber(nodes);
    List<Integer> linesWithIf =
        readFile(fileLocation, smallestLineNumber, biggestLineNumber, logger);
    int smallestIf = findSmallestIfLineNumber(linesWithIf);
    return smallestIf;
  }
}
