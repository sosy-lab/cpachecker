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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AutomatonGraphmlCommon {

  public static final String SINK_NODE_ID = "sink";

  public static enum AssumeCase {

    THEN("condition-true"),
    ELSE("condition-false");

    private final String name;

    private AssumeCase(String pName) {
      this.name = pName;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  public static enum KeyDef {
    INVARIANT("invariant", ElementType.NODE, "invariant", "string"),
    INVARIANTSCOPE("invariant.scope", ElementType.NODE, "invariant.scope", "string"),
    NAMED("named", ElementType.NODE, "namedValue", "string"),
    NODETYPE("nodetype", ElementType.NODE, "nodeType", "string", NodeType.ONPATH),
    ISFRONTIERNODE("frontier", ElementType.NODE, "isFrontierNode", "boolean", false),
    ISVIOLATIONNODE("violation", ElementType.NODE, "isViolationNode", "boolean", false),
    ISENTRYNODE("entry", ElementType.NODE, "isEntryNode", "boolean", false),
    ISSINKNODE("sink", ElementType.NODE, "isSinkNode", "boolean", false),
    ENTERLOOPHEAD("enterLoopHead", ElementType.EDGE, "enterLoopHead", "boolean", false),
    VIOLATEDPROPERTY("violatedProperty", ElementType.NODE, "violatedProperty", "string"),
    THREADID("threadId", ElementType.EDGE, "threadId", "string"),
    SOURCECODELANGUAGE("sourcecodelang", ElementType.GRAPH, "sourcecodeLanguage", "string"),
    PROGRAMFILE("programfile", ElementType.GRAPH, "programFile", "string"),
    PROGRAMHASH("programhash", ElementType.GRAPH, "programHash", "string"),
    SPECIFICATION("specification", ElementType.GRAPH, "specification", "string"),
    MEMORYMODEL("memorymodel", ElementType.GRAPH, "memoryModel", "string"),
    ARCHITECTURE("architecture", ElementType.GRAPH, "architecture", "string"),
    PRODUCER("producer", ElementType.GRAPH, "producer", "string"),
    SOURCECODE("sourcecode", ElementType.EDGE, "sourcecode", "string"),
    ORIGINLINE("startline", ElementType.EDGE, "startline", "int"),
    OFFSET("startoffset", ElementType.EDGE, "startoffset", "int"),
    ORIGINFILE("originfile", ElementType.EDGE, "originFileName", "string"),
    LINECOLS("lineCols", ElementType.EDGE, "lineColSet", "string"),
    CONTROLCASE("control", ElementType.EDGE, "control", "string"),
    ASSUMPTION("assumption", ElementType.EDGE, "assumption", "string"),
    ASSUMPTIONRESULTFUNCTION("assumption.resultfunction", ElementType.EDGE, "assumption.resultfunction", "string"),
    ASSUMPTIONSCOPE("assumption.scope", ElementType.EDGE, "assumption.scope", "string"),
    FUNCTIONENTRY("enterFunction", ElementType.EDGE, "enterFunction", "string"),
    FUNCTIONEXIT("returnFrom", ElementType.EDGE, "returnFromFunction", "string"),
    CFAPREDECESSORNODE("predecessor", ElementType.EDGE, "predecessor", "string"),
    CFASUCCESSORNODE("successor", ElementType.EDGE, "successor", "string"),
    GRAPH_TYPE("witness-type", ElementType.GRAPH, "witness-type", "string");

    public final String id;
    public final ElementType keyFor;
    public final String attrName;
    public final String attrType;

    /** The defaultValue is non-null, iff existent. */
    @Nullable public final String defaultValue;

    private KeyDef(String id, ElementType pKeyFor, String attrName, String attrType) {
      this.id = id;
      this.keyFor = pKeyFor;
      this.attrName = attrName;
      this.attrType = attrType;
      this.defaultValue = null;
    }

    private KeyDef(String id, ElementType pKeyFor, String attrName, String attrType, Object defaultValue) {
      this.id = id;
      this.keyFor = pKeyFor;
      this.attrName = attrName;
      this.attrType = attrType;
      this.defaultValue = defaultValue.toString();
    }

    @Override
    public String toString() {
      return id;
    }
  }

  public static enum ElementType {
    GRAPH,
    EDGE,
    NODE;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public static ElementType parse(String pElementType) {
      return ElementType.valueOf(pElementType.toUpperCase());
    }
  }

  public static enum NodeFlag {
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
      for (NodeFlag f : NodeFlag.values()) {
        stringToFlagMap.put(f.key.id, f);
      }
    }


    public static NodeFlag getNodeFlagByKey(final String key) {
      return stringToFlagMap.get(key);
    }
  }

  public enum GraphType {
    ERROR_WITNESS("violation_witness"),
    PROOF_WITNESS("correctness_witness");

    public final String text;

    private GraphType(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }

    public static Optional<GraphType> tryParse(String pTextualRepresentation) {
      for (GraphType element : values()) {
        if (element.text.equals(pTextualRepresentation)) {
          return Optional.of(element);
        }
      }
      if (pTextualRepresentation.equals("FALSE")) {
        return Optional.of(ERROR_WITNESS);
      }
      if (pTextualRepresentation.equals("TRUE")) {
        return Optional.of(PROOF_WITNESS);
      }
      if (pTextualRepresentation.equals("false_witness")) {
        return Optional.of(ERROR_WITNESS);
      }
      if (pTextualRepresentation.equals("true_witness")) {
        return Optional.of(PROOF_WITNESS);
      }
      return Optional.empty();
    }
  }

  public enum NodeType {
    ANNOTATION("annotation"),
    ONPATH("path");

    public final String text;

    private NodeType(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }

    public static NodeType fromString(String nodeTypeString) {
      return valueOf(nodeTypeString.trim().toLowerCase());
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

    private GraphMlTag(String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public static class GraphMlBuilder {

    private final Document doc;
    private final Element graph;

    public GraphMlBuilder(
        GraphType pGraphType,
        String pDefaultSourceFileName,
        Language pLanguage,
        MachineModel pMachineModel,
        VerificationTaskMetaData pVerificationTaskMetaData)
        throws ParserConfigurationException, DOMException, IOException {
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      this.doc = docBuilder.newDocument();
      Element root = doc.createElement("graphml");
      doc.appendChild(root);
      root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      root.setAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");

      EnumSet<KeyDef> keyDefs = EnumSet.allOf(KeyDef.class);
      root.appendChild(createKeyDefElement(KeyDef.ORIGINFILE, pDefaultSourceFileName));
      keyDefs.remove(KeyDef.ORIGINFILE);
      for (KeyDef keyDef : keyDefs) {
        root.appendChild(createKeyDefElement(keyDef, keyDef.defaultValue));
      }

      graph = doc.createElement("graph");
      root.appendChild(graph);
      graph.setAttribute("edgedefault", "directed");
      graph.appendChild(createDataElement(KeyDef.GRAPH_TYPE, pGraphType.toString()));
      graph.appendChild(createDataElement(KeyDef.SOURCECODELANGUAGE, pLanguage.toString()));
      graph.appendChild(
          createDataElement(KeyDef.PRODUCER, "CPAchecker " + CPAchecker.getCPAcheckerVersion()));
      for (String specification : pVerificationTaskMetaData.getSpecifications()) {
        graph.appendChild(createDataElement(KeyDef.SPECIFICATION, specification));
      }

      /*
       * TODO: We should allow multiple program files here.
       * As soon as we do, we should also hash each file separately.
       */
      if (pVerificationTaskMetaData.getProgramNames().isPresent()) {
        graph.appendChild(
            createDataElement(
                KeyDef.PROGRAMFILE,
                Joiner.on(", ").join(pVerificationTaskMetaData.getProgramNames().get())));
      }
      if (pVerificationTaskMetaData.getProgramHash().isPresent()) {
        graph.appendChild(
            createDataElement(
                KeyDef.PROGRAMHASH, pVerificationTaskMetaData.getProgramHash().get()));
      }

      graph.appendChild(
          createDataElement(KeyDef.MEMORYMODEL, pVerificationTaskMetaData.getMemoryModel()));
      graph.appendChild(createDataElement(KeyDef.ARCHITECTURE, getArchitecture(pMachineModel)));
    }

    private Element createElement(GraphMlTag tag) {
      return doc.createElement(tag.toString());
    }

    private Element createDataElement(final KeyDef key, final String value) {
      Element result = createElement(GraphMlTag.DATA);
      result.setAttribute("key", key.id);
      result.setTextContent(value);
      return result;
    }

    public Element createEdgeElement(final String from, final String to) {
      Element result = createElement(GraphMlTag.EDGE);
      result.setAttribute("source", from);
      result.setAttribute("target", to);
      graph.appendChild(result);
      return result;
    }

    public Element createNodeElement(String nodeId, NodeType nodeType) {
      Element result = createElement(GraphMlTag.NODE);
      result.setAttribute("id", nodeId);

      if (nodeType != defaultNodeType) {
        addDataElementChild(result, KeyDef.NODETYPE, nodeType.toString());
      }

      graph.appendChild(result);

      return result;
    }

    private Element createKeyDefElement(KeyDef keyDef, @Nullable String defaultValue) {
      return createKeyDefElement(
          keyDef.id, keyDef.keyFor, keyDef.attrName, keyDef.attrType, defaultValue);
    }

    private Element createKeyDefElement(
        String id,
        ElementType keyFor,
        String attrName,
        String attrType,
        @Nullable String defaultValue) {

      Preconditions.checkNotNull(doc);
      Preconditions.checkNotNull(id);
      Preconditions.checkNotNull(keyFor);
      Preconditions.checkNotNull(attrName);
      Preconditions.checkNotNull(attrType);

      Element result = createElement(GraphMlTag.KEY);

      result.setAttribute("id", id);
      result.setAttribute("for", keyFor.toString());
      result.setAttribute("attr.name", attrName);
      result.setAttribute("attr.type", attrType);

      if (defaultValue != null) {
        Element defaultValueElement = createElement(GraphMlTag.DEFAULT);
        defaultValueElement.setTextContent(defaultValue);
        result.appendChild(defaultValueElement);
      }

      return result;
    }

    public void addDataElementChild(Element childOf, final KeyDef key, final String value) {
      Element result = createDataElement(key, value);
      childOf.appendChild(result);
    }

    public void appendTo(Appendable pTarget) throws IOException {
      try {
        pTarget.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(doc), new StreamResult(CharStreams.asWriter(pTarget)));
      } catch (TransformerException ex) {
        if (ex.getException() instanceof IOException) {
          throw (IOException) ex.getException();
        }
        throw new RuntimeException("Error while writing witness.", ex);
      }
    }

  }

  public static boolean handleAsEpsilonEdge(CFAEdge edge) {
    if (handleAsEpsilonEdge0(edge)) {
      if (edge.getSuccessor().getNumLeavingEdges() <= 0) {
        return false;
      }
      return true;
    }
    return false;
  }

  private static boolean handleAsEpsilonEdge0(CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return !(edge.getSuccessor() instanceof FunctionExitNode);
    } else if (edge instanceof CFunctionReturnEdge) {
      return false;
    } else if (edge instanceof CDeclarationEdge) {
      CDeclarationEdge declEdge = (CDeclarationEdge) edge;
      CDeclaration decl = declEdge.getDeclaration();
      if (decl instanceof CFunctionDeclaration) {
        return true;
      } else if (decl instanceof CTypeDeclaration) {
        return true;
      } else if (decl instanceof CVariableDeclaration) {
        CVariableDeclaration varDecl = (CVariableDeclaration) decl;
        if (varDecl.getName().toUpperCase().startsWith("__CPACHECKER_TMP")) {
          return true; // Dirty hack; would be better if these edges had no file location
        }
        CFANode successor = edge.getSuccessor();
        Iterator<CFAEdge> leavingEdges = CFAUtils.allLeavingEdges(successor).iterator();
        if (!leavingEdges.hasNext()) {
          return false;
        }
        CFAEdge successorEdge = leavingEdges.next();
        if (leavingEdges.hasNext()) {
          return false;
        }
        if (successorEdge instanceof AStatementEdge) {
          AStatementEdge statementEdge = (AStatementEdge) successorEdge;
          if (statementEdge.getFileLocation().equals(edge.getFileLocation())
              && statementEdge.getStatement() instanceof AAssignment) {
            AAssignment assignment = (AAssignment) statementEdge.getStatement();
            ALeftHandSide leftHandSide = assignment.getLeftHandSide();
            if (leftHandSide instanceof AIdExpression) {
              AIdExpression lhs = (AIdExpression) leftHandSide;
              if (lhs.getDeclaration() != null && lhs.getDeclaration().equals(varDecl)) {
                return true;
              }
            }
          }
        }
        return false;
      }
    } else if (edge instanceof CFunctionSummaryStatementEdge) {
      return true;
    }

    return false;
  }

  public static String getArchitecture(MachineModel pMachineModel) {
    final String architecture;
    switch (pMachineModel) {
      case LINUX32:
        architecture = "32bit";
        break;
      case LINUX64:
        architecture = "64bit";
        break;
      default:
        architecture = pMachineModel.toString();
        break;
    }
    return architecture;
  }

}
