/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This class writes a set of predicates to a file in the same format that is
 * also used by {@link PredicateMapParser}.
 */
class PredicateMapWriter {

  private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();

  private static final Joiner LINE_JOINER = Joiner.on('\n');

  private final FormulaManagerView fmgr;

  public PredicateMapWriter(PredicateCPA pCpa) {
    fmgr = pCpa.getFormulaManager();
  }

  PredicateMapWriter(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  public void writePredicateMap(SetMultimap<CFANode, AbstractionPredicate> localPredicates,
      SetMultimap<String, AbstractionPredicate> functionPredicates, Set<AbstractionPredicate> globalPredicates,
      Collection<AbstractionPredicate> allPredicates, Appendable sb) throws IOException {

    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<AbstractionPredicate, String> predToString = Maps.newHashMap();

    // fill the above set and map
    for (AbstractionPredicate pred : allPredicates) {
      String s = fmgr.dumpFormula(pred.getSymbolicAtom());
      List<String> lines = Lists.newArrayList(LINE_SPLITTER.split(s));
      assert !lines.isEmpty();
      String predString = lines.get(lines.size()-1);
      lines.remove(lines.size()-1);
      if (!(predString.startsWith("(assert ") && predString.endsWith(")"))) {
        sb.append("Writing predicate map is only supported for solvers which support the Smtlib2 format, please try using Mathsat5.\n");
        return;
      }

      predToString.put(pred, predString);
      definitions.addAll(lines);
    }

    LINE_JOINER.appendTo(sb, definitions);
    sb.append("\n\n");

    writeSetOfPredicates(sb, "*", globalPredicates, predToString);

    for (Entry<String, Collection<AbstractionPredicate>> e : functionPredicates.asMap().entrySet()) {
      writeSetOfPredicates(sb, e.getKey(), e.getValue(), predToString);
    }

    for (Entry<CFANode, Collection<AbstractionPredicate>> e : localPredicates.asMap().entrySet()) {
      String key = e.getKey().getFunctionName() + " " + e.getKey().toString();
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }
  }

  private void writeSetOfPredicates(Appendable sb, String key,
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
