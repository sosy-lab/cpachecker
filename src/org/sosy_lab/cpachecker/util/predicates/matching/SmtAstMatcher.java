/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.matching;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;


public interface SmtAstMatcher {

  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF);
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF, Optional<Multimap<String, Formula>> bBindingRestrictions);

  // a+b  <-->  b+a
  public void defineCommutative(String pFunctionName);

  // a >= b  <-->  b <= a
  public void defineRotations(String pFunctionName, String pRotationFunctionName);

  public void defineFunctionAliases(String pFunctionName, Set<String> pAliases);

  // a >= b  --> a > b || a = b
  public void defineOperatorImplications(String pString, HashSet<String> pNewHashSet);

  public <T1 extends Formula, T2 extends Formula> T1 substitute(T1 f, Map<T2, T2> fromToMapping);
}
