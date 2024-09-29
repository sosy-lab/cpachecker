// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * This class is a custom {@link ObjectIdResolver}.
 *
 * <p>It is used to retrieve {@link CFAEdge}s from their respective IDs.
 */
public final class CfaEdgeIdResolver extends SimpleObjectIdResolver {
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
