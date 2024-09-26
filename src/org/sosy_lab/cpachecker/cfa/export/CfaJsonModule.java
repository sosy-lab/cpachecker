// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * This class is a Jackson module for serialization and deserialization.
 *
 * <p>Important: The {@link AstCfaRelation} in {@link CfaMetadata} is not serialized or
 * deserialized.
 */
public class CfaJsonModule extends SimpleModule {

  private static final long serialVersionUID = 1945912240762984485L;

  /* This record represents the CFA data. */
  final record CfaJsonData(
      TreeMultimap<String, CFANode> nodes,
      Set<CFAEdge> edges,
      NavigableMap<String, FunctionEntryNode> functions,
      @JsonSerialize(using = PartitionsSerializer.class)
          @JsonDeserialize(using = PartitionsDeserializer.class)
          Set<Partition> partitions,
      CfaMetadata metadata) {}

  /**
   * Sets up the module by registering all mixins.
   *
   * @param pContext The setup context.
   */
  @Override
  public void setupModule(SetupContext pContext) {
    super.setupModule(pContext);

    /* Register all mixins. */
    pContext.setMixInAnnotations(AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(
        CExpressionAssignmentStatement.class, CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CExpressionStatement.class, CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(CFunctionDeclaration.class, CFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(CFunctionEntryNode.class, CFunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(CFunctionType.class, CFunctionTypeMixin.class);
    pContext.setMixInAnnotations(CFunctionTypeWithNames.class, CFunctionTypeWithNamesMixin.class);
    pContext.setMixInAnnotations(CIdExpression.class, CIdExpressionMixin.class);
    pContext.setMixInAnnotations(CInitializerExpression.class, CInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(
        CIntegerLiteralExpression.class, CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(CReturnStatementEdge.class, CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CVariableDeclaration.class, CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(Loop.class, LoopMixin.class);
    pContext.setMixInAnnotations(LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(VariableClassification.class, VariableClassificationMixin.class);
  }

  /**
   * The PartitionHandler class is responsible for constructing instances of the {@link Partition}
   * class.
   *
   * <p>It provides methods for adding variables, values, edges, and mappings between variables and
   * partitions.
   *
   * <p>The getReference() method returns the Partition object.
   */
  private static final class PartitionHandler {
    private Partition partition;

    private NavigableSet<String> vars;
    private NavigableSet<BigInteger> values;
    private Multimap<CFAEdge, Integer> edges;
    private final Map<String, Partition> varToPartition = new HashMap<>();
    private final Table<CFAEdge, Integer, Partition> edgeToPartition = HashBasedTable.create();

    /**
     * Constructs a new PartitionHandler with the given index.
     *
     * @param pIndex The index of the partition.
     * @throws IOException If an error occurs during construction.
     */
    public PartitionHandler(int pIndex) throws IOException {
      try {

        /* Create a new instance of Partition via reflection. */
        Constructor<?> partitionConstructor =
            Partition.class.getDeclaredConstructor(Map.class, Table.class);

        ClassUtil.checkAndFixAccess(partitionConstructor, true);

        this.partition =
            (Partition) partitionConstructor.newInstance(this.varToPartition, this.edgeToPartition);

        /* Set the index field of the partition. */
        writeIndexField(pIndex);

        /* Set handler fields to partition fields. */
        this.vars = readVars();
        this.values = readValues();
        this.edges = readEdges();

      } catch (Exception e) {
        throw new IOException("Error while constructing PartitionHandler: " + e.getMessage(), e);
      }
    }

    public PartitionHandler addVar(String pVar) {
      vars.add(pVar);
      return this;
    }

    public PartitionHandler addValue(BigInteger pValue) {
      values.add(pValue);
      return this;
    }

    public PartitionHandler addEdge(CFAEdge pEdge, Integer pIndex) {
      edges.put(pEdge, pIndex);
      return this;
    }

    public PartitionHandler addVarToPartition(String pVar, Partition pPartition) {
      varToPartition.put(pVar, pPartition);
      return this;
    }

    public PartitionHandler addEdgeToPartition(
        CFAEdge pEdge, Integer pIndex, Partition pPartition) {
      edgeToPartition.put(pEdge, pIndex, pPartition);
      return this;
    }

    public Partition getReference() {
      return this.partition;
    }

    /**
     * Writes the index field of the partition.
     *
     * @param pIndex The new value for the index field.
     * @throws NoSuchFieldException if the index field does not exist in the Partition class.
     */
    private void writeIndexField(int pIndex) throws NoSuchFieldException {
      try {
        Field field = Partition.class.getDeclaredField("index");

        ClassUtil.checkAndFixAccess(field, true);

        field.set(this.partition, pIndex);

      } catch (IllegalAccessException e) {
        throw new NoSuchFieldException(
            "Error while attempting to set field index in Partition: " + e.getMessage());
      }
    }

    /* Retrieves the vars field from the partition. */
    @SuppressWarnings("unchecked")
    public NavigableSet<String> readVars() {
      return (NavigableSet<String>) getField("vars", this.partition);
    }

    /* Retrieves the values field from the partition. */
    @SuppressWarnings("unchecked")
    public NavigableSet<BigInteger> readValues() {
      return (NavigableSet<BigInteger>) getField("values", this.partition);
    }

    /* Retrieves the edges field from the partition. */
    @SuppressWarnings("unchecked")
    public Multimap<CFAEdge, Integer> readEdges() {
      return (Multimap<CFAEdge, Integer>) getField("edges", this.partition);
    }

    /* Retrieves the varToPartition field from a Partition object. */
    @SuppressWarnings("unchecked")
    public static Map<String, Partition> readVarToPartition(Partition partition) {
      return (Map<String, Partition>) getField("varToPartition", partition);
    }

    /* Retrieves the edgeToPartition field from a Partition object. */
    @SuppressWarnings("unchecked")
    public static Table<CFAEdge, Integer, Partition> readEdgeToPartition(Partition partition) {
      return (Table<CFAEdge, Integer, Partition>) getField("edgeToPartition", partition);
    }

    /**
     * Retrieves the value of a specified field from a {@link Partition} object.
     *
     * @param pName The name of the field to retrieve.
     * @param pPartition The Partition object from which to retrieve the field.
     * @return The value of the specified field.
     * @throws IllegalArgumentException If the specified field does not exist or cannot be accessed.
     */
    private static Object getField(String pName, Partition pPartition)
        throws IllegalArgumentException {
      try {
        Field field = Partition.class.getDeclaredField(pName);

        ClassUtil.checkAndFixAccess(field, true);

        return field.get(pPartition);

      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Error while attempting to retrieve field "
                + pName
                + " from Partition: "
                + e.getMessage(),
            e);
      }
    }
  }

  /**
   * Custom JSON serializer for serializing a set of {@link Partition}s.
   *
   * <p>It serializes the partitions as an array of objects. Each object represents a partition.
   */
  private static class PartitionsSerializer extends JsonSerializer<Set<Partition>> {

    @Override
    public void serialize(
        Set<Partition> pPartitions, JsonGenerator pGenerator, SerializerProvider pProvider)
        throws IOException {
      pGenerator.writeStartArray();

      for (Partition partition : pPartitions) {
        pGenerator.writeStartObject();

        /* Index */
        pGenerator.writeNumberField("index", partition.hashCode());

        /* Vars */
        pGenerator.writeArrayFieldStart("vars");
        for (String var : partition.getVars()) {
          pGenerator.writeString(var);
        }
        pGenerator.writeEndArray();

        /* Values */
        pGenerator.writeArrayFieldStart("values");
        for (BigInteger value : partition.getValues()) {
          pGenerator.writeObject(value);
        }
        pGenerator.writeEndArray();

        /* Edges (sorted) */
        List<Entry<CFAEdge, Collection<Integer>>> entries =
            new ArrayList<>(partition.getEdges().asMap().entrySet());
        Collections.sort(
            entries,
            Comparator.comparingInt(entry -> CfaEdgeIdGenerator.getIdFromEdge(entry.getKey())));

        pGenerator.writeArrayFieldStart("edges");
        for (Entry<CFAEdge, Collection<Integer>> entry : entries) {
          pGenerator.writeStartObject();
          pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(entry.getKey()));
          pGenerator.writeArrayFieldStart("indices");
          for (Integer index : entry.getValue()) {
            pGenerator.writeObject(index);
          }
          pGenerator.writeEndArray();
          pGenerator.writeEndObject();
        }
        pGenerator.writeEndArray();

        try {
          /* VarToPartition */
          /* Retrieve field via reflection. */
          Map<String, Partition> varToPartition = PartitionHandler.readVarToPartition(partition);

          /* Write field. */
          pGenerator.writeObjectFieldStart("varToPartition");
          for (Entry<String, Partition> entry : varToPartition.entrySet()) {
            pGenerator.writeObjectField(entry.getKey(), entry.getValue().hashCode());
          }
          pGenerator.writeEndObject();

          /* EdgeToPartition */
          /* Retrieve field via reflection. */
          Table<CFAEdge, Integer, Partition> edgeToPartition =
              PartitionHandler.readEdgeToPartition(partition);

          /* Write field. */
          pGenerator.writeArrayFieldStart("edgeToPartition");
          for (Cell<CFAEdge, Integer, Partition> cell : edgeToPartition.cellSet()) {
            pGenerator.writeStartObject();
            pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(cell.getRowKey()));
            pGenerator.writeNumberField("index", cell.getColumnKey());
            pGenerator.writeNumberField("partition", cell.getValue().hashCode());
            pGenerator.writeEndObject();
          }
          pGenerator.writeEndArray();

        } catch (IllegalArgumentException e) {
          throw new java.io.IOException("Error while serializing partition: " + e.getMessage(), e);
        }

        pGenerator.writeEndObject();
      }

      pGenerator.writeEndArray();
    }
  }

  /**
   * The PartitionsDeserializer class is responsible for deserializing JSON data into a set of
   * {@link Partition}s.
   *
   * <p>The deserialization process involves reading the JSON data, constructing PartitionHandler
   * objects, and adding variables, values, edges, and mappings to each PartitionHandler. Finally,
   * the deserialized Partitions are returned as a set.
   */
  private static class PartitionsDeserializer extends JsonDeserializer<Set<Partition>> {
    private static ThreadLocal<Map<Integer, PartitionHandler>> partitionHandlers =
        ThreadLocal.withInitial(HashMap::new);

    /* Retrieves an existing PartitionHandler or creates a new one if it does not exist. */
    public static PartitionHandler getPartitionHandler(int pIndex) throws IOException {
      PartitionHandler handler;

      Map<Integer, PartitionHandler> handlers = partitionHandlers.get();

      checkNotNull(handlers, "No partitionHandlers available");

      if (handlers.containsKey(pIndex)) {
        handler = handlers.get(pIndex);

      } else {
        handler = new PartitionHandler(pIndex);
        handlers.put(pIndex, handler);
      }

      return handler;
    }

    /**
     * Deserialize a JSON representation of partitions into a set of {@link Partition} objects.
     *
     * @param pParser The JSON parser.
     * @param pContext The deserialization context.
     * @return the set of deserialized partitions.
     * @throws IOException if an I/O error occurs during deserialization.
     */
    @Override
    public Set<Partition> deserialize(JsonParser pParser, DeserializationContext pContext)
        throws IOException {

      Set<Partition> deserializedPartitions = new HashSet<>();

      /* Get root node. */
      ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
      JsonNode rootNode = mapper.readTree(pParser);

      /* Iterate over the root node and construct PartitionHandlers. */
      for (JsonNode node : rootNode) {

        PartitionHandler handler;

        if (node.isObject()) {
          /* Node is an object: Full size deserialization. */

          /* Get handler. */
          Integer index = node.get("index").asInt();
          handler = getPartitionHandler(index);

          /* Vars */
          for (JsonNode var : node.get("vars")) {
            handler.addVar(var.asText());
          }

          /* Values */
          for (JsonNode value : node.get("values")) {
            handler.addValue(value.bigIntegerValue());
          }

          /* Edges */
          for (JsonNode edge : node.get("edges")) {
            CFAEdge cfaEdge = CfaEdgeIdResolver.getEdgeFromId(edge.get("edge").asInt());

            for (JsonNode edgeIndex : edge.get("indices")) {
              handler.addEdge(cfaEdge, edgeIndex.asInt());
            }
          }

          /* VarToPartition */
          Iterator<Map.Entry<String, JsonNode>> fields = node.get("varToPartition").fields();
          while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();

            Partition partition = getPartitionHandler(field.getValue().asInt()).getReference();

            handler.addVarToPartition(field.getKey(), partition);
          }

          /* EdgeToPartition */
          for (JsonNode etp : node.get("edgeToPartition")) {
            TableEntry tableEntry = EdgeToPartitionsDeserializer.deserializeTableEntry(etp);
            handler.addEdgeToPartition(
                tableEntry.edge(), tableEntry.index(), tableEntry.partition());
          }

        } else {

          /* Node is an integer: Deserialization from object id. */
          handler = getPartitionHandler(node.asInt());
        }

        deserializedPartitions.add(handler.getReference());
      }

      return ImmutableSet.copyOf(deserializedPartitions);
    }
  }

  /**
   * Represents an entry in a {@link Table}.
   *
   * <p>This record encapsulates information about a CFAEdge, an index, and a Partition.
   */
  private record TableEntry(CFAEdge edge, Integer index, Partition partition) {}

  /**
   * A converter class that converts a {@link Table} object to a list of {@link TableEntry} objects.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions
   * (EdgeToPartitions).
   */
  private static final class EdgeToPartitionsTableToListConverter
      extends StdConverter<Table<CFAEdge, Integer, Partition>, List<TableEntry>> {
    @Override
    public List<TableEntry> convert(Table<CFAEdge, Integer, Partition> pTable) {
      return pTable.cellSet().stream()
          .filter(cell -> cell.getValue() != null)
          .map(cell -> new TableEntry(cell.getRowKey(), cell.getColumnKey(), cell.getValue()))
          .collect(ImmutableList.toImmutableList());
    }
  }

  /**
   * EdgeToPartitionsDeserializer is a custom deserializer for converting JSON representations of
   * tables (lists of {@link TableEntry} objects) into {@link Table} objects with keys of type
   * {@link CFAEdge} and {@link Integer}, and values of type {@link Partition}.
   *
   * <p>This deserializer provides methods to deserialize individual {@link TableEntry} objects as
   * well as entire tables.
   */
  private static class EdgeToPartitionsDeserializer
      extends JsonDeserializer<Table<CFAEdge, Integer, Partition>> {

    /**
     * Deserializes a JSON node into a {@link TableEntry} object.
     *
     * @param pNode The JSON node to deserialize.
     * @return a TableEntry object containing the deserialized data.
     * @throws IOException if an I/O error occurs during deserialization.
     */
    public static TableEntry deserializeTableEntry(JsonNode pNode) throws IOException {

      CFAEdge edge = CfaEdgeIdResolver.getEdgeFromId(pNode.get("edge").asInt());
      Integer index = pNode.get("index").asInt();
      Partition partition =
          PartitionsDeserializer.getPartitionHandler(pNode.get("partition").asInt()).getReference();

      return new TableEntry(edge, index, partition);
    }

    /**
     * Deserializes a JSON representation of a table (list of {@link TableEntry} objects) into a
     * Table<CFAEdge, Integer, Partition> object.
     *
     * @param pParser The JsonParser used to parse the JSON content.
     * @param pContext The DeserializationContext.
     * @return a Table containing the deserialized table.
     * @throws IOException if an I/O error occurs during parsing.
     * @throws JsonProcessingException if a processing error occurs during parsing.
     */
    @Override
    public Table<CFAEdge, Integer, Partition> deserialize(
        JsonParser pParser, DeserializationContext pContext)
        throws IOException, JsonProcessingException {

      /* Get root node. */
      ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
      JsonNode rootNode = mapper.readTree(pParser);

      Table<CFAEdge, Integer, Partition> table = HashBasedTable.create();

      /* Iterate over the root node and add TableEntry objects to the table. */
      for (JsonNode node : rootNode) {
        TableEntry tableEntry = deserializeTableEntry(node);
        table.put(tableEntry.edge(), tableEntry.index(), tableEntry.partition());
      }

      return table;
    }
  }

  /**
   * A custom generator for generating unique IDs for CFA edges.
   *
   * <p>It is used to retrieve IDs from their respective {@link CFAEdge}s.
   */
  private static final class CfaEdgeIdGenerator extends ObjectIdGenerator<Integer> {

    private static final long serialVersionUID = 7470151299045493234L;
    private static ThreadLocal<CfaEdgeIdGenerator> currentGenerator = new ThreadLocal<>();

    private final Map<CFAEdge, Integer> edgeToIdMap = new HashMap<>();
    private final Class<?> scope;
    private int nextValue;

    /**
     * Constructs a new CfaEdgeIdGenerator.
     *
     * <p>This constructor is being used by Jackson.
     */
    @SuppressWarnings("unused")
    public CfaEdgeIdGenerator() {
      this(Object.class, -1);
    }

    /**
     * Constructs a new CfaEdgeIdGenerator with the given scope and next value.
     *
     * @param pScope The class representing the scope of the generator.
     * @param pNextValue The next value to be used by the generator.
     */
    public CfaEdgeIdGenerator(Class<?> pScope, int pNextValue) {
      this.scope = pScope;
      this.nextValue = pNextValue;
    }

    /**
     * Determines if this generator instance can be used for Object IDs of specific generator type
     * and scope.
     *
     * @param pGenerator The object generator to check.
     * @return True if the generator class and its scope match this generator, false otherwise.
     */
    @Override
    public boolean canUseFor(ObjectIdGenerator<?> pGenerator) {
      return pGenerator.getClass() == this.getClass() && pGenerator.getScope() == this.scope;
    }

    /* Creates a new instance of CfaEdgeIdGenerator with the given scope. */
    @Override
    public ObjectIdGenerator<Integer> forScope(Class<?> pScope) {
      return this.scope == pScope ? this : new CfaEdgeIdGenerator(pScope, this.nextValue);
    }

    /**
     * Generates an ID for the given object.
     *
     * <p>If the object is not a {@link CFAEdge}, an {@link IllegalArgumentException} is thrown.
     *
     * <p>The ID is generated and stored in the edgeToIdMap together with the object.
     *
     * @param pForObject The object for which to generate the ID.
     * @return The generated ID.
     */
    @Override
    public Integer generateId(Object pForObject) {
      if (pForObject == null) {
        return null;
      } else {
        if (!(pForObject instanceof CFAEdge)) {
          throw new IllegalArgumentException(
              "Wrong object: " + pForObject.getClass().getSimpleName() + " is not a CFAEdge");
        }

        int id = this.nextValue++;
        edgeToIdMap.put((CFAEdge) pForObject, id);
        return id;
      }
    }

    /**
     * Retrieves the scope of this generator.
     *
     * @return The scope of this generator.
     */
    @Override
    public Class<?> getScope() {
      return this.scope;
    }

    /**
     * Generates an IdKey object based on the given key.
     *
     * @param pKey The key used to generate the IdKey object.
     * @return The generated IdKey object.
     */
    @Override
    public IdKey key(Object pKey) {
      return pKey == null ? null : new ObjectIdGenerator.IdKey(this.getClass(), this.scope, pKey);
    }

    /**
     * Creates a new instance of the CfaEdgeIdGenerator for serialization.
     *
     * <p>It also sets the currentGenerator field to the newly created instance for later use.
     *
     * @param pContext The context object.
     * @return The new instance of the CfaEdgeIdGenerator.
     */
    @Override
    public ObjectIdGenerator<Integer> newForSerialization(Object pContext) {
      CfaEdgeIdGenerator generator = new CfaEdgeIdGenerator(this.scope, 1);
      currentGenerator.set(generator);
      return generator;
    }

    /**
     * Retrieves the ID of a {@link CFAEdge}.
     *
     * @param pEdge The CFAEdge.
     * @return The ID of the CFAEdge.
     * @throws IllegalStateException If no generator was set.
     * @throws IllegalArgumentException If no ID for the CFAEdge is found.
     */
    public static Integer getIdFromEdge(CFAEdge pEdge) {
      CfaEdgeIdGenerator generator = currentGenerator.get();

      checkNotNull(generator, "No generator available");

      Integer id = generator.edgeToIdMap.get(pEdge);

      if (id == null) {
        throw new IllegalArgumentException("No ID for edge " + pEdge + " found");
      }

      return id;
    }

    /* Custom serialization method to prevent serialization of the generator (ObjectIdGenerator implements Serializable). */
    @SuppressWarnings("unused")
    private void writeObject(ObjectOutputStream pStream) throws IOException {
      throw new NotSerializableException(getClass().getName());
    }

    /* Custom deserialization method to prevent deserialization of the generator (ObjectIdGenerator implements Serializable). */
    @SuppressWarnings("unused")
    private void readObject(ObjectInputStream pStream) throws IOException {
      throw new NotSerializableException(getClass().getName());
    }
  }

  /**
   * This class is a custom {@link ObjectIdResolver}.
   *
   * <p>It is used to retrieve {@link CFAEdge}s from their respective IDs.
   */
  private static class CfaEdgeIdResolver extends SimpleObjectIdResolver {
    private static final ThreadLocal<CfaEdgeIdResolver> currentResolver = new ThreadLocal<>();

    private final Map<Integer, CFAEdge> idToEdgeMap = new HashMap<>();

    /**
     * Creates a new instance of {@link CfaEdgeIdResolver} for deserialization.
     *
     * <p>It also sets the currentResolver field to the newly created instance for later use.
     *
     * @param pContext The deserialization context object.
     * @return The newly created instance.
     */
    @Override
    public ObjectIdResolver newForDeserialization(Object pContext) {
      CfaEdgeIdResolver resolver = new CfaEdgeIdResolver();
      currentResolver.set(resolver);
      return resolver;
    }

    /**
     * Binds an item to an ID.
     *
     * <p>It makes sure that the key is an Integer and the item is a CFAEdge.
     *
     * @param pId The ID.
     * @param pItem The object to bind.
     */
    @Override
    public void bindItem(IdKey pId, Object pItem) {
      if (pId.key.getClass() != Integer.class) {
        throw new IllegalArgumentException(
            "Wrong key: " + pId.key.getClass().getSimpleName() + " is not an Integer");
      }

      if (!(pItem instanceof CFAEdge)) {
        throw new IllegalArgumentException(
            "Wrong object: " + pItem.getClass().getSimpleName() + " is not a CFAEdge");
      }

      idToEdgeMap.put((Integer) pId.key, (CFAEdge) pItem);
      super.bindItem(pId, pItem);
    }

    /**
     * Retrieves a {@link CFAEdge} from its ID.
     *
     * @param pId The ID of the CFAEdge.
     * @return The CFAEdge with the specified ID.
     * @throws IllegalStateException If no resolver was set.
     * @throws IllegalArgumentException If no CFAEdge with the specified ID is found.
     */
    public static CFAEdge getEdgeFromId(Integer pId) {
      CfaEdgeIdResolver resolver = currentResolver.get();

      checkNotNull(resolver, "No resolver available");

      CFAEdge edge = resolver.idToEdgeMap.get(pId);

      if (edge == null) {
        throw new IllegalArgumentException("No edge with ID " + pId + " found");
      }

      return edge;
    }
  }

  /**
   * This class is a custom {@link ObjectIdResolver}.
   *
   * <p>It is used for {@link Partition} objects.
   */
  private static class PartitionIdResolver extends SimpleObjectIdResolver {

    /**
     * Resolves an object based on the given {@link
     * com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey}.
     *
     * <p>If the object is not already present in the internal map, it attempts to retrieve it using
     * the {@link PartitionsDeserializer#getPartitionHandler(int)} method and then binds it to the
     * map.
     *
     * @param pId The {@link com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey} to resolve.
     * @return the resolved object, or null if it cannot be resolved.
     */
    @Override
    public Object resolveId(ObjectIdGenerator.IdKey pId) {
      if (this._items == null) {
        this._items = new HashMap<>();
      }

      /* Check if the object is already present in the map. */
      Object resolved = this._items.get(pId);

      /* If not, try to retrieve it using the PartitionHandler. */
      if (resolved == null) {
        try {
          resolved = PartitionsDeserializer.getPartitionHandler((Integer) pId.key).getReference();
          this.bindItem(pId, resolved);

        } catch (IOException e) {
          return null;
        }
      }

      return resolved;
    }
  }

  /**
   * A converter that removes the leading and trailing brackets from a given string.
   *
   * <p>If the input string starts with "[" and ends with "]", the brackets are removed. Otherwise,
   * the input string is returned as is.
   *
   * <p>If the input string is null, null is returned.
   */
  private static final class BracketRemoverConverter extends StdConverter<String, String> {

    @Override
    public String convert(String pInput) {
      if (pInput == null) {
        return null;
      }

      if (pInput.startsWith("[") && pInput.endsWith("]")) {
        return pInput.substring(1, pInput.length() - 1);
      } else {
        return pInput;
      }
    }
  }

  /**
   * A converter that transforms a set of {@link CSimpleDeclaration} objects into a sorted list of
   * {@link CSimpleDeclaration} objects.
   *
   * <p>The sorting is based on the hash code of the {@link CSimpleDeclaration} objects.
   */
  private static final class OutOfScopeToSortedListConverter
      extends StdConverter<Set<CSimpleDeclaration>, List<CSimpleDeclaration>> {

    @Override
    public List<CSimpleDeclaration> convert(Set<CSimpleDeclaration> pSet) {
      List<CSimpleDeclaration> list = new ArrayList<>(pSet);
      list.sort(Comparator.comparing(CSimpleDeclaration::hashCode));

      return list;
    }
  }

  /**
   * This class is a mixin for {@link AAstNode}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfAAstNode")
  private static final class AAstNodeMixin {}

  /**
   * This class is a mixin for {@link BlankEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class BlankEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public BlankEdgeMixin(
        @JsonProperty("rawStatement") String pRawStatement,
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("predecessor") CFANode pPredecessor,
        @JsonProperty("successor") CFANode pSuccessor,
        @JsonProperty("description") String pDescription) {}
  }

  /**
   * This class is a mixin for {@link CAssumeEdge}.
   *
   * <p>rawStatement is deserialized using {@link BracketRemoverConverter}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CAssumeEdgeMixin {

    @SuppressWarnings("unused")
    @JsonDeserialize(converter = BracketRemoverConverter.class)
    private String rawStatement;

    @SuppressWarnings("unused")
    @JsonCreator
    public CAssumeEdgeMixin(
        @JsonProperty("rawStatement") String pRawStatement,
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("predecessor") CFANode pPredecessor,
        @JsonProperty("successor") CFANode pSuccessor,
        @JsonProperty("expression") CExpression pExpression,
        @JsonProperty("truthAssumption") boolean pTruthAssumption,
        @JsonProperty("swapped") boolean pSwapped,
        @JsonProperty("artificialIntermediate") boolean pArtificial) {}
  }

  /**
   * This class is a mixin for {@link CBinaryExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CBinaryExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CBinaryExpressionMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("type") CType pExpressionType,
        @JsonProperty("calculationType") CType pCalculationType,
        @JsonProperty("operand1") CExpression pOperand1,
        @JsonProperty("operand2") CExpression pOperand2,
        @JsonProperty("operator") BinaryOperator pOperator) {}
  }

  /**
   * This class is a mixin for {@link CDeclarationEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CDeclarationEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CDeclarationEdgeMixin(
        @JsonProperty("rawStatement") String pRawSignature,
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("predecessor") CFANode pPredecessor,
        @JsonProperty("successor") CFANode pSuccessor,
        @JsonProperty("declaration") CDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link CExpressionAssignmentStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionAssignmentStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionAssignmentStatementMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("leftHandSide") CLeftHandSide pLeftHandSide,
        @JsonProperty("rightHandSide") CExpression pRightHandSide) {}
  }

  /**
   * This class is a mixin for {@link CExpressionStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionStatementMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("expression") CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link CFAEdge}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonIdentityInfo(
      generator = CfaEdgeIdGenerator.class,
      resolver = CfaEdgeIdResolver.class,
      property = "edgeNumber")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfCFAEdge")
  private static final class CFAEdgeMixin {}

  /**
   * This class is a mixin for {@link CFALabelNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFALabelNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFALabelNodeMixin(
        @JsonProperty("function") AFunctionDeclaration pFunction,
        @JsonProperty("label") String pLabel) {}
  }

  /**
   * This class is a mixin for {@link CfaMetadata}.
   *
   * <p>It ensures that the {@link AstCfaRelation} is not being serialized.
   */
  private static final class CfaMetadataMixin {

    @SuppressWarnings("unused")
    @JsonIgnore
    private AstCfaRelation astCFARelation;
  }

  /**
   * This class is a mixin for {@link CFANode}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   *
   * <p>Edges are serialized as IDs.
   *
   * <p>outOfScopeVariables are sorted to ensure deterministic serialization.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  @JsonIdentityInfo(
      generator = PropertyGenerator.class,
      scope = CFANode.class,
      property = "nodeNumber")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfCFANode")
  private static final class CFANodeMixin {

    @SuppressWarnings("unused")
    @JsonIdentityReference(alwaysAsId = true)
    private List<CFAEdge> leavingEdges;

    @SuppressWarnings("unused")
    @JsonIdentityReference(alwaysAsId = true)
    private List<CFAEdge> enteringEdges;

    @SuppressWarnings("unused")
    @JsonSerialize(converter = OutOfScopeToSortedListConverter.class)
    private Set<CSimpleDeclaration> outOfScopeVariables;

    @SuppressWarnings("unused")
    @JsonCreator
    public CFANodeMixin(@JsonProperty("function") AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link CFunctionDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionDeclarationMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("type") CFunctionType pType,
        @JsonProperty("name") String pName,
        @JsonProperty("origName") String pOrigName,
        @JsonProperty("parameters") List<CParameterDeclaration> parameters,
        @JsonProperty("attributes") ImmutableSet<FunctionAttribute> pAttributes) {}
  }

  /**
   * This class is a mixin for {@link CFunctionEntryNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionEntryNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionEntryNodeMixin(
        @JsonProperty("location") FileLocation pFileLocation,
        @JsonProperty("functionDefinition") CFunctionDeclaration pFunctionDefinition,
        @JsonProperty("exitNode") FunctionExitNode pExitNode,
        @JsonProperty("returnVariable") Optional<CVariableDeclaration> pReturnVariable) {}
  }

  /**
   * This class is a mixin for {@link CFunctionType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeMixin(
        @JsonProperty("returnType") CType pReturnType,
        @JsonProperty("parameters") List<CType> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link CFunctionTypeWithNames}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeWithNamesMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeWithNamesMixin(
        @JsonProperty("returnType") CType pReturnType,
        @JsonProperty("parameters") List<CParameterDeclaration> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link CIdExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIdExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIdExpressionMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("type") CType pType,
        @JsonProperty("name") String pName,
        @JsonProperty("declaration") CSimpleDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link CInitializerExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CInitializerExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CInitializerExpressionMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("expression") CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link CIntegerLiteralExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIntegerLiteralExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIntegerLiteralExpressionMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("type") CType pType,
        @JsonProperty("value") BigInteger pValue) {}
  }

  /**
   * This class is a mixin for {@link CReturnStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("expression") Optional<CExpression> pExpression,
        @JsonProperty("assignment") Optional<CAssignment> pAssignment) {}
  }

  /**
   * This class is a mixin for {@link CReturnStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementEdgeMixin(
        @JsonProperty("rawStatement") String pRawStatement,
        @JsonProperty("returnStatement") CReturnStatement pReturnStatement,
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("predecessor") CFANode pPredecessor,
        @JsonProperty("successor") FunctionExitNode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link CSimpleType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CSimpleTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CSimpleTypeMixin(
        @JsonProperty("isConst") boolean pConst,
        @JsonProperty("isVolatile") boolean pVolatile,
        @JsonProperty("type") CBasicType pType,
        @JsonProperty("isLong") boolean pIsLong,
        @JsonProperty("isShort") boolean pIsShort,
        @JsonProperty("isSigned") boolean pIsSigned,
        @JsonProperty("isUnsigned") boolean pIsUnsigned,
        @JsonProperty("isComplex") boolean pIsComplex,
        @JsonProperty("isImaginary") boolean pIsImaginary,
        @JsonProperty("isLongLong") boolean pIsLongLong) {}
  }

  /**
   * This class is a mixin for {@link CStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CStatementEdgeMixin(
        @JsonProperty("rawStatement") String pRawStatement,
        @JsonProperty("statement") CStatement pStatement,
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("predecessor") CFANode pPredecessor,
        @JsonProperty("successor") CFANode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link CVariableDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CVariableDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CVariableDeclarationMixin(
        @JsonProperty("fileLocation") FileLocation pFileLocation,
        @JsonProperty("isGlobal") boolean pIsGlobal,
        @JsonProperty("cStorageClass") CStorageClass pCStorageClass,
        @JsonProperty("type") CType pType,
        @JsonProperty("name") String pName,
        @JsonProperty("origName") String pOrigName,
        @JsonProperty("qualifiedName") String pQualifiedName,
        @JsonProperty("initializer") CInitializer pInitializer) {}
  }

  /**
   * This class is a mixin for {@link FileLocation}.
   *
   * <p>It sets the order of the fields to ensure deterministic serialization.
   *
   * <p>It forces the serialization of the {@link Path} fileName field.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  @JsonPropertyOrder({
    "fileName",
    "niceFileName",
    "offset",
    "length",
    "startingLine",
    "endingLine",
    "startColumnInLine",
    "endColumnInLine",
    "startingLineInOrigin",
    "endingLineInOrigin",
    "offsetRelatedToOrigin"
  })
  private static final class FileLocationMixin {

    @SuppressWarnings("unused")
    @JsonProperty
    private Path fileName;

    @SuppressWarnings("unused")
    @JsonCreator
    public FileLocationMixin(
        @JsonProperty("fileName") Path pFileName,
        @JsonProperty("niceFileName") String pNiceFileName,
        @JsonProperty("offset") int pOffset,
        @JsonProperty("length") int pLength,
        @JsonProperty("startingLine") int pStartingLine,
        @JsonProperty("endingLine") int pEndingLine,
        @JsonProperty("startColumnInLine") int pStartColumnInLine,
        @JsonProperty("endColumnInLine") int pEndColumnInLine,
        @JsonProperty("startingLineInOrigin") int pStartingLineInOrigin,
        @JsonProperty("endingLineInOrigin") int pEndingLineInOrigin,
        @JsonProperty("offsetRelatedToOrigin") boolean pOffsetRelatedToOrigin) {}
  }

  /**
   * This class is a mixin for {@link FunctionEntryNode}.
   *
   * <p>It serializes its {@link FunctionExitNode} field as number.
   */
  private static final class FunctionEntryNodeMixin {

    @SuppressWarnings("unused")
    @JsonIdentityReference(alwaysAsId = true)
    private FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link FunctionExitNode}.
   *
   * <p>It serializes its {@link FunctionEntryNode} field as number.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class FunctionExitNodeMixin {

    @SuppressWarnings("unused")
    @JsonIdentityReference(alwaysAsId = true)
    private FunctionEntryNode entryNode;

    @SuppressWarnings("unused")
    @JsonCreator
    public FunctionExitNodeMixin(@JsonProperty("function") AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link Loop}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopMixin(
        @JsonProperty("loopHeads") Set<CFANode> pLoopHeads,
        @JsonProperty("nodes") Set<CFANode> pNodes) {}
  }

  /**
   * This class is a mixin for {@link LoopStructure}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopStructureMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopStructureMixin(@JsonProperty("loops") ImmutableListMultimap<String, Loop> pLoops) {}
  }

  /**
   * This class is a mixin for {@link Partition}.
   *
   * <p>It prevents cyclic references by serializing the {@link Partition} as index.
   *
   * <p>Since the first occurrences of partitions are fully serialized by {@link
   * PartitionsSerializer}, partitions are always serialized as their index.
   */
  @JsonIdentityInfo(
      generator = PropertyGenerator.class,
      resolver = PartitionIdResolver.class,
      scope = Partition.class,
      property = "index")
  @JsonIdentityReference(alwaysAsId = true)
  private static final class PartitionMixin {}

  /**
   * This class is a mixin for {@link Type}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfType")
  private static final class TypeMixin {}

  /**
   * This class is a mixin for {@link VariableClassification}.
   *
   * <p>It sets the {@link PartitionsDeserializer} for all Set<Partition> fields.
   *
   * <p>It converts the edgeToPartitions field to a list of TableEntry objects during serialization
   * and back to a Table object during deserialization.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class VariableClassificationMixin {

    @SuppressWarnings("unused")
    @JsonDeserialize(using = PartitionsDeserializer.class)
    private Set<Partition> partitions;

    @SuppressWarnings("unused")
    @JsonDeserialize(using = PartitionsDeserializer.class)
    private Set<Partition> intBoolPartitions;

    @SuppressWarnings("unused")
    @JsonDeserialize(using = PartitionsDeserializer.class)
    private Set<Partition> intEqualPartitions;

    @SuppressWarnings("unused")
    @JsonDeserialize(using = PartitionsDeserializer.class)
    private Set<Partition> intAddPartitions;

    @SuppressWarnings("unused")
    @JsonSerialize(converter = EdgeToPartitionsTableToListConverter.class)
    @JsonDeserialize(using = EdgeToPartitionsDeserializer.class)
    private Table<CFAEdge, Integer, Partition> edgeToPartitions;

    @SuppressWarnings("unused")
    @JsonCreator
    VariableClassificationMixin(
        @JsonProperty("hasRelevantNonIntAddVars") boolean pHasRelevantNonIntAddVars,
        @JsonProperty("intBoolVars") Set<String> pIntBoolVars,
        @JsonProperty("intEqualVars") Set<String> pIntEqualVars,
        @JsonProperty("intAddVars") Set<String> pIntAddVars,
        @JsonProperty("intOverflowVars") Set<String> pIntOverflowVars,
        @JsonProperty("relevantVariables") Set<String> pRelevantVariables,
        @JsonProperty("addressedVariables") Set<String> pAddressedVariables,
        @JsonProperty("relevantFields") Multimap<CCompositeType, String> pRelevantFields,
        @JsonProperty("addressedFields") Multimap<CCompositeType, String> pAddressedFields,
        @JsonProperty("partitions") Collection<Partition> pPartitions,
        @JsonProperty("intBoolPartitions") Set<Partition> pIntBoolPartitions,
        @JsonProperty("intEqualPartitions") Set<Partition> pIntEqualPartitions,
        @JsonProperty("intAddPartitions") Set<Partition> pIntAddPartitions,
        @JsonProperty("edgeToPartitions") Table<CFAEdge, Integer, Partition> pEdgeToPartitions,
        @JsonProperty("assumedVariables") Multiset<String> pAssumedVariables,
        @JsonProperty("assignedVariables") Multiset<String> pAssignedVariables) {}
  }
}
