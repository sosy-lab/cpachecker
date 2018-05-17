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
package org.sosy_lab.cpachecker.core.defaults.precision;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
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

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    CFAInfo cfa = GlobalInfo.getInstance().getCFAInfo().get();
    Builder<CFANode, MemoryLocation> precisionBuilder = ImmutableMultimap.builder();
    for (int i = 0; i < in.readInt(); i++) {
      precisionBuilder.put(cfa.getNodeByNodeNumber(in.readInt()), (MemoryLocation) in.readObject());
    }
    rawPrecision = precisionBuilder.build();
  }
}
