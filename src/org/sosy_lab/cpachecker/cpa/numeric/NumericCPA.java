// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import java.util.function.Supplier;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.numericdomains.Manager;
import org.sosy_lab.numericdomains.NumericalLibrary;
import org.sosy_lab.numericdomains.NumericalLibraryLoader;

@Options(prefix = "cpa.numeric")
public class NumericCPA extends AbstractCPA {

  @Option(
      secure = true,
      name = "numericLibrary",
      toUppercase = true,
      description = "Use this to switch the underlying numerical library.")
  private NumericalLibrary numericLibrary = NumericalLibrary.APRON;

  @Option(
      secure = true,
      name = "numericDomain",
      toUppercase = true,
      description = "Use this to switch between domains of a library.",
      values = {"POLYHEDRA", "OCTAGON", "ZONES", "BOX"})
  private String numericDomain = "OCTAGON";

  private final Supplier<Manager> managerSupplier;

  private final LogManager logger;

  public NumericCPA(Configuration config, LogManager logManager)
      throws InvalidConfigurationException {
    super(
        "SEP",
        "SEP",
        DelegateAbstractDomain.getInstance(),
        new NumericTransferRelation(config, logManager));
    config.inject(this);
    this.logger = logManager;

    NumericalLibraryLoader.loadLibrary(numericLibrary);

    managerSupplier = chooseDomain(numericDomain, numericLibrary);
    Manager manager = Manager.createManager(managerSupplier);
    logger.log(
        Level.INFO,
        "Using",
        manager.getDomainLibrary(),
        "version",
        manager.getDomainVersion(),
        "from",
        numericLibrary,
        "as numerical domain.");
    manager.dispose();
  }

  private Supplier<Manager> chooseDomain(String domainName, NumericalLibrary pLibrary) {
    switch (pLibrary) {
      case APRON:
        return chooseApronDomain(domainName);
      case ELINA:
        return chooseElinaDomain(domainName);
      default:
        throw new IllegalStateException("Unhandled library: " + pLibrary);
    }
  }

  private Supplier<Manager> chooseApronDomain(String domain) {
    switch (domain) {
      case "OCTAGON":
        return org.sosy_lab.numericdomains.apron.OctagonManager::createDefaultOctagonManager;
      case "POLYHEDRA":
        return org.sosy_lab.numericdomains.apron.PolyhedraManager::createDefaultPolyhedraManager;
      case "BOX":
        return org.sosy_lab.numericdomains.apron.BoxManager::createDefaultBoxManager;
      default:
        logger.log(Level.SEVERE, "Unknown domain for APRON: " + domain);
        throw new AssertionError("Unknown domain for APRON: " + domain);
    }
  }

  private Supplier<Manager> chooseElinaDomain(String domain) {
    switch (domain) {
      case "OCTAGON":
        return org.sosy_lab.numericdomains.elina.OctagonManager::createDefaultOctagonManager;
      case "POLYHEDRA":
        return org.sosy_lab.numericdomains.elina.PolyhedraManager::createDefaultPolyhedraManager;
      case "ZONES":
        return org.sosy_lab.numericdomains.elina.ZonesManager::createDefaultZonesManager;
      default:
        logger.log(Level.SEVERE, "Unknown domain for ELINA: " + domain);
        throw new AssertionError("Unknown domain for ELINA: " + domain);
    }
  }

  /**
   * This method returns a CPAfactory for the numeric analysis CPA.
   *
   * @return the CPAfactory for the numeric analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(NumericCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new NumericState(Manager.createManager(managerSupplier), logger);
  }
}
