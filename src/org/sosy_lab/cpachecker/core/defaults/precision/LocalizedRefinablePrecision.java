// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class LocalizedRefinablePrecision extends RefinablePrecision {

  private static final long serialVersionUID = 1L;

  /**
   * the immutable collection that determines which variables are tracked at a specific location -
   * if it is null, all variables are tracked
   */
  private transient ImmutableMultimap<CFANode, MemoryLocation> rawPrecision;

  LocalizedRefinablePrecision(VariableTrackingPrecision pBaseline) {
    super(pBaseline);
    rawPrecision = ImmutableMultimap.of();
  }

  private LocalizedRefinablePrecision(
      VariableTrackingPrecision pBaseline,
      ImmutableMultimap<CFANode, MemoryLocation> pRawPrecision) {
    super(pBaseline);
    rawPrecision = pRawPrecision;
  }

  @Override
  public LocalizedRefinablePrecision withIncrement(Multimap<CFANode, MemoryLocation> increment) {
    if (this.rawPrecision.entries().containsAll(increment.entries())) {
      return this;
    } else {
      // sorted multimap so that we have deterministic output
      SetMultimap<CFANode, MemoryLocation> refinedPrec = TreeMultimap.create(rawPrecision);
      refinedPrec.putAll(increment);

      return new LocalizedRefinablePrecision(super.getBaseline(), ImmutableMultimap.copyOf(refinedPrec));
    }
  }

  @Override
  public void serialize(Writer writer) throws IOException {
    for (CFANode currentLocation : rawPrecision.keySet()) {
      writer.write("\n" + currentLocation + ":\n");

      for (MemoryLocation variable : rawPrecision.get(currentLocation)) {
        writer.write(variable.serialize() + "\n");
      }
    }
  }

  @Override
  public VariableTrackingPrecision join(VariableTrackingPrecision consolidatedPrecision) {
    checkArgument(getClass().equals(consolidatedPrecision.getClass()));
    checkArgument(
        super.getBaseline().equals(((LocalizedRefinablePrecision) consolidatedPrecision).getBaseline()));

    SetMultimap<CFANode, MemoryLocation> joinedPrec = TreeMultimap.create(rawPrecision);
    joinedPrec.putAll(((LocalizedRefinablePrecision) consolidatedPrecision).rawPrecision);
    return new LocalizedRefinablePrecision(super.getBaseline(), ImmutableMultimap.copyOf(joinedPrec));
  }

  @Override
  public int getSize() {
    return rawPrecision.size();
  }

  @Override
  public String toString() {
    return rawPrecision.toString();
  }

  @Override
  public boolean isEmpty() {
    return rawPrecision.isEmpty();
  }

  @Override
  public boolean isTracking(MemoryLocation pVariable, Type pType, CFANode pLocation) {
    return super.isTracking(pVariable, pType, pLocation)
        && rawPrecision.containsEntry(pLocation, pVariable);
  }

  @Override
  public boolean tracksTheSameVariablesAs(VariableTrackingPrecision pOtherPrecision) {
    if (pOtherPrecision.getClass().equals(getClass())
        && super.getBaseline().equals(((LocalizedRefinablePrecision) pOtherPrecision).getBaseline())
        && rawPrecision.equals(((LocalizedRefinablePrecision) pOtherPrecision).rawPrecision)) {
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other)
        && other instanceof LocalizedRefinablePrecision
        && rawPrecision.equals(((LocalizedRefinablePrecision) other).rawPrecision);
  }

  @Override
  public int hashCode() {
    return super.hashCode() * 31 + rawPrecision.hashCode();
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    out.writeInt(rawPrecision.size());
    for (Entry<CFANode, MemoryLocation> e : rawPrecision.entries()) {
      out.writeInt(e.getKey().getNodeNumber());
      out.writeObject(e.getValue());
    }
  }

  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    CFAInfo cfa = GlobalInfo.getInstance().getCFAInfo().orElseThrow();
    ImmutableMultimap.Builder<CFANode, MemoryLocation> precisionBuilder =
        ImmutableMultimap.builder();
    for (int i = 0; i < in.readInt(); i++) {
      precisionBuilder.put(cfa.getNodeByNodeNumber(in.readInt()), (MemoryLocation) in.readObject());
    }
    rawPrecision = precisionBuilder.build();
  }
}
