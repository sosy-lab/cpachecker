// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.bdd;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import java.util.List;
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
  public static List<String> getAllPackages() {
    return ImmutableList.of("SYLVAN", "JAVA");
  }

  @Test
  public void existsTest() throws InvalidConfigurationException {
    config = Configuration.builder().setOption("bdd.package", bddPackage).build();
    nrm = new NamedRegionManager(new BDDManagerFactory(config, logger).createRegionManager());

    Region r0 = nrm.createPredicate("r0");
    Region r1 = nrm.createPredicate("r1");
    Region r2 = nrm.createPredicate("r2");

    Region complete = r2;
    complete = nrm.makeAnd(complete, r1);
    complete = nrm.makeAnd(complete, r0);

    Region reduced = nrm.makeExists(complete, r1, r2);
    assertWithMessage(nrm.dumpRegion(reduced).toString()).that(reduced).isEqualTo(r0);
  }
}
