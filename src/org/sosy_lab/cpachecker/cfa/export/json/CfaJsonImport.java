// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.IdentityKeyDeserializer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * Enables to import all {@link CFA} {@link CFANode}s, {@link CFAEdge}s, {@link FunctionEntryNode}s
 * {@link Partition}s and relevant {@link CfaMetadata}.
 *
 * <p>The import format is JSON.
 *
 * <p>It imports the {@link CFA} data from a JSON file containing a {@link CfaJsonData} record.
 */
public final class CfaJsonImport {
  private static final String STARTING_LOCATION_RECORD_PATH =
      "org.sosy_lab.cpachecker.util.ast.AstCfaRelation$StartingLocation";

  /**
   * Reads a {@link MutableCFA} from a JSON file.
   *
   * @param pCfaJsonFile The path to the JSON file containing the {@link CfaJsonData} record.
   * @return The {@link MutableCFA} object read from the JSON file.
   * @throws JsonParseException If there is an error parsing the JSON file.
   * @throws JsonMappingException If there is an error mapping the JSON data to the {@link
   *     CfaJsonData} record.
   * @throws IOException If there is an error reading the JSON file.
   */
  public MutableCFA read(@Nullable Path pCfaJsonFile)
      throws JsonParseException, JsonMappingException, IOException {

    checkNotNull(pCfaJsonFile, "No JSON file specified");

    /* Read CfaJsonData from file. */
    CfaJsonData cfaJsonData =
        getImportingObjectMapper().readValue(pCfaJsonFile.toFile(), CfaJsonData.class);

    return new MutableCFA(cfaJsonData.functions(), cfaJsonData.nodes(), cfaJsonData.metadata());
  }

  /**
   * Reads a {@link JsonNode} from the specified file path.
   *
   * <p>The node represents the whole JSON content of the file.
   *
   * @param pCfaJsonFile The path to the JSON file, can be null.
   * @return the JSON node read from the file.
   * @throws JsonParseException if the JSON content is not valid.
   * @throws JsonMappingException if the JSON content cannot be mapped to a JSON node.
   * @throws IOException if an I/O error occurs while reading the file.
   */
  public JsonNode readJsonNode(@Nullable Path pCfaJsonFile)
      throws JsonParseException, JsonMappingException, IOException {

    checkNotNull(pCfaJsonFile, "No JSON file specified");

    /* Read JsonNode from file. */
    return getImportingObjectMapper().readTree(pCfaJsonFile.toFile());
  }

  /**
   * Retrieves the StartingLocation record.
   *
   * <p>This workaround is necessary because the record is not directly accessible.
   *
   * @return the Class object representing the StartingLocation record.
   * @throws IOException if the class cannot be found or loaded.
   */
  private Class<?> getStartingLocationClass() throws IOException {
    try {
      return Class.forName(STARTING_LOCATION_RECORD_PATH);
    } catch (ClassNotFoundException e) {
      throw new IOException("Could not find record " + STARTING_LOCATION_RECORD_PATH, e);
    }
  }

  /**
   * Configures and returns an {@link ObjectMapper} for importing CFA data from JSON.
   *
   * <p>This method sets up an ObjectMapper with a custom deserializer and key deserializers that
   * are required by Jackson.
   *
   * @return a configured ObjectMapper for importing CFA data.
   * @throws IOException if an I/O error occurs during the configuration.
   */
  private ObjectMapper getImportingObjectMapper() throws IOException {
    /* Get basic ObjectMapper. */
    ObjectMapper objectMapper = new CfaJsonIO().getBasicCfaObjectMapper();

    SimpleModule simpleModule = new SimpleModule();

    /* Add key deserializer for CCompositeType. */
    simpleModule.addKeyDeserializer(CCompositeType.class, new IdentityKeyDeserializer());

    /* The following deserializers are required by jackson, but never called. */

    /* Add unused deserializer. */
    simpleModule.addDeserializer(
        ImmutableSortedSet.class,
        new JsonDeserializer<ImmutableSortedSet<Equivalence.Wrapper<?>>>() {

          @Override
          public ImmutableSortedSet<Equivalence.Wrapper<?>> deserialize(
              JsonParser pParser, DeserializationContext pContext) throws IOException {
            throw new UnsupportedOperationException("Not implemented");
          }
        });

    /* Add unused key deserializer. */
    KeyDeserializer unusedKeyDeserializer =
        new KeyDeserializer() {

          @Override
          public Object deserializeKey(
              final String pKey, final DeserializationContext pDeserializationContext)
              throws IOException, JsonProcessingException {
            throw new UnsupportedOperationException("Not implemented");
          }
        };

    simpleModule.addKeyDeserializer(CFAEdge.class, unusedKeyDeserializer);
    simpleModule.addKeyDeserializer(getStartingLocationClass(), unusedKeyDeserializer);
    simpleModule.addKeyDeserializer(CFANode.class, unusedKeyDeserializer);
    simpleModule.addKeyDeserializer(Pair.class, unusedKeyDeserializer);

    objectMapper.registerModule(simpleModule);

    return objectMapper;
  }
}
