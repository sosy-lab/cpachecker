// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopAbstraction;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.loopInformation.LoopData;
import org.sosy_lab.cpachecker.util.loopInformation.LoopInformation;
import org.sosy_lab.cpachecker.util.loopInformation.LoopType;
import org.sosy_lab.cpachecker.util.loopInformation.LoopVariables;

/**
 * This class takes a file and abstracts all the loops that have the value canBeAccelerated to make
 * the program verifiable for specific cpa's
 */
@Options(prefix = "loopacc")
public class LoopAbstraction {

  private List<LoopVariables> preUsedVariables = new ArrayList<>();
  private String content = "";
  private int lineNumber = 1;
  private String line = "";
  private boolean closed = true;

  private final Timer totalTime;
  private TimeSpan timeToAbstract;

  @Option(
      secure = true,
      description = "Whether to export the source code of the abstracted program.")
  private boolean exportAbstractedFile = true;

  @Option(
      secure = true,
      name = "abstractedFile",
      description = "Export the source code of the abstracted program to the given file name.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path abstractedFileName = Paths.get("abstractedProgram.c");

  private String abstractedSource;

  private boolean openIf = false;
  private int ifWithoutBracket = 0;
  private boolean hasBreak = false;



  public LoopAbstraction(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    totalTime = new Timer();
  }

  /**
   * This method changes all the necessary lines of codes needed to abstract the loops and saves it
   * in a new file
   *
   * @param loopInfo Information about all the loops in the file
   * @param logger logger that logs all the exceptions
   * @param abstractionLevel level of abstraction, "naive" and "advanced" possible, naive is a
   *     bigger over-approximation than advanced
   * @param automate the file will be overwritten if this is true
   * @param onlyAccL if this is true only the loops that can be accelerated will be abstracted
   */
  public void changeFileToAbstractFile(
      LoopInformation loopInfo,
      LogManager logger,
      AbstractionLevel abstractionLevel,
      boolean automate,
      boolean onlyAccL) {
    totalTime.start();
    List<LoopData> outerLoopTemp = new ArrayList<>();
    List<Integer> loopStarts = new ArrayList<>();
    for (LoopData loopData : loopInfo.getLoopData()) {
      if (loopData.getLoopType().equals(LoopType.WHILE)) {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin());
      } else if (loopData.getLoopType().equals(LoopType.FOR)) {
        loopStarts.add(
            loopData.getLoopStart().getEnteringEdge(0).getFileLocation().getStartingLineInOrigin());
      }
    }

    String fileLocation = loopInfo.getCFA().getFileNames().get(0).toString();

    content =
        "extern void __VERIFIER_error() __attribute__ ((__noreturn__));" + System.lineSeparator();

    content += "extern signed int __VERIFIER_nondet_int(void);" + System.lineSeparator();
    content += "extern char __VERIFIER_nondet_char(void);" + System.lineSeparator();
    content += "extern short __VERIFIER_nondet_short(void);" + System.lineSeparator();
    content += "extern long __VERIFIER_nondet_long(void);" + System.lineSeparator();
    content += "extern long double __VERIFIER_nondet_long_double(void);" + System.lineSeparator();
    content += "extern double __VERIFIER_nondet_double(void);" + System.lineSeparator();
    content += "extern float __VERIFIER_nondet_float(void);" + System.lineSeparator();
    content += "extern void __VERIFIER_assume(signed int cond);" + System.lineSeparator();

    try (Reader freader = Files.newBufferedReader(Paths.get(fileLocation))) {
      try (BufferedReader reader = new BufferedReader(freader)) {

        boolean accFlag = true;

        while (line != null) {
          if (loopStarts.contains(lineNumber) && accFlag) {
            for (LoopData loopD : loopInfo.getLoopData()) {
              if ((loopD
                              .getLoopStart()
                              .getEnteringEdge(0)
                              .getFileLocation()
                              .getStartingLineInOrigin()
                          == lineNumber
                      && loopD.getCanBeAccelerated()
                      && onlyAccL)
                  || (loopD
                              .getLoopStart()
                              .getEnteringEdge(0)
                              .getFileLocation()
                              .getStartingLineInOrigin()
                          == lineNumber
                      && !onlyAccL)) {

                CFANode endNodeCondition = findLastNodeInCondition(loopD);
                if (loopD.getLoopType().equals(LoopType.WHILE)) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed();
                  content = content + whileCondition(loopD, abstractionLevel);
                  lineNumber++;
                } else if (loopD.getLoopType().equals(LoopType.FOR)) {
                  line = reader.readLine();
                  line = variablesAlreadyUsed();
                  content = content + forCondition(loopD, abstractionLevel);
                  lineNumber++;
                }
                content += assignNonDeterministicValuesToVariables(loopD, abstractionLevel);

                if (!loopD.getIsOuterLoop()) {
                  if (loopD.getLoopType().equals(LoopType.WHILE)) {
                    content +=
                        ("__VERIFIER_assume(" + loopD.getCondition() + ");")
                            + System.lineSeparator();
                    while (lineNumber
                            >= endNodeCondition
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getEndingLineInOrigin()
                        && line != null
                        && lineNumber
                            <= loopD
                                .getLoopEnd()
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getEndingLineInOrigin()) {
                      readAndWorkOnLine(reader);
                    }
                    checkForEnd(reader);
                    while (line != null && !line.contains("}")) {
                      readAndWorkOnLine(reader);
                    }

                    content += assumeEnd(loopD);

                    for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                      checkForEnd(reader);
                      while (line != null && !line.contains("}")) {
                        readAndWorkOnLine(reader);
                      }
                      content += assumeEnd(loopD);
                    }
                    outerLoopTemp.clear();
                  } else if (loopD.getLoopType().equals(LoopType.FOR)) {
                    content +=
                        ("__VERIFIER_assume("
                                + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
                                + ");")
                            + System.lineSeparator();
                    while (lineNumber
                            >= endNodeCondition
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getStartingLineInOrigin()
                        && line != null
                        && lineNumber
                            <= loopD
                                .getLoopEnd()
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getEndingLineInOrigin()) {
                      readAndWorkOnLine(reader);
                    }
                    checkForEnd(reader);
                    while (line != null && !line.contains("}")) {
                      readAndWorkOnLine(reader);
                    }
                    content += assumeEnd(loopD);

                    for (int i = outerLoopTemp.size() - 1; i >= 0; i--) {
                      checkForEnd(reader);
                      while (line != null && !line.contains("}")) {
                        readAndWorkOnLine(reader);
                      }
                      content += assumeEnd(loopD);
                    }
                    outerLoopTemp.clear();
                  }
                } else if (loopD.getIsOuterLoop()) {
                  if (loopD.getLoopType().equals(LoopType.WHILE)) {
                    content +=
                        ("__VERIFIER_assume(" + loopD.getCondition() + ");")
                            + System.lineSeparator();
                  } else if (loopD.getLoopType().equals(LoopType.FOR)) {
                    content +=
                        ("__VERIFIER_assume("
                                + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
                                + ");")
                            + System.lineSeparator();
                  }
                  outerLoopTemp.add(loopD);
                  if (loopD.getLoopType().equals(LoopType.FOR)) {
                    while (lineNumber
                            >= endNodeCondition
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getEndingLineInOrigin()
                        && line != null
                        && (lineNumber
                            < loopD
                                .getInnerLoop()
                                .getIncomingEdges()
                                .asList()
                                .get(0)
                                .getFileLocation()
                                .getStartingLineInOrigin())) {
                      readAndWorkOnLine(reader);
                    }
                  } else if (loopD.getLoopType().equals(LoopType.WHILE)) {
                    while (lineNumber
                            >= endNodeCondition
                                .getEnteringEdge(0)
                                .getFileLocation()
                                .getEndingLineInOrigin()
                        && line != null
                        && (lineNumber
                            < loopD
                                .getInnerLoop()
                                .getIncomingEdges()
                                .asList()
                                .get(0)
                                .getFileLocation()
                                .getStartingLineInOrigin())) {
                      readAndWorkOnLine(reader);
                    }
                  }
                }
              } else {
                accFlag = false;
              }
            }
          } else if (!loopStarts.contains(lineNumber) || !accFlag) {
            accFlag = true;
            line = reader.readLine();
            if (line != null) {
              content += line + System.lineSeparator();
              lineNumber++;
            }
          }
        }
      }
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "LoopAbstraction: Something is not working with the file you try to import");
    }
    abstractedSource = content;
    if (exportAbstractedFile) {
      writeAbstractedFile(logger);
    }
    totalTime.stop();
    timeToAbstract = totalTime.getLengthOfLastInterval();
  }

  /**
   * Looks for the end of the loop
   *
   * @param reader Buffered reader that reads the file
   * @throws IOException IOException that gets thrown if there is a problem with the file
   */
  private void checkForEnd(BufferedReader reader) throws IOException {
    setLine(reader.readLine());
    setLine(variablesAlreadyUsed());
    boolean flagEnd =
        getClosed()
            && getLine().contains("}")
            && (!getLine().contains("if") || !getLine().contains("else"));
    while (getLine() != null && !flagEnd) {
      setClosed(ifCaseClosed());
      if (!getClosed()) {
        addToContent(getLine() + System.lineSeparator());
        lineNumber++;
      } else {
        flagEnd = true;
        addToContent(getLine() + System.lineSeparator());
        lineNumber++;
      }
      setLine(reader.readLine());
      setLine(variablesAlreadyUsed());
    }
    setClosed(ifCaseClosed());
    addToContent(getLine() + System.lineSeparator());
    lineNumber++;
  }

  /**
   * Reads the line and looks for clues that are needed in this file (e.g. closed) . Adds content to
   * string content after that.
   *
   * @param reader Buffered reader that reads the file.
   * @throws IOException Exception that gets thrown if there is a problem with the file-
   */
  private void readAndWorkOnLine(BufferedReader reader) throws IOException {
    setLine(reader.readLine());
    setLine(variablesAlreadyUsed());
    setClosed(ifCaseClosed());
    addToContent(getLine() + System.lineSeparator());
    lineNumber++;
  }

  /**
   * Adds the final line to a abstracted loop (assume(!condition))
   *
   * @param loopD LoopData, that contains all the information of the loop
   * @return returns a string that gets added to the content of the abstracted file
   */
  private String assumeEnd(LoopData loopD) {
    String ass = "";
    if (!loopD.getOnlyRandomCondition()) {
      if (!(loopD.getCondition().equals("1") && hasBreak)) {
        if (loopD.getLoopType().equals(LoopType.FOR)) {
          ass +=
              ("__VERIFIER_assume(!("
                  + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
                  + "));"
                  + System.lineSeparator());
        } else if (loopD.getLoopType().equals(LoopType.WHILE)) {
          ass += ("__VERIFIER_assume(!(" + loopD.getCondition() + "));" + System.lineSeparator());
        }
      }
    }
    return ass;
  }

  /**
   * Method that assigns variables non-deterministic values
   *
   * @param loopD all necessary information about this loop
   * @param abstractionLevel in a naive abstraction level all of the outputs will get
   *     non-deterministic values, in the advanced case only the IO-Variables will get
   *     non-deterministic
   * @return returns a string that get's added to the program-string with non-deterministic values
   *     assigned
   */
  private String assignNonDeterministicValuesToVariables(
      LoopData loopD, AbstractionLevel abstractionLevel) {
    String tmp = "";
    List<LoopVariables> variables = null;
    if (abstractionLevel.equals(AbstractionLevel.NAIVE)) {
      variables = loopD.getOutputs();
    } else {
      variables = loopD.getInputsOutputs();
    }
    for (LoopVariables x : variables) {
      if (x.getIsArray()) {
        String tempString = x.getVariableTypeAsString();
        if (x.getInitializationLine() >= lineNumber && !getPreUsedVariables().contains(x)) {
          tmp +=
              tempString
                  + " "
                  + x.getVariableNameAsString()
                  + "["
                  + x.getArrayLength()
                  + "]"
                  + ";"
                  + System.lineSeparator();
          addToPreUsedVariables(x);
        }
        tmp +=
            "for(int __cpachecker_tmp_i = 0; __cpachecker_tmp_i < "
                + x.getArrayLength()
                + "; __cpachecker_tmp_i++){"
                + x.getVariableNameAsString()
                + "[__cpachecker_tmp_i]";
        switch (x.getVariableTypeAsString()) {
          case "int":
          case "signed int":
            tmp += "=__VERIFIER_nondet_int();}" + System.lineSeparator();
            break;
          case "unsigned":
          case "unsigned int":
            tmp += "=__VERIFIER_nondet_uint();}" + System.lineSeparator();
            break;
          case "char":
          case "signed char":
            tmp += "=__VERIFIER_nondet_char();}" + System.lineSeparator();
            break;
          case "unsigned char":
            tmp += "=__VERIFIER_nondet_uchar();}" + System.lineSeparator();
            break;
          case "short":
          case "signed short":
            tmp += "=__VERIFIER_nondet_short();}" + System.lineSeparator();
            break;
          case "unsigned short":
            tmp += "=__VERIFIER_nondet_ushort();}" + System.lineSeparator();
            break;
          case "long":
          case "signed long":
            tmp += "=__VERIFIER_nondet_long();}" + System.lineSeparator();
            break;
          case "unsigned long":
            tmp += "=__VERIFIER_nondet_ulong();}" + System.lineSeparator();
            break;
          case "long double":
            tmp += "=__VERIFIER_nondet_long_double();}" + System.lineSeparator();
            break;
          case "double":
            tmp += "=__VERIFIER_nondet_double();}" + System.lineSeparator();
            break;
          case "float":
            tmp += "=__VERIFIER_nondet_float();}" + System.lineSeparator();
            break;
          default:
            break; // does nothing
        }
      } else {
        if (x.getInitializationLine() >= lineNumber && !preUsedVariables.contains(x)) {
          tmp +=
              x.getVariableTypeAsString()
                  + " "
                  + x.getVariableNameAsString()
                  + ";"
                  + System.lineSeparator();
          preUsedVariables.add(x);
        }
        tmp += x.getVariableNameAsString();
        switch (x.getVariableTypeAsString()) {
          case "int":
          case "signed int":
            tmp += "=__VERIFIER_nondet_int();" + System.lineSeparator();
            break;
          case "unsigned":
          case "unsigned int":
            tmp += "=__VERIFIER_nondet_uint();" + System.lineSeparator();
            break;
          case "char":
          case "signed char":
            tmp += "=__VERIFIER_nondet_char();" + System.lineSeparator();
            break;
          case "unsigned char":
            tmp += "=__VERIFIER_nondet_uchar();" + System.lineSeparator();
            break;
          case "short":
          case "signed short":
            tmp += "=__VERIFIER_nondet_short();" + System.lineSeparator();
            break;
          case "unsigned short":
            tmp += "=__VERIFIER_nondet_ushort();" + System.lineSeparator();
            break;
          case "long":
          case "signed long":
            tmp += "=__VERIFIER_nondet_long();" + System.lineSeparator();
            break;
          case "unsigned long":
            tmp += "=__VERIFIER_nondet_ulong();" + System.lineSeparator();
            break;
          case "long double":
            tmp += "=__VERIFIER_nondet_long_double();" + System.lineSeparator();
            break;
          case "double":
            tmp += "=__VERIFIER_nondet_double();" + System.lineSeparator();
            break;
          case "float":
            tmp += "=__VERIFIER_nondet_float();" + System.lineSeparator();
            break;
          default:
            break; // does nothing
        }
      }
    }
    return tmp;
  }

  /**
   * checks if the variable in the line already got used and if that is the case if it would
   * normally get initialized in this line which would get changed in this method
   *
   * @return returns a line of the program that can be added back to the program-string
   */
  private String variablesAlreadyUsed() {
    boolean uVFlag = false;
    String thisLine = getLine();
    for (LoopVariables s : getPreUsedVariables()) {
      if (s.getIsArray()) {
        if (thisLine != null
            && thisLine.contains(s.getVariableNameAsString() + "[")
            && thisLine.contains(s.getVariableTypeAsString())) {
          String tmpArray = Iterables.get(Splitter.on('=').split(thisLine), 1);
          thisLine =
              s.getVariableTypeAsString()
                  + " __cpachecker_tmp_array["
                  + s.getArrayLength()
                  + "] = "
                  + tmpArray;
          thisLine = thisLine + " " + s.getVariableNameAsString() + " = __cpachecker_tmp_array;";
        }
      } else {
        if (thisLine != null
            && thisLine.contains(s.getVariableTypeAsString())
            && thisLine.contains(";")
            && thisLine.contains(s.getVariableNameAsString())) {
          thisLine = Iterables.get(Splitter.on(s.getVariableTypeAsString()).split(thisLine), 1);
          uVFlag = true;
        }
        if (thisLine != null
            && (thisLine.startsWith(" ") || thisLine.startsWith(""))
            && thisLine.endsWith(s.getVariableNameAsString() + ";")
            && uVFlag) {
          thisLine = s.getVariableNameAsString() + "=" + s.getVariableNameAsString() + ";";
        }
      }
    }
    return thisLine;
  }

  /**
   * Method to abstract the while-loop-header
   *
   * @param loopD information about this specific loop
   * @param abstractionLevel naive or advanced will get different results
   * @return string that is the new abstracted header of the while loop
   */
  private String whileCondition(LoopData loopD, AbstractionLevel abstractionLevel) {
    if (abstractionLevel.equals(AbstractionLevel.NAIVE)) {
      return "if(" + loopD.getCondition() + "){" + System.lineSeparator();
    } else {
      return "for(int cpachecker_i=0; cpachecker_i <"
          + min(loopD.getAmountOfPaths(), loopD.getNumberOutputs())
          + "&&("
          + loopD.getCondition()
          + "); cpachecker_i++"
          + "){"
          + System.lineSeparator();
    }
  }

  /**
   * Method to abstract the for-loop-header
   *
   * @param loopD Information about this loop
   * @param abstractionLevel naive or abstracted, naive is a bigger over-approximation
   * @return string that is the new abstracted header of the for loop
   */
  private String forCondition(LoopData loopD, AbstractionLevel abstractionLevel) {
    boolean flag = true;
    String variable = "";
    for (LoopVariables x : getPreUsedVariables()) {
      if (Iterables.get(Splitter.on(';').split(loopD.getCondition()), 0)
          .contains(x.getVariableNameAsString())) {
        flag = false;
        variable = x.getVariableTypeAsString();
      }
    }
    if (abstractionLevel.equals(AbstractionLevel.NAIVE)) {
      if (flag) {
        return Iterables.get(Splitter.on(';').split(loopD.getCondition()), 0)
            + ";"
            + System.lineSeparator()
            + "if("
            + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
            + "){"
            + System.lineSeparator();
      } else {
        String cond = Iterables.get(Splitter.on(';').split(loopD.getCondition()), 0);

        if (cond.contains(variable)) {
          cond = Iterables.get(Splitter.on(variable).split(loopD.getCondition()), 1);
        }

        return cond
            + ";"
            + System.lineSeparator()
            + "if("
            + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
            + "){"
            + System.lineSeparator();
      }
    } else {
      if (flag) {
        return Iterables.get(Splitter.on(';').split(loopD.getCondition()), 0)
            + ";"
            + System.lineSeparator()
            + "for(int cpachecker_i=0; cpachecker_i <"
            + min(loopD.getAmountOfPaths(), loopD.getNumberOutputs())
            + "&&("
            + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
            + "); cpachecker_i++"
            + "){"
            + System.lineSeparator();
      } else {
        String cond = Iterables.get(Splitter.on(';').split(loopD.getCondition()), 0);

        if (cond.contains(variable)) {
          cond = Iterables.get(Splitter.on(variable).split(cond), 1);
        }

        return cond
            + ";"
            + System.lineSeparator()
            + "for(int cpachecker_i=0; cpachecker_i <"
            + min(loopD.getAmountOfPaths(), loopD.getNumberOutputs())
            + "&&("
            + Iterables.get(Splitter.on(';').split(loopD.getCondition()), 1)
            + "); cpachecker_i++"
            + "){"
            + System.lineSeparator();
      }
    }
  }

  /**
   * checks if there is a if-case that has to be closed, is used to get the __VERIFIER_assume(!) at
   * the right position, it would be in the loop otherwise
   *
   * @return boolean that shows if there is still an open if case or not
   */
  private boolean ifCaseClosed() {

    if (getLine().contains("break;")) {
      hasBreak = true;
    }

    boolean ifCaseC = getClosed();

    String temp = Iterables.get(Splitter.on('(').split(getLine()), 0);
    if (!ifCaseC && getLine().contains("}")) {
      ifCaseC = true;
    }
    if (temp.contains("{") && openIf) {
      openIf = false;
      ifCaseC = false;
    }
    if (ifWithoutBracket > 0) {
      ifWithoutBracket = 0;
      openIf = false;
    }
    if (temp.contains("if") || getLine().contains("else")) {
      ifCaseC = false;
      if (temp.contains("if")) {
        String temp2 = Iterables.get(Splitter.on("if").split(getLine()), 1);
        if (temp2.contains("}")) {
          ifCaseC = true;
        } else if (!temp.contains("{")) {
          ifCaseC = true;
          openIf = true;
          ifWithoutBracket += 1;
        }
      } else if (getLine().contains("else")) {
        String temp2 = Iterables.get(Splitter.on("else").split(getLine()), 1);
        if (temp2.contains("}") || !temp2.contains("{")) {
          ifCaseC = true;
        }
      }
    }
    return ifCaseC;
  }

  /**
   * Write changed file content in a new file
   *
   * @param logger logger to log exceptions
   */
  private void writeAbstractedFile(LogManager logger) {
    if (abstractedFileName == null) {
      return;
    }
    try (Writer fileWriter = Files.newBufferedWriter(abstractedFileName, Charset.defaultCharset())) {
      String fileContent = getContent();
      fileWriter.write(fileContent);
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Something is not working with the file you try to import");
    }
  }

  /**
   * Looks for the last node, that is part of the condition.
   *
   * @param loopData loopData object that contains all the informations of the loop
   * @return returns the last CFANode, that is part of the condition
   */
  private CFANode findLastNodeInCondition(LoopData loopData) {
    CFANode temp = null;
    if (!loopData.getNodesInCondition().isEmpty()) {
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

  private int min(int x, int y) {
    if (x > y) {
      return y;
    } else {
      return x;
    }
  }

  public TimeSpan getTimeToAbstract() {
    return timeToAbstract;
  }

  public String getAbstractedSource() {
    return abstractedSource;
  }

  private void setLine(String lineContent) {
    line = lineContent;
  }

  private void setClosed(boolean newClosed) {
    closed = newClosed;
  }

  private void addToContent(String newContent) {
    content += newContent;
  }

  private boolean getClosed() {
    return closed;
  }

  private String getLine() {
    return line;
  }

  private String getContent() {
    return content;
  }

  private List<LoopVariables> getPreUsedVariables() {
    return preUsedVariables;
  }

  private void addToPreUsedVariables(LoopVariables variable) {
    preUsedVariables.add(variable);
  }
}
