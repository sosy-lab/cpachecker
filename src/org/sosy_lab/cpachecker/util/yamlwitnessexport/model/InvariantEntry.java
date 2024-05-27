// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordDeserializer;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordSerializer;

@JsonDeserialize(using = InvariantRecordDeserializer.class)
@JsonSerialize(using = InvariantRecordSerializer.class)
public non-sealed class InvariantEntry extends AbstractInformationRecord
    implements CorrectnessWitnessSetElementEntry {

  @JsonProperty("location")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final LocationRecord location;

  public InvariantEntry(
      @JsonProperty("value") String pString,
      @JsonProperty("type") String pType,
      @JsonProperty("format") String pFormat,
      @JsonProperty("location") LocationRecord pLocation) {
    super(pString, pType, pFormat);
    location = pLocation;
  }

  public LocationRecord getLocation() {
    return location;
  }

  public enum InvariantRecordType {
    LOOP_INVARIANT("loop_invariant"),
    LOCATION_INVARIANT("location_invariant"),
    UNKNOWN("unknown");

    private static final Map<String, InvariantRecordType> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (InvariantRecordType type : InvariantRecordType.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    InvariantRecordType(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static InvariantRecordType fromKeyword(String keyword) {
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    public String getKeyword() {
      return keyword;
    }
  }

  public static class InvariantRecordDeserializer extends JsonDeserializer<InvariantEntry> {
    @Override
    public InvariantEntry deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'invariant' node. Move one level deeper to its children.
      JsonNode invariantNode = node.get("invariant");
      assert invariantNode != null;

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      InvariantEntry result =
          new InvariantEntry(
              mapper.treeToValue(invariantNode.get("value"), String.class),
              mapper.treeToValue(invariantNode.get("type"), String.class),
              mapper.treeToValue(invariantNode.get("format"), String.class),
              mapper.treeToValue(invariantNode.get("location"), LocationRecord.class));

      return result;
    }
  }

  public static class InvariantRecordSerializer extends JsonSerializer<InvariantEntry> {

    public InvariantRecordSerializer() {}

    @Override
    public void serialize(InvariantEntry value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // Start a wrapper object for "waypoint"
      gen.writeStartObject();
      gen.writeFieldName("invariant");

      // start the actual InvariantEntry object
      gen.writeStartObject();
      gen.writeFieldName("type");
      serializers.defaultSerializeValue(value.getType(), gen);

      gen.writeFieldName("location");
      serializers.defaultSerializeValue(value.getLocation(), gen);

      gen.writeFieldName("value");
      serializers.defaultSerializeValue(value.getValue(), gen);

      gen.writeFieldName("format");
      serializers.defaultSerializeValue(value.getFormat(), gen);
      // end the InvariantEntry object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof InvariantEntry invariantStoreEntryLoopInvariant
        && Objects.equals(value, invariantStoreEntryLoopInvariant.value)
        && Objects.equals(type, invariantStoreEntryLoopInvariant.type)
        && Objects.equals(format, invariantStoreEntryLoopInvariant.format)
        && Objects.equals(location, invariantStoreEntryLoopInvariant.location);
  }

  @Override
  public int hashCode() {
    int hashCode = value.hashCode();
    hashCode = 31 * hashCode + (type != null ? type.hashCode() : 0);
    hashCode = 31 * hashCode + (format != null ? format.hashCode() : 0);
    hashCode = 31 * hashCode + (location != null ? location.hashCode() : 0);
    return hashCode;
  }

  @Override
  public String toString() {
    return "InvariantEntry{"
        + " string='"
        + getValue()
        + "'"
        + ", type='"
        + getType()
        + "'"
        + ", format='"
        + getFormat()
        + "'"
        + ", location='"
        + getLocation()
        + "'"
        + "}";
  }
}
