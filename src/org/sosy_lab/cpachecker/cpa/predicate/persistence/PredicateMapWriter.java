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
package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateDumpFormat;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * This class writes a set of predicates to a file in the same format that is
 * also used by {@link PredicateMapParser}.
 */
@Options(prefix="cpa.predicate")
public class PredicateMapWriter {

  @Option(name="predmap.predicateFormat",
      description="Format for exporting predicates from precisions.")
  private PredicateDumpFormat format = PredicateDumpFormat.SMTLIB2;

  private final FormulaManagerView fmgr;

  public PredicateMapWriter(Configuration config, FormulaManagerView pFmgr) throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
  }

  public void writePredicateMap(
      SetMultimap<Pair<CFANode, Integer>,
      AbstractionPredicate> locationInstancePredicates,
      SetMultimap<CFANode, AbstractionPredicate> localPredicates,
      SetMultimap<String, AbstractionPredicate> functionPredicates,
      Set<AbstractionPredicate> globalPredicates,
      Collection<AbstractionPredicate> allPredicates,
      Appendable sb) throws IOException {

    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<AbstractionPredicate, String> predToString = Maps.newHashMap();

    // fill the above set and map
    for (AbstractionPredicate pred : allPredicates) {
      String predString;

      if (format == PredicateDumpFormat.SMTLIB2) {
        Pair<String, List<String>> p = splitFormula(fmgr, pred.getSymbolicAtom());
        predString = p.getFirst();
        definitions.addAll(p.getSecond());
      } else {
        predString = pred.getSymbolicAtom().toString();
      }

      predToString.put(pred, predString);
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

    for (Entry<Pair<CFANode, Integer>, Collection<AbstractionPredicate>> e : locationInstancePredicates.asMap().entrySet()) {
      CFANode loc = e.getKey().getFirst();
      String key = loc.getFunctionName()
           + " " + loc.toString() + "@" + e.getKey().getSecond();
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
