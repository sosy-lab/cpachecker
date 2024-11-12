// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cmdline.Output;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SequentializationWriter {

  private static final String licenseFilePath = ".idea/copyright/CPAchecker.xml";

  private static final String sequentializationComment =
      "// This sequentialization (transformation of a parallel program into an equivalent \n"
          + "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. \n"
          + "// \n"
          + "// Assertion fails from the function "
          + SeqToken.__SEQUENTIALIZATION_ERROR__
          + " mark faulty sequentializations. \n"
          + "// All other assertion fails are induced by faulty input programs.\n\n";

  private static final String targetDirectory = "output/";

  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  private enum FileExtension {
    I(".i"),
    YML(".yml");

    private final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }
  }

  private final LogManager logManager;

  private final String licenseComment;

  private final Path inputFilePath;

  private final String seqProgramName;

  private final String seqProgramPath;

  private final String seqMetadataPath;

  public SequentializationWriter(LogManager pLogManager, Path pInputFilePath) {
    logManager = pLogManager;
    licenseComment = extractLicenseComment();
    inputFilePath = pInputFilePath;
    String seqName = "mpor_seq__" + getFileNameWithoutExtension(inputFilePath);
    seqProgramName = seqName + FileExtension.I.suffix;
    seqProgramPath = targetDirectory + seqProgramName;
    seqMetadataPath = targetDirectory + seqName + FileExtension.YML.suffix;
  }

  private String getFileNameWithoutExtension(Path pInputFilePath) {
    String fileName = pInputFilePath.getFileName().toString();
    return fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
  }

  /**
   * Replaces the file name and line in {@code __assert_fail("0", "__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  public String buildFinalSequentialization(String pOutputFileName, String pInitProgram) {
    int currentLine = 1;
    StringBuilder rFinal = new StringBuilder();
    for (String line : Splitter.onPattern("\\r?\\n").split(pInitProgram)) {
      if (line.contains(Sequentialization.errorPlaceholder)) {
        CFunctionCallExpression assertFailCall = buildSeqErrorCall(pOutputFileName, currentLine);
        rFinal.append(
            line.replace(Sequentialization.errorPlaceholder, assertFailCall.toASTString()));
      } else {
        rFinal.append(line);
      }
      rFinal.append(SeqSyntax.NEWLINE);
      currentLine++;
    }
    return rFinal.toString();
  }

  public void write(String pSequentialization) {
    String initProgram = licenseComment + "\n" + sequentializationComment + pSequentialization;
    String finalProgram = buildFinalSequentialization(seqProgramName, initProgram);
    try {
      File seqProgramFile = new File(seqProgramPath);
      File parentDir = seqProgramFile.getParentFile();
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
      } else if (!seqProgramFile.createNewFile()) {
        throw Output.fatalError(
            "MPOR FAIL. No sequentialization created, file exists already: %s",
            seqProgramFile.getAbsolutePath());
      } else {
        // write content to the file
        try (Writer writer =
            Files.newBufferedWriter(seqProgramFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(finalProgram);
        }
        File seqMetadataFile = new File(seqMetadataPath);
        try (Writer writer =
            Files.newBufferedWriter(seqMetadataFile.toPath(), StandardCharsets.UTF_8)) {
          writer.write(createMetadata());
        }
        logManager.log(
            Level.INFO,
            () -> "MPOR SUCCESS. Sequentialization created: " + seqProgramFile.getAbsolutePath());
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

  private String createMetadata() {
    return "input_file_path : '"
        + inputFilePath
        + "'\n"
        + "input_file : '"
        + inputFilePath.getFileName()
        + "'\n";
  }

  /**
   * Returns the {@link CFunctionCallExpression} of {@code __assert_fail("0", "{pFileName}",
   * {pLine}, "__SEQUENTIALIZATION_ERROR__")}
   */
  public static CFunctionCallExpression buildSeqErrorCall(String pFileName, int pLine) {
    CStringLiteralExpression seqFileName =
        SeqStringLiteralExpression.buildStringLiteralExpr(SeqUtil.wrapInQuotationMarks(pFileName));
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        SeqVoidType.VOID,
        SeqIdExpression.ASSERT_FAIL,
        ImmutableList.of(
            SeqStringLiteralExpression.STRING_0,
            seqFileName,
            SeqIntegerLiteralExpression.buildIntLiteralExpr(pLine),
            SeqStringLiteralExpression.SEQUENTIALIZATION_ERROR),
        SeqFunctionDeclaration.ASSERT_FAIL);
  }
}
