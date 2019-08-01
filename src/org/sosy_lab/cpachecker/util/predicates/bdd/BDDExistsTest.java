/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.bdd;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

@RunWith(Parameterized.class)
public class BDDExistsTest {

  private NamedRegionManager nrm;
  private Configuration config;
  private LogManager logger = LogManager.createTestLogManager();

  @Parameter(0)
  public String bddPackage;

  @Parameters(name = "{0}")
  public static Object[] getAllPackages() {
    return new String[] {"SYLVAN", "JAVA"};
  }

  @Test
  public void existsTest() throws InvalidConfigurationException {
    config = Configuration.builder().setOption("bdd.package", bddPackage).build();
    nrm =
        new NamedRegionManager(new BDDManagerFactory(config, logger).createRegionManager());

    Region r0  = nrm.createPredicate("r0");
    Region r1  = nrm.createPredicate("r1");
    Region r2  = nrm.createPredicate("r2");

    Region complete = r2;
    complete = nrm.makeAnd(complete, r1);
    complete = nrm.makeAnd(complete, r0);

    Region reduced = nrm.makeExists(complete, r1, r2);
    assertWithMessage(new StringBuilder().append(nrm.dumpRegion(reduced)).toString())
        .that(reduced)
        .isEqualTo(r0);
  }
}
