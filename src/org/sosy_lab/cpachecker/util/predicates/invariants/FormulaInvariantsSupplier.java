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
import com.google.common.collect.Sets;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.LazyLocationMapping;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.FormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class FormulaInvariantsSupplier implements InvariantSupplier {

  private final AggregatedReachedSets aggregatedReached;
  private final CtoFormulaTypeHandler typeConverter;

  private Set<UnmodifiableReachedSet> lastUsedReachedSets = Collections.emptySet();
  private InvariantSupplier lastInvariantSupplier = TrivialInvariantSupplier.INSTANCE;

  private final Map<UnmodifiableReachedSet, ReachedSetBasedFormulaSupplier>
      singleInvariantSuppliers = new HashMap<>();

  public FormulaInvariantsSupplier(
      AggregatedReachedSets pAggregated, LogManager pLogger, MachineModel pMachineModel) {
    typeConverter = new CtoFormulaTypeHandler(pLogger, pMachineModel);
    aggregatedReached = pAggregated;
    updateInvariants(); // at initialization we want to update the invariants the first time
  }

  @Override
  public BooleanFormula getInvariantFor(
      CFANode pNode,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      @Nullable PathFormula pContext) {
    return lastInvariantSupplier.getInvariantFor(pNode, pFmgr, pPfmgr, pContext);
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
          new AggregatedInvariantSupplier(
              ImmutableSet.copyOf(singleInvariantSuppliers.values()), typeConverter);
    }
  }

  private static class AddPointerInformationVisitor extends FormulaTransformationVisitor {

    private final PointerTargetSet pts;
    private final FormulaManagerView fmgr;
    private final FunctionFormulaManagerView ffmgr;
    private final PersistentMap<String, CType> bases;
    private final CtoFormulaTypeHandler typeConverter;

    protected AddPointerInformationVisitor(
        FormulaManagerView pFmgr, PointerTargetSet pPts, CtoFormulaTypeHandler pTypeHandler) {
      super(pFmgr);
      fmgr = pFmgr;
      ffmgr = pFmgr.getFunctionFormulaManager();
      pts = pPts;
      bases = pts.getBases();
      typeConverter = pTypeHandler;
    }

    @Override
    public Formula visitFreeVariable(Formula atom, String varName) {
      if (bases.containsKey(varName)) {
        CType baseType = bases.get(varName);
        String uf = CToFormulaConverterWithPointerAliasing.getPointerAccessName(baseType);
        String baseVarName = PointerTargetSet.getBaseName(varName);
        return ffmgr.declareAndCallUF(
            uf,
            fmgr.getFormulaType(atom),
            fmgr.makeVariable(typeConverter.getPointerType(), baseVarName));
      }

      return atom;
    }
  }

  private static class ReachedSetBasedFormulaSupplier {

    private final LazyLocationMapping lazyLocationMapping;

    public ReachedSetBasedFormulaSupplier(LazyLocationMapping pLazyLocationMapping) {
      lazyLocationMapping = Objects.requireNonNull(pLazyLocationMapping);
    }

    public BooleanFormula getInvariantFor(
        CFANode pLocation, FormulaManagerView fmgr, PathFormulaManager pfmgr) {
      BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
      BooleanFormula invariant = bfmgr.makeBoolean(false);

      for (AbstractState locState : lazyLocationMapping.get(pLocation)) {
        invariant = bfmgr.or(invariant, extractReportedFormulas(fmgr, locState, pfmgr));
      }
      return invariant;
    }
  }

  private static class AggregatedInvariantSupplier implements InvariantSupplier {

    private final Collection<ReachedSetBasedFormulaSupplier> invariantSuppliers;
    private final Map<InvariantsCacheKey, BooleanFormula> cache = new HashMap<>();
    private final CtoFormulaTypeHandler typeConverter;

    private AggregatedInvariantSupplier(
        ImmutableCollection<ReachedSetBasedFormulaSupplier> pInvariantSuppliers,
        CtoFormulaTypeHandler pTypeHandler) {
      invariantSuppliers = checkNotNull(pInvariantSuppliers);
      typeConverter = pTypeHandler;
    }

    @Override
    public BooleanFormula getInvariantFor(
        CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr, PathFormula pContext) {
      InvariantsCacheKey key = new InvariantsCacheKey(pNode, pFmgr, pPfmgr);

      BooleanFormula invariant;
      if (cache.containsKey(key)) {
        invariant = cache.get(key);
      } else {
        final BooleanFormulaManager bfmgr = pFmgr.getBooleanFormulaManager();
        invariant =
            bfmgr.and(
                invariantSuppliers
                    .stream()
                    .map(s -> s.getInvariantFor(pNode, pFmgr, pPfmgr))
                    .filter(f -> !bfmgr.isTrue(f))
                    .collect(Collectors.toList()));

        cache.put(key, invariant);
      }

      // add pointer target information if possible/necessary
      if (pContext != null) {
        invariant =
            pFmgr.transformRecursively(
                new AddPointerInformationVisitor(
                    pFmgr, pContext.getPointerTargetSet(), typeConverter),
                invariant);
      }

      return verifyNotNull(invariant);
    }
  }

  private static class InvariantsCacheKey {
    private final CFANode node;
    private final FormulaManagerView fmgr;
    private final PathFormulaManager pfmgr;

    public InvariantsCacheKey(CFANode pNode, FormulaManagerView pFmgr, PathFormulaManager pPfmgr) {
      node = pNode;
      fmgr = pFmgr;
      pfmgr = pPfmgr;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hashCode(fmgr);
      result = prime * result + Objects.hashCode(node);
      result = prime * result + Objects.hashCode(pfmgr);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof InvariantsCacheKey)) {
        return false;
      }

      InvariantsCacheKey other = (InvariantsCacheKey) obj;
      return Objects.equals(node, other.node)
          && Objects.equals(fmgr, other.fmgr)
          && Objects.equals(pfmgr, other.pfmgr);
    }
  }

}
