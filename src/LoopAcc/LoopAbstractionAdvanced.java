/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package LoopAcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This class takes a file and changes all of the loops in a advanced abstraction to make the
 * program verifiable for specific cpa's
 */
public class LoopAbstractionAdvanced {
  private int lineNumber = 1;

  public LoopAbstractionAdvanced() {

  }

  /**
   * This method changes all the necessary lines of codes and saves it in a new file
   *
   * @param loopInfo Information about all the loops in the file
   */
  public void
      changeFileToAbstractFile(
          LoopInformation loopInfo,
          LogManager logger,
          String pathForNewFile,
          boolean automate) {
    ArrayList<LoopData> outerLoopTemp = new ArrayList<>();
    ArrayList<Integer> loopStarts = new ArrayList<>();
    boolean closed = true;
    for (LoopData loopData : loopInfo.getLoopData()) {
      if (loopData.getLoopType() == "while") {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin());
      } else if (loopData.getLoopType() == "for") {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin()
        );
      }
    }

    String fileLocation = "../cpachecker/" + loopInfo.getCFA().getFileNames().get(0).toString();

    String content =
        "extern void __VERIFIER_error() __attribute__ ((__noreturn__));" + System.lineSeparator();

    boolean flagInt = true;
    boolean flagChar = true;
    boolean flagShort = true;
    boolean flagLong = true;
    boolean flagLongLong = true;
    boolean flagDouble = true;
    for (LoopData lD : loopInfo.getLoopData()) {
      for (String io : lD.getInputsOutputs()) {
        switch (io.split("&")[1]) {
          case "int":
            if (flagInt) {
            content += "extern unsigned int __VERIFIER_nondet_uint(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(int cond);" + System.lineSeparator();
            flagInt = false;
          }
            break;
          case "char":
            if (flagChar) {
            content += "extern char __VERIFIER_nondet_char(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(char cond);" + System.lineSeparator();
            flagChar = false;
          }
            break;
          case "short":
            if (flagShort) {
            content += "extern short __VERIFIER_nondet_short(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(short cond);" + System.lineSeparator();
            flagShort = false;
          }
            break;
          case "long":
            if (flagLong) {
            content += "extern long __VERIFIER_nondet_long(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(long cond);" + System.lineSeparator();
            flagLong = false;
          }
            break;
          case "long long":
            if (flagLongLong) {
            content +=
                "extern longlong __VERIFIER_nondet_longlong(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(longlong cond);" + System.lineSeparator();
            flagLongLong = false;
          }
            break;
          case "double":
            if (flagDouble) {
            content += "extern double __VERIFIER_nondet_double(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(double cond);" + System.lineSeparator();
            flagDouble = false;
          }
            break;
        }

      }
    }

    try {
      FileReader freader = new FileReader(fileLocation);
      BufferedReader reader = new BufferedReader(freader);

      String line = "";

      while (line != null) {
        if (loopStarts.contains(lineNumber)) {
          for (LoopData loopD : loopInfo.getLoopData()) {


            if (((loopD.getLoopType() == "while"
                || loopD
                    .getLoopInLoop())
                && loopD.getLoopStart()
                    .getEnteringEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin() == lineNumber)
                || (loopD
                    .getLoopType() == "for"
                    && loopD.getLoopStart()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getStartingLineInOrigin()

                        == lineNumber)) {

              CFANode endNodeCondition = findLastNodeInCondition(loopD);
              if (loopD.getLoopType().equals("while") || loopD.getLoopInLoop()) {
                line = reader.readLine();
                content = content + whileCondition(loopD);
                lineNumber++;
              } else if (loopD.getLoopType().equals("for")) {
                // line = reader.readLine();
                line = reader.readLine();
                content = content + forCondition(loopD);
                lineNumber++;
              }
              for (String x : loopD.getInputsOutputs()) {
                    switch (x.split("&")[1]) {
                      case "int":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_uint();" + System.lineSeparator());
                        break;
                      case "char":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_char();" + System.lineSeparator());
                        break;
                      case "short":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_short();" + System.lineSeparator());
                        break;
                      case "long":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_long();" + System.lineSeparator());
                        break;
                      case "long long":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_longlong();"
                                + System.lineSeparator());
                        break;
                      case "double":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_double();" + System.lineSeparator());
                        break;
                    }
              }

              if (loopD.getIsOuterLoop() == false) {
              if (loopD.getLoopType() == "while") {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition() + ");") + System.lineSeparator();
                while (lineNumber >= endNodeCondition.getEnteringEdge(
                    0)
                    .getFileLocation()
                    .getEndingLineInOrigin()
                    && line != null
                    && lineNumber <= loopD.getLoopEnd()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getEndingLineInOrigin()) {

                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                /**
                 * if (loopD.getIsOuterLoop() && loopD.getLoopEnd().getNodeNumber() ==
                 * findEndNode(loopD.getInnerLoop()) .getNodeNumber()) { int counter = 2; while
                 * (counter > 0) { line = reader.readLine(); content += line +
                 * System.lineSeparator(); lineNumber++; if (line.contains("}")) { counter -= 1; }
                 *
                 * } content += ("__VERIFIER_assume(!" + loopD.getCondition() + ");" +
                 * System.lineSeparator()); }
                 */
                while (!closed) {
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                line = reader.readLine();
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                while (!line.contains("}")) {
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                content +=
                    ("__VERIFIER_assume(!("
                        + loopD.getCondition()
                        + "));"
                        + System.lineSeparator());

                for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                  line = reader.readLine();
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!closed) {
                    line = reader.readLine();
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!line.contains("}")) {
                    line = reader.readLine();
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (outerLoopTemp.get(i).getLoopType() == "for") {
                    content +=
                        ("__VERIFIER_assume(!("
                            + outerLoopTemp.get(i).getCondition().split(";")[1]
                            + "));"
                            + System.lineSeparator());
                  } else if (outerLoopTemp.get(i).getLoopType() == "while") {
                    content +=
                        ("__VERIFIER_assume(!("
                            + outerLoopTemp.get(i).getCondition()
                            + "));"
                            + System.lineSeparator());
                  }
                }
                outerLoopTemp.clear();
              } else if (loopD.getLoopType() == "for") {
                content +=
                    ("__VERIFIER_assume("
                        + loopD.getCondition().split(";")[1]
                        + ");")
                        + System.lineSeparator();
                while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin()
                    && line != null
                    && lineNumber < loopD.getLoopEnd()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getEndingLineInOrigin()
                ) {
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                while (!closed) {
                  lineNumber++;
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                }
                lineNumber++;
                line = reader.readLine();
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                while (!line.contains("}")) {
                  lineNumber++;
                line = reader.readLine();
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
              }
                content +=
                    ("__VERIFIER_assume(!("
                        + loopD.getCondition().split(";")[1]
                        + "));"
                        + System.lineSeparator());

                for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                  line = reader.readLine();
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!closed) {
                    line = reader.readLine();
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  line = reader.readLine();
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!line.contains("}")) {
                    line = reader.readLine();
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (outerLoopTemp.get(i).getLoopType() == "for") {
                  content +=
                      ("__VERIFIER_assume(!("
                          + outerLoopTemp.get(i)
                              .getCondition()
                              .split(";")[1]
                          + "));"
                          + System.lineSeparator());
                } else if (outerLoopTemp.get(i).getLoopType() == "while") {
                  content +=
                      ("__VERIFIER_assume(!("
                          + outerLoopTemp.get(i).getCondition()
                          + "));"
                          + System.lineSeparator());
                }
                }
                outerLoopTemp.clear();
              }
            } else if (loopD.getIsOuterLoop()) {
              if (loopD.getLoopType() == "while") {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition() + ");") + System.lineSeparator();
              } else if (loopD.getLoopType() == "for") {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition().split(";")[1] + ");")
                        + System.lineSeparator();
              }
              outerLoopTemp.add(loopD);
              if (loopD.getLoopType() == "for") {
              while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                  .getFileLocation()
                  .getEndingLineInOrigin()
                  && line != null
                  && (lineNumber < (loopD
                          .getInnerLoop()
                      .getIncomingEdges()
                      .asList()
                      .get(0)
                      .getFileLocation()
                      .getStartingLineInOrigin()
                  )
                  )
              )
              {
                line = reader.readLine();
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                lineNumber++;
              }
            } else if (loopD.getLoopType() == "while") {
              while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                  .getFileLocation()
                  .getEndingLineInOrigin()
                  && line != null
                  && (lineNumber < (loopD.getInnerLoop()
                      .getIncomingEdges()
                      .asList()
                      .get(0)
                      .getFileLocation()
                      .getStartingLineInOrigin()))) {
                line = reader.readLine();
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
            }
            }
          }
        }
        } else if (!loopStarts.contains(lineNumber)) {
          line = reader.readLine();
          if (line != null) {
            content += line + System.lineSeparator();
            lineNumber++;
          }
        }
      }
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Something is not working with the file you try to import");
    }
    printFile(loopInfo, content, pathForNewFile, logger, automate);
  }

  private String whileCondition(LoopData loopD) {
    return "for(int cpachecker_i=0; cpachecker_i <"
        + min(loopD.getAmountOfPaths(), loopD.getOutputs().size())
        + "&&("
        + loopD.getCondition()
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  }

  private String forCondition(LoopData loopD) {
    return loopD.getCondition().split(";")[0]
        + System.lineSeparator()
        + "for(int cpachecker_i=0; cpachecker_i <" 
        + min(loopD.getAmountOfPaths(), loopD.getOutputs().size())
        + "&&("
        + loopD.getCondition().split(";")[1]
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  }

  private boolean ifCaseClosed(String line, boolean closed) {

    boolean ifCaseC = closed;

    if (line != null) {
      String temp = line.split("\\(")[0];
      if (ifCaseC == false && line.contains("}")) {
        ifCaseC = true;
      }
      if (temp.contains("if") || line.contains("else")) {
      ifCaseC = false;
    }
  }
    return ifCaseC;
  }

  private void printFile(
      LoopInformation loopInfo,
      String content,
      String pathForNewFile,
      LogManager logger,
      boolean automate) {

    String fileName;

    if (automate == true) {
      fileName = pathForNewFile;
    } else {
      fileName =
          pathForNewFile + "abstract" + loopInfo.getCFA().getFileNames().get(0).getFileName();
    }

    File file =
        new File(fileName);

    file.getParentFile().mkdirs();
    try (FileWriter fileWriter =
        new FileWriter(file)) {
      String fileContent = content;
      fileWriter.write(fileContent);
      fileWriter.close();
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Something is not working with the file you try to import");
    }
  }

  private CFANode findLastNodeInCondition(LoopData loopData) {
    CFANode temp = null;
    if(!loopData.getNodesInCondition().isEmpty()) {
    temp = loopData.getNodesInCondition().get(0);
    for (CFANode node : loopData.getNodesInCondition()) {
      if (temp.getNodeNumber() > node.getNodeNumber()) {
        temp = node;
      }
    }
    } else {
      temp = loopData.getLoopStart();
    }
    return temp;
  }

  private CFANode findFirstNodeInCondition(LoopData loopData) {
    CFANode temp = loopData.getNodesInCondition().get(0);
    for (CFANode node : loopData.getNodesInCondition()) {
      if (temp.getNodeNumber() < node.getNodeNumber()) {
        temp = node;
      }
    }
    return temp;
  }

  private CFANode findEndNode(Loop loop) {

    CFANode end = loop.getLoopNodes().first();

    for (CFANode tempNode : loop.getLoopNodes()) {
      if (end.getNodeNumber() < tempNode.getNodeNumber()) {
        end = tempNode;
      }
    }

    return end;
  }

  private int min(int x, int y) {
    if (x > y) {
      return y;
    } else {
      return x;
    }
  }
}
