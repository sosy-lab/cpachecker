/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.automaton;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;


public class AutomatonGraphmlCommon {

  public final static String SINK_NODE_ID = "sink";

  public enum KeyDef {
    INVARIANT("invariant", "node", "invariant", "string"),
    NAMED("named", "node", "namedValue", "string"),

    NODETYPE("nodetype", "node", "nodeType", "string"),

    ISFRONTIERNODE("frontier","node","isFrontierNode","boolean"),
    ISVIOLATIONNODE("violation","node","isViolationNode","boolean"),
    ISENTRYNODE("entry","node","isEntryNode","boolean"),
    ISSINKNODE("sink","node","isSinkNode","boolean"),

    SOURCECODELANGUAGE("sourcecodelang", "graph", "sourcecodeLanguage", "string"),

    SOURCECODE("sourcecode", "edge", "sourcecode", "string"),
    TOKENS("tokens", "edge", "tokenSet", "string"),
    ORIGINLINE("originline", "edge", "lineNumberInOrigin", "int"),
    ORIGINFILE("originfile", "edge", "originFileName", "string"),
    LINECOLS("lineCols", "edge", "lineColSet", "string"),
    TOKENSNEGATED("negated", "edge", "negativeCase", "string"),
    ASSUMPTION("assumption", "edge", "assumption", "string"),

    FUNCTIONENTRY("enterFunction", "edge", "enterFunction", "string"),
    FUNCTIONEXIT("returnFrom", "edge", "returnFromFunction", "string");

    public final String id;
    public final String keyFor;
    public final String attrName;
    public final String attrType;

    KeyDef(String id, String keyFor, String attrName, String attrType) {
      this.id = id;
      this.keyFor = keyFor;
      this.attrName = attrName;
      this.attrType = attrType;
    }

    @Override
    public String toString() {
      return id;
    }
  }

  public enum NodeFlag {
    ISFRONTIER(KeyDef.ISFRONTIERNODE),
    ISVIOLATION(KeyDef.ISVIOLATIONNODE),
    ISENTRY(KeyDef.ISENTRYNODE),
    ISSINKNODE(KeyDef.ISSINKNODE);

    public final KeyDef key;

    private NodeFlag(KeyDef key) {
      this.key = key;
    }

    private final static Map<String, NodeFlag> stringToFlagMap = Maps.newHashMap();

    static {
      for (NodeFlag f: NodeFlag.values()) {
        stringToFlagMap.put(f.key.id, f);
      }
    }


    public static NodeFlag getNodeFlagByKey(final String key) {
      return stringToFlagMap.get(key);
    }
  }

  public enum GraphType {
    PROGRAMPATH("traces automaton"),
    CONDITION("assumptions automaton");

    public final String text;

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
    ONPATH("path");

    public final String text;

    NodeType(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }

    public static NodeType fromString(String nodeTypeString) {
      for (NodeType t: NodeType.values()) {
        if (t.text.equalsIgnoreCase(nodeTypeString.trim())) {
          return t;
        }
      }
      throw new RuntimeException(String.format("String '%s' does not descripe a node type!", nodeTypeString));
    }
  }

  public static final NodeType defaultNodeType = NodeType.ONPATH;

  public enum GraphMlTag {
    NODE("node"),
    DATA("data"),
    KEY("key"),
    GRAPH("graph"),
    DEFAULT("default"),
    EDGE("edge");

    public final String text;

    GraphMlTag(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public static class GraphMlBuilder {

    private final Document doc;
    private final Appendable target;

    public GraphMlBuilder(Appendable target) throws ParserConfigurationException {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      this.doc = docBuilder.newDocument();
      this.target = target;
    }

    public Element createElement(GraphMlTag tag) {
      return doc.createElement(tag.toString());
    }

    public Element createDataElement(final KeyDef key, final String value) {
      Element result = createElement(GraphMlTag.DATA);
      result.setAttribute("key", key.id);
      result.setTextContent(value);
      return result;
    }

    public Element createEdgeElement(final String from, final String to) {
      Element result = createElement(GraphMlTag.EDGE);
      result.setAttribute("source", from);
      result.setAttribute("target", to);
      return result;
    }

    public Element createKeyDefElement(KeyDef keyDef, @Nullable String defaultValue) {
      return createKeyDefElement(keyDef.id, keyDef.keyFor, keyDef.attrName, keyDef.attrType, defaultValue);
    }

    public Element createKeyDefElement(String id, String keyFor, String attrName, String attrType,
        @Nullable String defaultValue) {

      Preconditions.checkNotNull(doc);
      Preconditions.checkNotNull(id);
      Preconditions.checkNotNull(keyFor);
      Preconditions.checkNotNull(attrName);
      Preconditions.checkNotNull(attrType);

      Element result = createElement(GraphMlTag.KEY);

      result.setAttribute("id", id);
      result.setAttribute("for", keyFor);
      result.setAttribute("attr.name", attrName);
      result.setAttribute("attr.type", attrType);

      if (defaultValue != null) {
        Element defaultValueElement = createElement(GraphMlTag.DEFAULT);
        defaultValueElement.setTextContent(defaultValue);
        result.appendChild(defaultValueElement);
      }

      return result;
    }

    public Element createNodeElement(String nodeId, NodeType nodeType) throws IOException {
      Element result = createElement(GraphMlTag.NODE);
      result.setAttribute("id", nodeId);

      if (nodeType != defaultNodeType) {
        addDataElementChild(result, KeyDef.NODETYPE, nodeType.toString());
      }

      return result;
    }

    public void appendNewNode(String nodeId, NodeType nodeType) throws IOException {
      Element result = createNodeElement(nodeId, nodeType);
      appendToAppendable(result);
    }

    public Element addDataElementChild(Element childOf, final KeyDef key, final String value) {
      Element result = createDataElement(key, value);
      childOf.appendChild(result);
      return result;
    }

    public void appendDataElement(final KeyDef key, final String value) {
      Element result = createDataElement(key, value);
      appendToAppendable(result);
    }

    public void appendDocHeader() throws IOException {
      target.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
      target.append("<graphml xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n");
    }

    public void appendGraphHeader(GraphType pGraphType, String pSourceLanguage) throws IOException {
      target.append("<graph edgedefault=\"directed\">");
      appendDataElement(KeyDef.SOURCECODELANGUAGE, pSourceLanguage);
    }

    public void appendNewKeyDef(KeyDef keyDef, @Nullable String defaultValue) {
      appendToAppendable(createKeyDefElement(keyDef, defaultValue));
    }

    public void appendToAppendable(Node n) {
      try {
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(n), new StreamResult(sw));
        target.append(sw.toString());
      } catch (TransformerException | IOException ex) {
          throw new RuntimeException("Error while dumping program path", ex);
      }
    }

    public void appendFooter() throws IOException {
      target.append("</graph>\n");
      target.append("</graphml>\n");
    }

  }


}
