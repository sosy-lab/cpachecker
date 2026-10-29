// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.tools.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessagePayloadModule;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayloadModule;

public final class DssSchemaGenerator {
  private DssSchemaGenerator() {}

  public static void main(String[] args) throws Exception {
    Path output = Path.of(args.length > 0 ? args[0] : "output/dss-message-schema.json");

    if (output.getParent() != null) {
      Files.createDirectories(output.getParent());
    }

    ObjectMapper mapper =
        new ObjectMapper()
            .registerModule(new DssStatePayloadModule())
            .registerModule(new DssMessagePayloadModule());

    JacksonModule jacksonModule =
        new JacksonModule(
            JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
            JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE);
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
            .with(jacksonModule);
    SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

    JsonNode schema = generator.generateSchema(DssMessagePayload.class);
    mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), schema);

    System.out.println("Generated schema: " + output);
  }
}
