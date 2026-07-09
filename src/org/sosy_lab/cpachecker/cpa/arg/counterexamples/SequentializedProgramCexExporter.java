// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.TaskRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SequentializedProgramCexExporter {

  private static String trivialWitness() {
    return """
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <graphml xmlns="http://graphml.graphdrawing.org/xmlns" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <key attr.name="originFileName" attr.type="string" for="edge" id="originfile">
      <default>/path/to/input_file.c</default>
     </key>
     <key attr.name="invariant" attr.type="string" for="node" id="invariant"/>
     <key attr.name="invariant.scope" attr.type="string" for="node" id="invariant.scope"/>
     <key attr.name="namedValue" attr.type="string" for="node" id="named"/>
     <key attr.name="nodeType" attr.type="string" for="node" id="nodetype">
      <default>path</default>
     </key>
     <key attr.name="isFrontierNode" attr.type="boolean" for="node" id="frontier">
      <default>false</default>
     </key>
     <key attr.name="isViolationNode" attr.type="boolean" for="node" id="violation">
      <default>false</default>
     </key>
     <key attr.name="isEntryNode" attr.type="boolean" for="node" id="entry">
      <default>false</default>
     </key>
     <key attr.name="isSinkNode" attr.type="boolean" for="node" id="sink">
      <default>false</default>
     </key>
     <key attr.name="enterLoopHead" attr.type="boolean" for="edge" id="enterLoopHead">
      <default>false</default>
     </key>
     <key attr.name="violatedProperty" attr.type="string" for="node" id="violatedProperty"/>
     <key attr.name="threadId" attr.type="string" for="edge" id="threadId"/>
     <key attr.name="sourcecodeLanguage" attr.type="string" for="graph" id="sourcecodelang"/>
     <key attr.name="programFile" attr.type="string" for="graph" id="programfile"/>
     <key attr.name="programHash" attr.type="string" for="graph" id="programhash"/>
     <key attr.name="specification" attr.type="string" for="graph" id="specification"/>
     <key attr.name="architecture" attr.type="string" for="graph" id="architecture"/>
     <key attr.name="producer" attr.type="string" for="graph" id="producer"/>
     <key attr.name="sourcecode" attr.type="string" for="edge" id="sourcecode"/>
     <key attr.name="startline" attr.type="int" for="edge" id="startline"/>
     <key attr.name="startoffset" attr.type="int" for="edge" id="startoffset"/>
     <key attr.name="lineColSet" attr.type="string" for="edge" id="lineCols"/>
     <key attr.name="control" attr.type="string" for="edge" id="control"/>
     <key attr.name="assumption" attr.type="string" for="edge" id="assumption"/>
     <key attr.name="assumption.resultfunction" attr.type="string" for="edge" id="assumption.resultfunction"/>
     <key attr.name="assumption.scope" attr.type="string" for="edge" id="assumption.scope"/>
     <key attr.name="enterFunction" attr.type="string" for="edge" id="enterFunction"/>
     <key attr.name="returnFromFunction" attr.type="string" for="edge" id="returnFrom"/>
     <key attr.name="predecessor" attr.type="string" for="edge" id="predecessor"/>
     <key attr.name="successor" attr.type="string" for="edge" id="successor"/>
     <key attr.name="witness-type" attr.type="string" for="graph" id="witness-type"/>
     <key attr.name="creationtime" attr.type="string" for="graph" id="creationtime"/>
     <graph edgedefault="directed">
      <data key="witness-type">violation_witness</data>
      <data key="sourcecodelang">C</data>
      <data key="producer">CPAchecker</data>
      <data key="specification">CHECK( init(main()), LTL(G ! call(reach_error())) )</data>
      <data key="programfile">/path/to/input_file.c</data>
      <data key="programhash">29d100da65e2ae9d5c43d48b49216d155078158e341858baee0b08b697e15c8e</data>
      <data key="architecture">32bit</data>
      <data key="creationtime">2024-11-22T12:17:52+01:00</data>
      <node id="42">
        <data key="entry">true</data>
        <data key="violation">true</data>
      </node>
     </graph>
    </graphml>\
    """;
  }

  private static void updateDefaultGraphml(
      CFA pOriginalCfa, Specification pSpecification, Document pDocument) throws IOException {

    Path filePath = pOriginalCfa.getFileNames().getFirst();
    String cpaCheckerVersion = CPAchecker.getPlainVersion();
    String producer = "CPAchecker-" + cpaCheckerVersion;
    String programHash = AutomatonGraphmlCommon.computeHash(filePath);
    String architecture = AutomatonGraphmlCommon.getArchitecture(pOriginalCfa.getMachineModel());
    String creationTime = AutomatonGraphmlCommon.getCreationTime();

    // update entries
    updateKeyDefault(pDocument, "originfile", filePath.toString());
    updateDataValue(pDocument, "producer", producer);
    updateDataValue(
        pDocument, "specification", TaskRecord.getSpecificationAsString(pSpecification));
    updateDataValue(pDocument, "programfile", filePath.toString());
    updateDataValue(pDocument, "programhash", programHash);
    updateDataValue(pDocument, "architecture", architecture);
    updateDataValue(pDocument, "creationtime", creationTime);
  }

  /** Returns a dummy {@code .graphml} counterexample without any meaningful nodes or edges. */
  public static String buildDefaultSequentializationCounterexample(
      CFA pOriginalCfa, Specification pSpecification)
      throws ParserConfigurationException, IOException, SAXException, TransformerException {

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    StringWriter stringWriter = new StringWriter();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new InputSource(new StringReader(trivialWitness())));
    document.getDocumentElement().normalize();
    updateDefaultGraphml(pOriginalCfa, pSpecification, document);

    // Write witness to string
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

    return stringWriter.toString();
  }

  private static void updateDataValue(Document pDocument, String pKeyName, String pNewValue) {
    NodeList list = pDocument.getElementsByTagName("data");
    for (int i = 0; i < list.getLength(); i++) {
      Element data = (Element) list.item(i);
      if (pKeyName.equals(data.getAttribute("key"))) {
        data.setTextContent(pNewValue);
      }
    }
  }

  private static void updateKeyDefault(Document pDocument, String pKeyId, String pNewDefault) {
    NodeList keys = pDocument.getElementsByTagName("key");
    for (int i = 0; i < keys.getLength(); i++) {
      Element key = (Element) keys.item(i);
      if (pKeyId.equals(key.getAttribute("id"))) {
        NodeList defaults = key.getElementsByTagName("default");
        if (defaults.getLength() > 0) {
          defaults.item(0).setTextContent(pNewDefault);
        }
      }
    }
  }
}
