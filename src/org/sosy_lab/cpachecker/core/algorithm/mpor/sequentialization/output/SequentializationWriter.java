// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.time.ZoneId;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cmdline.Output;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORStatics;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SequentializationWriter {

  private static final String licenseFilePath = ".idea/copyright/CPAchecker.xml";

  private static final String targetDirectory = "output/";

  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  private final LogManager logManager;

  private final String licenseComment;

  private final String sequentializationComment;

  private final File outputFile;

  public final String outputFileName;

  public SequentializationWriter(LogManager pLogManager, Path pInputFilePath) {
    logManager = pLogManager;
    licenseComment = extractLicenseComment();
    sequentializationComment = createSequentializationComment(pInputFilePath.toString());
    outputFileName = "mpor_seq__" + processInputFileName(pInputFilePath);
    String outputFilePath = targetDirectory + outputFileName;
    outputFile = new File(outputFilePath);
  }

  private String processInputFileName(Path pInputFilePath) {
    String inputFileName = pInputFilePath.getFileName().toString();
    return inputFileName.endsWith(".i") ? inputFileName : inputFileName + ".i";
  }

  // TODO also create .yml file for each sequentialized program that contains metadata
  //  e.g. input program, num_treads, etc.

  public void write(String pSequentialization) {
    String initProgram = licenseComment + "\n" + sequentializationComment + pSequentialization;
    String finalProgram = createFinalProgram(initProgram);
    try {
      File parentDir = outputFile.getParentFile();
      // ensure the target directory exists
      if (!parentDir.exists()) {
        if (parentDir.mkdirs()) {
          logManager.log(Level.INFO, "Directory created: " + targetDirectory);
        } else {
          throw Output.fatalError(
              "MPOR FAIL. No sequentialization created, could not create target directory: %s",
              targetDirectory);
        }
        // ensure the file does not exist already (no overwriting)
      } else if (!outputFile.createNewFile()) {
        throw Output.fatalError(
            "MPOR FAIL. No sequentialization created, file exists already: %s",
            outputFile.getAbsolutePath());
      } else {
        // write content to the file
        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(finalProgram);
        }
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + outputFile.getAbsolutePath());
      }
    } catch (IOException e) {
      throw Output.fatalError(
          "MPOR FAIL. An IO error occurred while writing the outputProgram: %s", e.getMessage());
    }
  }

  private String extractLicenseComment() {
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(SequentializationWriter.licenseFilePath);
      document.getDocumentElement().normalize();

      // get the <copyright> element
      NodeList nodeList = document.getElementsByTagName("copyright");
      if (nodeList.getLength() > 0) {
        Element copyrightElement = (Element) nodeList.item(0);

        // get the <option> element with name="notice"
        NodeList optionList = copyrightElement.getElementsByTagName("option");
        for (int i = 0; i < optionList.getLength(); i++) {
          Element optionElement = (Element) optionList.item(i);
          String optionName = optionElement.getAttribute("name");

          if ("notice".equals(optionName)) {
            String license = optionElement.getAttribute("value");
            // add current year
            String currentYear = String.valueOf(Year.now(ZoneId.systemDefault()).getValue());
            license = license.replace("&#36;today.year", currentYear);
            // add comment
            Iterable<String> lines = Splitter.on('\n').split(license);
            StringBuilder commentedLicense = new StringBuilder();
            for (String line : lines) {
              commentedLicense.append("// ").append(line).append("\n");
            }
            return commentedLicense.toString();
          }
        }
      } else {
        throw Output.fatalError("MPOR FAIL. No <copyright> element found.");
      }
    } catch (Exception e) {
      throw Output.fatalError(
          "MPOR FAIL. An exception occurred while extracting the license: %s", e.getMessage());
    }
    throw Output.fatalError(
        "MPOR FAIL. No sequentialization created, could not extract the license from %s",
        licenseFilePath);
  }

  private String createSequentializationComment(String pInputFilePath) {
    return "// This sequentialization (transformation of a parallel program into an equivalent \n"
        + "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. \n"
        + "// \n"
        + "// Assertion fails from the function "
        + SeqToken.__SEQUENTIALIZATION_ERROR__
        + " mark faulty sequentializations. \n"
        + "// All other assertion fails are induced by faulty input programs. \n"
        + "// \n"
        + "// Input program file: "
        + pInputFilePath
        + "\n\n";
  }

  /**
   * Replaces all {@code -1} in {@code __assert_fail("0", "{output_file_name}", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with the actual line of code.
   */
  private String createFinalProgram(String pInitProgram) {
    int currentLine = 1;
    StringBuilder rFinal = new StringBuilder();
    for (String line : Splitter.onPattern("\\r?\\n").split(pInitProgram)) {
      if (line.contains(MPORStatics.seqError())) {
        CFunctionCallExpression assertFailCall =
            MPORAlgorithm.getSeqErrorCall(outputFileName, currentLine);
        rFinal.append(line.replace(MPORStatics.seqError(), assertFailCall.toASTString()));
      } else {
        rFinal.append(line);
      }
      rFinal.append(SeqSyntax.NEWLINE);
      currentLine++;
    }
    return rFinal.toString();
  }
}