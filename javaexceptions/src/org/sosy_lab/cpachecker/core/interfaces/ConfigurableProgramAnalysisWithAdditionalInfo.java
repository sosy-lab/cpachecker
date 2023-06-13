// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ExtendedWitnessFactory;

/**
 * Provides interface for CPA with additional info to be written to extended witness {@link
 * ExtendedWitnessFactory}
 */
public interface ConfigurableProgramAnalysisWithAdditionalInfo {

  /** Converter for additional witness tags supported by analysis */
  AdditionalInfoConverter exportAdditionalInfoConverter();

  /**
   * Create additional info based on current error path
   *
   * @param pPath base path to be enriched
   * @return result path with additional info
   */
  CFAPathWithAdditionalInfo createExtendedInfo(ARGPath pPath);
}
