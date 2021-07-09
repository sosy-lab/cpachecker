/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.base.Preconditions;
import com.google.common.xml.XmlEscapers;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

public class TestSuiteWriter {

  private CFA cfa;
  private LogManager logger;
  private boolean useTestCompOutput;
  private String outputFolder;
  private Set<TestCase> writtenTestCases;
  private String spec;
  private String producer;
  private static TestSuiteWriter singleton;
  private boolean addElapsedTime;
  private boolean compress;

  public static TestSuiteWriter getSingleton(
      CFA pCfa,
      LogManager pLogger,
      boolean pUseTestCompOutput,
      String pOutputFolder,
      String pSpec,
      String pProducer,
      boolean pAddElapsedTime,
      boolean pCompress) {
    if (singleton == null) {
      singleton =
          new TestSuiteWriter(
              pCfa,
              pLogger,
              pUseTestCompOutput,
              pOutputFolder,
              pSpec,
              pProducer,
              pAddElapsedTime,
              pCompress);
    } else {
      assert singleton.cfa.equals(pCfa);
      assert singleton.logger.equals(pLogger);
      assert singleton.useTestCompOutput == pUseTestCompOutput;
      assert singleton.outputFolder.equals(pOutputFolder);
      assert singleton.spec.equals(pSpec);
      assert singleton.producer.equals(pProducer);
      assert singleton.addElapsedTime == pAddElapsedTime;
      assert singleton.compress == pCompress;
    }
    return singleton;
  }

  private TestSuiteWriter(
      CFA pCfa,
      LogManager pLogger,
      boolean pUseTestCompOutput,
      String pOutputFolder,
      String pSpec,
      String pProducer,
      boolean pAddElapsedTime,
      boolean pCompress) {
    cfa = pCfa;
    logger = pLogger;
    useTestCompOutput = pUseTestCompOutput;
    outputFolder = pOutputFolder;
    writtenTestCases = new HashSet<>();
    spec = pSpec;
    producer = pProducer;
    addElapsedTime = pAddElapsedTime;
    compress = pCompress;
    initTestSuiteFolder();
  }

  private FileSystem openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    Preconditions.checkNotNull(outputFolder);
    // create parent directories if do not exist
    Path parent = Paths.get(outputFolder).getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    String uriString = "jar:" + Paths.get(outputFolder + ".zip").toUri().toString();
    return FileSystems.newFileSystem(URI.create(uriString), env, null);
  }

  private void writeTestSuiteFile(String file, String content) {
    if (compress) {
      // locate file system by using the syntax
      // defined in java.net.JarURLConnection
      try (FileSystem zipFS = openZipFS()) {
        Path pathInZipfile = zipFS.getPath(file);
        Files.write(pathInZipfile, content.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not write test-case!");
      }
    } else {
    try {
        Files.write(Paths.get(outputFolder + file), content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Could not write test-case!");
    }
  }

  }

  private void writeMetaData() throws IOException {
    StringBuilder builder = new StringBuilder();
    String programFile = "";
    if (cfa.getFileNames().size() == 1) {
      programFile = cfa.getFileNames().get(0).toString();

    } else {
      for (int i = 0; i < cfa.getFileNames().size(); i++) {
        programFile += cfa.getFileNames().get(i).toString() + ";";
      }
      programFile = programFile.substring(0, programFile.length() - 1);
    }

    builder.append(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
            + "<!DOCTYPE test-metadata SYSTEM \"https://gitlab.com/sosy-lab/software/test-format/blob/master/test-metadata.dtd\">\n");
    builder.append("<test-metadata>\n");

    builder.append("\t<sourcecodelang>");
    builder.append(cfa.getLanguage().toString());
    builder.append("</sourcecodelang>\n");

    builder.append("\t<producer>");
    builder.append(XmlEscapers.xmlContentEscaper().escape(producer));
    builder.append("</producer>\n");

    if (spec != null) {
      builder.append("\t<specification>");
      builder.append(spec);
      builder.append("</specification>\n");
    } else {
      builder.append("\t<specification/>\n");
    }

    builder.append("\t<programfile>");
    builder.append(programFile.toString());
    builder.append("</programfile>\n");

    builder.append("\t<programhash>");
    if (cfa.getFileNames().size() == 1) {
      builder.append(AutomatonGraphmlCommon.computeHash(cfa.getFileNames().get(0)));
    } else {
      builder.append(programFile.hashCode());
    }
    builder.append("</programhash>\n");

    builder.append("\t<entryfunction>");
    builder.append(cfa.getMainFunction().getFunctionName());
    builder.append("</entryfunction>\n");

    builder.append("\t<architecture>");
    builder.append(AutomatonGraphmlCommon.getArchitecture(cfa.getMachineModel()));
    builder.append("</architecture>\n");

    builder.append("\t<creationtime>");
    builder.append(ZonedDateTime.now(ZoneId.systemDefault()).withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    builder.append("</creationtime>\n");

    builder.append("</test-metadata>");
    String metaFile = "/metadata.xml";
    logger.log(Level.INFO, "writing metainfo to: " + outputFolder + metaFile);
    writeTestSuiteFile(metaFile, builder.toString());
  }

  private void writeTestCase(TestCase testcase) {

      StringBuilder builder = new StringBuilder();
      builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      builder.append(
          "<!DOCTYPE testcase SYSTEM \"https://gitlab.com/sosy-lab/software/test-format/blob/master/testcase.dtd\">\n");
      if (addElapsedTime) {
      builder.append("<testcase elapsedTime=\"" + testcase.getElapsedTime() + "\">\n");
      } else {
        builder.append("<testcase>\n");
      }
      for (TestCaseVariable var : testcase.getInputs()) {
        builder.append("\t<input  variable=\"" + var.getName() + "\">");
        builder.append(var.getValue());
        builder.append("</input>\n");
      }
      builder.append("</testcase>\n");
      writeTestSuiteFile(
          "/testcase-" + testcase.getId() + ".xml",
          builder.toString());
  }

  private void initTestSuiteFolder() {
    File outputFolderFile = new File(outputFolder);
    boolean folderExists = outputFolderFile.exists() || outputFolderFile.mkdirs();
    if (folderExists) {
      if (useTestCompOutput) {
        try {
          writeMetaData();
        } catch (IOException e) {
          logger.log(Level.SEVERE, "could not write metadata");
        }
      }
    }
  }


  public void writeFinalTestSuite(TestSuite<?> ts) {
    if (useTestCompOutput) {
      writePartialTestSuite(ts);
    } else {
      writeTestSuiteFile("/testsuite.txt", ts.toString());
      try {
        writeTestSuiteFile("/testsuite.json", ts.toJsonString());
      } catch (IOException e1) {
        logger.log(Level.SEVERE, "could not parse test-suite to json");
      }
    }
  }

  public void writePartialTestSuite(TestSuite<?> ts) {
    // only support partial testsuites for testcomp for now
    if (useTestCompOutput) {
      for (TestCase testcase : ts.getTestCases()) {
        if (!writtenTestCases.contains(testcase)) {
          writeTestCase(testcase);
          writtenTestCases.add(testcase);
        }
      }
    }
  }

}
