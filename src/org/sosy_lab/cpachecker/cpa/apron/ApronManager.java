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

  static {
    SetUp.init("lib/native/x86_64-linux/apron/");
  }

  @Option(name="domain", toUppercase=true, values={"BOX", "OCTAGON", "POLKA", "POLKA_STRICT", "POLKA_EQ"},
      description="Use this to change the underlying abstract domain in the APRON library")
  private String domainType = "OCTAGON";

  private Manager manager;

  public ApronManager(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    if (domainType.equals("BOX")) {
      manager = new Box();
    } else if (domainType.equals("OCTAGON")) {
      manager = new Octagon();
    } else if (domainType.equals("POLKA")) {
      manager = new Polka(false);
    } else if (domainType.equals("POLKA_STRICT")) {
      manager = new Polka(true);
    } else if (domainType.equals("POLKA_EQ")) {
      manager = new PolkaEq();
    } else {
      throw new InvalidConfigurationException("Invalid argument for domain option.");
    }
  }

  public Manager getManager() {
    return manager;
  }
}