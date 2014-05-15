/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.octagon;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;


public class Octagon {

  private final long octId;
  private final OctagonManager manager;
  private static List<OctPhantomReference> phantomReferences = new ArrayList<>();
  private static ReferenceQueue<Octagon> referenceQueue = new ReferenceQueue<>();

  Octagon(long l, OctagonManager manager) {
    octId = l;
    this.manager = manager;
    registerPhantomReference(this);
  }

  private static void registerPhantomReference(Octagon oct) {
    phantomReferences.add(new OctPhantomReference(oct, referenceQueue));
  }

  public static void removePhantomReferences() {
    Reference<? extends Octagon> reference;
    while ((reference = referenceQueue.poll()) != null) {
      ((OctPhantomReference)reference).cleanup();
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
    return (int)octId;
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof Octagon)) {
      return false;
    }
    Octagon otherOct = (Octagon) pObj;

    return manager.dimension(this) == otherOct.manager.dimension(otherOct) && manager.isEqual(this, otherOct);
  }

  @Override
  public String toString() {
    return "octagon with id: " + octId;
  }
}