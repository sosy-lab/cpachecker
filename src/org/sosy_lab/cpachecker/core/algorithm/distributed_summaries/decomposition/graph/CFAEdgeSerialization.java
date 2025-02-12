// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgeSerialization extends StdSerializer<CFAEdge> {

  public CFAEdgeSerialization() {
    super(CFAEdge.class);
  }

  @Override
  public void serialize(
      CFAEdge pCfaEdge, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
      throws IOException {
    pJsonGenerator.writeStartObject();
    pJsonGenerator.writeStringField("function", pCfaEdge.getPredecessor().getFunctionName());
    pJsonGenerator.writeStringField("file", pCfaEdge.getFileLocation().getNiceFileName());
    pJsonGenerator.writeNumberField(
        "startLine", pCfaEdge.getFileLocation().getStartingLineNumber());
    pJsonGenerator.writeNumberField(
        "startColumn", pCfaEdge.getFileLocation().getStartColumnInLine());
    pJsonGenerator.writeNumberField("endLine", pCfaEdge.getFileLocation().getEndingLineNumber());
    pJsonGenerator.writeNumberField("endColumn", pCfaEdge.getFileLocation().getEndColumnInLine());
    // CPAchecker introduces some edges that have no file location and empty statements and
    // descriptions.
    // Therefore, we need to export an id.
    pJsonGenerator.writeStringField(
        "cpaId",
        "N"
            + pCfaEdge.getPredecessor().getNodeNumber()
            + "N"
            + pCfaEdge.getSuccessor().getNodeNumber());
    pJsonGenerator.writeEndObject();
  }
}
