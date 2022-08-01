// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

public class SerializeSSAMap {

  public static String serialize(SSAMap pSSAMap) throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(pSSAMap);
      out.flush();
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
  }

  public static SSAMap deserialize(String pSerialize) {
    try (ByteArrayInputStream bis =
            new ByteArrayInputStream(Base64.getDecoder().decode(pSerialize));
        ObjectInputStream in = new ObjectInputStream(bis)) {
      return (SSAMap) in.readObject();
    } catch (IOException pE) {
      // don't fail as next message might correct the map
      return SSAMap.emptySSAMap();
    } catch (ClassNotFoundException pE) {
      // class should exist
      throw new AssertionError(pE);
    }
  }
}
