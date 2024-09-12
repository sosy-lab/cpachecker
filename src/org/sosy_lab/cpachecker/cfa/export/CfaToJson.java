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
import com.google.common.collect.TreeMultimap;
import com.google.common.io.MoreFiles;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * Enables to export all {@link CFA} {@link CFANode}s, {@link CFAEdge}s, {@link FunctionEntryNode}s,
 * {@link Partition}s and relevant {@link CfaMetadata}.
 *
 * <p>The export format is JSON.
 *
 * <p>It uses the {@link CfaJsonModule.CfaJsonData} record to store the {@link CFA} data.
 */
public final class CfaToJson {
  private final CfaJsonModule.CfaJsonData cfaJsonData;

  /**
   * Constructs the {@link CfaJsonModule.CfaJsonData} field with the given {@link CFA}.
   *
   * @param pCfa The Control Flow Automaton (CFA) to be converted to JSON.
   */
  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    /* Create a mapping of function names to nodes of the corresponding function.
    This collection is needed for the creation of a MutableCFA during deserialization.*/
    TreeMultimap<String, CFANode> nodes =
        cfa.nodes().stream()
            .collect(
                TreeMultimap::create,
                (multimap, node) -> multimap.put(node.getFunctionName(), node),
                TreeMultimap::putAll);

    /* Get partitions. */
    Optional<VariableClassification> varClassification = cfa.getVarClassification();
    Set<Partition> partitions =
        varClassification.isPresent() ? varClassification.get().getPartitions() : null;

    /* Create the CFA JSON data. */
    this.cfaJsonData =
        new CfaJsonModule.CfaJsonData(
            nodes, cfa.edges(), cfa.getAllFunctions(), partitions, cfa.getMetadata());
  }

  /**
   * Writes the {@link CFA} data.
   *
   * @param pJsonFilePath The path to the JSON file.
   * @throws IOException If an error with {@link FileOutputStream}, {@link BufferedOutputStream} or
   *     {@link JsonGenerator} occurs.
   */
  public void write(Path pJsonFilePath) throws IOException {
    /* Create any required directories. */
    MoreFiles.createParentDirectories(pJsonFilePath);

    /* Write the CFA data. */
    try (FileOutputStream fileOutputStream = new FileOutputStream(pJsonFilePath.toFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        JsonGenerator jsonGenerator =
            new JsonFactory().createGenerator(bufferedOutputStream, JsonEncoding.UTF8); ) {

      CfaJsonIO.provideConfiguredCfaObjectMapper().writeValue(jsonGenerator, this.cfaJsonData);
    }
  }
}
