/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;


public class PredicatePersistenceUtils {

  public static enum PredicateDumpFormat {PLAIN, SMTLIB2}
  public static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  public static final Joiner LINE_JOINER = Joiner.on('\n');

  public static Pair<String, List<String>> splitFormula(FormulaManagerView fmgr, BooleanFormula f) {
    StringBuilder fullString = new StringBuilder();
    Appenders.appendTo(fullString, fmgr.dumpFormula(f));

    List<String> lines = LINE_SPLITTER.splitToList(fullString);
    assert !lines.isEmpty();
    String formulaString = Iterables.getLast(lines);
    assert formulaString.startsWith("(assert ") && formulaString.endsWith(")") : "Unexpected formula format: " + formulaString;
    List<String> declarations = lines.subList(0, lines.size()-1);

    return Pair.of(formulaString, declarations);
  }

  static void writeSetOfPredicates(Appendable sb, String key,
      Collection<AbstractionPredicate> predicates,
      Map<AbstractionPredicate, String> predToString) throws IOException {
    if (!predicates.isEmpty()) {
      sb.append(key);
      sb.append(":\n");
      for (AbstractionPredicate pred : predicates) {
        sb.append(checkNotNull(predToString.get(pred)));
        sb.append('\n');
      }
      sb.append('\n');
    }
  }

}
