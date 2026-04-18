// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableMap;
import de.uni_freiburg.informatik.ultimate.util.datastructures.ImmutableSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.SubCFA;
import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;
import java.util.Comparator;

public class LocationPrecision implements AdjustablePrecision {

  private ImmutableSet<SubCFA> allowedProgramTransformations;
  private final ImmutableMap<CFANode, SubCFA> nodesToSubCFA;
  private boolean hasProgramTransformations;
  //private Comparator<SubCFA> precisionOrder = (a,b) -> 1;

  public LocationPrecision(Set<SubCFA> pPrecisions) {
    hasProgramTransformations = !pPrecisions.isEmpty();
    allowedProgramTransformations = ImmutableSet.of(pPrecisions);
    HashMap<CFANode, SubCFA> nodeSubCFAHashMap = new HashMap<>();
    for (SubCFA subCFA : allowedProgramTransformations) {
      for (CFANode node : subCFA.allNodes()) {
        nodeSubCFAHashMap.put(node, subCFA);
      }
    }
    nodesToSubCFA = ImmutableMap.copyOf(nodeSubCFAHashMap);
  }

  public Optional<SubCFA> isPartOfProgramTransformation(CFANode pNode){
    if(nodesToSubCFA.containsKey(pNode)){
      return Optional.of(nodesToSubCFA.get(pNode));
    }else{
      return Optional.empty();
    }
  }

  public ImmutableSet<SubCFA> getAllowedProgramTransformations() {
    return allowedProgramTransformations;
  }

  public boolean hasProgramTransformations() {
    return hasProgramTransformations;
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision otherPrecision) {
    return null;
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision otherPrecision) {
    return null;
  }

  @Override
  public boolean isEmpty() {
    return allowedProgramTransformations.isEmpty();
  }
}
