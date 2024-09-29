// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSortedSet;
import java.io.IOException;

/**
 * A deserializer for {@link ImmutableSortedSet} that is not intended to be called.
 *
 * <p>This class throws an {@link UnsupportedOperationException} when the deserialize method is
 * called.
 */
public class UnusedImmutableSortedSetDeserializer
    extends JsonDeserializer<ImmutableSortedSet<Equivalence.Wrapper<?>>> {

  @Override
  public ImmutableSortedSet<Equivalence.Wrapper<?>> deserialize(
      JsonParser pParser, DeserializationContext pContext) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }
}
