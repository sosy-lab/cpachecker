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
    ArrayList<String> preUsedVariables = new ArrayList<>();
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
    boolean flaguInt = true;
    boolean flagChar = true;
    boolean flaguChar = true;
    boolean flagShort = true;
    boolean flaguShort = true;
    boolean flagLong = true;
    boolean flaguLong = true;
    boolean flagLongLong = true;
    boolean flagDouble = true;
    for (LoopData lD : loopInfo.getLoopData()) {
      for (String io : lD.getInputsOutputs()) {
        switch (io.split("&")[1]) {
          case "int":
          case "signed int":
            if (flagInt) {
              content +=
                  "extern unsigned int __VERIFIER_nondet_int(void);" + System.lineSeparator();
              content += "extern void __VERIFIER_assume(int cond);" + System.lineSeparator();
              flagInt = false;
            }
            break;
          case "unsigned int":
            if (flaguInt) {
            content += "extern unsigned int __VERIFIER_nondet_uint(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(unsigned int cond);" + System.lineSeparator();
            flagInt = false;
          }
            break;
          case "char":
          case "signed char":
            if (flagChar) {
            content += "extern char __VERIFIER_nondet_char(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(char cond);" + System.lineSeparator();
            flagChar = false;
          }
            break;
          case "unsigned char":
            if (flaguChar) {
              content += "extern char __VERIFIER_nondet_uchar(void);" + System.lineSeparator();
              content +=
                  "extern void __VERIFIER_assume(unsigned char cond);" + System.lineSeparator();
              flagChar = false;
            }
            break;
          case "short":
          case "signed short":
            if (flagShort) {
            content += "extern short __VERIFIER_nondet_short(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(short cond);" + System.lineSeparator();
            flagShort = false;
          }
            break;
          case "unsigned short":
            if (flaguShort) {
              content += "extern short __VERIFIER_nondet_ushort(void);" + System.lineSeparator();
              content +=
                  "extern void __VERIFIER_assume(unsigned short cond);" + System.lineSeparator();
              flagShort = false;
            }
            break;
          case "long":
          case "signed long":
            if (flagLong) {
            content += "extern long __VERIFIER_nondet_long(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(long cond);" + System.lineSeparator();
            flagLong = false;
          }
        case "unsigned long":
          if (flaguLong) {
            content += "extern long __VERIFIER_nondet_ulong(void);" + System.lineSeparator();
            content +=
                "extern void __VERIFIER_assume(unsigned long cond);" + System.lineSeparator();
            flagLong = false;
          }
            break;
          case "long double":
            if (flagLongLong) {
            content +=
                "extern long double __VERIFIER_nondet_long_double(void);" + System.lineSeparator();
            content += "extern void __VERIFIER_assume(long double cond);" + System.lineSeparator();
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
          case "float":
            if (flagDouble) {
              content += "extern double __VERIFIER_nondet_float(void);" + System.lineSeparator();
              content += "extern void __VERIFIER_assume(float cond);" + System.lineSeparator();
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
              if (loopD.getLoopType().equals("while")) {
                line = reader.readLine();
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(
                          s.split(
                              "&")[0])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                content = content + whileCondition(loopD);
                lineNumber++;
              } else if (loopD.getLoopType().equals("for")) {
                line = reader.readLine();
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(s.split("&")[0])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                content = content + forCondition(loopD, preUsedVariables);
                lineNumber++;
              }
              for (String x : loopD.getInputsOutputs()) {
                    switch (x.split("&")[1]) {
                      case "int":
                      case "signed int":
                        if (Integer.parseInt(x.split("&")[2]) >= lineNumber
                            && !preUsedVariables.contains(x)) {
                          content +=
                              x.split("&")[1]
                                  + " "
                                  + x.split("&")[0]
                                  + ";"
                                  + System.lineSeparator();
                          preUsedVariables.add(x);
                        }
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_int();"
                                + System.lineSeparator());
                        break;
                      case "unsigned int":
                        if (Integer.parseInt(x.split("&")[2]) >= lineNumber
                            && !preUsedVariables.contains(x)) {
                          content +=
                              x.split("&")[1]
                                  + " "
                                  + x.split("&")[0]
                                  + ";"
                                  + System.lineSeparator();
                          preUsedVariables.add(x);
                        }
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_uint();" + System.lineSeparator());
                        break;
                      case "char":
                      case "signed char":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_char();" + System.lineSeparator());
                        break;
                      case "unsigned char":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_uchar();"
                                + System.lineSeparator());
                        break;
                      case "short":
                      case "signed short":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_short();" + System.lineSeparator());
                        break;
                      case "unsigned short":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_ushort();"
                                + System.lineSeparator());
                        break;
                      case "long":
                      case "signed long":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_long();" + System.lineSeparator());
                        break;
                      case "unsigned long":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_ulong();"
                                + System.lineSeparator());
                        break;
                      case "long double":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_long_double();"
                                + System.lineSeparator());
                        break;
                      case "double":
                        content += (x.split("&")[0] + "=__VERIFIER_nondet_double();" + System.lineSeparator());
                        break;
                      case "float":
                        content +=
                            (x.split("&")[0]
                                + "=__VERIFIER_nondet_float();"
                                + System.lineSeparator());
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
                  boolean uVFlag = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }

                /**
                 * while (!closed) { line = reader.readLine(); closed = ifCaseClosed(line, closed);
                 * content += line + System.lineSeparator(); lineNumber++; } line =
                 * reader.readLine(); closed = ifCaseClosed(line, closed); content += line +
                 * System.lineSeparator(); lineNumber++; while (!line.contains("}")) { line =
                 * reader.readLine(); closed = ifCaseClosed(line, closed); content += line +
                 * System.lineSeparator(); lineNumber++; }
                 */
                boolean flagTopIf2 = false;
                boolean flagKP2 = false;
                line = reader.readLine();
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                if (line.contains("if")) {
                  flagTopIf2 = true;
                  flagKP2 = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                while (!closed) {
                  if (flagTopIf2) {
                  line = reader.readLine();
                  boolean uVFlag1 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag1 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag1) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                }
                flagTopIf2 = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                if (flagKP2 || flagTopIf2) {
                line = reader.readLine();
                boolean uVFlag2 = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag2 = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag2) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
              }
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                while (!line.contains("}")) {
                  line = reader.readLine();
                  boolean uVFlag3 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag3 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag3) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
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
                  boolean flagIf = false;
                  boolean flagKP = false;
                  line = reader.readLine();
                  boolean uVFlag4 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag4 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag4) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                  if (line.contains("if")) {
                    flagIf = true;
                    flagKP = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                  while (!closed) {
                    if (flagIf) {
                    line = reader.readLine();
                    boolean uVFlag5 = false;
                    for (String s : preUsedVariables) {
                      if (line != null
                          && !s.isEmpty()
                          && line.contains(s.split("&")[1])
                          && line.contains(
                              s.split(
                                  "&")[0])
                          && line.contains(";")) {
                        line = line.split(s.split("&")[1])[1];
                        uVFlag5 = true;
                      }
                      if (line != null
                          && !s.isEmpty()
                          && (line.startsWith(" ")
                              || line.startsWith(
                                  ""))
                          && line.endsWith(s.split("&")[0] + ";")
                          && uVFlag5) {
                        line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                      }
                    }
                  }
                  flagIf = true;
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (flagKP || flagIf) {
                  line = reader.readLine();
                  boolean uVFlag6 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag6 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag6) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!line.contains("}")) {
                    line = reader.readLine();
                    boolean uVFlag7 = false;
                    for (String s : preUsedVariables) {
                      if (line != null
                          && !s.isEmpty()
                          && line.contains(s.split("&")[1])
                          && line.contains(
                              s.split(
                                  "&")[0])
                          && line.contains(";")) {
                        line = line.split(s.split("&")[1])[1];
                        uVFlag7 = true;
                      }
                      if (line != null
                          && !s.isEmpty()
                          && (line.startsWith(" ")
                              || line.startsWith(
                                  ""))
                          && line.endsWith(s.split("&")[0] + ";")
                          && uVFlag7) {
                        line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                      }
                    }
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
                    && lineNumber <= loopD
                        .getLoopEnd()
                        .getEnteringEdge(0)
                        .getFileLocation()
                        .getEndingLineInOrigin()
                ) {
                  line = reader.readLine();
                  boolean uVFlag = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                /**
                 * while (!closed) { lineNumber++; line = reader.readLine(); closed =
                 * ifCaseClosed(line, closed); content += line + System.lineSeparator(); }
                 * lineNumber++; line = reader.readLine(); closed = ifCaseClosed(line, closed);
                 * content += line + System.lineSeparator(); while (!line.contains("}")) {
                 * lineNumber++; line = reader.readLine(); closed = ifCaseClosed(line, closed);
                 * content += line + System.lineSeparator(); }
                 */
                boolean flagIfTop = false;
                boolean flagKP = false;
                line = reader.readLine();
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                if (line.contains("if")) {
                  flagIfTop = true;
                  flagKP = true;
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
              }
              while (!closed) {
                if (flagIfTop) {
                  line = reader.readLine();
                  boolean uVFlag8 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag8 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag8) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                }
                flagIfTop = true;
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
              if (flagKP || flagIfTop) {
              line = reader.readLine();
              boolean uVFlag9 = false;
              for (String s : preUsedVariables) {
                if (line != null
                    && !s.isEmpty()
                    && line.contains(s.split("&")[1])
                    && line.contains(";")) {
                  line = line.split(s.split("&")[1])[1];
                  uVFlag9 = true;
                }
                if (line != null
                    && !s.isEmpty()
                    && (line.startsWith(" ")
                        || line.startsWith(
                            ""))
                    && line.endsWith(s.split("&")[0] + ";")
                    && uVFlag9) {
                  line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                }
              }
              }
              closed = ifCaseClosed(line, closed);
              content += line + System.lineSeparator();
              lineNumber++;

              while (!line.contains("}")) {
                line = reader.readLine();
                boolean uVFlag10 = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag10 = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag10) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
              }
                content +=
                    ("__VERIFIER_assume(!("
                        + loopD.getCondition().split(";")[1]
                        + "));"
                        + System.lineSeparator());

                for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                  boolean flagIf = false;
                  boolean flagBotKP = false;
                  line = reader.readLine();
                  boolean uVFlag11 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag11 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag11) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                  if (line.contains("if")) {
                    flagIf = true;
                    flagBotKP = true;
                    closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                }
                  while (!closed) {
                    if (flagIf) {
                    line = reader.readLine();
                    boolean uVFlag12 = false;
                    for (String s : preUsedVariables) {
                      if (line != null
                          && !s.isEmpty()
                          && line.contains(s.split("&")[1])
                          && line.contains(
                              s.split(
                                  "&")[0])
                          && line.contains(";")) {
                        line = line.split(s.split("&")[1])[1];
                        uVFlag12 = true;
                      }
                      if (line != null
                          && !s.isEmpty()
                          && (line.startsWith(" ")
                              || line.startsWith(
                                  ""))
                          && line.endsWith(s.split("&")[0] + ";")
                          && uVFlag12) {
                        line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                      }
                    }
                  }
                  flagIf = true;
                    closed = ifCaseClosed(line, closed);
                    content += line + System.lineSeparator();
                    lineNumber++;
                  }
                  if (flagBotKP || flagIf) {
                  line = reader.readLine();
                  boolean uVFlag13 = false;
                  for (String s : preUsedVariables) {
                    if (line != null
                        && !s.isEmpty()
                        && line.contains(s.split("&")[1])
                        && line.contains(
                            s.split(
                                "&")[0])
                        && line.contains(";")) {
                      line = line.split(s.split("&")[1])[1];
                      uVFlag13 = true;
                    }
                    if (line != null
                        && !s.isEmpty()
                        && (line.startsWith(" ")
                            || line.startsWith(
                                ""))
                        && line.endsWith(s.split("&")[0] + ";")
                        && uVFlag13) {
                      line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                    }
                  }
                }
                  closed = ifCaseClosed(line, closed);
                  content += line + System.lineSeparator();
                  lineNumber++;
                  while (!line.contains("}")) {
                    line = reader.readLine();
                    boolean uVFlag14 = false;
                    for (String s : preUsedVariables) {
                      if (line != null
                          && !s.isEmpty()
                          && line.contains(s.split("&")[1])
                          && line.contains(s.split("&")[0])
                          && line.contains(";")) {
                        line = line.split(s.split("&")[1])[1];
                        uVFlag14 = true;
                      }
                      if (line != null
                          && !s.isEmpty()
                          && (line.startsWith(" ")
                              || line.startsWith(
                                  ""))
                          && line.endsWith(s.split("&")[0] + ";")
                          && uVFlag14) {
                        line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                      }
                    }
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
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
                closed = ifCaseClosed(line, closed);
                content += line + System.lineSeparator();
                lineNumber++;
                // lineNumber++;
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
                boolean uVFlag = false;
                for (String s : preUsedVariables) {
                  if (line != null
                      && !s.isEmpty()
                      && line.contains(s.split("&")[1])
                      && line.contains(";")) {
                    line = line.split(s.split("&")[1])[1];
                    uVFlag = true;
                  }
                  if (line != null
                      && !s.isEmpty()
                      && (line.startsWith(" ")
                          || line.startsWith(
                              ""))
                      && line.endsWith(s.split("&")[0] + ";")
                      && uVFlag) {
                    line = s.split("&")[0] + "=" + s.split("&")[0] + ";";
                  }
                }
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

  private String forCondition(LoopData loopD, ArrayList<String> preUsedVariables) {
    boolean flag = true;
    String variable = "";
    for (String x : preUsedVariables) {
      if (loopD.getCondition().split(";")[0].contains(x.split("&")[0])) {
        flag = false;
        variable = x.split("&")[1];
      }
    }
    if (flag) {
    return loopD.getCondition().split(";")[0]
        + ";"
        + System.lineSeparator()
        + "for(int cpachecker_i=0; cpachecker_i <"
        + min(loopD.getAmountOfPaths(), loopD.getOutputs().size())
        + "&&("
        + loopD.getCondition().split(";")[1]
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  } else {
    String cond = loopD.getCondition().split(";")[0];

    if (cond.contains(variable)) {
      cond = cond.split(variable)[1];
    }

    return cond
        + ";"
        + System.lineSeparator()
        + "for(int cpachecker_i=0; cpachecker_i <"
        + min(loopD.getAmountOfPaths(), loopD.getOutputs().size())
        + "&&("
        + loopD.getCondition().split(";")[1]
        + "); cpachecker_i++"
        + "){"
        + System.lineSeparator();
  }
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
      if (temp.contains("if")) {
        String temp2 = line.split("if")[1];
        if (temp2.contains("}")) {
          ifCaseC = true;
        }
      } else if (line.contains("else")) {
        String temp2 = line.split("else")[1];
        if (temp2.contains("}")) {
          ifCaseC = true;
        }
      }
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
