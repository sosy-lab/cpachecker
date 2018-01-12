/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.AdditionalInfoConverter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.TransitionCondition;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

public class SMGAdditionalInfoConverter implements AdditionalInfoConverter {
Map<String, KeyDef> tagConverter = ImmutableMap.of(
    "Warning", KeyDef.WARNING,
    "Note", KeyDef.NOTE);

  @Override
  public TransitionCondition convert(
      TransitionCondition originalTransition, String pTag, Object pValue) {
    String value = pValue.toString();
    if ("Note".equals(pTag)) {
      String source = originalTransition.getMapping().get(KeyDef.SOURCECODE);
      value = source != null ? source
                             : pValue.toString();
    }

    return originalTransition.putAndCopy(tagConverter.get(pTag), value);
  }
}
