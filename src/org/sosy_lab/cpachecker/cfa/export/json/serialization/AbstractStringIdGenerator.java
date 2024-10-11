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
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;

/**
 * This abstract {@link ObjectIdGenerator} provides a base implementation for generating
 * string-based object IDs within a specified scope.
 *
 * <p>Additionally, this class overrides the default serialization and deserialization methods to
 * prevent the generator from being serialized or deserialized, as ObjectIdGenerator implements
 * Serializable.
 *
 * @see CfaJsonExport
 * @see SimpleNameIdGenerator
 * @see CfaNodeIdGenerator
 * @see CfaEdgeIdGenerator
 * @see FileLocationIdGenerator
 */
public abstract class AbstractStringIdGenerator extends ObjectIdGenerator<String> {
  private static final long serialVersionUID = 4819953343438971132L;

  protected final Class<?> scope;

  protected abstract AbstractStringIdGenerator newInstance(Class<?> pScope);

  /**
   * Constructor.
   *
   * @param pScope the class type that defines the scope for this ID generator.
   */
  protected AbstractStringIdGenerator(Class<?> pScope) {
    scope = pScope;
  }

  /**
   * Creates a new generator instance for serialization.
   *
   * @param pContext The context object.
   * @return The new instance of the generator.
   */
  @Override
  public ObjectIdGenerator<String> newForSerialization(Object pContext) {
    return newInstance(scope);
  }

  /**
   * Generates an IdKey object based on the given key.
   *
   * @param pKey The key used to generate the IdKey object.
   * @return The generated IdKey object.
   */
  @Override
  public IdKey key(Object pKey) {
    checkNotNull(pKey, "The key must not be null");

    return new ObjectIdGenerator.IdKey(this.getClass(), scope, pKey);
  }

  /**
   * Determines if this generator instance can be used for Object IDs of specific generator type and
   * scope.
   *
   * @param pGenerator The object generator to check.
   * @return true if the generator class and its scope match this generator, false otherwise.
   */
  @Override
  public boolean canUseFor(ObjectIdGenerator<?> pGenerator) {
    return pGenerator.getClass() == this.getClass() && pGenerator.getScope() == scope;
  }

  /* Returns a generator instance with the given scope. */
  @Override
  public ObjectIdGenerator<String> forScope(Class<?> pScope) {
    return this.scope == pScope ? this : newInstance(pScope);
  }

  /* Returns the scope of this generator. */
  @Override
  public Class<?> getScope() {
    return this.scope;
  }

  /* Custom serialization method to prevent serialization of the generator (ObjectIdGenerator implements Serializable). */
  @SuppressWarnings("unused")
  protected void writeObject(ObjectOutputStream pStream) throws IOException {
    throw new NotSerializableException(getClass().getName());
  }

  /* Custom deserialization method to prevent deserialization of the generator (ObjectIdGenerator implements Serializable). */
  @SuppressWarnings("unused")
  protected void readObject(ObjectInputStream pStream) throws IOException {
    throw new NotSerializableException(getClass().getName());
  }
}
