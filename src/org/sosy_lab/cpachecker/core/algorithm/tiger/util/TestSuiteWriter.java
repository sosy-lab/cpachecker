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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

public class TestSuiteWriter {

  private CFA cfa;
  private LogManager logger;
  private String originalMainFunction;
  private boolean useTestCompOutput;
  private String outputFolder;
  private Set<TestCase> writtenTestCases;
  private String spec;

  public TestSuiteWriter(
      CFA pCfa,
      LogManager pLogger,
      String pOriginalMainFunction,
      boolean pUseTestCompOutput,
      String pOutputFolder,
      String pSpec) {
    cfa = pCfa;
    logger = pLogger;
    originalMainFunction = pOriginalMainFunction;
    useTestCompOutput = pUseTestCompOutput;
    outputFolder = pOutputFolder;
    writtenTestCases = new HashSet<TestCase>();
    spec = pSpec;
    initTestSuiteFolder();
  }

  private Element createAndAppendElement(
      String elementName,
      String elementTest,
      Element parentElement,
      Document dom) {
    Element newElement = dom.createElement(elementName);
    newElement.appendChild(dom.createTextNode(elementTest));
    parentElement.appendChild(newElement);
    return newElement;
  }

  private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
    // Get file input stream for reading the file content
    FileInputStream fis = new FileInputStream(file);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    // Read file data and update in message digest
    while ((bytesCount = fis.read(byteArray)) != -1) {
      digest.update(byteArray, 0, bytesCount);
    } ;

    // close the stream; We don't need it now.
    fis.close();

    // Get the hash's bytes
    byte[] bytes = digest.digest();

    // This bytes[] has bytes in decimal format;
    // Convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    // return complete hash
    return sb.toString();
  }

  private void writeMetaData() {
    Document dom;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      dom = db.newDocument();
      Element root = dom.createElement("test-metadata");

      createAndAppendElement("sourcecodelang", cfa.getLanguage().toString(), root, dom);
      createAndAppendElement("producer", "CPA-Tiger", root, dom);
      createAndAppendElement("specification", spec, root, dom);
      Path file = cfa.getFileNames().get(0);
      createAndAppendElement("programfile", file.toString(), root, dom);
      createAndAppendElement(
          "programhash",
          AutomatonGraphmlCommon.computeHash(file),
          root,
          dom);
      createAndAppendElement("entryfunction", originalMainFunction, root, dom);

      createAndAppendElement(
          "architecture",
          AutomatonGraphmlCommon.getArchitecture(cfa.getMachineModel()),
          root,
          dom);
      createAndAppendElement(
          "creationtime",
          ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
          root,
          dom);

      dom.appendChild(root);
      try {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMImplementation domImpl = dom.getImplementation();
        DocumentType doctype =
            domImpl.createDocumentType(
                "doctype",
                "+//IDN sosy-lab.org//DTD test-format test-metadata 1.0//EN",
                "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/test-metadata.dtd");
        tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());

        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // send DOM to file
        DOMSource domSource = new DOMSource(dom);
        StreamResult streamResult =
            new StreamResult(new FileOutputStream(outputFolder + "/metadata.xml"));
        tr.transform(
            domSource,
            streamResult);

      } catch (TransformerException te) {
        logger.log(Level.WARNING, te.getMessage());
      } catch (IOException ioe) {
        logger.log(Level.WARNING, ioe.getMessage());
      }
    } catch (ParserConfigurationException pce) {
      logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }
  }

  private void writeTestCase(TestCase testcase) {
    Document dom;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      dom = db.newDocument();
      Element root = dom.createElement("testcase");
      // TODO order of variables if important for testcomp!
      for (Entry<String, BigInteger> var : testcase.getInputs().entrySet()) {
        Element input = createAndAppendElement("input", var.getValue().toString(), root, dom);
        input.setAttribute("variable", var.getKey());
      }

      dom.appendChild(root);
      try {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMImplementation domImpl = dom.getImplementation();
        DocumentType doctype =
            domImpl.createDocumentType(
                "doctype",
                "+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN",
                "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/testcase.dtd");
        tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());

        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // send DOM to file
        tr.transform(
            new DOMSource(dom),
            new StreamResult(
                new FileOutputStream(outputFolder + "/testcase-" + testcase.getId() + ".xml")));

      } catch (TransformerException te) {
        logger.log(Level.WARNING, te.getMessage());
      } catch (IOException ioe) {
        logger.log(Level.WARNING, ioe.getMessage());
      }
    } catch (ParserConfigurationException pce) {
      logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
    }
  }

  private void initTestSuiteFolder() {
    File outputFolderFile = new File(outputFolder);
    if (!outputFolderFile.exists()) {
      outputFolderFile.mkdirs();
    }
    if (useTestCompOutput) {
      writeMetaData();
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
