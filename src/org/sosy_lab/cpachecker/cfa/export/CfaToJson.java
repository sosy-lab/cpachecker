// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.io.MoreFiles;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.CompositeCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;

/**
 * Enables the export of all {@link CFA} {@link FunctionEntryNode}s, {@link CFANode}s, {@link
 * CFAEdge}s and relevant Metadata.
 *
 * <p>The export format is JSON.
 */
public final class CfaToJson {
  private final Set<CFANode> nodes;
  private final List<CFAEdge> edges;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    /* Collect all nodes and edges by traversing the CFA. */

    NodeCollectingCFAVisitor nodeVisitor = new NodeCollectingCFAVisitor();
    EdgeCollectingCFAVisitor edgeVisitor = new EdgeCollectingCFAVisitor();

    CFAVisitor visitor =
        new NodeCollectingCFAVisitor(new CompositeCFAVisitor(nodeVisitor, edgeVisitor));

    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
      CFATraversal.dfs().traverse(entryNode, visitor);
    }

    nodes = nodeVisitor.getVisitedNodes();
    edges = edgeVisitor.getVisitedEdges();
  }

  /**
   * Writes the {@link CFA} data.
   *
   * @param pOutdir Directory to which the JSON files are to be written.
   * @throws IOException If an error with {@link FileOutputStream} or {@link JsonGenerator} occurs.
   */
  public void write(Path pOutdir) throws IOException {
    /* Acquire a configured ObjectMapper for CFA serialization. */
    ObjectMapper cfaObjectMapper = provideConfiguredCfaObjectMapper();

    /* Define the file path and create any required directories. */
    Path jsonFilePath = pOutdir.resolve("cfa.json");
    MoreFiles.createParentDirectories(jsonFilePath);

    /* Write the CFA data. */
    try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFilePath.toFile());
        JsonGenerator jsonGenerator =
            new JsonFactory().createGenerator(fileOutputStream, JsonEncoding.UTF8); ) {

      jsonGenerator.writeStartObject();

      /* Write all Nodes. */
      jsonGenerator.writeFieldName("nodes");
      cfaObjectMapper.writeValue(jsonGenerator, nodes);

      /* Write all Edges. */
      jsonGenerator.writeFieldName("edges");
      cfaObjectMapper.writeValue(jsonGenerator, edges);

      jsonGenerator.writeEndObject();
    }
  }

  /**
   * Configures and provides an instance of {@link ObjectMapper} for CFA serialization.
   *
   * @return The configured {@link ObjectMapper} instance which only maps fields and uses
   *     indentation and newlines.
   */
  private static ObjectMapper provideConfiguredCfaObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();

    /* Only map fields of objects. */
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

    /* Enable serialization with indentation and newlines. */
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    /* Add mixins for FunctionEntryNode, FunctionExitNode and CFAEdge. */
    objectMapper.addMixIn(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    objectMapper.addMixIn(FunctionExitNode.class, FunctionExitNodeMixin.class);
    objectMapper.addMixIn(CFAEdge.class, CfaEdgeMixin.class);

    return objectMapper;
  }

  /**
   * This class is a mixin for {@link FunctionEntryNode}.
   *
   * <p>It specifies the serializer for {@link FunctionExitNode}.
   */
  private abstract static class FunctionEntryNodeMixin {

    @JsonSerialize(using = CfaNodeSerializer.class)
    private FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link FunctionExitNode}.
   *
   * <p>It specifies the serializer for {@link FunctionEntryNode}.
   */
  private abstract static class FunctionExitNodeMixin {

    @JsonSerialize(using = CfaNodeSerializer.class)
    private FunctionEntryNode entryNode;
  }

  /**
   * This interface is a mixin for {@link CFAEdge}.
   *
   * <p>It specifies the serializers for the predecessor and successor {@link CFANode}s.
   */
  private interface CfaEdgeMixin {

    @JsonSerialize(using = CfaNodeSerializer.class)
    CFANode getPredecessor();

    @JsonSerialize(using = CfaNodeSerializer.class)
    CFANode getSuccessor();
  }

  /* A custom JSON serializer for serializing CFANode objects as their node numbers. */
  private static class CfaNodeSerializer extends JsonSerializer<CFANode> {

    /**
     * Serializes a {@link CFANode} object to JSON.
     *
     * <p>It serializes a {@link CFANode} object as its node number.
     *
     * @param pCfaNode The {@link CFANode} object to be serialized.
     * @param pJsonGenerator The JSON generator to write the serialized JSON to.
     * @param pSerializerProvider The serializer provider.
     * @throws IOException If an I/O error occurs while writing the JSON.
     */
    @Override
    public void serialize(
        CFANode pCfaNode, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
        throws IOException {
      pJsonGenerator.writeNumber(pCfaNode.getNodeNumber());
    }
  }
}
    // TODO for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    // .resolve("functions").resolve(entryNode.getFunctionName() + ".json");
