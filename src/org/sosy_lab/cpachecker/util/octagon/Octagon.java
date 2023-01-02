// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;

public class Octagon {

  private final long octId;
  private final OctagonManager manager;
  private static List<OctagonPhantomReference> phantomReferences = new ArrayList<>();
  private static ReferenceQueue<Octagon> referenceQueue = new ReferenceQueue<>();

  Octagon(long l, OctagonManager manager) {
    octId = l;
    this.manager = manager;
    registerPhantomReference(this);
  }

  private static void registerPhantomReference(Octagon oct) {
    phantomReferences.add(new OctagonPhantomReference(oct, referenceQueue));
  }

  public static void removePhantomReferences() {
    Reference<? extends Octagon> reference;
    while ((reference = referenceQueue.poll()) != null) {
      ((OctagonPhantomReference) reference).cleanup();
    }
  }

  long getOctId() {
    return octId;
  }

  public OctagonManager getManager() {
    return manager;
  }

  @Override
  public int hashCode() {
    return (int) octId;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof Octagon)) {
      return false;
    }
    Octagon otherOct = (Octagon) pObj;

    return manager.dimension(this) == otherOct.manager.dimension(otherOct)
        && manager.isEqual(this, otherOct);
  }

  @Override
  public String toString() {
    return "octagon with id: " + octId;
  }
}
