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
