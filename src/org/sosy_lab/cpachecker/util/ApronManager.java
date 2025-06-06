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
    // Previously Apron added the location to the classpath using some hacky old Java code.
    // This does not work anymore and is discouraged anyway!
    // The loading below does only work if you add the lib/native/x86_64... folder to your local
    // classpath
    // TODO: fix loading of APRON
    NativeLibraries.loadLibrary("japron");
    NativeLibraries.loadLibrary("jgmp");

    manager = createManager(pAbstractDomain);
  }

  public Manager getManager() {
    return manager;
  }

  private Manager createManager(AbstractDomain pAbstractDomain) {

    return switch (pAbstractDomain) {
      case BOX -> new Box();
      case OCTAGON -> new Octagon();
      case POLKA -> new Polka(false);
      case POLKA_STRICT -> new Polka(true);
      case POLKA_EQ -> new PolkaEq();
    };
  }
}
