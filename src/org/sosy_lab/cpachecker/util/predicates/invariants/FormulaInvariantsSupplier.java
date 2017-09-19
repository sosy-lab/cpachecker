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
package org.sosy_lab.cpachecker.util.predicates.invariants;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractReportedFormulas;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.LazyLocationMapping;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

public class FormulaInvariantsSupplier implements InvariantSupplier {

  private final AggregatedReachedSets aggregatedReached;

  private Set<UnmodifiableReachedSet> lastUsedReachedSets = Collections.emptySet();
  private InvariantSupplier lastInvariantSupplier = TrivialInvariantSupplier.INSTANCE;

  private final Map<UnmodifiableReachedSet, ReachedSetBasedFormulaSupplier>
      singleInvariantSuppliers = new HashMap<>();

  public FormulaInvariantsSupplier(AggregatedReachedSets pAggregated) {
    aggregatedReached = pAggregated;
    updateInvariants(); // at initialization we want to update the invariants the first time
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode,
      Optional<CallstackStateEqualsWrapper> pCallstackInfo,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      @Nullable PathFormula pContext)
      throws InterruptedException {
    return lastInvariantSupplier.getInvariantFor(pNode, pCallstackInfo, pFmgr, pPfmgr, pContext);
  }

  public void updateInvariants() {
    Set<UnmodifiableReachedSet> tmp = aggregatedReached.snapShot();
    if (!tmp.equals(lastUsedReachedSets)) {
      // if we have a former aggregated supplier we do only replace the changed parts
      Set<UnmodifiableReachedSet> oldElements = Sets.difference(lastUsedReachedSets, tmp);
      Set<UnmodifiableReachedSet> newElements = Sets.difference(tmp, lastUsedReachedSets);

      oldElements.forEach(r -> singleInvariantSuppliers.remove(r));
      newElements.forEach(
          r ->
              singleInvariantSuppliers.put(
                  r, new ReachedSetBasedFormulaSupplier(new LazyLocationMapping(r))));

      lastUsedReachedSets = tmp;
      lastInvariantSupplier =
          new AggregatedInvariantSupplier(ImmutableSet.copyOf(singleInvariantSuppliers.values()));
    }
  }

  private static class AddPointerInformationVisitor extends FormulaTransformationVisitor {

    private final PathFormula context;
    private final FormulaManagerView fmgr;
    private final PathFormulaManager pfgmr;

    protected AddPointerInformationVisitor(
        FormulaManagerView pFmgr, PathFormula pContext, PathFormulaManager pPfmgr) {
      super(pFmgr);
      fmgr = pFmgr;
      pfgmr = pPfmgr;
      context = pContext;
    }

    @Override
    public Formula visitFreeVariable(Formula atom, String varName) {
      if (context.getPointerTargetSet().isActualBase(varName)) {
        return fmgr.uninstantiate(
            pfgmr.makeFormulaForVariable(
                context, varName, context.getPointerTargetSet().getBases().get(varName), false));
      } else {
        SSAMap ssa = context.getSsa();

        if (!ssa.containsVariable(varName)) {
          if (varName.startsWith("*(") && varName.endsWith(")")) {
            String unwrappedVarName = varName.substring(2, varName.length() - 1);
            if (!ssa.containsVariable(unwrappedVarName)) {
              // Variable needs to be eliminated later
              return atom;
            }

            CType type = ((CPointerType) ssa.getType(unwrappedVarName)).getType();
            atom =
                fmgr.uninstantiate(
                    pfgmr.makeFormulaForVariable(context, unwrappedVarName, type, true));
            return atom;
          }
          // Variable needs to be eliminated later
          return atom;
        }

        // nothing special, just return the variable as is
        return atom;
      }
    }
  }

  private static class ReachedSetBasedFormulaSupplier {

    private final LazyLocationMapping lazyLocationMapping;

    public ReachedSetBasedFormulaSupplier(LazyLocationMapping pLazyLocationMapping) {
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    }

    public BooleanFormula getInvariantFor(
        CFANode pLocation,
        Optional<CallstackStateEqualsWrapper> pCallstackInformation,
        FormulaManagerView fmgr) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeFalse();

      for (AbstractState locState : lazyLocationMapping.get(pLocation, pCallstackInformation)) {
        invariant = bfmgr.or(invariant, extractReportedFormulas(fmgr, locState));
      }
      return invariant;
    }
  }

  private static class AggregatedInvariantSupplier implements InvariantSupplier {

    private final Collection<ReachedSetBasedFormulaSupplier> invariantSuppliers;
    private final Map<InvariantsCacheKey, List<BooleanFormula>> cache = new HashMap<>();

    private AggregatedInvariantSupplier(
        ImmutableCollection<ReachedSetBasedFormulaSupplier> pInvariantSuppliers) {
      invariantSuppliers = checkNotNull(pInvariantSuppliers);
    }

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pNode,
        Optional<CallstackStateEqualsWrapper> callstackInformation,
        FormulaManagerView pFmgr,
        PathFormulaManager pPfmgr,
        PathFormula pContext)
        throws InterruptedException {
      InvariantsCacheKey key = new InvariantsCacheKey(pNode, callstackInformation, pFmgr, pPfmgr);

      List<BooleanFormula> invariants;
      if (cache.containsKey(key)) {
        invariants = cache.get(key);
      } else {
        invariants =
            Collections3.transformedImmutableListCopy(
                invariantSuppliers, s -> s.getInvariantFor(pNode, callstackInformation, pFmgr));

        cache.put(key, invariants);
      }

      final BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();

      if (pContext != null) {
        List<BooleanFormula> adjustedInvariants = Lists.newArrayListWithCapacity(invariants.size());
        Set<String> variables = pContext.getSsa().allVariables();
        for (BooleanFormula invariant : invariants) {
          // Handle pointer aliasing
          BooleanFormula inv =
              pFmgr.transformRecursively(
                  invariant, new AddPointerInformationVisitor(pFmgr, pContext, pPfmgr));
          // Drop information about unknown variables
          if (!variables.containsAll(pFmgr.extractVariableNames(inv))) {
            inv =
                pFmgr.filterLiterals(
                    inv, bf -> variables.containsAll(pFmgr.extractVariableNames(bf)));
          }
          adjustedInvariants.add(inv);
        }
        invariants = adjustedInvariants;
      }

      return verifyNotNull(bfmgr.and(invariants));
    }
  }

  private static class InvariantsCacheKey {
    private final CFANode node;
    private final Optional<CallstackStateEqualsWrapper> callstackInformation;
    private final FormulaManagerView fmgr;
    private final PathFormulaManager pfmgr;

    public InvariantsCacheKey(
        CFANode pNode,
        Optional<CallstackStateEqualsWrapper> pCallstackInformation,
        FormulaManagerView pFmgr,
        PathFormulaManager pPfmgr) {
      node = pNode;
      callstackInformation = pCallstackInformation;
      fmgr = pFmgr;
      pfmgr = pPfmgr;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      InvariantsCacheKey that = (InvariantsCacheKey) pO;
      return Objects.equals(node, that.node) &&
          Objects.equals(callstackInformation, that.callstackInformation) &&
          Objects.equals(fmgr, that.fmgr) &&
          Objects.equals(pfmgr, that.pfmgr);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, callstackInformation, fmgr, pfmgr);
    }
  }
}
