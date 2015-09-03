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

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection.LogicalConnection;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class SmtFunctionApplicationPattern implements SmtAstPattern {

  public final Optional<Comparable<?>> function;
  public final Optional<String> bindMatchTo;
  public final SmtAstPatternSelection argumentPatterns;
  public final ImmutableSet<SmtAstMatchFlag> flags; // TODO: Move the flags to SmtAstPatternSelection
  public final Optional<SmtFormulaMatcher> customFormulaMatcher;

  public SmtFunctionApplicationPattern(
      Optional<Comparable<?>> pFunction,
      Optional<SmtFormulaMatcher> pCustomFormulaMatcher,
      Optional<String> pBindMatchTo,
      SmtAstPatternSelection pArgumentPatterns,
      SmtAstMatchFlag...pFlags) {

    this.function = pFunction;
    this.bindMatchTo = pBindMatchTo;
    this.argumentPatterns = pArgumentPatterns;
    this.customFormulaMatcher = pCustomFormulaMatcher;
    this.flags = ImmutableSet.copyOf(pFlags);
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

  public Iterator<SmtAstPatternSelectionElement> getArgumentPatternIterator(boolean reversed) {
    return getArgumentPatterns(reversed).iterator();
  }

  public List<SmtAstPatternSelectionElement> getArgumentPatterns(boolean reversed) {
    if (reversed) {
      return Lists.reverse(argumentPatterns.getPatterns());
    }
    return argumentPatterns.getPatterns();
  }

  @Override
  public String toString() {
    String functionText = function.isPresent() ? function.get().toString() : "?";
    String bindToText = bindMatchTo.isPresent() ? bindMatchTo.get().toString() : "-";
    return String.format("%s | %s | %d args %s", functionText, bindToText, argumentPatterns.getPatterns().size(), argumentPatterns.getRelationship().toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((argumentPatterns == null) ? 0 : argumentPatterns.hashCode());
    result = prime * result + ((bindMatchTo == null) ? 0 : bindMatchTo.hashCode());
    result = prime * result + ((function == null) ? 0 : function.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof SmtFunctionApplicationPattern)) { return false; }
    SmtFunctionApplicationPattern other = (SmtFunctionApplicationPattern) obj;
    if (argumentPatterns == null) {
      if (other.argumentPatterns != null) { return false; }
    } else if (!argumentPatterns.equals(other.argumentPatterns)) { return false; }
    if (bindMatchTo == null) {
      if (other.bindMatchTo != null) { return false; }
    } else if (!bindMatchTo.equals(other.bindMatchTo)) { return false; }
    if (function == null) {
      if (other.function != null) { return false; }
    } else if (!function.equals(other.function)) { return false; }
    return true;
  }

}
