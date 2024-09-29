// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A custom generator for generating unique IDs for CFA edges.
 *
 * <p>It is used to retrieve IDs from their respective {@link CFAEdge}s.
 */
public final class CfaEdgeIdGenerator extends ObjectIdGenerator<Integer> {

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
   * Determines if this generator instance can be used for Object IDs of specific generator type and
   * scope.
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
