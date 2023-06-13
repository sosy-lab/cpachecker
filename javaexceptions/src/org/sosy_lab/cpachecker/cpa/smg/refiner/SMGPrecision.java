// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentMultimap;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public abstract class SMGPrecision implements Precision {

  private final SMGPrecisionAbstractionOptions options;
  private final int maxLength;
  private final int threshold = 0; // TODO always zero??

  protected SMGPrecision(SMGPrecisionAbstractionOptions pOptions) {
    options = pOptions;
    maxLength = 2;
  }

  protected SMGPrecision(SMGPrecisionAbstractionOptions pOptions, int pMaxLength) {
    options = pOptions;
    maxLength = pMaxLength;
  }

  public static SMGPrecision createStaticPrecision(boolean pEnableHeapAbstraction) {
    SMGPrecisionAbstractionOptions options =
        new SMGPrecisionAbstractionOptions(pEnableHeapAbstraction, false, false);
    return new SMGStaticPrecision(options);
  }

  public static SMGPrecision createStaticPrecision(boolean pEnableHeapAbstraction, int pMaxLength) {
    SMGPrecisionAbstractionOptions options =
        new SMGPrecisionAbstractionOptions(pEnableHeapAbstraction, false, false);
    return new SMGStaticPrecision(options, pMaxLength);
  }

  public abstract Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement);

  public abstract SMGPrecision join(SMGPrecision pPrecision);

  public static SMGPrecision createRefineablePrecision(SMGPrecision pPrecision) {
    return new SMGRefineablePrecision(
        new SMGPrecisionAbstractionOptions(pPrecision.options.allowsHeapAbstraction(), true, true),
        PersistentMultimap.of(),
        PersistentMultimap.of(),
        PersistentMultimap.of());
  }

  public abstract boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode);

  public abstract Set<SMGMemoryPath> getTrackedMemoryPathsOnNode(CFANode pCfaNode);

  public abstract Set<MemoryLocation> getTrackedStackVariablesOnNode(CFANode pCfaNode);

  public boolean allowsHeapAbstractionOnNode(CFANode pCfaNode, BlockOperator pBlockOperator) {
    return options.allowsHeapAbstraction() && pBlockOperator.isBlockEnd(pCfaNode, threshold);
  }

  public SMGPrecisionAbstractionOptions getAbstractionOptions() {
    return options;
  }

  public int getMaxLength() {
    return maxLength;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGPrecision)) {
      return false;
    }
    SMGPrecision other = (SMGPrecision) o;
    return threshold == other.threshold
        && maxLength == other.maxLength
        && options.equals(other.options);
  }

  @Override
  public int hashCode() {
    return options.hashCode() + maxLength;
  }

  public abstract Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode location);

  private static class SMGRefineablePrecision extends SMGPrecision {

    private final PersistentMultimap<CFANode, SMGMemoryPath> trackedMemoryPaths;
    private final PersistentMultimap<CFANode, MemoryLocation> trackedStackVariables;
    private final PersistentMultimap<CFANode, SMGAbstractionBlock> abstractionBlocks;

    private SMGRefineablePrecision(
        SMGPrecisionAbstractionOptions pOptions,
        PersistentMultimap<CFANode, SMGMemoryPath> pTrackedMemoryPaths,
        PersistentMultimap<CFANode, SMGAbstractionBlock> pAbstractionBlocks,
        PersistentMultimap<CFANode, MemoryLocation> pTrackedStackVariables) {
      super(pOptions);
      trackedMemoryPaths = pTrackedMemoryPaths;
      abstractionBlocks = pAbstractionBlocks;
      trackedStackVariables = pTrackedStackVariables;
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

      PersistentMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = trackedMemoryPaths;
      PersistentMultimap<CFANode, MemoryLocation> resultStackVariables = trackedStackVariables;
      PersistentMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = abstractionBlocks;

      for (Entry<CFANode, SMGPrecisionIncrement> entry : pPrecisionIncrement.entrySet()) {
        SMGPrecisionIncrement inc = entry.getValue();
        CFANode cfaNode = entry.getKey();
        resultAbstractionBlocks =
            resultAbstractionBlocks.putAllAndCopy(cfaNode, inc.getAbstractionBlock());
        resultMemoryPaths = resultMemoryPaths.putAllAndCopy(cfaNode, inc.getPathsToTrack());
        resultStackVariables =
            resultStackVariables.putAllAndCopy(cfaNode, inc.getStackVariablesToTrack());
      }

      return new SMGRefineablePrecision(
          getAbstractionOptions(),
          resultMemoryPaths,
          resultAbstractionBlocks,
          resultStackVariables);
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {

      if (pPrecision instanceof SMGStaticPrecision) {
        return pPrecision;
      }

      SMGRefineablePrecision other = (SMGRefineablePrecision) pPrecision;
      assert getAbstractionOptions().equals(pPrecision.getAbstractionOptions());

      return new SMGRefineablePrecision(
          getAbstractionOptions(),
          trackedMemoryPaths.putAllAndCopy(other.trackedMemoryPaths),
          abstractionBlocks.putAllAndCopy(other.abstractionBlocks),
          trackedStackVariables.putAllAndCopy(other.trackedStackVariables));
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
      return "SMGRefineablePrecision [trackedMemoryPaths="
          + trackedMemoryPaths
          + ", trackedStackVariables="
          + trackedStackVariables
          + ", abstractionBlocks="
          + abstractionBlocks
          + "]";
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SMGRefineablePrecision) && !super.equals(o)) {
        return false;
      }
      SMGRefineablePrecision other = (SMGRefineablePrecision) o;
      return trackedMemoryPaths.equals(other.trackedMemoryPaths)
          && trackedStackVariables.equals(other.trackedStackVariables)
          && abstractionBlocks.equals(other.abstractionBlocks);
    }

    @Override
    public int hashCode() {
      return super.hashCode() * 31
          + Objects.hash(trackedMemoryPaths, trackedStackVariables, abstractionBlocks);
    }
  }

  private static class SMGStaticPrecision extends SMGPrecision {

    private SMGStaticPrecision(SMGPrecisionAbstractionOptions pAllowsHeapAbstraction) {
      super(pAllowsHeapAbstraction);
    }

    public SMGStaticPrecision(
        SMGPrecisionAbstractionOptions pAllowsHeapAbstraction, int pMaxLength) {
      super(pAllowsHeapAbstraction, pMaxLength);
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
      return "Static precision " + getAbstractionOptions();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof SMGStaticPrecision && super.equals(o);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }
  }

  public static final class SMGPrecisionAbstractionOptions {

    private final boolean heapAbstraction;
    private final boolean fieldAbstraction;
    private final boolean stackAbstraction;

    public SMGPrecisionAbstractionOptions(
        boolean pHeapAbstraction, boolean pFieldAbstraction, boolean pStackAbstraction) {
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
      return Objects.hash(fieldAbstraction, heapAbstraction, stackAbstraction);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SMGPrecisionAbstractionOptions)) {
        return false;
      }
      SMGPrecisionAbstractionOptions other = (SMGPrecisionAbstractionOptions) obj;
      return fieldAbstraction == other.fieldAbstraction
          && heapAbstraction == other.heapAbstraction
          && stackAbstraction == other.stackAbstraction;
    }

    @Override
    public String toString() {
      return "SMGPrecisionAbstractionOptions [heapAbstraction="
          + heapAbstraction
          + ", fieldAbstraction="
          + fieldAbstraction
          + ", stackAbstraction="
          + stackAbstraction
          + "]";
    }
  }
}
