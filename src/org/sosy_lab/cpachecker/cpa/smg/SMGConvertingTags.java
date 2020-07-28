// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ConvertingTags;

/** Intermediate enum for {@link AdditionalInfoConverter} used at {@link SMGCPA} */
public enum SMGConvertingTags implements ConvertingTags {
  WARNING,
  NOTE
}
