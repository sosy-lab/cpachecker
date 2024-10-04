// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json;

import com.fasterxml.jackson.databind.util.ClassUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.EdgeToPartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionIdResolver;
import org.sosy_lab.cpachecker.cfa.export.json.deserialization.PartitionsDeserializer;
import org.sosy_lab.cpachecker.cfa.export.json.serialization.PartitionsSerializer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * The PartitionHandler class is responsible for constructing instances of the {@link Partition}
 * class.
 *
 * <p>It provides methods for adding variables, values, edges, and mappings between variables and
 * partitions.
 *
 * <p>The getReference() method returns the Partition object.
 *
 * @see PartitionsSerializer
 * @see EdgeToPartitionsDeserializer
 * @see PartitionsDeserializer
 * @see PartitionIdResolver
 */
public class PartitionHandler {
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

      partition =
          (Partition) partitionConstructor.newInstance(this.varToPartition, this.edgeToPartition);

      /* Set the index field of the partition. */
      writeIndexField(pIndex);

      /* Set handler fields to partition fields. */
      this.vars = readVars();
      this.values = readValues();
      this.edges = readEdges();

    } catch (NoSuchMethodException
        | NoSuchFieldException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
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

  public PartitionHandler addEdgeToPartition(CFAEdge pEdge, Integer pIndex, Partition pPartition) {
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

      field.set(partition, pIndex);

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
