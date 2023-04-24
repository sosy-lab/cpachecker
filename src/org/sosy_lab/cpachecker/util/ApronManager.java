// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import apron.Box;
import apron.Manager;
import apron.Octagon;
import apron.Polka;
import apron.PolkaEq;
import org.sosy_lab.common.NativeLibraries;

public class ApronManager {

  public enum AbstractDomain {
    BOX,
    OCTAGON,
    POLKA,
    POLKA_STRICT,
    POLKA_EQ
  }

  private final Manager manager;

  public ApronManager(AbstractDomain pAbstractDomain) {
    try {
      // Previously Apron added the location to the classpath using some hacky old Java code.
      // This does not work anymore and is discouraged anyway!
      // The loading below does only work if you add the lib/native/x86_64... folder to your local
      // classpath
      // TODO: fix loading of APRON
      NativeLibraries.loadLibrary("apron");
      NativeLibraries.loadLibrary("jgmp");
    } catch (RuntimeException e) {
      throw e;
    }

    manager = createManager(pAbstractDomain);
  }

  public Manager getManager() {
    return manager;
  }

  private Manager createManager(AbstractDomain pAbstractDomain) {

    switch (pAbstractDomain) {
      case BOX:
        return new Box();
      case OCTAGON:
        return new Octagon();
      case POLKA:
        return new Polka(false);
      case POLKA_STRICT:
        return new Polka(true);
      case POLKA_EQ:
        return new PolkaEq();
      default:
        throw new UnsupportedOperationException("Unexpected argument for domain option.");
    }
  }
}
