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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public abstract class SMGPrecision implements Precision {

  private final SMGPrecisionAbstractionOptions options;
  private final BlockOperator blockOperator;
  private final LogManager logger;
  private final int threshold = 0;
  private final VariableClassification varClass;
  private final Optional<LiveVariables> liveVars;
  private final SMGHeapAbstractionThreshold heapAbsThreshold;

  public SMGPrecision(LogManager pLogger, SMGPrecisionAbstractionOptions pOptions,
      BlockOperator pBlockOperator, VariableClassification pVarClass,
      Optional<LiveVariables> pLiveVars, SMGHeapAbstractionThreshold pHeapAbsThreshold) {
    logger = pLogger;
    options = pOptions;
    blockOperator = pBlockOperator;
    varClass = pVarClass;
    liveVars = pLiveVars;
    heapAbsThreshold = pHeapAbsThreshold;
  }

  public static SMGPrecision createStaticPrecision(boolean pEnableHeapAbstraction,
      LogManager pLogger, BlockOperator pBlockOperator, CFA pCFA, boolean pUseSMGMerge,
      boolean pUseLiveVariableAnalysis) {
    SMGPrecisionAbstractionOptions options =
        new SMGPrecisionAbstractionOptions(pEnableHeapAbstraction, false, false,
            pUseLiveVariableAnalysis, false, pUseSMGMerge);
    return new SMGStaticPrecision(pLogger, options, pBlockOperator,
        pCFA.getVarClassification().orElse(VariableClassification.empty(pLogger)),
        pCFA.getLiveVariables(), new SMGHeapAbstractionThreshold(2, 2, 3));
  }

  public abstract SMGPrecision refineOptions(SMGPrecisionAbstractionOptions pNewOptions, SMGHeapAbstractionThreshold pNewThreshold);

  public abstract Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement);

  public abstract SMGPrecision join(SMGPrecision pPrecision);

  public static SMGPrecision createRefineablePrecision(SMGPrecision pPrecision,
      boolean useInterpoaltion) {

    SetMultimap<CFANode, SMGMemoryPath> emptyMemoryPaths = ImmutableSetMultimap.of();
    SetMultimap<CFANode, SMGAbstractionBlock> emptyAbstractionBlocks = ImmutableSetMultimap.of();
    SetMultimap<CFANode, MemoryLocation> emptyStackVariable = ImmutableSetMultimap.of();

    return new SMGRefineablePrecision(pPrecision.logger,
        new SMGPrecisionAbstractionOptions(pPrecision.useHeapAbstraction(), useInterpoaltion,
            useInterpoaltion,
            pPrecision.forgetDeadVariables(), useInterpoaltion, pPrecision.useSMGMerge()),
        pPrecision.getBlockOperator(),
        emptyMemoryPaths, emptyAbstractionBlocks, emptyStackVariable, pPrecision.getVarClass(),
        pPrecision.getLiveVars(),
        new SMGHeapAbstractionThreshold(2, 2, 2));
  }

  public LogManager getLogger() {
    return logger;
  }

  public final boolean useInterpoaltion() {
    return options.useInterpoaltion();
  }

  public abstract boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode);

  public abstract Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pCfaNode);

  public abstract Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode);

  public Set<MemoryLocation> getDeadVariablesOnLocation(CFANode node,
      Set<MemoryLocation> variablesOnNode) {
    if (!liveVars.isPresent()) {
      return ImmutableSet.of();
    }

    LiveVariables liveVariables = liveVars.get();

    FluentIterable<String> liveVariablesDcl =
        liveVariables.getLiveVariableNamesForNode(node);

    return FluentIterable.from(variablesOnNode).filter((MemoryLocation loc) -> {

      if (loc.isOnFunctionStack()) {
        return !liveVariablesDcl.contains(
            MemoryLocation.valueOf(loc.getFunctionName(), loc.getIdentifier()).getAsSimpleString());
      } else {
        return !liveVariablesDcl.contains(loc.getIdentifier());
      }
    }).toSet();

  }

  public abstract boolean usesHeapInterpoaltion();

  public boolean useHeapAbstractionOnNode(CFANode pCfaNode) {
    return options.useHeapAbstraction() && blockOperator.isBlockEnd(pCfaNode, threshold);
  }

  public final boolean useHeapAbstraction() {
    return options.useHeapAbstraction();
  }

  public final boolean useFieldAbstraction() {
    return options.useFieldAbstraction();
  }

  public final boolean useStackAbstraction() {
    return options.useStackAbstraction();
  }

  public boolean forgetDeadVariables() {
    return options.forgetDeadVariables();
  }

  public boolean useSMGMerge() {
    return options.useSMGMerge();
  }

  public SMGPrecisionAbstractionOptions getAbstractionOptions() {
    return options;
  }

  public BlockOperator getBlockOperator() {
    return blockOperator;
  }

  public VariableClassification getVarClass() {
    return varClass;
  }

  public Optional<LiveVariables> getLiveVars() {
    return liveVars;
  }

  public SMGHeapAbstractionThreshold getHeapAbsThreshold() {
    return heapAbsThreshold;
  }

  public abstract Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode location);

  private static class SMGRefineablePrecision extends SMGPrecision {

    private final ImmutableSetMultimap<CFANode, SMGMemoryPath> trackedMemoryPaths;
    private final ImmutableSetMultimap<CFANode, MemoryLocation> trackedStackVariables;
    private final ImmutableSetMultimap<CFANode, SMGAbstractionBlock> abstractionBlocks;

    private SMGRefineablePrecision(LogManager pLogger, SMGPrecisionAbstractionOptions pOptions,
        BlockOperator pBlockOperator,
        SetMultimap<CFANode, SMGMemoryPath> pTrackedMemoryPaths,
        SetMultimap<CFANode, SMGAbstractionBlock> pAbstractionBlocks,
        SetMultimap<CFANode, MemoryLocation> pTrackedStackVariables,
        VariableClassification pVarClass, Optional<LiveVariables> pLiveVars,
        SMGHeapAbstractionThreshold pHeapAbsThreshold) {
      super(pLogger, pOptions, pBlockOperator, pVarClass, pLiveVars, pHeapAbsThreshold);
      trackedMemoryPaths = ImmutableSetMultimap.copyOf(pTrackedMemoryPaths);
      abstractionBlocks = ImmutableSetMultimap.copyOf(pAbstractionBlocks);
      trackedStackVariables = ImmutableSetMultimap.copyOf(pTrackedStackVariables);
    }

    @Override
    public boolean usesHeapInterpoaltion() {
      return useHeapAbstraction();
    }

    @Override
    public Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pLocationNode) {
      if (trackedMemoryPaths.containsKey(pLocationNode)) {
        return trackedMemoryPaths.get(pLocationNode);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode) {
      if (trackedStackVariables.containsKey(pCfaNode)) {
        return trackedStackVariables.get(pCfaNode);
      } else {
        return ImmutableSet.of();
      }
    }

    @Override
    public Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement) {

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create();
      resultMemoryPaths.putAll(trackedMemoryPaths);
      SetMultimap<CFANode, MemoryLocation> resultStackVariables = HashMultimap.create();
      resultStackVariables.putAll(trackedStackVariables);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create();
      resultAbstractionBlocks.putAll(abstractionBlocks);

      for (Entry<CFANode, SMGPrecisionIncrement> entry : pPrecisionIncrement.entrySet()) {
        SMGPrecisionIncrement inc = entry.getValue();
        CFANode cfaNode = entry.getKey();
        Collection<SMGAbstractionBlock> incAbstractionBlocks = inc.getAbstractionBlock();
        Collection<SMGMemoryPath> incMemoryPaths = inc.getPathsToTrack();
        Collection<MemoryLocation> incStackVariables = inc.getStackVariablesToTrack();
        resultAbstractionBlocks.putAll(cfaNode, incAbstractionBlocks);
        resultMemoryPaths.putAll(cfaNode, incMemoryPaths);
        resultStackVariables.putAll(cfaNode, incStackVariables);
      }

      return new SMGRefineablePrecision(getLogger(), getAbstractionOptions(), getBlockOperator(),
          resultMemoryPaths,
          resultAbstractionBlocks, resultStackVariables, getVarClass(), getLiveVars(), getHeapAbsThreshold());
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {

      if (pPrecision instanceof SMGStaticPrecision) {
        return pPrecision;
      }

      SMGRefineablePrecision other = (SMGRefineablePrecision) pPrecision;

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create();
      resultMemoryPaths.putAll(trackedMemoryPaths);
      resultMemoryPaths.putAll(other.trackedMemoryPaths);
      SetMultimap<CFANode, MemoryLocation> resultStackVariables = HashMultimap.create();
      resultStackVariables.putAll(trackedStackVariables);
      resultStackVariables.putAll(other.trackedStackVariables);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create();
      resultAbstractionBlocks.putAll(abstractionBlocks);
      resultAbstractionBlocks.putAll(other.abstractionBlocks);

      SMGPrecisionAbstractionOptions options = new SMGPrecisionAbstractionOptions(
          useHeapAbstraction() && pPrecision.useHeapAbstraction(),
          useFieldAbstraction() && pPrecision.useFieldAbstraction(),
          useStackAbstraction() && pPrecision.useStackAbstraction(),
          forgetDeadVariables() && pPrecision.forgetDeadVariables(),
          useInterpoaltion() && pPrecision.useInterpoaltion(),
          useSMGMerge());

      return new SMGRefineablePrecision(getLogger(), options, getBlockOperator(),
          resultMemoryPaths,
          resultAbstractionBlocks, resultStackVariables, getVarClass(), getLiveVars(), getHeapAbsThreshold());
    }

    @Override
    public boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode) {

      if (trackedMemoryPaths.containsKey(pCfaNode)) {
        Set<SMGMemoryPath> trackedMemPaths = trackedMemoryPaths.get(pCfaNode);
        return trackedMemPaths.contains(pPath);
      } else {
        return false;
      }
    }

    @Override
    public Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode location) {
      return abstractionBlocks.get(location);
    }

    @Override
    public String toString() {
      return "SMGRefineablePrecision [trackedMemoryPaths=" + trackedMemoryPaths
          + ", trackedStackVariables=" + trackedStackVariables + ", abstractionBlocks="
          + abstractionBlocks + "]";
    }

    @Override
    public SMGPrecision refineOptions(SMGPrecisionAbstractionOptions pNewOptions,
        SMGHeapAbstractionThreshold pNewThreshold) {
      return new SMGRefineablePrecision(getLogger(), pNewOptions, getBlockOperator(),
          trackedMemoryPaths,
          abstractionBlocks, trackedStackVariables, getVarClass(), getLiveVars(), pNewThreshold);
    }
  }

  private static class SMGStaticPrecision extends SMGPrecision {

    private SMGStaticPrecision(LogManager pLogger,
        SMGPrecisionAbstractionOptions pAllowsHeapAbstraction, BlockOperator pBlockOperator,
        VariableClassification pVarClass, Optional<LiveVariables> pOptional,
        SMGHeapAbstractionThreshold pHeapAbsThreshold) {
      super(pLogger, pAllowsHeapAbstraction, pBlockOperator, pVarClass, pOptional,
          pHeapAbsThreshold);
    }

    @Override
    public boolean usesHeapInterpoaltion() {
      return false;
    }

    @Override
    public boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode) {
      return true;
    }

    @Override
    public Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement) {
      return this;
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {
      return this;
    }

    @Override
    public Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode pLocation) {
      return ImmutableSet.of();
    }

    @Override
    public Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pCfaNode) {
      throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode) {
      throw new UnsupportedOperationException("Method not yet implemented.");
    }

    @Override
    public String toString() {
      return "Static precision " + getAbstractionOptions().toString();
    }

    @Override
    public SMGPrecision refineOptions(SMGPrecisionAbstractionOptions pNewOptions,
        SMGHeapAbstractionThreshold pHeapAbsThrshold) {
      return this;
    }
  }

  public static final class SMGPrecisionAbstractionOptions {

    private final boolean heapAbstraction;
    private final boolean fieldAbstraction;
    private final boolean stackAbstraction;
    private final boolean liveVariableAnalysis;
    private final boolean useInterpoaltion;
    private final boolean smgMerge;

    public SMGPrecisionAbstractionOptions(boolean pHeapAbstraction, boolean pFieldAbstraction,
        boolean pStackAbstraction, boolean pLiveVariableAnalysis,
        boolean pUseInterpoaltion, boolean pUseSMGMerge) {
      super();
      heapAbstraction = pHeapAbstraction;
      fieldAbstraction = pFieldAbstraction;
      stackAbstraction = pStackAbstraction;
      liveVariableAnalysis = pLiveVariableAnalysis;
      useInterpoaltion = pUseInterpoaltion;
      smgMerge = pUseSMGMerge;
    }

    public boolean useInterpoaltion() {
      return useInterpoaltion;
    }

    public boolean useSMGMerge() {
      return smgMerge;
    }

    public boolean forgetDeadVariables() {
      return liveVariableAnalysis;
    }

    public boolean useHeapAbstraction() {
      return heapAbstraction;
    }

    public boolean useFieldAbstraction() {
      return fieldAbstraction;
    }

    public boolean useStackAbstraction() {
      return stackAbstraction;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (fieldAbstraction ? 1231 : 1237);
      result = prime * result + (heapAbstraction ? 1231 : 1237);
      result = prime * result + (stackAbstraction ? 1231 : 1237);
      result = prime * result + (liveVariableAnalysis ? 1231 : 1237);
      result = prime * result + (useInterpoaltion ? 1231 : 1237);
      result = prime * result + (smgMerge ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SMGPrecisionAbstractionOptions other = (SMGPrecisionAbstractionOptions) obj;
      if (fieldAbstraction != other.fieldAbstraction) {
        return false;
      }
      if (heapAbstraction != other.heapAbstraction) {
        return false;
      }
      if (stackAbstraction != other.stackAbstraction) {
        return false;
      }
      if (liveVariableAnalysis != other.liveVariableAnalysis) {
        return false;
      }
      if (useInterpoaltion != other.useInterpoaltion) {
        return false;
      }
      if (smgMerge != other.smgMerge) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "SMGPrecisionAbstractionOptions [heapAbstraction=" + heapAbstraction
          + ", fieldAbstraction=" + fieldAbstraction + ", stackAbstraction=" + stackAbstraction
          + ", liveVariableAnalysis=" + liveVariableAnalysis
          + ", interpolation=" + useInterpoaltion
          + ", smgMerge=" + smgMerge + "]";
    }
  }
}