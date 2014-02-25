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
package org.sosy_lab.cpachecker.cpa.invariants;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;


public class InvariantsWriter {

  private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  private static final Joiner LINE_JOINER = Joiner.on('\n');

  private final FormulaManagerView fmgr;

  public InvariantsWriter(PredicateCPA pCpa) {
    this(pCpa.getFormulaManager());
  }

  public InvariantsWriter(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  public void write(UnmodifiableReachedSet pReachedSet, Appendable pAppendable) throws IOException {
    SetMultimap<CFANode, InvariantsState> locationPredicates = HashMultimap.create();
    for (AbstractState state : pReachedSet) {
      CFANode location = AbstractStates.extractLocation(state);
      if (location != null) {
        InvariantsState invariantsState = AbstractStates.extractStateByType(state, InvariantsState.class);
        locationPredicates.put(location, invariantsState);
      }
    }
    write(locationPredicates, pAppendable);
  }

  public void write(final CFANode pCfaNode, UnmodifiableReachedSet pReachedSet, Appendable pAppendable) throws IOException {
    FluentIterable<InvariantsState> states = FluentIterable.from(pReachedSet).filter(new Predicate<AbstractState>() {

      @Override
      public boolean apply(@Nullable AbstractState pArg0) {
        return pArg0 != null && AbstractStates.extractLocation(pArg0).equals(pCfaNode);
      }

    }).transform(new Function<AbstractState, InvariantsState>() {

      @Override
      @Nullable
      public InvariantsState apply(@Nullable AbstractState pArg0) {
        if (pArg0 == null) {
          return null;
        }
        return AbstractStates.extractStateByType(pArg0, InvariantsState.class);
      }

    });
    SetMultimap<CFANode, InvariantsState> statesToNode = HashMultimap.create();
    statesToNode.putAll(pCfaNode, states);
    write(statesToNode, pAppendable);
  }

  private void write(SetMultimap<CFANode, InvariantsState> pStates, Appendable pAppendable) throws IOException {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    Set<String> definitions = Sets.newLinkedHashSet();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    Map<Collection<InvariantsState>, String> predToString = Maps.newHashMap();

    // fill the above set and map
    Iterator<Collection<InvariantsState>> iterator = pStates.asMap().values().iterator();
    while (iterator.hasNext()) {
      Collection<InvariantsState> invariantDisjunctiveParts = iterator.next();
      String predString;

      // Create the disjunction of the found states
      BooleanFormula formula = bfmgr.makeBoolean(false);
      for (InvariantsState state : invariantDisjunctiveParts) {
        if (state != null) {
          formula = bfmgr.or(formula, state.getFormulaApproximation(fmgr));
        }
      }

      // Skip states with no information
      if (bfmgr.isTrue(formula)) {
        iterator.remove();
        continue;
      }

      String s = fmgr.dumpFormula(formula).toString();

      List<String> lines = Lists.newArrayList(LINE_SPLITTER.split(s));
      assert !lines.isEmpty();

      // Get the predicate
      predString = lines.get(lines.size()-1);

      // Remove the predicate from the dump
      lines.remove(lines.size() - 1);

      // Check that the dump format is correct
      if (!(predString.startsWith("(assert ") && predString.endsWith(")"))) {
        pAppendable.append("Writing invariants is only supported for solvers which support the Smtlib2 format, please try using Mathsat5.\n");
        return;
      }

      // Add the definition part of the dump to the set of definitions
      definitions.addAll(lines);

      // Record the predicate to write it later at t
      predToString.put(invariantDisjunctiveParts, predString);
    }

    LINE_JOINER.appendTo(pAppendable, definitions);
    pAppendable.append("\n\n");

    for (Map.Entry<CFANode, Collection<InvariantsState>> entry : pStates.asMap().entrySet()) {
      writeInvariant(pAppendable, toKey(entry.getKey()), entry.getValue(), predToString);
    }
  }

  private void writeInvariant(Appendable pAppendable, String pKey,
      Collection<InvariantsState> pDisjunctiveParts,
      Map<Collection<InvariantsState>, String> predToString) throws IOException {
    pAppendable.append(pKey);
    pAppendable.append(":\n");
    pAppendable.append(checkNotNull(predToString.get(pDisjunctiveParts)));
    pAppendable.append('\n');
  }

  private static String toKey(CFANode pCfaNode) {
    return pCfaNode.getFunctionName() + " " + pCfaNode.toString();
  }

}
