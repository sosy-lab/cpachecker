// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointDeserializer;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord.WaypointSerializer;

@Immutable
@JsonDeserialize(using = WaypointDeserializer.class)
@JsonSerialize(using = WaypointSerializer.class)
public class WaypointRecord {
  @JsonProperty("type")
  private final WaypointType type;

  @JsonProperty("action")
  private final WaypointAction action;

  @JsonProperty("constraint")
  private final InformationRecord constraint;

  @JsonProperty("location")
  private final LocationRecord location;

  public WaypointRecord(
      @JsonProperty("type") WaypointType type,
      @JsonProperty("action") WaypointAction action,
      @JsonProperty("constraint") InformationRecord constraint,
      @JsonProperty("location") LocationRecord location) {
    // if not mentioned in json the type is assumed to be VISIT:
    this.type = type == null ? WaypointType.VISIT : type;
    // if not mentioned in json the action is assumed to be FOLLOW
    this.action = action == null ? WaypointAction.FOLLOW : action;
    this.constraint = constraint;
    this.location = location;
  }

  public WaypointRecord withAction(WaypointAction pAction) {
    return new WaypointRecord(this.getType(), pAction, this.getConstraint(), this.getLocation());
  }

  public WaypointRecord withType(WaypointType pType) {
    return new WaypointRecord(pType, this.getAction(), this.getConstraint(), this.getLocation());
  }

  public WaypointRecord withConstraint(InformationRecord pConstraint) {
    return new WaypointRecord(this.getType(), this.getAction(), pConstraint, this.getLocation());
  }

  public WaypointType getType() {
    return type;
  }

  public WaypointAction getAction() {
    return action;
  }

  public InformationRecord getConstraint() {
    return constraint;
  }

  public LocationRecord getLocation() {
    return location;
  }

  @Override
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null || getClass() != pOther.getClass()) {
      return false;
    }
    WaypointRecord other = (WaypointRecord) pOther;
    return type.equals(other.type)
        && action.equals(other.action)
        && constraint.equals(other.constraint)
        && location.equals(other.location);
  }

  @Override
  public int hashCode() {
    int hashCode = type.hashCode();
    hashCode = 31 * hashCode + action.hashCode();
    hashCode = 31 * hashCode + (constraint != null ? constraint.hashCode() : 0);
    hashCode = 31 * hashCode + (location != null ? location.hashCode() : 0);
    return hashCode;
  }

  @Override
  public String toString() {
    return "WaypointRecord [type="
        + type
        + ", action="
        + action
        + ", constraint="
        + constraint
        + ", location="
        + location
        + "]";
  }

  public enum WaypointType {
    VISIT("visit"),
    BRANCHING("branching"),
    ASSUMPTION("assumption"),
    FUNCTION_ENTER("function_enter"),
    FUNCTION_RETURN("function_return"),
    TARGET("target"),
    UNKNOWN("unknown");

    private static final Map<String, WaypointType> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (WaypointType type : WaypointType.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    WaypointType(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static WaypointType fromKeyword(String keyword) {
      if (keyword == null) {
        return VISIT;
      }
      if (keyword.equals("identifier_evaluation")) {
        // handle deprecated old keyword name
        return FUNCTION_ENTER;
      }
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    String getKeyword() {
      return keyword;
    }
  }

  public enum WaypointAction {
    FOLLOW("follow"),
    AVOID("avoid"),
    UNKNOWN("unknown");

    private static final Map<String, WaypointAction> map;
    private final String keyword;

    static {
      map = new HashMap<>();
      for (WaypointAction type : WaypointAction.values()) {
        map.put(type.getKeyword(), type);
      }
    }

    WaypointAction(String pKeyword) {
      keyword = pKeyword;
    }

    @JsonCreator
    public static WaypointAction fromKeyword(String keyword) {
      return map.getOrDefault(keyword, UNKNOWN);
    }

    @JsonValue
    String getKeyword() {
      return keyword;
    }
  }

  public static class WaypointDeserializer extends JsonDeserializer<WaypointRecord> {
    @Override
    public WaypointRecord deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      JsonNode node = mapper.readTree(jp);

      // The node should now be the 'waypoint' node. Move one level deeper to its children.
      JsonNode waypointNode = node.get("waypoint");
      assert waypointNode != null;

      // Delegate the actual object mapping back to Jackson:
      // WaypointRecord result = mapper.treeToValue(waypointNode, WaypointRecord.class);
      // CAVEAT: does not work, since this would use the custom deserializer.
      // Using the original deserializer is apparently very hard.
      // For now just manually construct this
      // (less elegant, but we probably never touch that code again, so it is fine):
      WaypointRecord result =
          new WaypointRecord(
              mapper.treeToValue(waypointNode.get("type"), WaypointType.class),
              mapper.treeToValue(waypointNode.get("action"), WaypointAction.class),
              mapper.treeToValue(waypointNode.get("constraint"), InformationRecord.class),
              mapper.treeToValue(waypointNode.get("location"), LocationRecord.class));

      return result;
    }
  }

  public static class WaypointSerializer extends JsonSerializer<WaypointRecord> {

    public WaypointSerializer() {}

    @Override
    public void serialize(WaypointRecord value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {

      // Start a wrapper object for "waypoint"
      gen.writeStartObject();
      gen.writeFieldName("waypoint");

      // start the actual WaypointRecord object
      gen.writeStartObject();
      gen.writeFieldName("type");
      serializers.defaultSerializeValue(value.getType(), gen);

      gen.writeFieldName("action");
      serializers.defaultSerializeValue(value.getAction(), gen);

      if (value.getConstraint() != null) {
        gen.writeFieldName("constraint");
        serializers.defaultSerializeValue(value.getConstraint(), gen);
      }

      gen.writeFieldName("location");
      serializers.defaultSerializeValue(value.getLocation(), gen);
      // end the WaypointRecord object
      gen.writeEndObject();

      // End the wrapper object
      gen.writeEndObject();
    }
  }
}
