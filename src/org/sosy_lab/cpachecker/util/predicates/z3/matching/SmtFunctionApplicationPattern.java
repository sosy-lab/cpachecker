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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import java.util.Collection;
import java.util.Iterator;

import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternSelection.LogicalConnection;

import com.google.common.base.Optional;

public class SmtFunctionApplicationPattern implements SmtAstPattern {

  public final Optional<Comparable<?>> function;
  public final Optional<String> bindMatchTo;
  public final SmtAstPatternSelection argumentPatterns;

  public SmtFunctionApplicationPattern(
      Optional<Comparable<?>> pFunction,
      Optional<String> pBindMatchTo,
      SmtAstPatternSelection pArgumentPatterns) {

    this.function = pFunction;
    this.bindMatchTo = pBindMatchTo;
    this.argumentPatterns = pArgumentPatterns;
  }

  @Override
  public Optional<String> getBindMatchTo() {
    return bindMatchTo;
  }

  public LogicalConnection getArgumentsLogic() {
    return argumentPatterns.getRelationship();
  }

  public int getArgumentPatternCount() {
    return argumentPatterns.getPatterns().size();
  }

  public Iterator<SmtAstPattern> getArgumentPatternIterator() {
    return argumentPatterns.getPatterns().iterator();
  }

  public Collection<SmtAstPattern> getArgumentPatterns() {
    return argumentPatterns.getPatterns();
  }

}
