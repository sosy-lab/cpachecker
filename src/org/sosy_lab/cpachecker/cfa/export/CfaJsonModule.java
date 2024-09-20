// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.Language;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/* This class is a Jackson module for serialization and deserialization. */
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
    pContext.setMixInAnnotations(CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(CFunctionDeclaration.class, CFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(CFunctionType.class, CFunctionTypeMixin.class);
    pContext.setMixInAnnotations(Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(CFunctionTypeWithNames.class, CFunctionTypeWithNamesMixin.class);
    pContext.setMixInAnnotations(CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(CVariableDeclaration.class, CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(CInitializerExpression.class, CInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(
        CIntegerLiteralExpression.class, CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(CFunctionEntryNode.class, CFunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(CIdExpression.class, CIdExpressionMixin.class);
    pContext.setMixInAnnotations(CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        CExpressionAssignmentStatement.class, CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(CExpressionStatement.class, CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(CReturnStatementEdge.class, CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(VariableClassification.class, VariableClassificationMixin.class);
    pContext.setMixInAnnotations(Loop.class, LoopMixin.class);
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

        /* Edges */
        pGenerator.writeArrayFieldStart("edges");
        for (Entry<CFAEdge, Collection<Integer>> entry : partition.getEdges().asMap().entrySet()) {
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
          Map<String, Partition> varToPartition = PartitionHandler.getVarToPartition(partition);

          /* Write field. */
          pGenerator.writeObjectFieldStart("varToPartition");
          for (Entry<String, Partition> entry : varToPartition.entrySet()) {
            pGenerator.writeObjectField(entry.getKey(), entry.getValue().hashCode());
          }
          pGenerator.writeEndObject();

          /* EdgeToPartition */
          /* Retrieve field via reflection. */
          Table<CFAEdge, Integer, Partition> edgeToPartition =
              PartitionHandler.getEdgeToPartition(partition);

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
    private static Map<Integer, Partition> deserializedPartitions = new HashMap<>();
    private Map<Integer, PartitionHandler> partitionHandlers = new HashMap<>();

    /* Retrieves a existing handler or creates a new one if it does not exist. */
    private PartitionHandler getPartitionHandler(int pIndex) throws IOException {
      PartitionHandler handler;

      if (partitionHandlers.containsKey(pIndex)) {

        handler = partitionHandlers.get(pIndex);
      } else {

        handler = new PartitionHandler(pIndex);
        partitionHandlers.put(pIndex, handler);
      }

      return handler;
    }

    /**
     * Deserialize a JSON representation of partitions into a set of {@link Partition} objects.
     *
     * @param pParser The JSON parser.
     * @param pContext The deserialization context.
     * @return The set of deserialized partitions.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @Override
    public Set<Partition> deserialize(JsonParser pParser, DeserializationContext pContext)
        throws IOException {

      /* Get root node. */
      ObjectMapper mapper = (ObjectMapper) pParser.getCodec();
      JsonNode rootNode = mapper.readTree(pParser);

      /* Iterate over the root node and construct PartitionHandlers. */
      for (JsonNode node : rootNode) {
        Integer index = node.get("index").asInt();

        PartitionHandler handler = getPartitionHandler(index);

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
          CFAEdge edge = CfaEdgeIdResolver.getEdgeFromId(etp.get("edge").asInt());
          Partition partition = getPartitionHandler(etp.get("partition").asInt()).getReference();

          handler.addEdgeToPartition(edge, etp.get("index").asInt(), partition);
        }

        deserializedPartitions.put(handler.getReference().hashCode(), handler.getReference());
      }

      return ImmutableSet.copyOf(deserializedPartitions.values());
    }
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

        /* Set handler fields to partition fields. */
        setIndexField(pIndex);
        this.vars = getVars();
        this.values = getValues();
        this.edges = getEdges();

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
     * Sets the index field of the partition.
     *
     * @param pIndex The new value for the index field.
     * @throws NoSuchFieldException If the index field does not exist in the Partition class.
     */
    private void setIndexField(int pIndex) throws NoSuchFieldException {
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
    public NavigableSet<String> getVars() {
      return (NavigableSet<String>) getField("vars", this.partition);
    }

    /* Retrieves the values field from the partition. */
    @SuppressWarnings("unchecked")
    public NavigableSet<BigInteger> getValues() {
      return (NavigableSet<BigInteger>) getField("values", this.partition);
    }

    /* Retrieves the edges field from the partition. */
    @SuppressWarnings("unchecked")
    public Multimap<CFAEdge, Integer> getEdges() {
      return (Multimap<CFAEdge, Integer>) getField("edges", this.partition);
    }

    /* Retrieves the varToPartition field from a Partition object. */
    @SuppressWarnings("unchecked")
    public static Map<String, Partition> getVarToPartition(Partition partition) {
      return (Map<String, Partition>) getField("varToPartition", partition);
    }

    /* Retrieves the edgeToPartition field from a Partition object. */
    @SuppressWarnings("unchecked")
    public static Table<CFAEdge, Integer, Partition> getEdgeToPartition(Partition partition) {
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
   * Represents an entry in a {@link Table}.
   *
   * <p>This record encapsulates information about a CFAEdge, an index, and a Partition.
   */
  private record TableEntry(CFAEdge edge, Integer index, Partition partition) {}

  /**
   * A converter class that converts a {@link Table} object to a list of {@link TableEntry} objects.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions.
   */
  private static final class EtpTableToListConverter
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
   * A custom generator for generating unique IDs for CFA edges.
   *
   * <p>It is used to retrieve IDs from their respective {@link CFAEdge}s.
   */
  private static final class CfaEdgeIdGenerator extends ObjectIdGenerator<Integer> {

    private static final long serialVersionUID = 7470151299045493234L;
    private static CfaEdgeIdGenerator currentGenerator;

    private final Map<CFAEdge, Integer> edgeToIdMap = new HashMap<>();
    private final Class<?> scope;
    private int nextValue;

    /**
     * Constructs a new CfaEdgeIdGenerator.
     *
     * <p>It is being used by Jackson.
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
     * @param pForObject The object for which to generate the ID.
     * @return The generated ID.
     */
    @Override
    public Integer generateId(Object pForObject) {
      if (pForObject == null) {
        return null;
      } else {
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
     * @param pContext The context object.
     * @return The new instance of the CfaEdgeIdGenerator.
     */
    @Override
    public ObjectIdGenerator<Integer> newForSerialization(Object pContext) {
      return currentGenerator = new CfaEdgeIdGenerator(this.scope, 1);
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
      checkState(currentGenerator != null, "No generator available");

      Integer id = currentGenerator.edgeToIdMap.get(pEdge);

      if (id == null) {
        throw new IllegalArgumentException("No ID for edge " + pEdge + " found");
      }

      return id;
    }
  }

  /**
   * This class is a custom {@link ObjectIdResolver}.
   *
   * <p>It is used for {@link Partition} objects.
   */
  private static class PartitionIdResolver extends SimpleObjectIdResolver {

    /**
     * Creates a new instance of ObjectIdResolver for deserialization.
     *
     * <p>It binds the previously deserialized partitions to their respective IDs.
     *
     * @param pContext The context object.
     * @return The newly created Resolver.
     * @throws IllegalStateException if no partitions are available.
     */
    @Override
    public ObjectIdResolver newForDeserialization(Object pContext) {
      checkState(
          PartitionsDeserializer.deserializedPartitions != null, "No partitions available to bind");

      PartitionIdResolver partitionIdResolver = new PartitionIdResolver();

      /* Bind previously deserialized partitions to their respective IDs. */
      for (Partition partition : PartitionsDeserializer.deserializedPartitions.values()) {
        partitionIdResolver.bindItem(
            new IdKey(Partition.class, Partition.class, Integer.valueOf(partition.hashCode())),
            partition);
      }

      return partitionIdResolver;
    }
  }

  /**
   * This class is a custom {@link ObjectIdResolver}.
   *
   * <p>It is used to retrieve {@link CFAEdge}s from their respective IDs.
   */
  private static class CfaEdgeIdResolver extends SimpleObjectIdResolver {
    private static CfaEdgeIdResolver currentResolver;

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
      return currentResolver = new CfaEdgeIdResolver();
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
      checkState(currentResolver != null, "No resolver available");

      CFAEdge edge = currentResolver.idToEdgeMap.get(pId);

      if (edge == null) {
        throw new IllegalArgumentException("No edge with ID " + pId + " found");
      }

      return edge;
    }
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
   * This class is a mixin for {@link VariableClassification}.
   *
   * <p>It converts the edgeToPartitions field to a list of TableEntry objects during serialization
   * and back to a Table object during deserialization.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class VariableClassificationMixin {

    @SuppressWarnings("unused")
    @JsonSerialize(converter = EtpTableToListConverter.class)
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
   * This class is a mixin for {@link CAssumeEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CAssumeEdgeMixin {

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
   * This class is a mixin for {@link FileLocation}.
   *
   * <p>It forces the serialization of the {@link Path} fileName field.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
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
   * This class is a mixin for {@link CfaMetadata}.
   *
   * <p>It ensures that the {@link AstCfaRelation} is not being serialized.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CfaMetadataMixin {

    @SuppressWarnings("unused")
    @JsonIgnore
    private AstCfaRelation astCFARelation;

    @SuppressWarnings("unused")
    @JsonCreator
    private CfaMetadataMixin(
        @JsonProperty("machineModel") MachineModel pMachineModel,
        @JsonProperty("language") Language pCFALanguage,
        @JsonProperty("inputLanguage") Language pInputLanguage,
        @JsonProperty("fileNames") List<Path> pFileNames,
        @JsonProperty("mainFunctionEntry") FunctionEntryNode pMainFunctionEntry,
        @JsonProperty("connectedness") CfaConnectedness pConnectedness,
        @JsonProperty("astCFARelation") AstCfaRelation pAstCfaRelation,
        @JsonProperty("loopStructure") LoopStructure pLoopStructure,
        @JsonProperty("variableClassification") VariableClassification pVariableClassification,
        @JsonProperty("liveVariables") LiveVariables pLiveVariables) {}
  }

  /**
   * This class is a mixin for {@link CFANode}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
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
}
