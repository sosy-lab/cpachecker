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
package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import apron.Box;
import apron.Manager;
import apron.Octagon;
import apron.Polka;
import apron.PolkaEq;
import apron.SetUp;

@Options(prefix="cpa.apron")
public class ApronManager {

  @Option(secure=true, name="domain", toUppercase=true, values={"BOX", "OCTAGON", "POLKA", "POLKA_STRICT", "POLKA_EQ"},
      description="Use this to change the underlying abstract domain in the APRON library")
  private String domainType = "OCTAGON";

  private Manager manager;

  public ApronManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    manager = getManager(domainType);
  }

  public ApronManager(String pDomainType) throws InvalidConfigurationException {
    manager = getManager(pDomainType);
  }

  private Manager getManager(String pDomainType)
      throws InvalidConfigurationException {

    SetUp.init(NativeLibraries.getNativeLibraryPath().resolve("apron").toAbsolutePath().toString());
    switch (pDomainType) {
      case "BOX":
        return new Box();
      case "OCTAGON":
        return new Octagon();
      case "POLKA":
        return new Polka(false);
      case "POLKA_STRICT":
        return new Polka(true);
      case "POLKA_EQ":
        return new PolkaEq();
      default:
        throw new InvalidConfigurationException("Invalid argument for domain option.");
    }

  }

  public Manager getManager() {
    return manager;
  }
}