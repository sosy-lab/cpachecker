// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.bdd;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.regions.CountingRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.SynchronizedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.TimedRegionManager;

/**
 * Factory for creating a RegionManager for one of the available BDD packages (chosen according to
 * configuration).
 */
@Options(prefix = "bdd")
public class BDDManagerFactory {

  @Option(
      secure = true,
      name = "package",
      description =
          "Which BDD package should be used?\n"
              + "- java:   JavaBDD (default, no dependencies, many features)\n"
              + "- sylvan: Sylvan (only 64bit Linux, uses multiple threads)\n"
              + "- cudd:   CUDD (native library required, reordering not supported)\n"
              + "- micro:  MicroFactory (maximum number of BDD variables is 1024, slow, but less"
              + " memory-comsumption)\n"
              + "- buddy:  Buddy (native library required)\n"
              + "- cal:    CAL (native library required)\n"
              + "- jdd:    JDD\n"
              + "- pjbdd:  A java native parallel bdd framework",
      values = {"JAVA", "SYLVAN", "CUDD", "MICRO", "BUDDY", "CAL", "JDD", "PJBDD"},
      toUppercase = true)
  // documentation of the packages can be found at source of BDDFactory.init()
  private String bddPackage = "JAVA";

  @Option(secure = true, description = "sequentialize all accesses to the BDD library.")
  private boolean synchronizeLibraryAccess = false;

  @Option(
      secure = true,
      description =
          "Measure the time spent in the BDD library. "
              + "The behaviour in case of concurrent accesses is undefined!")
  private boolean measureLibraryAccess = false;

  @Option(
      secure = true,
      description =
          "Count accesses for the BDD library. " + "Counting works for concurrent accesses.")
  private boolean countLibraryAccess = false;

  private final Configuration config;
  private final LogManager logger;

  public BDDManagerFactory(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
  }

  public RegionManager createRegionManager() throws InvalidConfigurationException {
    RegionManager rmgr;
    if (bddPackage.equals("SYLVAN")) {
      rmgr = new SylvanBDDRegionManager(config, logger);
    } else if (bddPackage.equals("PJBDD")) {
      rmgr = new PJBDDRegionManager(config);
    } else {
      rmgr = new JavaBDDRegionManager(bddPackage, config, logger);
    }
    if (measureLibraryAccess) {
      rmgr = new TimedRegionManager(rmgr);
    }
    if (countLibraryAccess) {
      rmgr = new CountingRegionManager(rmgr);
    }
    if (synchronizeLibraryAccess) {
      rmgr = new SynchronizedRegionManager(rmgr);
    }
    return rmgr;
  }
}
