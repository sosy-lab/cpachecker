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

import com.google.common.xml.XmlEscapers;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
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

  public static TestSuiteWriter getSingleton(
      CFA pCfa,
      LogManager pLogger,
      boolean pUseTestCompOutput,
      String pOutputFolder,
      String pSpec,
      String pProducer, boolean pAddElapsedTime) {
    if (singleton == null) {
      singleton =
          new TestSuiteWriter(pCfa, pLogger, pUseTestCompOutput, pOutputFolder, pSpec, pProducer, pAddElapsedTime);
    } else {
      assert singleton.cfa.equals(pCfa);
      assert singleton.logger.equals(pLogger);
      assert singleton.useTestCompOutput == pUseTestCompOutput;
      assert singleton.outputFolder.equals(pOutputFolder);
      assert singleton.spec.equals(pSpec);
      assert singleton.producer.equals(pProducer);
      assert singleton.addElapsedTime == pAddElapsedTime;
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
      boolean pAddElapsedTime) {
    cfa = pCfa;
    logger = pLogger;
    useTestCompOutput = pUseTestCompOutput;
    outputFolder = pOutputFolder;
    writtenTestCases = new HashSet<>();
    spec = pSpec;
    producer = pProducer;
    addElapsedTime = pAddElapsedTime;
    initTestSuiteFolder();
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
    builder.append(ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    builder.append("</creationtime>\n");

    builder.append("</test-metadata>");
    Path metaFile = Paths.get(outputFolder + "/metadata.xml");
    logger.log(Level.INFO, "writing metainfo to: " + metaFile.toString());
    Files.write(metaFile, builder.toString().getBytes(StandardCharsets.UTF_8));

    // logger.log(
    // Level.INFO,
    // "Writing Metadata with FQL Statement: " + spec + " and producer " + producer);
    //
    // Document dom;
    // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    // try {
    // DocumentBuilder db = dbf.newDocumentBuilder();
    // dom = db.newDocument();
    // Element root = dom.createElement("test-metadata");
    //
    // createAndAppendElement("sourcecodelang", cfa.getLanguage().toString(), root, dom);
    // createAndAppendElement(
    // "producer",
    // XmlEscapers.xmlContentEscaper().escape(producer),
    // root,
    // dom);
    // createAndAppendElement("specification", spec, root, dom);
    // Path file = cfa.getFileNames().get(0);
    // createAndAppendElement("programfile", file.toString(), root, dom);
    // createAndAppendElement(
    // "programhash",
    // AutomatonGraphmlCommon.computeHash(file),
    // root,
    // dom);
    // createAndAppendElement("entryfunction", originalMainFunction, root, dom);
    //
    // createAndAppendElement(
    // "architecture",
    // AutomatonGraphmlCommon.getArchitecture(cfa.getMachineModel()),
    // root,
    // dom);
    // createAndAppendElement(
    // "creationtime",
    // ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    // root,
    // dom);
    //
    // dom.appendChild(root);
    // try {
    // Transformer tr = TransformerFactory.newInstance().newTransformer();
    // tr.setOutputProperty(OutputKeys.INDENT, "yes");
    // tr.setOutputProperty(OutputKeys.METHOD, "xml");
    // tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    // DOMImplementation domImpl = dom.getImplementation();
    // DocumentType doctype =
    // domImpl.createDocumentType(
    // "doctype",
    // "+//IDN sosy-lab.org//DTD test-format test-metadata 1.0//EN",
    // "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/test-metadata.dtd");
    // tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
    // tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
    //
    // tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    //
    // // send DOM to file
    // DOMSource domSource = new DOMSource(dom);
    // StreamResult streamResult =
    // new StreamResult(new File(outputFolder + "/metadata.xml"));
    // tr.transform(
    // domSource,
    // streamResult);
    //
    // } catch (TransformerException te) {
    // logger.log(Level.WARNING, te.getMessage());
    // }
    // } catch (ParserConfigurationException pce) {
    // logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
    // } catch (IOException e) {
    // logger.log(Level.WARNING, e.getMessage());
    // }
  }

  private void writeTestCase(TestCase testcase) {

    try {
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
      Files.write(
          Paths.get(outputFolder + "/testcase-" + testcase.getId() + ".xml"),
          builder.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Could not write test-case!");
    }

    // Document dom;
    // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    // try {

    // DocumentBuilder db = dbf.newDocumentBuilder();
    // dom = db.newDocument();
    // Element root = dom.createElement("testcase");
    // // TODO order of variables if important for testcomp!
    // for (TestCaseVariable var : testcase.getInputs()) {
    // String value = var.getValue().toString();
    // Element input = createAndAppendElement("input", value, root, dom);
    // input.setAttribute("variable", var.getName());
    // }
    //
    // long timeInSeconds =
    // TimeUnit.MILLISECONDS.convert(testcase.getElapsedTime(), TimeUnit.NANOSECONDS);
    // Element time = createAndAppendElement("elapsedTime", Long.toString(timeInSeconds), root,
    // dom);
    //
    // dom.appendChild(root);
    // try {
    // Transformer tr = TransformerFactory.newInstance().newTransformer();
    // tr.setOutputProperty(OutputKeys.INDENT, "yes");
    // tr.setOutputProperty(OutputKeys.METHOD, "xml");
    // tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    // DOMImplementation domImpl = dom.getImplementation();
    // DocumentType doctype =
    // domImpl.createDocumentType(
    // "doctype",
    // "+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN",
    // "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/testcase.dtd");
    // tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
    // tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
    //
    // tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    //
    // // send DOM to file
    // File outputFile = new File(outputFolder + "/testcase-" + testcase.getId() + ".xml");
    // tr.transform(
    // new DOMSource(dom),
    // new StreamResult(outputFile));
    //
    // } catch (TransformerException te) {
    // logger.log(Level.WARNING, te.getMessage());
    // }
    // } catch (ParserConfigurationException pce) {
    // logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
    // }
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
      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(
                  new FileOutputStream(outputFolder + "/testsuite.txt"),
                  "utf-8"))) {
        writer.write(ts.toString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(
                  new FileOutputStream(outputFolder + "/testsuite.json"),
                  "utf-8"))) {
        writer.write(ts.toJsonString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
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
