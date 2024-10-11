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
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.CFAEdgeMixin;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A custom generator for generating unique IDs for {@link CFAEdge}s.
 *
 * <p>It is used to retrieve IDs from their respective {@link CFAEdge}s.
 *
 * <p>It is necessary to store the {@link #currentGenerator} because the {@link CfaEdgeIdGenerator}
 * is not directly accessible from custom serializers and its construction is initialized with
 * Jackson annotations and therefore unchangeable.
 *
 * @see CfaJsonExport
 * @see CFAEdgeMixin
 * @see PartitionsSerializer
 */
public class CfaEdgeIdGenerator extends SimpleNameIdGenerator {
  private static final long serialVersionUID = -2736431011237852503L;

  private static ThreadLocal<CfaEdgeIdGenerator> currentGenerator = new ThreadLocal<>();

  private final Map<CFAEdge, String> edgeToIdMap;

  /* Jackson requires a default constructor for serialization. */
  public CfaEdgeIdGenerator() {
    this(Object.class);
  }

  /**
   * Constructs a new {@link CfaEdgeIdGenerator}.
   *
   * @param pScope The class scope for which the ID generator is created.
   */
  private CfaEdgeIdGenerator(Class<?> pScope) {
    super(pScope);
    this.edgeToIdMap = new HashMap<>();
  }

  /**
   * Creates a new instance of {@link CfaEdgeIdGenerator} with the specified scope.
   *
   * @param pScope The class scope for the new instance.
   * @return a new instance of {@link CfaEdgeIdGenerator}.
   */
  @Override
  protected AbstractStringIdGenerator newInstance(Class<?> pScope) {
    return new CfaEdgeIdGenerator(pScope);
  }

  /**
   * Creates a new instance of {@link CfaEdgeIdGenerator} for serialization.
   *
   * <p>It also sets the {@link #currentGenerator} field to the newly created instance for later
   * use.
   *
   * @param pContext The context object.
   * @return The new generator instance.
   */
  @Override
  public ObjectIdGenerator<String> newForSerialization(Object pContext) {
    CfaEdgeIdGenerator generator = new CfaEdgeIdGenerator(scope);
    currentGenerator.set(generator);
    return generator;
  }

  /**
   * Generates a unique identifier for the given object.
   *
   * <p>It also stores the generated ID in the {@link #edgeToIdMap} for later retrieval.
   *
   * @param pForObject The object for which to generate an ID; must not be null and must be an
   *     instance of {@link CFAEdge}.
   * @return a unique identifier for the given object.
   * @throws NullPointerException if the given object is null.
   * @throws IllegalArgumentException if the given object is not an instance of {@link CFAEdge}.
   */
  @Override
  public String generateId(Object pForObject) {
    checkNotNull(pForObject, "The object to generate an ID for must not be null");

    if (!(pForObject instanceof CFAEdge)) {
      throw new IllegalArgumentException(
          "Wrong object: " + pForObject.getClass().getSimpleName() + " is not a CFAEdge");
    }

    String id = super.generateId(pForObject);
    edgeToIdMap.put((CFAEdge) pForObject, id);
    return id;
  }

  /**
   * Retrieves the ID of a {@link CFAEdge}.
   *
   * @param pEdge The {@link CFAEdge}.
   * @return the ID of the {@link CFAEdge}.
   * @throws NullPointerException if no generator was set.
   * @throws IllegalArgumentException if no ID for the {@link CFAEdge} is found.
   */
  public static String getIdFromEdge(CFAEdge pEdge) {
    CfaEdgeIdGenerator generator = currentGenerator.get();

    checkNotNull(generator, "No generator available");

    String id = generator.edgeToIdMap.get(pEdge);

    if (id == null) {
      throw new IllegalArgumentException("No ID for edge " + pEdge + " found");
    }

    return id;
  }
}
