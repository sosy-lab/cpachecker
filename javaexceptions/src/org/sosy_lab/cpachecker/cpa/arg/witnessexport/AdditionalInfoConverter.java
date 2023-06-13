// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithAdditionalInfo;

public interface AdditionalInfoConverter {

  /**
   * Converter from additional info {@link ConfigurableProgramAnalysisWithAdditionalInfo} to {@link
   * TransitionCondition}
   *
   * @param originalTransition transition to enrich by additional information
   * @param pTag additional tag
   * @param pValue corresponding value
   * @return extended transition for dumping by {@link ExtendedWitnessFactory}
   */
  TransitionCondition convert(
      TransitionCondition originalTransition, ConvertingTags pTag, Object pValue);
}
