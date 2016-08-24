/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import org.sosy_lab.common.NativeLibraries;

import apron.Box;
import apron.Manager;
import apron.Octagon;
import apron.Polka;
import apron.PolkaEq;
import apron.SetUp;

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
    SetUp.init(NativeLibraries.getNativeLibraryPath().resolve("apron")
        .toAbsolutePath().toString());
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
        throw new UnsupportedOperationException(
            "Unexpected argument for domain option.");
    }
  }
}