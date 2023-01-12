// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

@Options(prefix = "faultLocalization.export")
public class FaultLocalizationInfoExporter {

  public static class IntermediateFaults {
    private final Collection<Fault> faults;
    private final CFAEdge errorLocation;

    public IntermediateFaults(Collection<Fault> pFaults, CFAEdge pErrorLocation) {
      faults = pFaults;
      errorLocation = pErrorLocation;
    }

    public Collection<Fault> getFaults() {
      return faults;
    }

    public CFAEdge getErrorLocation() {
      return errorLocation;
    }
  }

  @Option(secure = true, description = "Where to write machine readable faults.")
  @FileOption(Type.OUTPUT_FILE)
  private Path outputFile = Path.of("faultlocalization.json");

  @Option(secure = true, description = "Whether to zip the resulting JSON file.")
  private boolean compressed = false;

  public FaultLocalizationInfoExporter(Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.recursiveInject(this);
  }

  public void export(Collection<Fault> pFaultSet, CFAEdge pErrorLocation) throws IOException {
    FaultsConverter converter = new FaultsConverter();
    String json = converter.faultToJson(new IntermediateFaults(pFaultSet, pErrorLocation));
    if (compressed) {
      IO.writeGZIPFile(outputFile, StandardCharsets.UTF_8, json);
    } else {
      IO.writeFile(outputFile, StandardCharsets.UTF_8, json);
    }
  }

  public static class FaultsConverter {

    private final ObjectMapper mapper;

    public FaultsConverter() {
      mapper = new ObjectMapper();
      SimpleModule serializer =
          new SimpleModule("FaultsSerializer", new Version(1, 0, 0, null, null, null));
      serializer.addSerializer(IntermediateFaults.class, new FaultsSerializer());
      mapper.registerModule(serializer);
    }

    public String faultToJson(IntermediateFaults pMessage) throws IOException {
      // return mapper.writeValueAsBytes(pMessage);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pMessage);
    }
  }

  private static class FaultsSerializer extends StdSerializer<IntermediateFaults> {

    private static final long serialVersionUID = 851310667701420047L;

    private FaultsSerializer() {
      super(IntermediateFaults.class);
    }

    @Override
    public void serialize(
        IntermediateFaults pMessage,
        JsonGenerator pJsonGenerator,
        SerializerProvider pSerializerProvider)
        throws IOException {
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeFieldName("faults");
      pJsonGenerator.writeStartArray();
      for (Fault fault : pMessage.getFaults()) {
        writeFault(pJsonGenerator, fault);
      }
      pJsonGenerator.writeEndArray();
      writeLocation(pJsonGenerator, pMessage.getErrorLocation(), "error-location");
      pJsonGenerator.writeEndObject();
    }

    private void writeFault(JsonGenerator pJsonGenerator, Fault pFault) throws IOException {
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeFieldName("fault");
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeNumberField("score", pFault.getScore());
      pJsonGenerator.writeNumberField("intendedIndex", pFault.getIntendedIndex());
      writeFaultInfo(pJsonGenerator, pFault.getInfos());
      pJsonGenerator.writeFieldName("contributions");
      pJsonGenerator.writeStartArray();
      for (FaultContribution contribution : pFault) {
        writeContribution(pJsonGenerator, contribution);
      }
      pJsonGenerator.writeEndArray();
      pJsonGenerator.writeEndObject();
      pJsonGenerator.writeEndObject();
    }

    private void writeFaultInfo(JsonGenerator pJsonGenerator, List<FaultInfo> pInfos)
        throws IOException {
      pJsonGenerator.writeFieldName("infos");
      pJsonGenerator.writeStartArray();
      for (FaultInfo info : pInfos) {
        pJsonGenerator.writeStartObject();
        pJsonGenerator.writeFieldName("fault-info");
        pJsonGenerator.writeStartObject();
        pJsonGenerator.writeStringField("description", info.getDescription());
        pJsonGenerator.writeNumberField("score", info.getScore());
        pJsonGenerator.writeStringField("type", info.getType().name());
        pJsonGenerator.writeEndObject();
        pJsonGenerator.writeEndObject();
      }
      pJsonGenerator.writeEndArray();
    }

    private void writeContribution(
        JsonGenerator pJsonGenerator, FaultContribution pFaultContribution) throws IOException {
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeFieldName("fault-contribution");
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeNumberField("score", pFaultContribution.getScore());
      writeFaultInfo(pJsonGenerator, pFaultContribution.getInfos());
      writeLocation(pJsonGenerator, pFaultContribution.correspondingEdge(), "location");
      pJsonGenerator.writeEndObject();
      pJsonGenerator.writeEndObject();
    }

    private void writeLocation(JsonGenerator pJsonGenerator, CFAEdge pEdge, String pName)
        throws IOException {
      pJsonGenerator.writeFieldName(pName);
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeNumberField(
          "startLine", pEdge.getFileLocation().getStartingLineInOrigin());
      pJsonGenerator.writeNumberField("endLine", pEdge.getFileLocation().getEndingLineInOrigin());
      pJsonGenerator.writeNumberField("startOffset", pEdge.getFileLocation().getNodeOffset());
      pJsonGenerator.writeNumberField(
          "endOffset",
          pEdge.getFileLocation().getNodeOffset() + pEdge.getFileLocation().getNodeLength());
      pJsonGenerator.writeStringField("filename", pEdge.getFileLocation().getFileName().toString());
      pJsonGenerator.writeStringField("code", pEdge.getCode());
      pJsonGenerator.writeEndObject();
    }
  }
}
