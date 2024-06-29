// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.io.MoreFiles;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * Enables to export all {@link CFA} {@link FunctionEntryNode}s, {@link CFANode}s, {@link CFAEdge}s
 * and relevant {@link CfaMetadata}.
 *
 * <p>The export format is JSON.
 */
public final class CfaToJson extends CfaJsonIO {
  private final CfaJsonData cfaJsonData;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    //    /* Collect all nodes and edges by traversing the CFA. */
    //
    //    NodeCollectingCFAVisitor nodeVisitor = new NodeCollectingCFAVisitor();
    //    EdgeCollectingCFAVisitor edgeVisitor = new EdgeCollectingCFAVisitor();
    //
    //    CFAVisitor visitor =
    //        new NodeCollectingCFAVisitor(new CompositeCFAVisitor(nodeVisitor, edgeVisitor));
    //
    //    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    //      CFATraversal.dfs().traverse(entryNode, visitor);
    //    }
    //
    //    // TODO
    //    for (FunctionEntryNode entryNode : cfa.entryNodes()) {
    //     .resolve("functions").resolve(entryNode.getFunctionName() + ".json");

    this.cfaJsonData =
        new CfaJsonData(cfa.nodes(), cfa.edges(), cfa.getAllFunctions(), cfa.getMetadata());
  }

  /**
   * Writes the {@link CFA} data.
   *
   * @param pJsonFilePath The path to the JSON file.
   * @throws IOException If an error with {@link FileOutputStream} or {@link JsonGenerator} occurs.
   */
  public void write(Path pJsonFilePath) throws IOException {
    /* Create any required directories. */
    MoreFiles.createParentDirectories(pJsonFilePath);

    /* Write the CFA data. */
    try (FileOutputStream fileOutputStream = new FileOutputStream(pJsonFilePath.toFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        JsonGenerator jsonGenerator =
            new JsonFactory().createGenerator(bufferedOutputStream, JsonEncoding.UTF8); ) {

      provideConfiguredCfaObjectMapper().writeValue(jsonGenerator, this.cfaJsonData);
    }

  //  private final static class CfaJsonExportDeserializer extends JsonDeserializer<CfaJsonExport> {
  //
  //    @Override
  //    public CfaJsonExport deserialize(JsonParser pJsonParser, DeserializationContext pContext)
  //        throws IOException {
  //      JsonNode rootNode = pJsonParser.getCodec().readTree(pJsonParser);
  //
  //      CfaMetadata metadata = deserializeMetadata(rootNode.get("metadata"), pJsonParser);
  //      //Set<CFANode> nodes = deserializeNodes(rootNode.get("nodes"), pJsonParser);
  //      //NavigableMap<String, FunctionEntryNode> functions =
  //      //    deserializeFunctions(rootNode.get("functions"));
  //      //Set<CFAEdge> edges = deserializeEdges(rootNode.get("edges"));
  //
  //      return new CfaJsonExport(null, null, null, null);
  //    }
  //
  //    private CfaMetadata deserializeMetadata(JsonNode pNode, JsonParser pJsonParser) throws
  // IOException {
  //      return pJsonParser.getCodec().readValue(pJsonParser, CfaMetadata.class);
  //    }
  //
  //    private Set<CFANode> deserializeNodes(JsonNode pNode, JsonParser pJsonParser) throws
  // IOException {
  //      HashSet<CFANode> nodes = new HashSet<>();
  //      for (JsonNode node : pNode) {
  //        nodes.add(pJsonParser.getCodec().readValue(pJsonParser, CFANode.class));
  //      }
  //      return nodes;
  //    }
  //
  //    private NavigableMap<String, FunctionEntryNode> deserializeFunctions(JsonNode pNode) {
  //      // TODO: Implement deserialization logic for nodes
  //      return null;
  //    }
  //
  //    private Set<CFAEdge> deserializeEdges(JsonNode pNode) {
  //      // TODO: Implement deserialization logic for edges
  //      return null;
  //    }
  //  }
}
