// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package LoopAcc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This class looks for if cases directly after loops since they aren't any different in the cfa to
 * the loop condition, so this class will look for it in the file itself
 */
public class LoopGetIfAfterLoopCondition {

  private static FileLocation fileLocation;
  private static int smallestLineNumber;
  private static int biggestLineNumber;
  private static List<Integer> linesWithIf;
  private static int smallestIf;
  private static LogManager logger;

  // diese Implementierung könnte ineffizienter sein
  private static void readFile() {
    try (FileReader freader = new FileReader(fileLocation.getFileName())) {
      try (BufferedReader reader = new BufferedReader(freader)) {
        String line = "";
        int lineNumber = 1;
        while (line != null) {
          line = reader.readLine();
          if (lineNumber >= smallestLineNumber && lineNumber <= biggestLineNumber && line != null) {
            findIf(line, lineNumber);
          }
          lineNumber++;
        }
      }
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Something is not working with the file you try to import");
    }
  }

  private static int findSmallestIfLineNumber(List<Integer> ifLines) {
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

  private static void findIf(String text, int lineNumber) {
    String temp = text.split("\\(")[0];
    if (temp.contains("if")) {
      linesWithIf.add(lineNumber);
    }
  }

  private static int getSmallestLineNumber(List<CFANode> nodes) {
    int small = -1;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).getNumLeavingEdges() > 0) {
        small = nodes.get(i).getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
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

  private static int getBiggestLineNumber(List<CFANode> nodes) {
    int big = -1;
    for (int i = 0; i < nodes.size(); i++) {
      if (nodes.get(i).getNumLeavingEdges() > 0) {
        big = nodes.get(i).getLeavingEdge(0).getFileLocation().getStartingLineInOrigin();
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

  public static int getSmallestIf(List<CFANode> nodes, LogManager pLogger) {
    logger = pLogger;
    for (CFANode n : nodes) {
      if (n.getNumLeavingEdges() > 0) {
        fileLocation = n.getLeavingEdge(0).getFileLocation();
        break;
      }
    }
    smallestLineNumber = getSmallestLineNumber(nodes);
    biggestLineNumber = getBiggestLineNumber(nodes);
    linesWithIf = new ArrayList<>();
    readFile();
    smallestIf = findSmallestIfLineNumber(linesWithIf);
    return smallestIf;
  }
}
