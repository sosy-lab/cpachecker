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

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class SMGPrecision implements Precision {

  private final SMGPrecisionAbstractionOptions options;
  private final BlockOperator blockOperator;
  private final LogManager logger;
  private final int threshold = 0;

  public SMGPrecision(LogManager pLogger, SMGPrecisionAbstractionOptions pOptions,
      BlockOperator pBlockOperator) {
    logger = pLogger;
    options = pOptions;
    blockOperator = pBlockOperator;
  }

  public static SMGPrecision createStaticPrecision(boolean pEnableHeapAbstraction,
      LogManager pLogger, BlockOperator pBlockOperator) {
    SMGPrecisionAbstractionOptions options =
        new SMGPrecisionAbstractionOptions(pEnableHeapAbstraction, false, false);
    return new SMGStaticPrecision(pLogger, options, pBlockOperator);
  }

  public abstract Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement);

  public abstract SMGPrecision join(SMGPrecision pPrecision);

  public static SMGPrecision createRefineablePrecision(SMGPrecision pPrecision) {
    SetMultimap<CFANode, SMGMemoryPath> emptyMemoryPaths = ImmutableSetMultimap.of();
    SetMultimap<CFANode, SMGAbstractionBlock> emptyAbstractionBlocks = ImmutableSetMultimap.of();
    SetMultimap<CFANode, MemoryLocation> emptyStackVariable = ImmutableSetMultimap.of();

    return new SMGRefineablePrecision(pPrecision.logger,
        new SMGPrecisionAbstractionOptions(pPrecision.allowsHeapAbstraction(), true, true),
        pPrecision.getBlockOperator(),
        emptyMemoryPaths, emptyAbstractionBlocks, emptyStackVariable);
  }

  public LogManager getLogger() {
    return logger;
  }

  public abstract boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode);

  public abstract Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pCfaNode);

  public abstract Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode);

  public abstract boolean usesHeapInterpolation();

  public boolean allowsHeapAbstractionOnNode(CFANode pCfaNode) {
    return options.allowsHeapAbstraction() && blockOperator.isBlockEnd(pCfaNode, threshold);
  }

  public final boolean allowsHeapAbstraction() {
    return options.allowsHeapAbstraction();
  }

  public final boolean allowsFieldAbstraction() {
    return options.allowsFieldAbstraction();
  }

  public final boolean allowsStackAbstraction() {
    return options.allowsStackAbstraction();
  }

  protected SMGPrecisionAbstractionOptions getAbstractionOptions() {
    return options;
  }

  public BlockOperator getBlockOperator() {
    return blockOperator;
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
        SetMultimap<CFANode, MemoryLocation> pTrackedStackVariables) {
      super(pLogger, pOptions, pBlockOperator);
      trackedMemoryPaths = ImmutableSetMultimap.copyOf(pTrackedMemoryPaths);
      abstractionBlocks = ImmutableSetMultimap.copyOf(pAbstractionBlocks);
      trackedStackVariables = ImmutableSetMultimap.copyOf(pTrackedStackVariables);
    }

    @Override
    public boolean usesHeapInterpolation() {
      return allowsHeapAbstraction();
    }

    @Override
    public Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pLocationNode) {
      return trackedMemoryPaths.get(pLocationNode);
    }

    @Override
    public Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode) {
      return trackedStackVariables.get(pCfaNode);
    }

    @Override
    public Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement) {

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create(trackedMemoryPaths);
      SetMultimap<CFANode, MemoryLocation> resultStackVariables = HashMultimap.create(trackedStackVariables);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create(abstractionBlocks);

      for (Entry<CFANode, SMGPrecisionIncrement> entry : pPrecisionIncrement.entrySet()) {
        SMGPrecisionIncrement inc = entry.getValue();
        CFANode cfaNode = entry.getKey();
        resultAbstractionBlocks.putAll(cfaNode, inc.getAbstractionBlock());
        resultMemoryPaths.putAll(cfaNode, inc.getPathsToTrack());
        resultStackVariables.putAll(cfaNode, inc.getStackVariablesToTrack());
      }

      return new SMGRefineablePrecision(getLogger(), getAbstractionOptions(), getBlockOperator(),
          resultMemoryPaths, resultAbstractionBlocks, resultStackVariables);
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {

      if (pPrecision instanceof SMGStaticPrecision) {
        return pPrecision;
      }

      SMGRefineablePrecision other = (SMGRefineablePrecision) pPrecision;

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create(trackedMemoryPaths);
      resultMemoryPaths.putAll(other.trackedMemoryPaths);
      SetMultimap<CFANode, MemoryLocation> resultStackVariables = HashMultimap.create(trackedStackVariables);
      resultStackVariables.putAll(other.trackedStackVariables);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create(abstractionBlocks);
      resultAbstractionBlocks.putAll(other.abstractionBlocks);

      assert getAbstractionOptions().equals(pPrecision.getAbstractionOptions());

      return new SMGRefineablePrecision(getLogger(), getAbstractionOptions(), getBlockOperator(),
          resultMemoryPaths,
          resultAbstractionBlocks, resultStackVariables);
    }

    @Override
    public boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode) {
      return trackedMemoryPaths.get(pCfaNode).contains(pPath);
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
  }

  private static class SMGStaticPrecision extends SMGPrecision {

    private SMGStaticPrecision(LogManager pLogger,
        SMGPrecisionAbstractionOptions pAllowsHeapAbstraction, BlockOperator pBlockOperator) {
      super(pLogger, pAllowsHeapAbstraction, pBlockOperator);
    }

    @Override
    public boolean usesHeapInterpolation() {
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
  }

  private static final class SMGPrecisionAbstractionOptions {

    private final boolean heapAbstraction;
    private final boolean fieldAbstraction;
    private final boolean stackAbstraction;

    public SMGPrecisionAbstractionOptions(boolean pHeapAbstraction, boolean pFieldAbstraction,
        boolean pStackAbstraction) {
      super();
      heapAbstraction = pHeapAbstraction;
      fieldAbstraction = pFieldAbstraction;
      stackAbstraction = pStackAbstraction;
    }

    public boolean allowsHeapAbstraction() {
      return heapAbstraction;
    }

    public boolean allowsFieldAbstraction() {
      return fieldAbstraction;
    }

    public boolean allowsStackAbstraction() {
      return stackAbstraction;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(fieldAbstraction, heapAbstraction, stackAbstraction);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SMGPrecisionAbstractionOptions)) { return false; }
      SMGPrecisionAbstractionOptions other = (SMGPrecisionAbstractionOptions) obj;
      return fieldAbstraction == other.fieldAbstraction
          && heapAbstraction == other.heapAbstraction
          && stackAbstraction != other.stackAbstraction;
    }

    @Override
    public String toString() {
      return "SMGPrecisionAbstractionOptions [heapAbstraction=" + heapAbstraction
          + ", fieldAbstraction=" + fieldAbstraction + ", stackAbstraction=" + stackAbstraction + "]";
    }
  }
}