/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.VariableClassification;

import java.util.Map;
import java.util.Optional;

class BnBRegionsBuilder {
  @SuppressWarnings("unused")
  private final FormulaEncodingWithPointerAliasingOptions options;
  private final Optional<VariableClassification> variableClassification;
  private final TypeHandlerWithPointerAliasing typeHandler;

  BnBRegionsBuilder(FormulaEncodingWithPointerAliasingOptions pOptions,
      Optional<VariableClassification> pVariableClassification,
      TypeHandlerWithPointerAliasing pTypeHandler) {
    options = pOptions;
    variableClassification = pVariableClassification;
    typeHandler = pTypeHandler;
  }

  public MemoryRegionManager build() {
    if(!variableClassification.isPresent()) {
      return new BnBRegionManager(variableClassification, ImmutableMultimap.<CType, String>of());
    }
    VariableClassification var = variableClassification.get();
    Multimap<CCompositeType, String> relevant = var.getRelevantFields();
    Multimap<CCompositeType, String> addressed = var.getAddressedFields();

    Multimap<CType, String> bnb = HashMultimap.create();
    for(Map.Entry<CCompositeType, String> p : relevant.entries()) {
      if(!addressed.containsEntry(p.getKey(), p.getValue())) {
        CType type = typeHandler.simplifyType(p.getKey());
        bnb.put(type, p.getValue());
      }
    }
    return new BnBRegionManager(variableClassification, ImmutableMultimap.<CType, String>copyOf(bnb));
  }

}
