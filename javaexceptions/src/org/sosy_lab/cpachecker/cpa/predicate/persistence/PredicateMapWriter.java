// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.LINE_JOINER;
import static org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.splitFormula;

import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateDumpFormat;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

/**
 * This class writes a set of predicates to a file in the same format that is also used by {@link
 * PredicateMapParser}.
 */
@Options(prefix = "cpa.predicate")
public class PredicateMapWriter {

  @Option(
      secure = true,
      name = "predmap.predicateFormat",
      description = "Format for exporting predicates from precisions.")
  private PredicateDumpFormat format = PredicateDumpFormat.SMTLIB2;

  private final FormulaManagerView fmgr;

  public PredicateMapWriter(Configuration config, FormulaManagerView pFmgr)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
  }

  public void writePredicateMap(
      SetMultimap<PredicatePrecision.LocationInstance, AbstractionPredicate>
          locationInstancePredicates,
      SetMultimap<CFANode, AbstractionPredicate> localPredicates,
      SetMultimap<String, AbstractionPredicate> functionPredicates,
      Set<AbstractionPredicate> globalPredicates,
      Collection<AbstractionPredicate> allPredicates,
      Appendable sb)
      throws IOException {

    // In this set, we collect the definitions and declarations necessary
    // for the predicates (e.g., for variables)
    // The order of the definitions is important!
    Set<String> definitions = new LinkedHashSet<>();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<AbstractionPredicate, String> predToString = new HashMap<>();

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

    for (Entry<String, Collection<AbstractionPredicate>> e :
        functionPredicates.asMap().entrySet()) {
      writeSetOfPredicates(sb, e.getKey(), e.getValue(), predToString);
    }

    for (Entry<CFANode, Collection<AbstractionPredicate>> e : localPredicates.asMap().entrySet()) {
      String key = e.getKey().getFunctionName() + " " + e.getKey();
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }

    for (Entry<PredicatePrecision.LocationInstance, Collection<AbstractionPredicate>> e :
        locationInstancePredicates.asMap().entrySet()) {
      String key =
          String.format(
              "%s %s@%d",
              e.getKey().getFunctionName(), e.getKey().getLocation(), e.getKey().getInstance());
      writeSetOfPredicates(sb, key, e.getValue(), predToString);
    }
  }

  private void writeSetOfPredicates(
      Appendable sb,
      String key,
      Collection<AbstractionPredicate> predicates,
      Map<AbstractionPredicate, String> predToString)
      throws IOException {
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
