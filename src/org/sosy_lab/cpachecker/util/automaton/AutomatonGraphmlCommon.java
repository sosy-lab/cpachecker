// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.automaton;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.XMLUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class AutomatonGraphmlCommon {

  private AutomatonGraphmlCommon() {}

  private static final String CPACHECKER_TMP_PREFIX = "__CPACHECKER_TMP";
  public static final String SINK_NODE_ID = "sink";

  public enum AssumeCase {
    THEN("condition-true"),
    ELSE("condition-false");

    private final String name;

    AssumeCase(String pName) {
      name = pName;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  public enum KeyDef {
    INVARIANT("invariant", ElementType.NODE, "invariant", "string"),
    INVARIANTSCOPE("invariant.scope", ElementType.NODE, "invariant.scope", "string"),
    NAMED("named", ElementType.NODE, "namedValue", "string"),
    LABEL("label", ElementType.NODE, "label", "string"),
    NODETYPE("nodetype", ElementType.NODE, "nodeType", "string", NodeType.ONPATH),
    ISFRONTIERNODE("frontier", ElementType.NODE, "isFrontierNode", "boolean", false),
    ISVIOLATIONNODE("violation", ElementType.NODE, "isViolationNode", "boolean", false),
    ISENTRYNODE("entry", ElementType.NODE, "isEntryNode", "boolean", false),
    ISSINKNODE("sink", ElementType.NODE, "isSinkNode", "boolean", false),
    ISCYCLEHEAD("cyclehead", ElementType.NODE, "isCycleHead", "boolean", false),
    ENTERLOOPHEAD("enterLoopHead", ElementType.EDGE, "enterLoopHead", "boolean", false),
    VIOLATEDPROPERTY("violatedProperty", ElementType.NODE, "violatedProperty", "string"),
    THREADNAME("threadName", ElementType.EDGE, "threadName", "string"),
    THREADID("threadId", ElementType.EDGE, "threadId", "string"),
    CREATETHREAD("createThread", ElementType.EDGE, "createThread", "string"),
    SOURCECODELANGUAGE("sourcecodelang", ElementType.GRAPH, "sourcecodeLanguage", "string"),
    PROGRAMFILE("programfile", ElementType.GRAPH, "programFile", "string"),
    PROGRAMHASH("programhash", ElementType.GRAPH, "programHash", "string"),
    SPECIFICATION("specification", ElementType.GRAPH, "specification", "string"),
    ARCHITECTURE("architecture", ElementType.GRAPH, "architecture", "string"),
    PRODUCER("producer", ElementType.GRAPH, "producer", "string"),
    CREATIONTIME("creationtime", ElementType.GRAPH, "creationTime", "string"),
    SOURCECODE("sourcecode", ElementType.EDGE, "sourcecode", "string"),
    STARTLINE("startline", ElementType.EDGE, "startline", "int"),
    ENDLINE("endline", ElementType.EDGE, "endline", "int"),
    OFFSET("startoffset", ElementType.EDGE, "startoffset", "int"),
    ENDOFFSET("endoffset", ElementType.EDGE, "endoffset", "int"),
    ORIGINFILE("originfile", ElementType.EDGE, "originFileName", "string"),
    LINECOLS("lineCols", ElementType.EDGE, "lineColSet", "string"),
    CONTROLCASE("control", ElementType.EDGE, "control", "string"),
    ASSUMPTION("assumption", ElementType.EDGE, "assumption", "string"),
    ASSUMPTIONRESULTFUNCTION(
        "assumption.resultfunction", ElementType.EDGE, "assumption.resultfunction", "string"),
    ASSUMPTIONSCOPE("assumption.scope", ElementType.EDGE, "assumption.scope", "string"),
    FUNCTIONENTRY("enterFunction", ElementType.EDGE, "enterFunction", "string"),
    FUNCTIONEXIT("returnFrom", ElementType.EDGE, "returnFromFunction", "string"),
    CFAPREDECESSORNODE("predecessor", ElementType.EDGE, "predecessor", "string"),
    CFASUCCESSORNODE("successor", ElementType.EDGE, "successor", "string"),
    WITNESS_TYPE("witness-type", ElementType.GRAPH, "witness-type", "string"),
    INPUTWITNESSHASH("inputwitnesshash", ElementType.GRAPH, "inputWitnessHash", "string"),

    // KeyDefs for extended witness format:
    NOTE("note", ElementType.EDGE, "note", "string"),
    WARNING("warning", ElementType.EDGE, "warning", "string"),
    DECL("declaration", ElementType.EDGE, "declaration", "boolean", false);

    public final String id;
    public final ElementType keyFor;
    public final String attrName;
    public final String attrType;

    /** The defaultValue is non-null, iff existent. */
    @Nullable public final String defaultValue;

    KeyDef(String pId, ElementType pKeyFor, String pAttrName, String pAttrType) {
      this(pId, pKeyFor, pAttrName, pAttrType, null);
    }

    KeyDef(
        String pId,
        ElementType pKeyFor,
        String pAttrName,
        String pAttrType,
        @Nullable Object pDefaultValue) {
      id = Preconditions.checkNotNull(pId);
      keyFor = Preconditions.checkNotNull(pKeyFor);
      attrName = Preconditions.checkNotNull(pAttrName);
      attrType = Preconditions.checkNotNull(pAttrType);
      defaultValue = pDefaultValue == null ? null : pDefaultValue.toString();
    }

    @Override
    public String toString() {
      return id;
    }
  }

  public enum ElementType {
    GRAPH,
    EDGE,
    NODE;

    @Override
    public String toString() {
      return Ascii.toLowerCase(name());
    }

    public static ElementType parse(String pElementType) {
      return ElementType.valueOf(Ascii.toUpperCase(pElementType));
    }
  }

  public enum NodeFlag {
    ISFRONTIER(KeyDef.ISFRONTIERNODE),
    ISVIOLATION(KeyDef.ISVIOLATIONNODE),
    ISENTRY(KeyDef.ISENTRYNODE),
    ISSINKNODE(KeyDef.ISSINKNODE),
    ISCYCLEHEAD(KeyDef.ISCYCLEHEAD);

    public final KeyDef key;

    NodeFlag(KeyDef pKey) {
      key = pKey;
    }

    private static final ImmutableMap<String, NodeFlag> stringToFlagMap =
        Maps.uniqueIndex(Arrays.asList(NodeFlag.values()), flag -> flag.key.id);

    public static NodeFlag getNodeFlagByKey(final String key) {
      return stringToFlagMap.get(key);
    }
  }

  public enum WitnessType {
    VIOLATION_WITNESS("violation_witness"),
    CORRECTNESS_WITNESS("correctness_witness");

    public final String text;

    WitnessType(String pText) {
      text = pText;
    }

    @Override
    public String toString() {
      return text;
    }

    public static Optional<WitnessType> tryParse(String pTextualRepresentation) {
      for (WitnessType element : values()) {
        if (element.text.equals(pTextualRepresentation)) {
          return Optional.of(element);
        }
      }
      return switch (pTextualRepresentation) {
        case "FALSE", "false_witness" -> Optional.of(VIOLATION_WITNESS);
        case "TRUE", "true_witness" -> Optional.of(CORRECTNESS_WITNESS);
        default -> Optional.empty();
      };
    }
  }

  public enum NodeType {
    ANNOTATION("annotation"),
    ONPATH("path");

    public final String text;

    NodeType(String pText) {
      text = pText;
    }

    @Override
    public String toString() {
      return text;
    }

    public static NodeType fromString(String nodeTypeString) {
      return valueOf(Ascii.toLowerCase(nodeTypeString.trim()));
    }
  }

  public static final NodeType defaultNodeType = NodeType.ONPATH;

  public enum GraphMLTag {
    NODE("node"),
    DATA("data"),
    KEY("key"),
    GRAPH("graph"),
    DEFAULT("default"),
    EDGE("edge");

    public final String text;

    GraphMLTag(String pText) {
      text = pText;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  /**
   * Compute SHA1 hash of file content.
   *
   * @return A lowercase base16 encoded SHA256 hash.
   */
  public static String computeHash(Path pPath) throws IOException {
    HashCode hash = MoreFiles.asByteSource(pPath).hash(Hashing.sha256());
    return BaseEncoding.base16().lowerCase().encode(hash.asBytes());
  }

  /**
   * Compute SHA1 hash of file content.
   *
   * @return A lowercase base16 encoded SHA hash.
   */
  public static String computeSha1Hash(Path pPath) throws IOException {
    @SuppressWarnings("deprecation") // SHA1 is required by witness format
    HashCode hash = MoreFiles.asByteSource(pPath).hash(Hashing.sha1());
    return BaseEncoding.base16().lowerCase().encode(hash.asBytes());
  }

  public static class GraphMlBuilder {

    private final Document doc;
    private final Element graph;
    private final Set<KeyDef> definedKeys = EnumSet.noneOf(KeyDef.class);
    private final Map<KeyDef, Node> keyDefsToAppend = new EnumMap<>(KeyDef.class);

    public GraphMlBuilder(
        WitnessType pGraphType,
        @Nullable String pDefaultSourceFileName,
        CFA pCfa,
        VerificationTaskMetaData pVerificationTaskMetaData)
        throws ParserConfigurationException, DOMException, IOException {
      DocumentBuilderFactory docFactory = XMLUtils.getSecureDocumentBuilderFactory(true);

      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      doc = docBuilder.newDocument();
      Element root = doc.createElement("graphml");
      doc.appendChild(root);
      root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      root.setAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns");

      defineKey(KeyDef.ORIGINFILE, Optional.of(pDefaultSourceFileName));
      for (KeyDef keyDef : KeyDef.values()) {
        if (keyDef.keyFor == ElementType.GRAPH) {
          defineKey(keyDef);
        }
      }

      graph = doc.createElement("graph");
      root.appendChild(graph);
      graph.setAttribute("edgedefault", "directed");
      graph.appendChild(createDataElement(KeyDef.WITNESS_TYPE, pGraphType.toString()));
      graph.appendChild(
          createDataElement(KeyDef.SOURCECODELANGUAGE, pCfa.getLanguage().toString()));
      graph.appendChild(
          createDataElement(KeyDef.PRODUCER, pVerificationTaskMetaData.getProducerString()));

      int nSpecs = 0;
      for (Property property : pVerificationTaskMetaData.getProperties()) {
        graph.appendChild(createDataElement(KeyDef.SPECIFICATION, property.toFullString(pCfa)));
        ++nSpecs;
      }

      for (Path specFile : pVerificationTaskMetaData.getNonPropertySpecificationFiles()) {
        graph.appendChild(
            createDataElement(
                KeyDef.SPECIFICATION, Files.readString(specFile, StandardCharsets.UTF_8).trim()));
        ++nSpecs;
      }

      if (nSpecs == 0) {
        graph.appendChild(createDataElement(KeyDef.SPECIFICATION, "TRUE"));
      }

      for (Path inputWitness : pVerificationTaskMetaData.getInputWitnessFiles()) {
        graph.appendChild(createDataElement(KeyDef.INPUTWITNESSHASH, computeHash(inputWitness)));
      }

      for (Path programFile : pCfa.getFileNames()) {
        graph.appendChild(createDataElement(KeyDef.PROGRAMFILE, programFile.toString()));
      }
      for (Path programFile : pCfa.getFileNames()) {
        graph.appendChild(createDataElement(KeyDef.PROGRAMHASH, computeHash(programFile)));
      }

      graph.appendChild(
          createDataElement(KeyDef.ARCHITECTURE, getArchitecture(pCfa.getMachineModel())));
      graph.appendChild(createDataElement(KeyDef.CREATIONTIME, getCreationTime()));
    }

    private void defineKey(KeyDef pKeyDef) {
      defineKey(pKeyDef, Optional.empty());
    }

    private void defineKey(KeyDef pKeyDef, Optional<String> pOverrideDefaultValue) {
      if (definedKeys.add(pKeyDef)) {
        keyDefsToAppend.put(pKeyDef, createKeyDefElement(pKeyDef, pOverrideDefaultValue));
      }
    }

    private Element createElement(GraphMLTag tag) {
      return doc.createElement(tag.toString());
    }

    private Element createDataElement(final KeyDef key, final String value) {
      defineKey(key);
      Element result = createElement(GraphMLTag.DATA);
      result.setAttribute("key", key.id);
      result.setTextContent(value);
      return result;
    }

    public Element createEdgeElement(final String from, final String to) {
      Element result = createElement(GraphMLTag.EDGE);
      result.setAttribute("source", from);
      result.setAttribute("target", to);
      graph.appendChild(result);
      return result;
    }

    public Element createNodeElement(String nodeId, NodeType nodeType) {
      Element result = createElement(GraphMLTag.NODE);
      result.setAttribute("id", nodeId);

      if (nodeType != defaultNodeType) {
        addDataElementChild(result, KeyDef.NODETYPE, nodeType.toString());
      }

      graph.appendChild(result);

      return result;
    }

    private Element createKeyDefElement(KeyDef pKeyDef, Optional<String> pDefaultValue) {

      Element result = createElement(GraphMLTag.KEY);

      result.setAttribute("id", pKeyDef.id);
      result.setAttribute("for", pKeyDef.keyFor.toString());
      result.setAttribute("attr.name", pKeyDef.attrName);
      result.setAttribute("attr.type", pKeyDef.attrType);

      String defaultValue = pDefaultValue.orElse(pKeyDef.defaultValue);
      if (defaultValue != null) {
        Element defaultValueElement = createElement(GraphMLTag.DEFAULT);
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
      Node root = doc.getFirstChild();
      Node insertionLocation = root.getFirstChild();
      for (Node graphMLKeyDefNode : Iterables.consumingIterable(keyDefsToAppend.values())) {
        while (insertionLocation != null
            && insertionLocation.getNodeName().equals(GraphMLTag.KEY.toString())) {
          insertionLocation = insertionLocation.getNextSibling();
        }
        root.insertBefore(graphMLKeyDefNode, insertionLocation);
      }

      try {
        pTarget.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");

        TransformerFactory tf = XMLUtils.getSecureTransformerFactory();

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(doc), new StreamResult(CharStreams.asWriter(pTarget)));
      } catch (TransformerException ex) {
        for (Throwable cause : Throwables.getCausalChain(ex)) {
          Throwables.throwIfInstanceOf(cause, IOException.class);
        }
        throw new RuntimeException("Error while writing witness.", ex);
      }
    }
  }

  public static boolean handleAsEpsilonEdge(
      CFAEdge pEdge, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    if (pAdditionalInfo != null && !pAdditionalInfo.getInfos().isEmpty()) {
      return false;
    }
    return handleAsEpsilonEdge(pEdge);
  }

  /**
   * This method checks whether an edge qualifies as epsilon edge. Epsilon edges are irrelevant
   * edges that are not required in the witness.
   * <li>global declarations (there is no other path possible in the CFA),
   * <li>CPAchecker-internal temporary variable declarations (irrelevant for the witness),
   * <li>blank edges and function summary edges (not required for a path in the witness).
   */
  public static boolean handleAsEpsilonEdge(CFAEdge edge) {
    return handleAsEpsilonEdge0(edge) && edge.getSuccessor().getNumLeavingEdges() > 0;
  }

  private static boolean handleAsEpsilonEdge0(CFAEdge edge) {
    switch (edge) {
      case BlankEdge blankEdge -> {
        if (isMainFunctionEntry(edge)) {
          return false;
        }
        if (edge.getSuccessor() instanceof FunctionExitNode functionExitNode) {
          return isEmptyStub(functionExitNode.getEntryNode());
        }
        if (AutomatonGraphmlCommon.treatAsTrivialAssume(edge)) {
          return false;
        }
        if (AutomatonGraphmlCommon.treatAsWhileTrue(edge)) {
          return false;
        }
        return true;
      }
      case CFunctionCallEdge cFunctionCallEdge -> {
        return isEmptyStub(cFunctionCallEdge.getSuccessor());
      }
      case CFunctionReturnEdge cFunctionReturnEdge -> {
        return isEmptyStub(cFunctionReturnEdge.getFunctionEntry());
      }
      case CDeclarationEdge declEdge -> {
        CDeclaration decl = declEdge.getDeclaration();
        if (decl instanceof CFunctionDeclaration) {
          return true;
        } else if (decl instanceof CTypeDeclaration) {
          return true;
        } else if (decl instanceof CVariableDeclaration varDecl) {
          if (Ascii.toUpperCase(varDecl.getName()).startsWith(CPACHECKER_TMP_PREFIX)) {
            return true; // Dirty hack; would be better if these edges had no file location
          }
          if (isSplitDeclaration(edge)) {
            return true;
          }
        }
      }
      case CFunctionSummaryStatementEdge summaryEdge -> {
        return true;
      }
      case AStatementEdge statementEdge -> {
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionStatement expressionStatement) {
          AExpression expression = expressionStatement.getExpression();
          if ((expression instanceof AIdExpression idExpression)
              && Ascii.toUpperCase(idExpression.getName()).startsWith(CPACHECKER_TMP_PREFIX)) {
            return true;
          }
        } else {
          return isTmpPartOfTernaryExpressionAssignment(statementEdge);
        }
      }
      case null /*TODO check if null is necessary*/, default -> {}
    }

    return false;
  }

  private static boolean isTmpPartOfTernaryExpressionAssignment(AStatementEdge statementEdge) {
    AStatement statement = statementEdge.getStatement();
    if (!(statement instanceof AExpressionAssignmentStatement tmpAssignment)) {
      return false;
    }

    ALeftHandSide lhs = tmpAssignment.getLeftHandSide();
    if (!(lhs instanceof AIdExpression idExpression)) {
      return false;
    }

    if (!Ascii.toUpperCase(idExpression.getName()).startsWith(CPACHECKER_TMP_PREFIX)) {
      return false;
    }
    FluentIterable<CFAEdge> successorEdges = statementEdge.getSuccessor().getLeavingEdges();
    if (successorEdges.size() != 1) {
      return false;
    }
    CFAEdge successorEdge = successorEdges.iterator().next();
    if (!(successorEdge instanceof AStatementEdge successorStatementEdge)) {
      return false;
    }
    FileLocation edgeLoc = statementEdge.getFileLocation();
    FileLocation successorEdgeLoc = successorEdge.getFileLocation();
    if (!(successorEdgeLoc.getNodeOffset() <= edgeLoc.getNodeOffset()
        && successorEdgeLoc.getNodeOffset() + successorEdgeLoc.getNodeLength()
            >= edgeLoc.getNodeOffset() + edgeLoc.getNodeLength())) {
      return false;
    }

    AStatement successorStatement = successorStatementEdge.getStatement();
    if (!(successorStatement instanceof AExpressionAssignmentStatement targetAssignment)) {
      return false;
    }

    return targetAssignment.getRightHandSide().equals(idExpression);
  }

  public static boolean isMainFunctionEntry(CFAEdge pEdge) {
    return isFunctionStartDummyEdge(pEdge)
        && !(pEdge.getPredecessor() instanceof FunctionEntryNode);
  }

  public static boolean isFunctionStartDummyEdge(CFAEdge pEdge) {
    return pEdge instanceof BlankEdge edge
        && edge.getDescription().equals("Function start dummy edge");
  }

  public static String getArchitecture(MachineModel pMachineModel) {
    final String architecture =
        switch (pMachineModel) {
          case LINUX32 -> "32bit";
          case LINUX64 -> "64bit";
          default -> pMachineModel.toString();
        };
    return architecture;
  }

  public static String getCreationTime() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    return now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  public static Set<FileLocation> getFileLocationsFromCfaEdge(
      CFAEdge pEdge, FunctionEntryNode pMainEntry, CFAEdgeWithAdditionalInfo pAdditionalInfo) {
    if (handleAsEpsilonEdge(pEdge, pAdditionalInfo)) {
      return ImmutableSet.of();
    }
    return getFileLocationsFromCfaEdge0(pEdge, pMainEntry);
  }

  public static Set<FileLocation> getFileLocationsFromCfaEdge(
      CFAEdge pEdge, FunctionEntryNode pMainEntry) {
    if (handleAsEpsilonEdge(pEdge)) {
      return ImmutableSet.of();
    }
    return getFileLocationsFromCfaEdge0(pEdge, pMainEntry);
  }

  public static Set<FileLocation> getFileLocationsFromCfaEdge0(
      CFAEdge pEdge, FunctionEntryNode pMainEntry) {

    if (isMainFunctionEntry(pEdge)
        && pMainEntry.getFunctionName().equals(pEdge.getSuccessor().getFunctionName())) {
      FileLocation location = pMainEntry.getFileLocation();
      if (location.isRealLocation()) {
        location =
            new FileLocation(
                location.getFileName(),
                location.getNiceFileName(),
                location.getNodeOffset(),
                pMainEntry.getFunctionDefinition().toString().length(),
                location.getStartingLineNumber(),
                location.getStartingLineNumber(),
                location.getStartColumnInLine(),
                location.getEndColumnInLine(),
                location.getStartingLineInOrigin(),
                location.getStartingLineInOrigin(),
                location.isOffsetRelatedToOrigin());
      }
      Set<FileLocation> result = Sets.newHashSet(location);
      for (AParameterDeclaration param : pMainEntry.getFunctionDefinition().getParameters()) {
        result.add(param.getFileLocation());
      }
      return result;
    }
    if (pEdge instanceof AStatementEdge statementEdge) {
      FileLocation statementLocation = statementEdge.getStatement().getFileLocation();
      if (statementLocation.isRealLocation()) {
        return Collections.singleton(statementLocation);
      }
    }
    if (pEdge instanceof FunctionCallEdge functionCallEdge) {
      AFunctionCall call = functionCallEdge.getFunctionCall();
      if (call instanceof AFunctionCallAssignmentStatement statement) {
        FileLocation callLocation = statement.getRightHandSide().getFileLocation();
        if (callLocation.isRealLocation()) {
          return Collections.singleton(callLocation);
        }
      }
    }
    if (pEdge instanceof AssumeEdge assumeEdge) {
      FileLocation location = assumeEdge.getFileLocation();
      if (isDefaultCase(assumeEdge)) {
        CFANode successorNode = assumeEdge.getSuccessor();
        FileLocation switchLocation =
            Iterables.getOnlyElement(successorNode.getLeavingEdges()).getFileLocation();
        if (switchLocation.isRealLocation()) {
          location = switchLocation;
        } else {
          SwitchDetector switchDetector = new SwitchDetector(assumeEdge);
          CFATraversal.dfs().backwards().traverseOnce(assumeEdge.getSuccessor(), switchDetector);
          List<FileLocation> caseLocations =
              transformedImmutableListCopy(
                  switchDetector.getEdgesBackwardToSwitchNode(), CFAEdge::getFileLocation);
          location = FileLocation.merge(caseLocations);
        }
      }
      if (location.isRealLocation()) {
        return Collections.singleton(location);
      }
    }
    if (pEdge instanceof ADeclarationEdge declarationEdge) {
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof AVariableDeclaration) {
        return Collections.singleton(declaration.getFileLocation());
      }
    }
    return CFAUtils.getFileLocationsFromCfaEdge(pEdge);
  }

  public static boolean isPartOfSwitchStatement(AssumeEdge pAssumeEdge) {
    SwitchDetector switchDetector = new SwitchDetector(pAssumeEdge);
    CFATraversal.dfs().backwards().traverseOnce(pAssumeEdge.getSuccessor(), switchDetector);
    return switchDetector.switchDetected();
  }

  public static boolean isDefaultCase(CFAEdge pEdge) {
    if (!(pEdge instanceof AssumeEdge assumeEdge)) {
      return false;
    }

    if (assumeEdge.getTruthAssumption()) {
      return false;
    }
    FluentIterable<CFAEdge> successorEdges = assumeEdge.getSuccessor().getLeavingEdges();
    if (successorEdges.size() != 1) {
      return false;
    }
    CFAEdge successorEdge = successorEdges.iterator().next();
    return successorEdge instanceof BlankEdge blankSuccessorEdge
        && blankSuccessorEdge.getDescription().equals("default");
  }

  public static class SwitchDetector implements CFAVisitor {

    private final AExpression assumeExpression;

    private final AExpression switchOperand;

    private final List<AssumeEdge> edgesBackwardToSwitchNode = new ArrayList<>();

    private CFANode switchNode = null;

    public SwitchDetector(AssumeEdge pAssumeEdge) {
      assumeExpression = pAssumeEdge.getExpression();
      if (assumeExpression instanceof ABinaryExpression aBinaryExpression) {
        switchOperand = aBinaryExpression.getOperand1();
      } else {
        switchOperand = assumeExpression;
      }
    }

    public boolean switchDetected() {
      return switchNode != null;
    }

    public List<AssumeEdge> getEdgesBackwardToSwitchNode() {
      Preconditions.checkState(switchDetected());
      return Collections.unmodifiableList(edgesBackwardToSwitchNode);
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      if (switchOperand == assumeExpression) {
        return TraversalProcess.ABORT;
      }
      if (pEdge instanceof AssumeEdge edge) {
        AExpression expression = edge.getExpression();
        if (!(expression instanceof ABinaryExpression aBinaryExpression)) {
          return TraversalProcess.ABORT;
        }
        AExpression operand = aBinaryExpression.getOperand1();
        if (!operand.equals(switchOperand)) {
          return TraversalProcess.ABORT;
        }
        edgesBackwardToSwitchNode.add(edge);
        return TraversalProcess.CONTINUE;
      } else if (pEdge instanceof BlankEdge edge) {
        String switchPrefix = "switch (";
        if (edge.getDescription().equals(switchPrefix + switchOperand + ")")
            && edge.getFileLocation().isRealLocation()
            && assumeExpression.getFileLocation().getNodeOffset()
                == edge.getFileLocation().getNodeOffset() + switchPrefix.length()) {
          switchNode = edge.getSuccessor();
          return TraversalProcess.ABORT;
        }
        return TraversalProcess.CONTINUE;
      }
      return TraversalProcess.SKIP;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      return TraversalProcess.CONTINUE;
    }
  }

  /**
   * Checks if the given edge is a variable declaration edge without initializer that has the same
   * file location as its sole successor edge, which in turn provides the initialization of the
   * declared variable.
   *
   * <p>Basically, this detects the first part of declarations with initializers that we split
   * during CFA construction.
   *
   * @param pEdge the edge to check.
   * @return {@code true} if the edge is part of a split declaration, {@code false} otherwise.
   */
  public static boolean isSplitDeclaration(CFAEdge pEdge) {
    if (pEdge instanceof ADeclarationEdge declEdge) {
      ADeclaration decl = declEdge.getDeclaration();
      if (decl instanceof AFunctionDeclaration) {
        return false;
      } else if (decl instanceof CTypeDeclaration) {
        return false;
      } else if (decl instanceof AVariableDeclaration varDecl) {
        CFANode successor = pEdge.getSuccessor();
        boolean intermediateDeclarationsExpected = true;
        boolean cont = true;
        while (cont) {
          cont = false;
          Iterator<CFAEdge> leavingEdges = successor.getLeavingEdges().iterator();
          if (!leavingEdges.hasNext()) {
            return false;
          }
          CFAEdge successorEdge = leavingEdges.next();
          if (leavingEdges.hasNext()) {
            CFAEdge alternativeSuccessorEdge = leavingEdges.next();
            if (leavingEdges.hasNext()) {
              return false;
            } else if (successorEdge instanceof FunctionCallEdge
                && alternativeSuccessorEdge instanceof CFunctionSummaryStatementEdge) {
              successorEdge = alternativeSuccessorEdge;
            } else if (successorEdge instanceof CFunctionSummaryStatementEdge
                && alternativeSuccessorEdge instanceof FunctionCallEdge) {
              // nothing to do
            } else {
              return false;
            }
          }

          if (successorEdge.getFileLocation().equals(pEdge.getFileLocation())) {
            AAssignment assignment = null;
            if (successorEdge instanceof FunctionCallEdge functionCallEdge) {
              FunctionSummaryEdge summaryEdge = functionCallEdge.getSummaryEdge();
              AFunctionCall functionCall = functionCallEdge.getFunctionCall();
              if (functionCall instanceof AAssignment aAssignment) {
                assignment = aAssignment;
                successorEdge = summaryEdge;
              }
            } else if (successorEdge instanceof AStatementEdge statementEdge) {
              intermediateDeclarationsExpected = false;
              if (statementEdge.getStatement() instanceof AAssignment aAssignment) {
                assignment = aAssignment;
              }
            }
            if (assignment != null) {
              ALeftHandSide leftHandSide = assignment.getLeftHandSide();
              if (leftHandSide instanceof AIdExpression lhs) {
                if (lhs.getDeclaration() != null && lhs.getDeclaration().equals(varDecl)) {
                  return true;
                }
                // The current edge may just be the matching initialization of a preceding
                // split declaration, e.g. in a line originally written as "int x = 0, y = 1";
                cont = true;
                successor = successorEdge.getSuccessor();
              }
            }
          }

          if (intermediateDeclarationsExpected
              && successorEdge instanceof ADeclarationEdge otherDeclEdge) {
            if (otherDeclEdge.getDeclaration() instanceof AVariableDeclaration) {
              // The current edge may just be the matching declaration of a preceding
              // split declaration, e.g. in a line originally written as "int x = 0, y = 1";
              cont = true;
              successor = successorEdge.getSuccessor();
            }
          }
        }
      }
    }
    return false;
  }

  public static boolean isSplitAssumption(CFAEdge pEdge) {
    return pEdge instanceof AssumeEdge assumeEdge && assumeEdge.isArtificialIntermediate();
  }

  public static boolean isPointerCallAssumption(CFAEdge pEdge) {
    if (!(pEdge instanceof AssumeEdge assumeEdge)) {
      return false;
    }

    if (!assumeEdge.getTruthAssumption()) {
      assumeEdge = CFAUtils.getComplimentaryAssumeEdge(assumeEdge);
    }
    AExpression expression = assumeEdge.getExpression();
    if (!(expression instanceof ABinaryExpression binaryExpression)) {
      return false;
    }

    Set<String> namesOnEdge =
        FluentIterable.of(binaryExpression.getOperand1(), binaryExpression.getOperand2())
            .filter(AUnaryExpression.class)
            .filter(unaryExpr -> unaryExpr.getOperator() == UnaryOperator.AMPER)
            .transform(AUnaryExpression::getOperand)
            .filter(AIdExpression.class)
            .transform(AIdExpression::getName)
            .toSet();
    if (namesOnEdge.isEmpty()) {
      return false;
    }
    return !assumeEdge
        .getSuccessor()
        .getLeavingEdges()
        .filter(e -> e.getFileLocation().equals(pEdge.getFileLocation()))
        .filter(FunctionCallEdge.class)
        .filter(e -> namesOnEdge.contains(e.getSuccessor().getFunctionName()))
        .isEmpty();
  }

  public static boolean isPartOfTerminatingAssumption(CFAEdge pEdge) {
    if (!(pEdge instanceof AssumeEdge assumeEdge)) {
      return false;
    }

    AssumeEdge siblingEdge = CFAUtils.getComplimentaryAssumeEdge(assumeEdge);
    if (assumeEdge.getSuccessor() instanceof CFATerminationNode
        || siblingEdge.getSuccessor() instanceof CFATerminationNode) {
      return true;
    }
    return isTerminatingAssumption(assumeEdge) || isTerminatingAssumption(siblingEdge);
  }

  private static boolean isTerminatingAssumption(CFAEdge pEdge) {
    if (!(pEdge instanceof AssumeEdge assumeEdge)) {
      return false;
    }

    // Check if the subsequent edge matches the termination-value assignment
    FluentIterable<CFAEdge> leavingEdges = assumeEdge.getSuccessor().getLeavingEdges();
    if (leavingEdges.size() != 1) {
      return false;
    }
    CFAEdge leavingEdge = leavingEdges.iterator().next();
    if (!(leavingEdge instanceof AStatementEdge terminationValueAssignmentEdge)) {
      return false;
    }

    AStatement statement = terminationValueAssignmentEdge.getStatement();
    if (!(statement instanceof AExpressionAssignmentStatement terminationValueAssignment)) {
      return false;
    }

    ALeftHandSide lhs = terminationValueAssignment.getLeftHandSide();
    AExpression rhs = terminationValueAssignment.getRightHandSide();
    if (!(lhs instanceof AIdExpression idExpression && rhs instanceof ALiteralExpression value)) {
      return false;
    }

    if (!Ascii.toUpperCase(idExpression.getName()).startsWith(CPACHECKER_TMP_PREFIX)) {
      return false;
    }

    // Now check if this is followed by a terminating assume
    leavingEdges = terminationValueAssignmentEdge.getSuccessor().getLeavingEdges();
    if (leavingEdges.size() != 2) {
      return false;
    }
    Optional<CFAEdge> potentialTerminationValueAssumeEdge =
        leavingEdges.firstMatch(e -> e.getSuccessor() instanceof CFATerminationNode).toJavaUtil();
    if (!potentialTerminationValueAssumeEdge.isPresent()
        || !(potentialTerminationValueAssumeEdge.orElseThrow()
            instanceof AssumeEdge terminationValueAssumption)) {
      return false;
    }

    AExpression terminationValueAssumeExpression = terminationValueAssumption.getExpression();
    if (!(terminationValueAssumeExpression
        instanceof ABinaryExpression terminationValueAssumeBinExpr)) {
      return false;
    }

    List<AExpression> operands =
        Arrays.asList(
            terminationValueAssumeBinExpr.getOperand1(),
            terminationValueAssumeBinExpr.getOperand2());
    if (!operands.contains(idExpression)) {
      return false;
    }
    boolean flip = false;
    if (!operands.contains(value)) {
      flip = true;
    }
    ABinaryOperator operator = terminationValueAssumeBinExpr.getOperator();
    if (operator.equals(BinaryOperator.NOT_EQUALS)
        || operator.equals(
            org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.NOT_EQUALS)) {
      return flip == terminationValueAssumption.getTruthAssumption();
    }
    if (operator.equals(BinaryOperator.EQUALS)
        || operator.equals(
            org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.EQUALS)) {
      return flip ^ terminationValueAssumption.getTruthAssumption();
    }
    return false;
  }

  private static boolean isEmptyStub(FunctionEntryNode pEntryNode) {
    Iterator<CFAEdge> startEdges = pEntryNode.getLeavingEdges().iterator();
    if (!startEdges.hasNext()) {
      return false;
    }
    CFAEdge startEdge = startEdges.next();
    if (startEdges.hasNext() || !(startEdge instanceof BlankEdge)) {
      return false;
    }
    CFANode innerNode = startEdge.getSuccessor();
    Iterator<CFAEdge> defaultReturnEdges = innerNode.getLeavingEdges().iterator();
    if (!defaultReturnEdges.hasNext()) {
      return false;
    }
    CFAEdge defaultReturnEdge = defaultReturnEdges.next();
    if (defaultReturnEdges.hasNext() || !(defaultReturnEdge instanceof BlankEdge)) {
      return false;
    }
    return pEntryNode
        .getExitNode()
        .map(exitNode -> exitNode.equals(defaultReturnEdge.getSuccessor()))
        .orElse(false); // if there is no function exit node, the function cannot be a stub
  }

  public static boolean treatAsWhileTrue(CFAEdge pEdge) {
    CFANode pred = pEdge.getPredecessor();
    return pEdge instanceof BlankEdge
        && pred.getNumLeavingEdges() == 1
        && pred.getEnteringEdges()
            .filter(BlankEdge.class)
            .anyMatch(e -> e.getDescription().equals("while"));
  }

  private static boolean treatAsTrivialAssume(CFAEdge pEdge) {
    CFANode pred = pEdge.getPredecessor();
    if (pred.getNumLeavingEdges() != 1) {
      return false;
    }
    if (!(pEdge instanceof BlankEdge edge)) {
      return false;
    }

    if (!edge.getDescription().isEmpty()) {
      return false;
    }
    return !edge.getRawStatement().isEmpty()
        && !treatAsWhileTrue(pEdge)
        && !isFunctionStartDummyEdge(pEdge);
  }
}
