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
 * Enables to export all {@link CFA} {@link CFANode}s, {@link CFAEdge}s, {@link FunctionEntryNode}s
 * and relevant {@link CfaMetadata}.
 *
 * <p>The export format is JSON.
 *
 * <p>It extends {@link CfaJsonIO} and uses the {@link CfaJsonIO.CfaJsonData} record to store the
 * {@link CFA} data.
 */
public final class CfaToJson extends CfaJsonIO {
  private final CfaJsonData cfaJsonData;

  public CfaToJson(CFA pCfa) {
    CFA cfa = checkNotNull(pCfa);

    this.cfaJsonData =
        new CfaJsonData(cfa.nodes(), cfa.edges(), cfa.getAllFunctions(), cfa.getMetadata());
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

      provideConfiguredCfaObjectMapper().writeValue(jsonGenerator, this.cfaJsonData);
    }
  }
}
