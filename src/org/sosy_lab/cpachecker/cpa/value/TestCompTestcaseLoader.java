// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** A class to load the specified testcase from testcomp-output */
public class TestCompTestcaseLoader {

  public static Map<Integer, String> loadTestcase(Path pathToFile)
      throws ParserConfigurationException, SAXException, IOException {

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    // protect against XML eXternal Entity injection (XXE) following the recommendations on
    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    // but do not disable DTD
    // see https://xerces.apache.org/xerces-j/features.html
    // and http://xerces.apache.org/xerces2-j/features.html for features

    // ignore the external DTD completely
    docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    // Do not include external entitites
    docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    // Do not include external parameter entities or the external DTD subset.
    docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

    docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);

    // Add these as per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
    docFactory.setXIncludeAware(false);
    docFactory.setExpandEntityReferences(false);
    // ensure that central mechanism for safeguarding XML processing
    // "Feature for Secure Processing (FSP)" is enabled
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Map<Integer, String> inputs = new HashMap<>();
    Document doc = docBuilder.parse(pathToFile.toFile());

    // Assuming that the testcase is valid, hence directly get the input
    NodeList nList = doc.getElementsByTagName("input");

    for (int i = 0; i < nList.getLength(); i++) {
      // Expected format is similar to <input variable="int" type ="int">1</input>
      Node current = nList.item(i);
      if (!Strings.isNullOrEmpty(current.getTextContent())) {
        inputs.put(i, current.getTextContent());
      }
    }
    return inputs;
  }
}
