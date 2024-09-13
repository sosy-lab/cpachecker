// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/* This class is a Jackson module for serialization and deserialization. */
public class CfaJsonModule extends com.fasterxml.jackson.databind.module.SimpleModule {

  private static final long serialVersionUID = 1945912240762984485L;

  private static org.sosy_lab.common.log.LogManager logger;

  /* This record represents the CFA data. */
  final record CfaJsonData(
      com.google.common.collect.TreeMultimap<String, org.sosy_lab.cpachecker.cfa.model.CFANode>
          nodes,
      java.util.Set<org.sosy_lab.cpachecker.cfa.model.CFAEdge> edges,
      java.util.NavigableMap<String, org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode> functions,
      @JsonSerialize(using = PartitionsSerializer.class)
          @JsonDeserialize(using = PartitionsDeserializer.class)
          java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition> partitions,
      org.sosy_lab.cpachecker.cfa.CfaMetadata metadata) {}

  /**
   * Constructs a new CfaJsonModule with the specified logger.
   *
   * @param pLogger The logger to be used by the module.
   */
  public CfaJsonModule(org.sosy_lab.common.log.LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * Sets up the module by registering all mixins.
   *
   * @param pContext The setup context.
   */
  @Override
  public void setupModule(SetupContext pContext) {
    super.setupModule(pContext);

    /* Register all mixins. */
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.CFAEdge.class, CFAEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.CfaMetadata.class, CfaMetadataMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.CFANode.class, CFANodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.class,
        CFunctionDeclarationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.FileLocation.class, FileLocationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode.class, FunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.FunctionExitNode.class, FunctionExitNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.variableclassification.Partition.class, PartitionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CFunctionType.class, CFunctionTypeMixin.class);
    pContext.setMixInAnnotations(org.sosy_lab.cpachecker.cfa.types.Type.class, TypeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames.class,
        CFunctionTypeWithNamesMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.types.c.CSimpleType.class, CSimpleTypeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.AAstNode.class, AAstNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration.class,
        CVariableDeclarationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression.class,
        CInitializerExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.class,
        CIntegerLiteralExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode.class,
        CFunctionEntryNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression.class, CIdExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.CFALabelNode.class, CFALabelNodeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.BlankEdge.class, BlankEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge.class, CDeclarationEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge.class, CAssumeEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.class, CBinaryExpressionMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge.class, CStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement.class,
        CExpressionAssignmentStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement.class,
        CExpressionStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge.class,
        CReturnStatementEdgeMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement.class, CReturnStatementMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.LoopStructure.class, LoopStructureMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.variableclassification.VariableClassification.class,
        VariableClassificationMixin.class);
    pContext.setMixInAnnotations(
        org.sosy_lab.cpachecker.util.LoopStructure.Loop.class, LoopMixin.class);
  }

  /**
   * Custom JSON serializer for serializing a set of {@link
   * org.sosy_lab.cpachecker.util.variableclassification.Partition}s.
   *
   * <p>It serializes the partitions as an array of objects. Each object represents a partition.
   */
  private static class PartitionsSerializer
      extends com.fasterxml.jackson.databind.JsonSerializer<
          java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>> {

    @Override
    public void serialize(
        java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition> pPartitions,
        com.fasterxml.jackson.core.JsonGenerator pGenerator,
        com.fasterxml.jackson.databind.SerializerProvider pProvider)
        throws java.io.IOException {
      pGenerator.writeStartArray();

      for (org.sosy_lab.cpachecker.util.variableclassification.Partition partition : pPartitions) {
        pGenerator.writeStartObject();

        /* Index */
        pGenerator.writeNumberField("index", partition.hashCode());

        /* Vars */
        pGenerator.writeArrayFieldStart("vars");
        for (java.lang.String var : partition.getVars()) {
          pGenerator.writeString(var);
        }
        pGenerator.writeEndArray();

        /* Values */
        pGenerator.writeArrayFieldStart("values");
        for (java.math.BigInteger value : partition.getValues()) {
          pGenerator.writeObject(value);
        }
        pGenerator.writeEndArray();

        /* Edges */
        pGenerator.writeArrayFieldStart("edges");
        for (java.util.Map.Entry<
                org.sosy_lab.cpachecker.cfa.model.CFAEdge, java.util.Collection<java.lang.Integer>>
            entry : partition.getEdges().asMap().entrySet()) {
          pGenerator.writeStartObject();
          pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(entry.getKey()));
          pGenerator.writeArrayFieldStart("indices");
          for (java.lang.Integer index : entry.getValue()) {
            pGenerator.writeObject(index);
          }
          pGenerator.writeEndArray();
          pGenerator.writeEndObject();
        }
        pGenerator.writeEndArray();

        try {
          /* VarToPartition */
          /* Retrieve field via reflection. */
          java.lang.reflect.Field varToPartitionField =
              org.sosy_lab.cpachecker.util.variableclassification.Partition.class.getDeclaredField(
                  "varToPartition");
          varToPartitionField.setAccessible(true);
          @SuppressWarnings("unchecked")
          java.util.Map<
                  java.lang.String, org.sosy_lab.cpachecker.util.variableclassification.Partition>
              varToPartition =
                  (java.util.Map<
                          java.lang.String,
                          org.sosy_lab.cpachecker.util.variableclassification.Partition>)
                      varToPartitionField.get(partition);

          /* Write field. */
          pGenerator.writeObjectFieldStart("varToPartition");
          for (java.util.Map.Entry<
                  java.lang.String, org.sosy_lab.cpachecker.util.variableclassification.Partition>
              entry : varToPartition.entrySet()) {
            pGenerator.writeObjectField(entry.getKey(), entry.getValue().hashCode());
          }
          pGenerator.writeEndObject();

          /* EdgeToPartition */
          /* Retrieve field via reflection. */
          java.lang.reflect.Field edgeToPartitionField =
              org.sosy_lab.cpachecker.util.variableclassification.Partition.class.getDeclaredField(
                  "edgeToPartition");
          edgeToPartitionField.setAccessible(true);
          @SuppressWarnings("unchecked")
          com.google.common.collect.Table<
                  org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                  java.lang.Integer,
                  org.sosy_lab.cpachecker.util.variableclassification.Partition>
              edgeToPartition =
                  (com.google.common.collect.Table<
                          org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                          java.lang.Integer,
                          org.sosy_lab.cpachecker.util.variableclassification.Partition>)
                      edgeToPartitionField.get(partition);

          /* Write field. */
          pGenerator.writeArrayFieldStart("edgeToPartition");
          for (com.google.common.collect.Table.Cell<
                  org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                  java.lang.Integer,
                  org.sosy_lab.cpachecker.util.variableclassification.Partition>
              cell : edgeToPartition.cellSet()) {
            pGenerator.writeStartObject();
            pGenerator.writeNumberField("edge", CfaEdgeIdGenerator.getIdFromEdge(cell.getRowKey()));
            pGenerator.writeNumberField("index", cell.getColumnKey());
            pGenerator.writeNumberField("partition", cell.getValue().hashCode());
            pGenerator.writeEndObject();
          }
          pGenerator.writeEndArray();

        } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new java.io.IOException("Error while serializing partition: " + e.getMessage(), e);
        }

        pGenerator.writeEndObject();
      }

      pGenerator.writeEndArray();
    }
  }

  private static class PartitionsDeserializer
      extends com.fasterxml.jackson.databind.JsonDeserializer<
          java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>> {

    @Override
    public java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition> deserialize(
        com.fasterxml.jackson.core.JsonParser pParser,
        com.fasterxml.jackson.databind.DeserializationContext pContext)
        throws java.io.IOException {
      // CfaEdgeIdResolver.getEdgeFromId());

      return null;
    }
  }

  /**
   * Represents an entry in a {@link com.google.common.collect.Table}.
   *
   * <p>This record encapsulates information about a CFAEdge, an index, and a Partition.
   */
  private record TableEntry(
      org.sosy_lab.cpachecker.cfa.model.CFAEdge edge,
      java.lang.Integer index,
      org.sosy_lab.cpachecker.util.variableclassification.Partition partition) {}

  /**
   * A converter class that converts a {@link com.google.common.collect.Table} object to a list of
   * {@link TableEntry} objects.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions.
   */
  private static final class EtpTableToListConverter
      extends com.fasterxml.jackson.databind.util.StdConverter<
          com.google.common.collect.Table<
              org.sosy_lab.cpachecker.cfa.model.CFAEdge,
              java.lang.Integer,
              org.sosy_lab.cpachecker.util.variableclassification.Partition>,
          java.util.List<TableEntry>> {
    @Override
    public java.util.List<TableEntry> convert(
        com.google.common.collect.Table<
                org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                java.lang.Integer,
                org.sosy_lab.cpachecker.util.variableclassification.Partition>
            pTable) {
      return pTable.cellSet().stream()
          .map(cell -> new TableEntry(cell.getRowKey(), cell.getColumnKey(), cell.getValue()))
          .collect(java.util.stream.Collectors.toList());
    }
  }

  /**
   * A custom generator for generating unique IDs for CFA edges.
   *
   * <p>It is used to retrieve IDs from their respective {@link
   * org.sosy_lab.cpachecker.cfa.model.CFAEdge}s.
   */
  private static final class CfaEdgeIdGenerator
      extends com.fasterxml.jackson.annotation.ObjectIdGenerator<java.lang.Integer> {

    private static final long serialVersionUID = 7470151299045493234L;
    private static CfaEdgeIdGenerator currentGenerator;

    private final java.util.Map<org.sosy_lab.cpachecker.cfa.model.CFAEdge, java.lang.Integer>
        edgeToIdMap = new java.util.HashMap<>();
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
    public boolean canUseFor(com.fasterxml.jackson.annotation.ObjectIdGenerator<?> pGenerator) {
      return pGenerator.getClass() == this.getClass() && pGenerator.getScope() == this.scope;
    }

    /* Creates a new instance of CfaEdgeIdGenerator with the given scope. */
    @Override
    public com.fasterxml.jackson.annotation.ObjectIdGenerator<java.lang.Integer> forScope(
        Class<?> pScope) {
      return this.scope == pScope ? this : new CfaEdgeIdGenerator(pScope, this.nextValue);
    }

    /**
     * Generates an ID for the given object.
     *
     * @param pForObject The object for which to generate the ID.
     * @return The generated ID.
     */
    @Override
    public java.lang.Integer generateId(Object pForObject) {
      if (pForObject == null) {
        return null;
      } else {
        int id = this.nextValue++;
        edgeToIdMap.put((org.sosy_lab.cpachecker.cfa.model.CFAEdge) pForObject, id);
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
      return pKey == null
          ? null
          : new com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey(
              this.getClass(), this.scope, pKey);
    }

    /**
     * Creates a new instance of the CfaEdgeIdGenerator for serialization.
     *
     * @param pContext The context object.
     * @return The new instance of the CfaEdgeIdGenerator.
     */
    @Override
    public com.fasterxml.jackson.annotation.ObjectIdGenerator<java.lang.Integer>
        newForSerialization(Object pContext) {
      return currentGenerator = new CfaEdgeIdGenerator(this.scope, 1);
    }

    /**
     * Retrieves the ID of a {@link org.sosy_lab.cpachecker.cfa.model.CFAEdge}.
     *
     * @param pEdge The CFAEdge.
     * @return The ID of the CFAEdge.
     * @throws IllegalStateException If no generator was set.
     * @throws IllegalArgumentException If no ID for the CFAEdge is found.
     */
    public static java.lang.Integer getIdFromEdge(org.sosy_lab.cpachecker.cfa.model.CFAEdge pEdge) {
      if (currentGenerator == null) {
        throw new IllegalStateException("No generator available");
      }

      java.lang.Integer id = currentGenerator.edgeToIdMap.get(pEdge);

      if (id == null) {
        throw new IllegalArgumentException("No ID for edge " + pEdge + " found");
      }

      return id;
    }
  }

  /**
   * This class is a custom {@link com.fasterxml.jackson.annotation.ObjectIdResolver}.
   *
   * <p>It is used to retrieve {@link org.sosy_lab.cpachecker.cfa.model.CFAEdge}s from their
   * respective IDs.
   */
  private static class CfaEdgeIdResolver
      extends com.fasterxml.jackson.annotation.SimpleObjectIdResolver {
    private static CfaEdgeIdResolver currentResolver;

    private final java.util.Map<java.lang.Integer, org.sosy_lab.cpachecker.cfa.model.CFAEdge>
        idToEdgeMap = new java.util.HashMap<>();

    /**
     * Creates a new instance of {@link CfaEdgeIdResolver} for deserialization.
     *
     * <p>It also sets the currentResolver field to the newly created instance for later use.
     *
     * @param pContext The deserialization context object.
     * @return The newly created instance.
     */
    @Override
    public com.fasterxml.jackson.annotation.ObjectIdResolver newForDeserialization(
        Object pContext) {
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
    public void bindItem(
        com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey pId, Object pItem) {
      if (pId.key.getClass() != java.lang.Integer.class) {
        throw new IllegalArgumentException(
            "Wrong key: " + pId.key.getClass().getSimpleName() + " is not an Integer");
      }

      if (!(pItem instanceof org.sosy_lab.cpachecker.cfa.model.CFAEdge)) {
        throw new IllegalArgumentException(
            "Wrong object: " + pItem.getClass().getSimpleName() + " is not a CFAEdge");
      }

      idToEdgeMap.put(
          (java.lang.Integer) pId.key, (org.sosy_lab.cpachecker.cfa.model.CFAEdge) pItem);
      super.bindItem(pId, pItem);
    }

    /**
     * Retrieves a {@link org.sosy_lab.cpachecker.cfa.model.CFAEdge} from its ID.
     *
     * @param pId The ID of the CFAEdge.
     * @return The CFAEdge with the specified ID.
     * @throws IllegalStateException If no resolver was set.
     * @throws IllegalArgumentException If no CFAEdge with the specified ID is found.
     */
    public static org.sosy_lab.cpachecker.cfa.model.CFAEdge getEdgeFromId(java.lang.Integer pId) {
      if (currentResolver == null) {
        throw new IllegalStateException("No resolver available");
      }

      org.sosy_lab.cpachecker.cfa.model.CFAEdge edge = currentResolver.idToEdgeMap.get(pId);

      if (edge == null) {
        throw new IllegalArgumentException("No edge with ID " + pId + " found");
      }

      return edge;
    }
  }

  /**
   * A converter class that converts a list of {@link TableEntry} objects to a {@link
   * com.google.common.collect.Table} object.
   *
   * <p>The Table object represents a mapping between CFAEdges, Integers, and Partitions.
   */
  private static final class ListToEtpTableConverter
      extends com.fasterxml.jackson.databind.util.StdConverter<
          java.util.List<TableEntry>,
          com.google.common.collect.Table<
              org.sosy_lab.cpachecker.cfa.model.CFAEdge,
              java.lang.Integer,
              org.sosy_lab.cpachecker.util.variableclassification.Partition>> {
    @Override
    public com.google.common.collect.Table<
            org.sosy_lab.cpachecker.cfa.model.CFAEdge,
            java.lang.Integer,
            org.sosy_lab.cpachecker.util.variableclassification.Partition>
        convert(java.util.List<TableEntry> pList) {
      return pList.stream()
          .collect(
              com.google.common.collect.HashBasedTable::create,
              (table, entry) -> table.put(entry.edge, entry.index, entry.partition),
              com.google.common.collect.HashBasedTable::putAll);
    }
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.util.LoopStructure.Loop}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopMixin(
        @JsonProperty("loopHeads")
            java.util.Set<org.sosy_lab.cpachecker.cfa.model.CFANode> pLoopHeads,
        @JsonProperty("nodes") java.util.Set<org.sosy_lab.cpachecker.cfa.model.CFANode> pNodes) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.util.variableclassification.VariableClassification}.
   *
   * <p>It converts the edgeToPartitions field to a list of TableEntry objects during serialization
   * and back to a Table object during deserialization.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class VariableClassificationMixin {

    @JsonSerialize(converter = EtpTableToListConverter.class)
    @JsonDeserialize(converter = ListToEtpTableConverter.class)
    private com.google.common.collect.Table<
            org.sosy_lab.cpachecker.cfa.model.CFAEdge,
            java.lang.Integer,
            org.sosy_lab.cpachecker.util.variableclassification.Partition>
        edgeToPartitions;

    @SuppressWarnings("unused")
    @JsonCreator
    VariableClassificationMixin(
        @JsonProperty("hasRelevantNonIntAddVars") boolean pHasRelevantNonIntAddVars,
        @JsonProperty("intBoolVars") java.util.Set<java.lang.String> pIntBoolVars,
        @JsonProperty("intEqualVars") java.util.Set<java.lang.String> pIntEqualVars,
        @JsonProperty("intAddVars") java.util.Set<java.lang.String> pIntAddVars,
        @JsonProperty("intOverflowVars") java.util.Set<java.lang.String> pIntOverflowVars,
        @JsonProperty("relevantVariables") java.util.Set<java.lang.String> pRelevantVariables,
        @JsonProperty("addressedVariables") java.util.Set<java.lang.String> pAddressedVariables,
        @JsonProperty("relevantFields")
            com.google.common.collect.Multimap<
                    org.sosy_lab.cpachecker.cfa.types.c.CCompositeType, java.lang.String>
                pRelevantFields,
        @JsonProperty("addressedFields")
            com.google.common.collect.Multimap<
                    org.sosy_lab.cpachecker.cfa.types.c.CCompositeType, java.lang.String>
                pAddressedFields,
        @JsonProperty("partitions")
            java.util.Collection<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pPartitions,
        @JsonProperty("intBoolPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntBoolPartitions,
        @JsonProperty("intEqualPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntEqualPartitions,
        @JsonProperty("intAddPartitions")
            java.util.Set<org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pIntAddPartitions,
        @JsonProperty("edgeToPartitions")
            com.google.common.collect.Table<
                    org.sosy_lab.cpachecker.cfa.model.CFAEdge,
                    java.lang.Integer,
                    org.sosy_lab.cpachecker.util.variableclassification.Partition>
                pEdgeToPartitions,
        @JsonProperty("assumedVariables")
            com.google.common.collect.Multiset<java.lang.String> pAssumedVariables,
        @JsonProperty("assignedVariables")
            com.google.common.collect.Multiset<java.lang.String> pAssignedVariables) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.util.LoopStructure}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class LoopStructureMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    private LoopStructureMixin(
        @JsonProperty("loops")
            com.google.common.collect.ImmutableListMultimap<
                    java.lang.String, org.sosy_lab.cpachecker.util.LoopStructure.Loop>
                pLoops) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CExpression> pExpression,
        @JsonProperty("assignment")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CAssignment> pAssignment) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CReturnStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CReturnStatementEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("returnStatement")
            org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement pReturnStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.FunctionExitNode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CExpressionAssignmentStatementMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CExpressionAssignmentStatementMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("leftHandSide") org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide pLeftHandSide,
        @JsonProperty("rightHandSide")
            org.sosy_lab.cpachecker.cfa.ast.c.CExpression pRightHandSide) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CStatementEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CStatementEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("statement") org.sosy_lab.cpachecker.cfa.ast.c.CStatement pStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CBinaryExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CBinaryExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pExpressionType,
        @JsonProperty("calculationType") org.sosy_lab.cpachecker.cfa.types.c.CType pCalculationType,
        @JsonProperty("operand1") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pOperand1,
        @JsonProperty("operand2") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pOperand2,
        @JsonProperty("operator")
            org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator pOperator) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CAssumeEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CAssumeEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression,
        @JsonProperty("truthAssumption") boolean pTruthAssumption,
        @JsonProperty("swapped") boolean pSwapped,
        @JsonProperty("artificialIntermediate") boolean pArtificial) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CDeclarationEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CDeclarationEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawSignature,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("declaration") org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.BlankEdge}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class BlankEdgeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public BlankEdgeMixin(
        @JsonProperty("rawStatement") java.lang.String pRawStatement,
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("predecessor") org.sosy_lab.cpachecker.cfa.model.CFANode pPredecessor,
        @JsonProperty("successor") org.sosy_lab.cpachecker.cfa.model.CFANode pSuccessor,
        @JsonProperty("description") java.lang.String pDescription) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFALabelNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFALabelNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFALabelNodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction,
        @JsonProperty("label") java.lang.String pLabel) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIdExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIdExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("declaration")
            org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration pDeclaration) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionEntryNodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionEntryNodeMixin(
        @JsonProperty("location") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("functionDefinition")
            org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration pFunctionDefinition,
        @JsonProperty("exitNode") org.sosy_lab.cpachecker.cfa.model.FunctionExitNode pExitNode,
        @JsonProperty("returnVariable")
            java.util.Optional<org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration>
                pReturnVariable) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CIntegerLiteralExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CIntegerLiteralExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("value") java.math.BigInteger pValue) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CInitializerExpressionMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CInitializerExpressionMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("expression") org.sosy_lab.cpachecker.cfa.ast.c.CExpression pExpression) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CVariableDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CVariableDeclarationMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("isGlobal") boolean pIsGlobal,
        @JsonProperty("cStorageClass")
            org.sosy_lab.cpachecker.cfa.types.c.CStorageClass pCStorageClass,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("origName") java.lang.String pOrigName,
        @JsonProperty("qualifiedName") java.lang.String pQualifiedName,
        @JsonProperty("initializer") org.sosy_lab.cpachecker.cfa.ast.c.CInitializer pInitializer) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.AAstNode}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfAAstNode")
  private static final class AAstNodeMixin {}

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.c.CSimpleType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CSimpleTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CSimpleTypeMixin(
        @JsonProperty("isConst") boolean pConst,
        @JsonProperty("isVolatile") boolean pVolatile,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CBasicType pType,
        @JsonProperty("isLong") boolean pIsLong,
        @JsonProperty("isShort") boolean pIsShort,
        @JsonProperty("isSigned") boolean pIsSigned,
        @JsonProperty("isUnsigned") boolean pIsUnsigned,
        @JsonProperty("isComplex") boolean pIsComplex,
        @JsonProperty("isImaginary") boolean pIsImaginary,
        @JsonProperty("isLongLong") boolean pIsLongLong) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeWithNamesMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeWithNamesMixin(
        @JsonProperty("returnType") org.sosy_lab.cpachecker.cfa.types.c.CType pReturnType,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.types.Type}.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   */
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfType")
  private static final class TypeMixin {}

  /**
   * This class is a mixin for {@link org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionTypeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionTypeMixin(
        @JsonProperty("returnType") org.sosy_lab.cpachecker.cfa.types.c.CType pReturnType,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.types.c.CType> pParameters,
        @JsonProperty("takesVarArgs") boolean pTakesVarArgs) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.FileLocation}.
   *
   * <p>It forces the serialization of the {@link java.nio.file.Path} fileName field.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class FileLocationMixin {

    @JsonProperty private java.nio.file.Path fileName;

    @SuppressWarnings("unused")
    @JsonCreator
    public FileLocationMixin(
        @JsonProperty("fileName") java.nio.file.Path pFileName,
        @JsonProperty("niceFileName") java.lang.String pNiceFileName,
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
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFAEdge}.
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
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.CfaMetadata}.
   *
   * <p>It ensures that the {@link org.sosy_lab.cpachecker.util.ast.AstCfaRelation} is not being
   * serialized.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CfaMetadataMixin {

    @JsonIgnore private org.sosy_lab.cpachecker.util.ast.AstCfaRelation astCFARelation;

    @SuppressWarnings("unused")
    @JsonCreator
    private CfaMetadataMixin(
        @JsonProperty("machineModel") org.sosy_lab.cpachecker.cfa.types.MachineModel pMachineModel,
        @JsonProperty("language") org.sosy_lab.cpachecker.cfa.Language pLanguage,
        @JsonProperty("fileNames") java.util.List<java.nio.file.Path> pFileNames,
        @JsonProperty("mainFunctionEntry")
            org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode pMainFunctionEntry,
        @JsonProperty("connectedness") org.sosy_lab.cpachecker.cfa.CfaConnectedness pConnectedness,
        @JsonProperty("astCFARelation")
            org.sosy_lab.cpachecker.util.ast.AstCfaRelation pAstCfaRelation,
        @JsonProperty("loopStructure") org.sosy_lab.cpachecker.util.LoopStructure pLoopStructure,
        @JsonProperty("variableClassification")
            org.sosy_lab.cpachecker.util.variableclassification.VariableClassification
                pVariableClassification,
        @JsonProperty("liveVariables") org.sosy_lab.cpachecker.util.LiveVariables pLiveVariables) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.CFANode}.
   *
   * <p>Identity information is being serialized to prevent infinite recursion.
   *
   * <p>Type information is being serialized to account for subtype polymorphism.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  @JsonIdentityInfo(
      generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class,
      scope = org.sosy_lab.cpachecker.cfa.model.CFANode.class,
      property = "nodeNumber")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "typeOfCFANode")
  private static final class CFANodeMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFANodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration}.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class CFunctionDeclarationMixin {

    @SuppressWarnings("unused")
    @JsonCreator
    public CFunctionDeclarationMixin(
        @JsonProperty("fileLocation") org.sosy_lab.cpachecker.cfa.ast.FileLocation pFileLocation,
        @JsonProperty("type") org.sosy_lab.cpachecker.cfa.types.c.CFunctionType pType,
        @JsonProperty("name") java.lang.String pName,
        @JsonProperty("origName") java.lang.String pOrigName,
        @JsonProperty("parameters")
            java.util.List<org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration> parameters,
        @JsonProperty("attributes")
            com.google.common.collect.ImmutableSet<
                    org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute>
                pAttributes) {}
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode}.
   *
   * <p>It serializes its {@link org.sosy_lab.cpachecker.cfa.model.FunctionExitNode} field as
   * number.
   */
  private static final class FunctionEntryNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private org.sosy_lab.cpachecker.cfa.model.FunctionExitNode exitNode;
  }

  /**
   * This class is a mixin for {@link org.sosy_lab.cpachecker.cfa.model.FunctionExitNode}.
   *
   * <p>It serializes its {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode} field as
   * number.
   *
   * <p>It specifies the constructor to use during deserialization.
   */
  private static final class FunctionExitNodeMixin {

    @JsonIdentityReference(alwaysAsId = true)
    private org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode entryNode;

    @SuppressWarnings("unused")
    @JsonCreator
    public FunctionExitNodeMixin(
        @JsonProperty("function") org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration pFunction) {}
  }

  /**
   * This class is a mixin for {@link
   * org.sosy_lab.cpachecker.util.variableclassification.Partition}.
   *
   * <p>It prevents cyclic references by serializing the {@link
   * org.sosy_lab.cpachecker.util.variableclassification.Partition} as index.
   *
   * <p>Since the first occurrences of partitions are fully serialized by {@link
   * PartitionsSerializer}, partitions are always serialized as their index.
   */
  @JsonIdentityInfo(
      generator = com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator.class,
      scope = org.sosy_lab.cpachecker.util.variableclassification.Partition.class,
      property = "index")
  @JsonIdentityReference(alwaysAsId = true)
  private static final class PartitionMixin {}
}
