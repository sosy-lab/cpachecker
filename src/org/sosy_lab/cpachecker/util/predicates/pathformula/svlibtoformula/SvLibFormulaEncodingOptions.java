// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.pathformula.FormulaEncodingOptions;

/** This class collects some configurations options for the SV-LIB-to-formula encoding process. */
@Options(prefix = "cpa.predicate.svlib")
public class SvLibFormulaEncodingOptions extends FormulaEncodingOptions {
  public SvLibFormulaEncodingOptions(Configuration config) throws InvalidConfigurationException {
    super(config);
    config.inject(this);
  }
}
