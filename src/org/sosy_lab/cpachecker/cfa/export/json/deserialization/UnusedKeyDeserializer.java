// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.deserialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import java.io.IOException;

/**
 * A deserializer for keys that is not intended to be called.
 *
 * <p>This class throws an {@link UnsupportedOperationException} when the deserializeKey method is
 * called.
 */
public class UnusedKeyDeserializer extends KeyDeserializer {

  @Override
  public Object deserializeKey(
      final String pKey, final DeserializationContext pDeserializationContext)
      throws IOException, JsonProcessingException {
    throw new UnsupportedOperationException("Not implemented");
  }
}
