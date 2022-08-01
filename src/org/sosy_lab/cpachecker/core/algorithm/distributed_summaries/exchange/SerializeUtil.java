// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

public class SerializeUtil {

  public static <T extends Serializable> String serialize(T pObject) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(pObject);
      out.flush();
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
  }

  public static <T extends Serializable> T deserialize(String pSerialize, Class<T> pClass) {
    try (ByteArrayInputStream bis =
            new ByteArrayInputStream(Base64.getDecoder().decode(pSerialize));
        ObjectInputStream in = new ObjectInputStream(bis)) {
      return pClass.cast(in.readObject());
    } catch (IOException | ClassNotFoundException pE) {
      // in no scenario deserializing a message should cause exceptions
      throw new AssertionError(pE);
    }
  }
}
