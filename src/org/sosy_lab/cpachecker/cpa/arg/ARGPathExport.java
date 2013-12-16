/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;


public class ARGPathExport {

  private final static String SINK_NODE_ID = "sink";

  public enum GraphType {
    PROGRAMPATH("path"),
    CONDITION("condition");

    private final String text;

    GraphType(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public enum NodeType {
    ANNOTATION("annotation"),
    ONPATH("pathnode"),
    SINKNODE("sinknode");

    private final String text;

    NodeType(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  private static Element appendNewNode(Document doc, Element target, String nodeId, NodeType nodeType) throws IOException {
    Element result = doc.createElement("node");
    result.setAttribute("id", nodeId);
    result.setAttribute("type", nodeType.toString());

    target.appendChild(result);
    return result;
  }

  private static Element appendNewEdge(Document doc, Element target, String from, String to, CFAEdge edge) throws IOException {
    Set<Integer> tokens = CFAUtils.getTokensFromCFAEdge(edge);
    String tokensText = tokensToText(tokens);

    Element result = doc.createElement("edge");
    result.setAttribute("source", from);
    result.setAttribute("target", to);
    result.setAttribute("tokens", tokensText);

    Element data = doc.createElement("data");
    data.setAttribute("key", "sourcecode");
    data.setAttribute("lang", "C");
    data.setTextContent(edge.getCode());
    result.appendChild(data);

    target.appendChild(result);
    return result;
  }

  private static void appendKeyDefinitions(Document doc, Element target, GraphType graphType) {
    if (graphType == GraphType.CONDITION) {
      appendNewKeyDef(doc, target, "assumption", "node", "assumption", "string");
      appendNewKeyDef(doc, target, "invariant", "node", "invariant", "string");
      appendNewKeyDef(doc, target, "tokens", "node", "tokenSet", "string");
      appendNewKeyDef(doc, target, "named", "node", "namedValue", "string");
    }
    appendNewKeyDef(doc, target, "sourcecode", "node", "sourcecode", "string");
  }

  private static Element createAndAppendGraphRoot(Document doc, Element target, GraphType type) {
    Element result = doc.createElement("graph");
    result.setAttribute("edgedefault", "directed");
    result.setAttribute("type", GraphType.PROGRAMPATH.toString());

    target.appendChild(result);
    return result;
  }

  private static String tokensToText(Set<Integer> tokens) {
    StringBuilder result = new StringBuilder();
    RangeSet<Integer> tokenRanges = TreeRangeSet.create();
    for (Integer token: tokens) {
      tokenRanges.add(Range.closed(token, token));
    }
    for (Range<Integer> range : tokenRanges.asRanges()) {
      if (result.length() > 0) {
        result.append(",");
      }
      Integer from = range.lowerEndpoint();
      Integer to = range.upperEndpoint();
      if (to - from == 0) {
        result.append(from);
      } else {
        result.append(from);
        result.append("-");
        result.append(to);
      }
    }

    return result.toString();
  }

  private static void appendNewKeyDef(Document doc, Element target, String id, String keyFor, String attrName, String attrType) {
    Element keyElement = doc.createElement("key");

    keyElement.setAttribute("id", id);
    keyElement.setAttribute("for", keyFor);
    keyElement.setAttribute("attr.name", attrName);
    keyElement.setAttribute("attr.type", attrType);

    target.appendChild(keyElement);
  }

  private static String getStateIdent(ARGState state) {
    return getStateIdent(state, "");
  }

  private static String getStateIdent(ARGState state, String identPostfix) {
    return String.format("ARG%d%s", state.getStateId(), identPostfix);
  }

  private static String getPseudoStateIdent(ARGState state, int subStateNo, int subStateCount)
  {
    return getStateIdent(state, String.format("_%d_%d", subStateNo, subStateCount));
  }

  public static void producePathAutomatonGraphMl(Appendable sb, ARGState pRootState,
      Set<ARGState> pPathStates, String name) throws IOException {

    GraphType graphType = GraphType.PROGRAMPATH;

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder;
    try {
      docBuilder = docFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      return;
    }

    // Root of document
    Document doc = docBuilder.newDocument();
    Element root = doc.createElementNS("http://graphml.graphdrawing.org/xmlns", "graphml");
    root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    doc.appendChild(root);
    // TODO: Full schema details

    // Version of format..
    // TODO! (we could use the version of a XML schema)

    // GraphML keys
    appendKeyDefinitions(doc, root, graphType);

    // Root of graph
    Element graph = createAndAppendGraphRoot(doc, root, graphType);

    Element sinkNode = null;

    // ...
    int multiEdgeCount = 0; // see below
    for (ARGState s : pPathStates) {
      // Location of the state
      CFANode loc = AbstractStates.extractLocation(s);

      // Write the state
      String sourceStateNodeId = getStateIdent(s);
      appendNewNode(doc, graph, sourceStateNodeId, NodeType.ONPATH);

      // For each child (successor) of the state do ...
      for (ARGState child : s.getChildren()) {
        // The child might be covered by another state
        // --> switch to the covering state
        if (child.isCovered()) {
          child = child.getCoveringState();
          assert !child.isCovered();
        }

        // Only proceed with this state if the path states contains the child

        String childStateId = getStateIdent(child);
        CFANode childLoc = AbstractStates.extractLocation(child);
        CFAEdge edgeToNextState = loc.getEdgeTo(childLoc);
        String prevStateId = sourceStateNodeId;

        if (edgeToNextState instanceof MultiEdge) {
          // The successor state might have several incoming MultiEdges.
          // In this case the state names like ARG<successor>_0 would occur
          // several times.
          // So we add this counter to the state names to make them unique.
          multiEdgeCount++;

          // Write out a long linear chain of pseudo-states (one state encodes multiple edges)
          // because the AutomatonCPA also iterates through the MultiEdge.
          List<CFAEdge> edges = ((MultiEdge)edgeToNextState).getEdges();

          // inner part (without last edge)
          for (int i = 0; i < edges.size()-1; i++) {
            CFAEdge innerEdge = edges.get(i);
            String pseudoStateId = getPseudoStateIdent(child, i, multiEdgeCount);

            appendNewNode(doc, graph, pseudoStateId, NodeType.ONPATH);
            appendNewEdge(doc, graph, prevStateId, pseudoStateId, innerEdge);
            prevStateId = pseudoStateId;
          }

          // last edge connecting it with the real successor
          edgeToNextState = edges.get(edges.size()-1);
        }

        if (pPathStates.contains(child)) {
          appendNewEdge(doc, graph, prevStateId, childStateId, edgeToNextState);
        } else {
          if (sinkNode == null) {
            sinkNode = appendNewNode(doc, graph, SINK_NODE_ID, NodeType.SINKNODE);
          }
          // Path to non-assumption edge (this becomes the sink)
          exportUntilNonAssume(doc, graph, prevStateId, edgeToNextState, pPathStates);
        }
      }
    }

    writeXmlToAppendable(sb, doc);
  }

  private static void writeXmlToAppendable(Appendable sb, Document doc) throws TransformerFactoryConfigurationError,
      IOException {
    try {
      StringWriter sw = new StringWriter();
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

      transformer.transform(new DOMSource(doc), new StreamResult(sw));
      sb.append(sw.toString());
    } catch (TransformerException ex) {
        throw new RuntimeException("Error while dumping program path", ex);
    }
  }

  private static void exportUntilNonAssume(Document doc, Element graph, String prevStateId,
      CFAEdge startWith, Set<ARGState> pathStates) throws IOException {

    if (startWith.getEdgeType() != CFAEdgeType.AssumeEdge) {
      appendNewEdge(doc, graph, prevStateId, SINK_NODE_ID, startWith);
    } else {
      CFANode successorLoc = startWith.getSuccessor();
      String newPrevStateId = prevStateId + "_" + successorLoc.getNodeNumber();
      appendNewNode(doc, graph, newPrevStateId, NodeType.ONPATH);
      appendNewEdge(doc, graph, prevStateId, newPrevStateId, startWith);

      for (CFAEdge e : CFAUtils.allLeavingEdges(successorLoc)) {
        // Recursive!
        exportUntilNonAssume(doc, graph, newPrevStateId, e, pathStates);
      }
    }
  }




}
