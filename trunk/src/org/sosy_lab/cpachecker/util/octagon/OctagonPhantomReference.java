// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.octagon;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class OctagonPhantomReference extends PhantomReference<Octagon> {

  private Long octRef;
  private OctagonManager manager;

  public OctagonPhantomReference(Octagon reference, ReferenceQueue<? super Octagon> queue) {
    super(reference, queue);
    octRef = reference.getOctId();
    manager = reference.getManager();
  }

  public void cleanup() {
    manager.free(octRef);
  }
}
