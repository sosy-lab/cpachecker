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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This class takes a file and changes all of the loops in a naiv abstraction to make the program
 * verifiable for specific cpa's
 */
public class LoopAbstractionNaiv {
  private int lineNumber = 1;

  public LoopAbstractionNaiv() {

  }

  /**
   * This method changes all the necessary lines of codes and saves it in a new file
   *
   * @param loopInfo Information about all the loops in the file
   */
  public void
      changeFileToAbstractFile(LoopInformation loopInfo, LogManager logger, String pathForNewFile) {
    ArrayList<Integer> loopStarts = new ArrayList<>();
    for(LoopData loopData:loopInfo.getLoopData()) {
      if (loopData.getLoopType() == "while") {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin());
    } else if (loopData.getLoopType() == "for") {
      loopStarts.add(
          loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin()
              - 1);
    }
    }

    FileLocation fileLocation =
        loopInfo.getLoopData().get(0).getLoopStart().getEnteringEdge(0).getFileLocation();

    String content =
        "extern void __VERIFIER_error() __attribute__ ((__noreturn__));" + System.lineSeparator();
    content += "extern unsigned int __VERIFIER_nondet_uint(void);" + System.lineSeparator();
    content +=
        "extern void __VERIFIER_assume(int cond);"
            + System.lineSeparator();

    try {
      FileReader freader = new FileReader(fileLocation.getFileName());
      BufferedReader reader = new BufferedReader(freader);

      String line = "";


      while (line != null) {
        if (loopStarts.contains(lineNumber)) {
          for (LoopData loopD : loopInfo.getLoopData()) {
            if ((loopD.getLoopType() == "while"
                && loopD.getLoopStart()
                    .getEnteringEdge(0)
                    .getFileLocation()
                    .getStartingLineInOrigin() == lineNumber)
                || (loopD.getLoopType() == "for"
                    && loopD.getLoopStart()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getStartingLineInOrigin()
                        - 1 == lineNumber)) {
              CFANode endNodeCondition = findLastNodeInCondition(loopD);
              if (loopD.getLoopType().equals("while")) {
                line = reader.readLine();
                content = content + "if(" + loopD.getCondition() + "){" + System.lineSeparator();
                lineNumber++;
              } else if (loopD.getLoopType().equals("for")) {
                line = reader.readLine();
                line = reader.readLine();
                content +=
                    "if(" + loopD.getCondition().split(";")[1] + ") {" + System.lineSeparator();
                lineNumber++;
              }
              for (String x : loopD.getOutputs()) {
                content += (x + "=__VERIFIER_nondet_uint();" + System.lineSeparator());
              }


              if (loopD.getLoopType() == "while") {
                content +=
                    ("__VERIFIER_assume(" + loopD.getCondition() + ");") + System.lineSeparator();
                while (lineNumber >= endNodeCondition.getEnteringEdge(0)
                    .getFileLocation()
                    .getEndingLineInOrigin()

                  && line != null
                  && lineNumber <= loopD
                      .getLoopEnd()
                      .getEnteringEdge(0)
                      .getFileLocation()
                      .getEndingLineInOrigin()
                ) {
                line = reader.readLine();
                content += line + System.lineSeparator();
                lineNumber++;
              }
              content +=
                  ("__VERIFIER_assume(!" + loopD.getCondition() + ");" + System.lineSeparator());
            }
            else if (loopD.getLoopType() == "for") {
              content +=
                  ("__VERIFIER_assert("
                      + loopD.getCondition()
                          .split(
                              ";")[1]
                      + ");")
                      + System.lineSeparator();
              while (lineNumber >= endNodeCondition
                  .getEnteringEdge(
                  0)
                  .getFileLocation()
                  .getStartingLineInOrigin()
                  && line != null
                  && lineNumber < loopD
                      .getLoopEnd()
                      .getEnteringEdge(0)
                      .getFileLocation()
                      .getEndingLineInOrigin()
              ) {
                line = reader.readLine();
                content += line + System.lineSeparator();
                lineNumber++;
              }

              content +=
                  ("__VERIFIER_assert(!"
                      + loopD.getCondition().split(";")[1]
                      + ");"
                      + System.lineSeparator());
            }
          }
          }
        }
        else if (!loopStarts.contains(lineNumber)) {
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
    File file =
        new File(
            pathForNewFile + "abstract" + loopInfo.getCFA().getFileNames().get(0).getFileName());
    file.getParentFile().mkdirs();
    try (FileWriter fileWriter =
        new FileWriter(file)) {
      String fileContent = content;
      fileWriter.write(fileContent);
  } catch (IOException e) {
    logger.logUserException(
        Level.WARNING,
        e,
        "Something is not working with the file you try to export");
  }
  }

  private CFANode findLastNodeInCondition(LoopData loopData) {
    CFANode temp = loopData.getNodesInCondition().get(0);
    for (CFANode node : loopData.getNodesInCondition()) {
      if (temp.getNodeNumber() > node.getNodeNumber()) {
        temp = node;
      }
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

}
