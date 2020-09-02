// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager.AbstractionType;

@Options(prefix = "cpa.predicate")
public class PredicateAbstractionManagerOptions {

  @Deprecated
  @Option(
      secure = true,
      name = "abstraction.cartesian",
      description = "whether to use Boolean (false) or Cartesian (true) abstraction")
  private boolean cartesianAbstraction = false;

  @Option(
      secure = true,
      name = "abstraction.computation",
      description = "whether to use Boolean or Cartesian abstraction or both")
  private AbstractionType abstractionType = AbstractionType.BOOLEAN;

  @Option(
      secure = true,
      name = "abstraction.dumpHardQueries",
      description = "dump the abstraction formulas if they took to long")
  private boolean dumpHardAbstractions = false;

  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  @Option(
      secure = true,
      name = "abstraction.reuseAbstractionsFrom",
      description = "An initial set of comptued abstractions that might be reusable")
  private Path reuseAbstractionsFrom;

  @Option(
      secure = true,
      description = "Max. number of edge of the abstraction tree to prescan for reuse")
  private int maxAbstractionReusePrescan = 1;

  @Option(secure = true, name = "abs.useCache", description = "use caching of abstractions")
  private boolean useCache = true;

  @Option(
      secure = true,
      name = "refinement.splitItpAtoms",
      description =
          "split each arithmetic equality into two inequalities when extracting predicates from"
              + " interpolants")
  private boolean splitItpAtoms = false;

  @Option(
      secure = true,
      name = "abstraction.identifyTrivialPredicates",
      description =
          "Identify those predicates where the result is trivially known before abstraction"
              + " computation and omit them.")
  private boolean identifyTrivialPredicates = false;

  @Option(
      secure = true,
      name = "abstraction.simplify",
      description =
          "Simplify the abstraction formula that is stored to represent the state space. Helpful"
              + " when debugging (formulas get smaller).")
  private boolean simplifyAbstractionFormula = false;

  public PredicateAbstractionManagerOptions(Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
  }

  boolean isCartesianAbstraction() {
    return cartesianAbstraction;
  }

  AbstractionType getAbstractionType() {
    return abstractionType;
  }

  void setAbstractionType(AbstractionType pCartesian) {
    abstractionType = pCartesian;
  }

  boolean isDumpHardAbstractions() {
    return dumpHardAbstractions;
  }

  public Path getReuseAbstractionsFrom() {
    return reuseAbstractionsFrom;
  }

  int getMaxAbstractionReusePrescan() {
    return maxAbstractionReusePrescan;
  }

  boolean isUseCache() {
    return useCache;
  }

  boolean isSplitItpAtoms() {
    return splitItpAtoms;
  }

  boolean isIdentifyTrivialPredicates() {
    return identifyTrivialPredicates;
  }

  boolean isSimplifyAbstractionFormula() {
    return simplifyAbstractionFormula;
  }
}
