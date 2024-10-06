// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import java.io.IOException;
import java.util.HashMap;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonImport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.PartitionMixin;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;

/**
 * This class is a custom {@link ObjectIdResolver}.
 *
 * <p>It is used for {@link Partition} objects.
 *
 * @see CfaJsonImport
 * @see PartitionMixin
 */
public final class PartitionIdResolver extends SimpleObjectIdResolver {

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
        throw new IllegalStateException("Cannot resolve Partition object", e);
      }
    }

    return resolved;
  }
}
