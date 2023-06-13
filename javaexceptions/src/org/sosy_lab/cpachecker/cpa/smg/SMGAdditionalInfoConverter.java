// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ConvertingTags;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

public class SMGAdditionalInfoConverter implements AdditionalInfoConverter {

  final Map<ConvertingTags, KeyDef> tagConverter =
      ImmutableMap.of(SMGConvertingTags.NOTE, KeyDef.NOTE);

  @Override
  public TransitionCondition convert(
      TransitionCondition originalTransition, ConvertingTags pTag, Object pValue) {
    if (pTag instanceof SMGConvertingTags) {
      String value = pValue.toString();
      return originalTransition.putAndCopy(tagConverter.get(pTag), value);
    } else {
      return originalTransition;
    }
  }
}
