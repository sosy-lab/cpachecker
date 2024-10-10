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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SequentializationWriter {

  private static final String licenseFilePath = ".idea/copyright/CPAchecker.xml";

  private static final String targetDirectory = "test/programs/mpor_seq/";

  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  private final LogManager logManager;

  private final String licenseComment;

  private final String sequentializationComment;

  private final File outputFile;

  public SequentializationWriter(LogManager pLogManager, Path pInputFilePath) {
    logManager = pLogManager;
    licenseComment = extractLicenseComment();
    sequentializationComment = createSequentializationComment(pInputFilePath.toString());
    String inputFileName = pInputFilePath.getFileName().toString();
    String outputFilePath = targetDirectory + "mpor_seq__" + inputFileName;
    outputFile = new File(outputFilePath);
  }

  public void write(String pSequentialization) {
    String outputProgram = licenseComment + "\n" + sequentializationComment + pSequentialization;
    try {
      File parentDir = outputFile.getParentFile();
      // ensure the target directory exists
      if (!parentDir.exists()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "MPOR FAIL. No sequentialization created, make sure the target directory exists in"
                    + " CPAchecker: "
                    + targetDirectory);

        // ensure the file does not exist already (no overwriting)
      } else if (!outputFile.createNewFile()) {
        logManager.log(
            Level.SEVERE,
            () ->
                "MPOR FAIL. No sequentialization created, file exists already: "
                    + outputFile.getAbsolutePath());
      } else {
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + outputFile.getAbsolutePath());
        // write content to the file
        try (Writer writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(outputProgram);
        }
      }
    } catch (IOException e) {
      logManager.log(
          Level.SEVERE,
          () -> "An IO error occurred while writing the outputProgram: " + e.getMessage());
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
        logManager.log(Level.SEVERE, () -> "No <copyright> element found.");
      }
    } catch (Exception e) {
      logManager.log(
          Level.SEVERE,
          () -> "An exception occurred while extracting the license: " + e.getMessage());
    }
    throw new AssertionError(
        "MPOR FAIL. No sequentialization created, could not extract the license from "
            + licenseFilePath);
  }

  private String createSequentializationComment(String pInputFilePath) {
    return "// This sequentialization (transformation of a parallel program into an equivalent \n"
        + "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. \n"
        + "// Input file: "
        + pInputFilePath
        + "\n\n";
  }
}
