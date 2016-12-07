/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.ArrayQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.Precisions;


public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final BAMCPA bamCpa;
  private final ARGPath path;
  private final ARGState rootOfSubgraph;
  private final Timer removeCachedSubtreeTimer;
  private final boolean collectPreisionFromAllSubgraph;

  private final Function<AbstractState, Precision> GET_PRECISION = new Function<AbstractState, Precision>() {
    @Nullable
    @Override
    public Precision apply(@Nullable AbstractState state) {
      return delegate.asReachedSet().getPrecision(delegate.asReachedSet().getLastState());
      // TODO do we really need the target-precision for refinements and not the actual one?
      // return transfer.getPrecisionForState(Preconditions.checkNotNull(subgraphStatesToReachedState.get(state)), delegate.asReachedSet());
    }
  };

  public BAMReachedSet(BAMCPA cpa, ARGReachedSet pMainReachedSet, ARGPath pPath,
      ARGState pRootOfSubgraph,
      Timer pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    this.bamCpa = cpa;
    this.path = pPath;
    this.rootOfSubgraph = pRootOfSubgraph;
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;
    this.collectPreisionFromAllSubgraph = bamCpa.collectPrecisionFromAllSubgraph();

    assert rootOfSubgraph.getSubgraph().containsAll(path.asStatesList()) : "path should traverse reachable states";
    assert pRootOfSubgraph == path.getFirstState() : "path should start with root-state";
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new UnmodifiableReachedSet() {

      private final Collection<AbstractState> subgraph =
          Collections.unmodifiableCollection(rootOfSubgraph.getSubgraph());

      @Override
      public Collection<AbstractState> asCollection() {
        return subgraph;
      }

      @Override
      public Iterator<AbstractState> iterator() {
        return subgraph.iterator();
      }

      @Override
      public Collection<Precision> getPrecisions() {
        return Collections2.transform(subgraph, GET_PRECISION);
      }

      @Override
      public Collection<AbstractState> getReached(AbstractState state) {
        throw new UnsupportedOperationException("should not be needed");
      }

      @Override
      public Collection<AbstractState> getReached(CFANode location) {
        throw new UnsupportedOperationException("should not be needed");
      }

      @Override
      public AbstractState getFirstState() {
        return rootOfSubgraph;
      }

      @Override
      public AbstractState getLastState() {
        return path.getLastState();
      }

      @Override
      public boolean hasWaitingState() {
        // BAM-reached-set has no waiting states
        return false;
      }

      @Override
      public Collection<AbstractState> getWaitlist() {
        // BAM-reached-set has no waiting states
        return Collections.emptySet();
      }

      @Override
      public Precision getPrecision(AbstractState state) {

        if (!collectPreisionFromAllSubgraph) {
          return GET_PRECISION.apply(state);
        }

        assert state instanceof BackwardARGState;

        ARGState argState = ((BackwardARGState)state).getARGState();
        BAMDataManager data = bamCpa.getData();

        ReachedSet currentReachedSet = getReachedSet((BackwardARGState) state);
        if (currentReachedSet == null) {
          //The main reached set can not be extracted.
        } else {
          Precision targetPrecision = currentReachedSet.getPrecision(argState);
          Precision newPrecision = collectPrecision(currentReachedSet, argState, targetPrecision, new HashSet<>());
          return newPrecision;
        }

        if (data.hasInitialState(argState)) {
          ReachedSet rSet = data.getReachedSetForInitialState(argState);
          Precision prec = rSet.getPrecision(rSet.getLastState());
          return prec;
          //PredicatePrecision pPrec = Precisions.extractPrecisionByType(prec, PredicatePrecision.class);
          //VariableTrackingPrecision vPrec = Precisions.extractPrecisionByType(prec, VariableTrackingPrecision.class);
        }
        return GET_PRECISION.apply(state);
      }

      @Override
      public void forEach(BiConsumer<? super AbstractState, ? super Precision> pAction) {
        subgraph.forEach(state -> pAction.accept(state, GET_PRECISION.apply(state)));
      }

      @Override
      public boolean contains(AbstractState state) {
        return subgraph.contains(state);
      }

      @Override
      public boolean isEmpty() {
        return subgraph.isEmpty();
      }

      @Override
      public int size() {
        throw new UnsupportedOperationException("should not be needed");
      }

      private Precision collectPrecision(ReachedSet reached, AbstractState state, Precision pTargetPrecision,
          Collection<ReachedSet> handledSets) {
        Collection<VariableTrackingPrecision> valuePrecisions = new HashSet<>();
        Collection<PredicatePrecision> predicatePrecisions = new HashSet<>();
        Queue<ARGState> worklist = new ArrayQueue<>();
        BAMDataManager data = bamCpa.getData();
        ARGState currentState;
        Precision currentPrecision;

        worklist.add((ARGState) state);

        while (!worklist.isEmpty()) {
          currentState = worklist.poll();
          if (currentState.isCovered() || currentState.isDestroyed()) {
            continue;
          }

          currentPrecision = reached.getPrecision(currentState);
          valuePrecisions.add(Precisions.extractPrecisionByType(currentPrecision, VariableTrackingPrecision.class));
          predicatePrecisions.add(Precisions.extractPrecisionByType(currentPrecision, PredicatePrecision.class));
          worklist.addAll(currentState.getChildren());
          if (data.hasInitialState(currentState)) {
            ReachedSet other = data.getReachedSetForInitialState(currentState);
            if (!handledSets.contains(other)) {
              AbstractState reducedState = other.getFirstState();
              handledSets.add(other);
              Precision collectedPrecision = collectPrecision(other, reducedState, pTargetPrecision, handledSets);
              valuePrecisions.add(Precisions.extractPrecisionByType(collectedPrecision, VariableTrackingPrecision.class));
              predicatePrecisions.add(Precisions.extractPrecisionByType(collectedPrecision, PredicatePrecision.class));
            }
          }
        }
        VariableTrackingPrecision initialValuePrecision = Precisions.extractPrecisionByType(pTargetPrecision, VariableTrackingPrecision.class);
        PredicatePrecision newPredicatePrecision = PredicatePrecision.unionOf(predicatePrecisions);
        VariableTrackingPrecision newValuePrecision = initialValuePrecision;

        for (VariableTrackingPrecision prec : valuePrecisions) {
          newValuePrecision = newValuePrecision.join(prec);
        }
        pTargetPrecision = Precisions.replaceByType(pTargetPrecision, newValuePrecision, VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
        pTargetPrecision = Precisions.replaceByType(pTargetPrecision, newPredicatePrecision, Predicates.instanceOf(PredicatePrecision.class));
        return pTargetPrecision;
      }

      private ReachedSet getReachedSet(BackwardARGState state) {
        BackwardARGState currentState = state;
        ARGState currentARG;
        ARGState targetState = state.getARGState();
        Queue<ARGState> worklist = new ArrayQueue<>();
        BAMDataManager data = bamCpa.getData();
        worklist.add(currentState);

        while (!worklist.isEmpty()) {
          currentState = (BackwardARGState) worklist.poll();
          currentARG = currentState.getARGState();
          if (data.hasInitialState(currentARG)) {
            ReachedSet rSet = data.getReachedSetForInitialState(currentARG);
            if (rSet.contains(targetState)) {
              return rSet;
            }
          }
          worklist.addAll(currentState.getParents());
        }
        return null;
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public void removeSubtree(
      ARGState element, Precision newPrecision, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    removeSubtree(element, ImmutableList.of(newPrecision), ImmutableList.of(pPrecisionType));
  }

  @Override
  public void removeSubtree(
      ARGState element,
      List<Precision> newPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    Preconditions.checkArgument(newPrecisions.size()==pPrecisionTypes.size());
    assert rootOfSubgraph.getSubgraph().contains(element);
    final ARGSubtreeRemover argSubtreeRemover = new ARGSubtreeRemover(bamCpa, removeCachedSubtreeTimer);
    argSubtreeRemover.removeSubtree(delegate, path, element, newPrecisions, pPrecisionTypes);

    // post-processing, cleanup data-structures.
    // We remove all states reachable from 'element'. This step is not precise,
    // because sub-reached-sets might be changed and we do not remove the corresponding states.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    // We can ignore waitlist-updates and coverage here, because there is no coverage in a BAM-CEX.
    for (ARGState state : element.getSubgraph()) {
      state.removeFromARG();
    }
  }

  @Override
  public void removeSubtree(ARGState pE) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(){
    return "BAMReachedSet {{" + delegate.asReachedSet().asCollection().toString() + "}}";
  }
}