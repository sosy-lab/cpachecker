/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.propertyscope;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeCallGraph.CallEdge;
import org.sosy_lab.cpachecker.cpa.propertyscope.PropertyScopeCallGraph.FunctionNode;
import org.sosy_lab.cpachecker.cpa.propertyscope.ScopeLocation.Reason;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class PropertyScopeCallGraphToGraphMLWriter {

  private final PropertyScopeCallGraph graph;
  private final Reason reason;
  private final Set<String> relevantProps;

  public PropertyScopeCallGraphToGraphMLWriter(
      PropertyScopeCallGraph pGraph,
      Reason pReason,
      Set<String> pRelevantProps) {
    graph = pGraph;
    reason = pReason;
    relevantProps = pRelevantProps;
  }

  public void writeTo(Writer writer)
      throws IOException, ParserConfigurationException, TransformerException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    Element rootElement = doc.createElementNS("http://graphml.graphdrawing.org/xmlns", "graphml");
    doc.appendChild(rootElement);

    Element nodeLabelElement = doc.createElement("key");
    nodeLabelElement.setAttribute("id", "label");
    nodeLabelElement.setAttribute("attr.name", "label");
    nodeLabelElement.setAttribute("for", "node");
    nodeLabelElement.setAttribute("attr.type", "string");
    rootElement.appendChild(nodeLabelElement);

    Element nodeCFAEdgeElement = doc.createElement("key");
    nodeCFAEdgeElement.setAttribute("id", "node_cfa_count");
    nodeCFAEdgeElement.setAttribute("attr.name", "node_cfa_count");
    nodeCFAEdgeElement.setAttribute("for", "node");
    nodeCFAEdgeElement.setAttribute("attr.type", "int");
    rootElement.appendChild(nodeCFAEdgeElement);

    Element propertyRelevantCFAEdgesElement = doc.createElement("key");
    propertyRelevantCFAEdgesElement.setAttribute("id", "cfa_edges_in_scope");
    propertyRelevantCFAEdgesElement.setAttribute("attr.name", "cfa_edges_in_scope");
    propertyRelevantCFAEdgesElement.setAttribute("for", "node");
    propertyRelevantCFAEdgesElement.setAttribute("attr.type", "int");
    rootElement.appendChild(propertyRelevantCFAEdgesElement);

    Element cFAEdgesInScopeNamesElement = doc.createElement("key");
    cFAEdgesInScopeNamesElement.setAttribute("id", "cfa_edges_in_scope_names");
    cFAEdgesInScopeNamesElement.setAttribute("attr.name", "cfa_edges_in_scope_names");
    cFAEdgesInScopeNamesElement.setAttribute("for", "node");
    cFAEdgesInScopeNamesElement.setAttribute("attr.type", "string");
    rootElement.appendChild(cFAEdgesInScopeNamesElement);

    Element cFAEdgesInScopeNamesReadableElement = doc.createElement("key");
    cFAEdgesInScopeNamesReadableElement.setAttribute("id", "cfa_edges_in_scope_names_readable");
    cFAEdgesInScopeNamesReadableElement.setAttribute(
        "attr.name", "cfa_edges_in_scope_names_readable");
    cFAEdgesInScopeNamesReadableElement.setAttribute("for", "node");
    cFAEdgesInScopeNamesReadableElement.setAttribute("attr.type", "string");
    rootElement.appendChild(cFAEdgesInScopeNamesReadableElement);

    Element propertyScopeRelevanceElement = doc.createElement("key");
    propertyScopeRelevanceElement.setAttribute("id", "prop_scope_relevance");
    propertyScopeRelevanceElement.setAttribute("attr.name", "prop_scope_relevance");
    propertyScopeRelevanceElement.setAttribute("for", "node");
    propertyScopeRelevanceElement.setAttribute("attr.type", "double");
    rootElement.appendChild(propertyScopeRelevanceElement);

    Element functionARGCountElement = doc.createElement("key");
    functionARGCountElement.setAttribute("id", "arg_occurence_count");
    functionARGCountElement.setAttribute("attr.name", "arg_occurence_count");
    functionARGCountElement.setAttribute("for", "node");
    functionARGCountElement.setAttribute("attr.type", "int");
    rootElement.appendChild(functionARGCountElement);

    Element relevantPropElement = doc.createElement("key");
    relevantPropElement.setAttribute("id", "relevant_properties");
    relevantPropElement.setAttribute("attr.name", "relevant_properties");
    relevantPropElement.setAttribute("for", "graph");
    relevantPropElement.setAttribute("attr.type", "string");
    rootElement.appendChild(relevantPropElement);

    Element readableRelevantPropElement = doc.createElement("key");
    readableRelevantPropElement.setAttribute("id", "relevant_properties_readable");
    readableRelevantPropElement.setAttribute("attr.name", "relevant_properties_readable");
    readableRelevantPropElement.setAttribute("for", "graph");
    readableRelevantPropElement.setAttribute("attr.type", "string");
    rootElement.appendChild(readableRelevantPropElement);

    Element graphElement = doc.createElement("graph");
    graphElement.setAttribute("id", "graph");
    graphElement.setAttribute("edgedefault", "directed");
    rootElement.appendChild(graphElement);

    String b64props = relevantProps.stream()
        .map(p ->  Base64.getEncoder().encodeToString(p.getBytes(StandardCharsets.UTF_8)))
        .collect(Collectors.joining(";"));
    Element relevantPropDataElem = doc.createElement("data");
    relevantPropDataElem.setAttribute("key", "relevant_properties");
    relevantPropDataElem.setTextContent(b64props);
    graphElement.appendChild(relevantPropDataElem);

    String props = relevantProps.stream().collect(Collectors.joining(";"));
    Element readableRelevantPropDataElem = doc.createElement("data");
    readableRelevantPropDataElem.setAttribute("key", "relevant_properties_readable");
    readableRelevantPropDataElem.setTextContent(props);
    graphElement.appendChild(readableRelevantPropDataElem);

    for (FunctionNode node : graph.getNodes().values()) {
      Element nodeElement = doc.createElement("node");
      nodeElement.setAttribute("id", node.getName());
      graphElement.appendChild(nodeElement);

      Element nodeCFAEdgeDataElement = doc.createElement("data");
      nodeCFAEdgeDataElement.setAttribute("key", "node_cfa_count");
      nodeCFAEdgeDataElement.setTextContent(Objects.toString(node.getCfaEdges()));
      nodeElement.appendChild(nodeCFAEdgeDataElement);

      Element nodeCFAEdInScopeDataElem = doc.createElement("data");
      nodeCFAEdInScopeDataElem.setAttribute("key", "cfa_edges_in_scope");
      nodeCFAEdInScopeDataElem.setTextContent(Integer.toString(node.getScopedCFAEdgesCount(reason)));
      nodeElement.appendChild(nodeCFAEdInScopeDataElem);

      String b64scopedEdges = node.getScopedCFAEdges(reason).stream()
          .map(p -> Base64.getEncoder()
              .encodeToString(p.toString().getBytes(StandardCharsets.UTF_8)))
          .collect(Collectors.joining(";"));
      Element cFAEdgesInScopeNamesDataElement = doc.createElement("data");
      cFAEdgesInScopeNamesDataElement.setAttribute("key", "cfa_edges_in_scope_names");
      cFAEdgesInScopeNamesDataElement.setTextContent(b64scopedEdges);
      nodeElement.appendChild(cFAEdgesInScopeNamesDataElement);

      String scopedEdges = node.getScopedCFAEdges(reason).stream()
          .map(Object::toString).collect(Collectors.joining(";"));
      Element cFAEdgesInScopeNamesReadableDataElement = doc.createElement("data");
      cFAEdgesInScopeNamesReadableDataElement
          .setAttribute("key", "cfa_edges_in_scope_names_readable");
      cFAEdgesInScopeNamesReadableDataElement.setTextContent(scopedEdges);
      nodeElement.appendChild(cFAEdgesInScopeNamesReadableDataElement);

      Element nodeScopeRelevanceDataElem = doc.createElement("data");
      nodeScopeRelevanceDataElem.setAttribute("key", "prop_scope_relevance");
      nodeScopeRelevanceDataElem.setTextContent(String.format(Locale.ROOT, "%f",
          node.calculatePropertyScopeImportance(reason)));
      nodeElement.appendChild(nodeScopeRelevanceDataElem);

      Element argCountDataElement = doc.createElement("data");
      argCountDataElement.setAttribute("key", "arg_occurence_count");
      argCountDataElement.setTextContent(Integer.toString(node.getArgOccurenceCount()));
      nodeElement.appendChild(argCountDataElement);

      Element nodeLabelDataElem = doc.createElement("data");
      nodeLabelDataElem.setAttribute("key", "label");
      nodeLabelDataElem.setTextContent(node.getName());
      nodeElement.appendChild(nodeLabelDataElem);

    }

    for (CallEdge edge : graph.getEdges()) {
      Element edgeElement = doc.createElement("edge");
      edgeElement.setAttribute("source", edge.getSource().getName());
      edgeElement.setAttribute("target", edge.getSink().getName());

      graphElement.appendChild(edgeElement);
    }

    // write the xml
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(writer);
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(source, result);


  }

}
