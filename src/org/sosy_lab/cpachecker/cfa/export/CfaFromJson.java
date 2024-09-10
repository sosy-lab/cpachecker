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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * Enables to import all {@link CFA} {@link CFANode}s, {@link CFAEdge}s, {@link FunctionEntryNode}s
 * and relevant {@link CfaMetadata}.
 *
 * <p>The import format is JSON.
 *
 * <p>It imports the {@link CFA} data from a JSON file containing a {@link CfaJsonIO.CfaJsonData}
 * record.
 */
public final class CfaFromJson {

  /**
   * Reads a {@link CFA} from a JSON file.
   *
   * @param pCfaJsonFile The path to the JSON file containing the {@link CfaJsonIO.CfaJsonData}
   *     record.
   * @return The {@link CFA} object read from the JSON file.
   * @throws JsonParseException If there is an error parsing the JSON file.
   * @throws JsonMappingException If there is an error mapping the JSON data to the {@link
   *     CfaJsonIO.CfaJsonData} record.
   * @throws IOException If there is an error reading the JSON file.
   */
  public static CFA read(@Nullable Path pCfaJsonFile)
      throws JsonParseException, JsonMappingException, IOException {

    /* Get configured ObjectMapper. */
    ObjectMapper objectMapper = CfaJsonIO.provideConfiguredCfaObjectMapper();

    /* Add custom deserializers. */
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addDeserializer(Multiset.class, new MultisetDeserializer());
    simpleModule.addDeserializer(ImmutableSortedSet.class, new ImmutableSortedSetDeserializer());
    simpleModule.addDeserializer(CFAEdge.class, new CFAEdgeDeserializer());
    objectMapper.registerModule(simpleModule);

    /* Read CfaJsonData from file. */
    objectMapper.readValue(pCfaJsonFile.toFile(), CfaJsonIO.CfaJsonData.class);

    // TODO: Create CFA from CfaJsonData.

    return null;
  }

  /**
   * A custom JSON deserializer for {@link Multiset} objects.
   *
   * <p>This class is responsible for deserializing a JSON representation of a {@link Multiset} into
   * a {@link Multiset} object.
   */
  private static class MultisetDeserializer extends JsonDeserializer<Multiset<?>> {

    /**
     * Deserializes a JSON representation of a {@link Multiset} into a {@link Multiset} object.
     *
     * @param pJsonParser The JSON parser.
     * @param pDeserializationContext The deserialization context.
     * @return The deserialized {@link Multiset} object.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @Override
    public Multiset<?> deserialize(
        JsonParser pJsonParser, DeserializationContext pDeserializationContext) throws IOException {

      // TODO: Javadoc

      JsonNode node = pJsonParser.getCodec().readTree(pJsonParser);
      List<Comparable<?>> list = new ArrayList<>();

      for (JsonNode elementNode : node) {
        try (JsonParser elementParser = elementNode.traverse()) {
          Object element = pDeserializationContext.readValue(elementParser, Object.class);
          list.add((Comparable<?>) element);
        }
      }

      return FluentIterable.from(list).toMultiset();
    }
  }

  /**
   * A custom deserializer for {@link ImmutableSortedSet} objects.
   *
   * <p>This class is responsible for deserializing a JSON representation of an {@link
   * ImmutableSortedSet} into an actual {@link ImmutableSortedSet} object.
   */
  private static class ImmutableSortedSetDeserializer
      extends JsonDeserializer<ImmutableSortedSet<?>> {

    /**
     * Deserializes a JSON representation of an {@link ImmutableSortedSet} into an {@link
     * ImmutableSortedSet} object.
     *
     * @param pJsonParser The JSON parser.
     * @param pDeserializationContext The deserialization context.
     * @return The deserialized {@link ImmutableSortedSet} object.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @Override
    public ImmutableSortedSet<?> deserialize(
        JsonParser pJsonParser, DeserializationContext pDeserializationContext) throws IOException {

      // TODO: Javadoc

      JsonNode node = pJsonParser.getCodec().readTree(pJsonParser);
      Set<Comparable<?>> set = new HashSet<>();

      for (JsonNode elementNode : node) {
        try (JsonParser elementParser = elementNode.traverse()) {
          Comparable<?> element =
              pDeserializationContext.readValue(elementParser, Comparable.class);
          set.add(element);
        }
      }

      return ImmutableSortedSet.copyOf(set);
    }
  }

  /**
   * A custom deserializer for {@link CFAEdge} objects.
   *
   * <p>This class is responsible for deserializing a JSON representation of a {@link CFAEdge} into
   * a {@link CFAEdge} object.
   */
  private static class CFAEdgeDeserializer extends JsonDeserializer<CFAEdge> {

    /**
     * Deserializes a JSON representation of a {@link CFAEdge} into a {@link CFAEdge} object.
     *
     * @param pJsonParser The JSON parser.
     * @param pDeserializationContext The deserialization context.
     * @return The deserialized {@link CFAEdge} object.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @Override
    public CFAEdge deserialize(
        JsonParser pJsonParser, DeserializationContext pDeserializationContext) throws IOException {

      // TODO: Javadoc + Implementation

      JsonNode node = pJsonParser.getCodec().readTree(pJsonParser);

      return null;
    }
  }
}
