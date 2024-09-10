// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import org.sosy_lab.cpachecker.cfa.export.CfaJsonIO.CfaJsonData;
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
 * <p>It imports the {@link CFA} data from a JSON file containing a {@link CfaJsonIO.CfaJsonData}
 * record.
 */
public final class CfaFromJson {

  /**
   * Reads a {@link MutableCFA} from a JSON file.
   *
   * @param pCfaJsonFile The path to the JSON file containing the {@link CfaJsonIO.CfaJsonData}
   *     record.
   * @return The {@link MutableCFA} object read from the JSON file.
   * @throws JsonParseException If there is an error parsing the JSON file.
   * @throws JsonMappingException If there is an error mapping the JSON data to the {@link
   *     CfaJsonIO.CfaJsonData} record.
   * @throws IOException If there is an error reading the JSON file.
   */
  public static MutableCFA read(@Nullable Path pCfaJsonFile)
      throws JsonParseException, JsonMappingException, IOException {

    /* Get configured ObjectMapper. */
    ObjectMapper objectMapper = CfaJsonIO.provideConfiguredCfaObjectMapper();

    /* Add custom deserializers. */
    SimpleModule simpleModule = new SimpleModule();

    simpleModule.addDeserializer(ImmutableSortedSet.class, new ImmutableSortedSetDeserializer());

    /* Key deserializers. */
    KeyDeserializer keyDeserializer =
        new KeyDeserializer() {
          @Override
          public Object deserializeKey(String key, DeserializationContext ctxt)
              throws IOException, JsonProcessingException {
            throw new UnsupportedOperationException("Not implemented");
          }
        };

    simpleModule.addKeyDeserializer(CFAEdge.class, keyDeserializer);
    simpleModule.addKeyDeserializer(
        StartingLocationKeyDeserializer.getStartingLocationClass(), keyDeserializer);
    simpleModule.addKeyDeserializer(CCompositeType.class, keyDeserializer);
    simpleModule.addKeyDeserializer(CFANode.class, keyDeserializer);
    simpleModule.addKeyDeserializer(Pair.class, keyDeserializer);

    objectMapper.registerModule(simpleModule);

    /* Read CfaJsonData from file. */
    CfaJsonData cfaJsonData =
        objectMapper.readValue(pCfaJsonFile.toFile(), CfaJsonIO.CfaJsonData.class);
    return new MutableCFA(cfaJsonData.functions(), cfaJsonData.nodes(), cfaJsonData.metadata());
  }

  /**
   * A custom JSON deserializer for {@link ImmutableSortedSet}.
   *
   * <p>This class is responsible for deserializing a JSON representation of ImmutableSortedSet into
   * an instance of ImmutableSortedSet<Equivalence.Wrapper<?>>.
   */
  private static class ImmutableSortedSetDeserializer
      extends JsonDeserializer<ImmutableSortedSet<Equivalence.Wrapper<?>>> {

    @Override
    public ImmutableSortedSet<Equivalence.Wrapper<?>> deserialize(
        JsonParser p, DeserializationContext ctxt) throws IOException {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

  /**
   * This class is a key deserializer for the StartingLocation class.
   *
   * <p>The StartingLocation class is located at
   * "org.sosy_lab.cpachecker.util.ast.AstCfaRelation$StartingLocation".
   *
   * <p>The class provides a static method getStartingLocationClass() that returns the
   * StartingLocation class. If the class cannot be found, an IOException is thrown.
   */
  private static class StartingLocationKeyDeserializer extends KeyDeserializer {
    private static final String STARTING_LOCATION_PATH =
        "org.sosy_lab.cpachecker.util.ast.AstCfaRelation$StartingLocation";

    public static Class<?> getStartingLocationClass() throws IOException {
      try {
        return Class.forName(STARTING_LOCATION_PATH);
      } catch (Exception e) {
        throw new IOException("Could not find class " + STARTING_LOCATION_PATH, e);
      }
    }

    @Override
    public Object deserializeKey(
        final String pKey, final DeserializationContext pDeserializationContext)
        throws IOException, JsonProcessingException {
      throw new UnsupportedOperationException("Not implemented");
    }
  }
}
