package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.io.IOException;

public class GuavaJsonModule extends SimpleModule {

  private static final long serialVersionUID = 6156246470715650341L;

  private ObjectCodec codec;

  public static void main(String[] args) {
    // Create an ObjectMapper and register the GuavaJsonModule
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new GuavaModule());
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    Multimap<String, String> a = ArrayListMultimap.create();

    // Add some key-value pairs to the Multimap
    a.put("key1", "value1");
    a.put("key1", "value2");
    a.put("key2", "value3");

    // Serialize the Multimap to JSON and print the result
    try {
      String json = mapper.writeValueAsString(a);
      System.out.println(json);

      // Deserialize the JSON back to a Multimap and print the result
      Multimap<String, String> deserializedMultimap =
          mapper.readValue(json, new TypeReference<Multimap<String, String>>() {});
      System.out.println(deserializedMultimap.get("key1"));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  public GuavaJsonModule() {
    this.addSerializer(new ImmutableListMultimapSerializer());
    // this.addSerializer(new ImmutableListSerializer());
    // this.addDeserializer(Multimap.class, new MultimapDeserializer());
    // this.addSerializer(new MultisetSerializer());
    // this.addDeserializer(Multiset.class, new MultisetDeserializer());
    // this.addDeserializer(ImmutableSet.class, new ImmutableSetDeserializer());
    // this.addDeserializer(ImmutableSortedSet.class, new ImmutableSortedSetDeserializer());
    // this.addDeserializer(ImmutableMap.class, new ImmutableMapDeserializer());
    // this.addDeserializer(ImmutableList.class, new ImmutableListDeserializer());
    // this.addDeserializer(ImmutableCollection.class, new ImmutableCollectionDeserializer());
    // this.addDeserializer(ImmutableListMultimap.class, new ImmutableListMultimapDeserializer());
    this.codec = null;
  }

  public void setCodec(ObjectCodec pCodec) {
    this.codec = pCodec;
  }

  private static void wrapExceptions(ExceptionThrowingRunnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface ExceptionThrowingRunnable {
    void run() throws Exception;
  }

  private class ImmutableListMultimapSerializer extends StdSerializer<ImmutableListMultimap<?, ?>> {

    private static final long serialVersionUID = 2515765471158050088L;

    public ImmutableListMultimapSerializer() {
      super(ImmutableListMultimap.class, false);
    }

    @SuppressWarnings("resource")
    @Override
    public void serialize(
        ImmutableListMultimap<?, ?> pMultimap,
        JsonGenerator pGenerator,
        SerializerProvider pProvider)
        throws IOException {

      pGenerator.writeStartArray();
      pMultimap
          .asMap()
          .forEach(
              (key, values) ->
                  wrapExceptions(
                      () -> {
                        if (codec == null) {
                          throw new RuntimeException("Codec is null");
                        }
                        pGenerator.setCodec(codec);

                        pGenerator.writeStartObject();
                        pGenerator.writeObjectField("key", key);
                        pGenerator.writeArrayFieldStart("values");
                        for (Object value : values) {
                          pGenerator.writeObject(value);
                        }
                        pGenerator.writeEndArray();
                        pGenerator.writeEndObject();
                      }));
      pGenerator.writeEndArray();
    }
  }

  private static class ImmutableListSerializer extends StdSerializer<ImmutableList<?>> {

    private static final long serialVersionUID = 2515765471158050088L;

    public ImmutableListSerializer() {
      super(ImmutableList.class, false);
    }

    @Override
    public void serialize(
        ImmutableList<?> pList, JsonGenerator pGenerator, SerializerProvider pProvider)
        throws IOException {
      System.out.println("Serializing ImmutableList");
      pGenerator.writeStartArray();
      pList.forEach(
          element ->
              wrapExceptions(
                  () -> {
                    pGenerator.writeStartObject();
                    pGenerator.writeObjectField("element", element);
                    pGenerator.writeEndObject();
                  }));
      pGenerator.writeEndArray();
    }
  }

  private static class MultimapDeserializer extends StdDeserializer<Multimap<?, ?>> {

    private static final long serialVersionUID = 2515765471158050088L;

    public MultimapDeserializer() {
      super(
          TypeFactory.defaultInstance()
              .constructMapLikeType(Multimap.class, Object.class, Object.class));
    }

    @Override
    public Multimap<?, ?> deserialize(JsonParser pParser, DeserializationContext pContext)
        throws IOException, JsonProcessingException {

      JsonNode rootNode = pParser.getCodec().readTree(pParser);
      JsonNode arrayNode = rootNode.elements().next();
      ObjectMapper mapper = (ObjectMapper) pParser.getCodec();

      String type = rootNode.fields().next().getKey();
      Multimap<Object, Object> multimap;

      switch (type) {
        case "ArrayListMultimap":
          multimap = ArrayListMultimap.create();
          break;
        case "HashMultimap":
          multimap = HashMultimap.create();
          break;
        case "LinkedHashMultimap":
          multimap = LinkedHashMultimap.create();
          break;
        case "LinkedListMultimap":
          multimap = LinkedListMultimap.create();
          break;
        default:
          throw new IllegalArgumentException("Unknown Multimap type: " + type);
      }

      if (arrayNode.isArray()) {
        for (JsonNode node : arrayNode) {
          try (JsonParser keyParser = node.get("key").traverse();
              JsonParser valuesParser = node.get("values").traverse()) {

            Object key = mapper.readValue(keyParser, Object.class);
            Object values = mapper.readValue(valuesParser, Object.class);

            if (values instanceof Iterable) {
              for (Object value : (Iterable<?>) values) {
                multimap.put(key, value);
              }
            } else {
              multimap.put(key, values);
            }
          }
        }
      }

      return multimap;
    }
  }

  private static class MultisetSerializer extends StdSerializer<Multiset<?>> {

    private static final long serialVersionUID = -2946811405070843579L;

    public MultisetSerializer() {
      super(Multiset.class, false);
    }

    @Override
    public void serialize(
        Multiset<?> pMultiset, JsonGenerator pGenerator, SerializerProvider pProvider)
        throws IOException {
      pGenerator.writeString("Multiset");
      pGenerator.writeStartArray();
      pMultiset.forEachEntry(
          (element, count) ->
              wrapExceptions(
                  () -> {
                    pGenerator.writeStartObject();
                    pGenerator.writeObjectField("element", element);
                    pGenerator.writeObjectField("count", count);
                    pGenerator.writeEndObject();
                  }));
      pGenerator.writeEndArray();
    }
  }

  private static class MultisetDeserializer extends JsonDeserializer<Multiset<?>> {

    @Override
    public Multiset<?> deserialize(JsonParser pParser, DeserializationContext pContext)
        throws IOException, JsonProcessingException {
      // JsonNode rootNode = pParser.getCodec().readTree(pParser);
      // Multiset <Object> multimap = ArrayListMultimap.create();
      // ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
      //
      // if (rootNode.isArray()) {
      //  for (JsonNode node : rootNode) {
      //    try (JsonParser keyParser = node.get("key").traverse();
      //        JsonParser valuesParser = node.get("values").traverse()) {
      //
      //      Object key = mapper.readValue(keyParser, Object.class);
      //      Object values = mapper.readValue(valuesParser, Object.class);
      //
      //      if (values instanceof Iterable) {
      //        for (Object value : (Iterable<?>) values) {
      //          multimap.put(key, value);
      //        }
      //      } else {
      //        multimap.put(key, values);
      //      }
      //    }
      //  }
      // }
      return null;
    }
  }
}
