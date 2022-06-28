//// This file is part of CPAchecker,
//// a tool for configurable software verification:
//// https://cpachecker.sosy-lab.org
////
//// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
////
//// SPDX-License-Identifier: Apache-2.0
//
// package org.sosy_lab.cpachecker.core.algorithm.giageneration;
//
// import java.util.Optional;
// import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
// import org.sosy_lab.cpachecker.cpa.arg.ARGState;
// import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
// import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
//
// public class GIAARGStateEdgeWithAssumptionNaming extends GIAARGStateEdge {
//  public GIAARGStateEdgeWithAssumptionNaming(
//      ARGState pSource, ARGState pTarget, CFAEdge pEdge, Optional<AbstractionFormula> pAssumption)
// {
//    super(pSource, pTarget, pEdge, pAssumption);
//  }
//
//  public GIAARGStateEdgeWithAssumptionNaming(ARGState pSource, CFAEdge pEdge) {
//    super(pSource, pEdge);
//  }
//
//  public GIAARGStateEdgeWithAssumptionNaming(ARGState pSource, ARGState pTarget, CFAEdge pEdge) {
//    super(pSource, pTarget, pEdge);
//  }
//
//  @Override
//  public String getSourceName() {
//    Optional<AutomatonState> automatonState = GIAGenerator.getWitnessAutomatonState(source);
//    if (automatonState.isPresent()) {
//      return GIAGenerator.getNameOrError(automatonState.orElseThrow());
//    }
//    return GIAGenerator.getNameOrError(source);
//  }
//
//  @Override
//  public String getTargetName() {
//    if (this.target.isPresent()) {
//      Optional<AutomatonState> automatonState =
//          GIAGenerator.getWitnessAutomatonState(target.orElseThrow());
//      if (automatonState.isPresent()) {
//        return GIAGenerator.getNameOrError(automatonState.orElseThrow());
//      }
//    }
//    return this.target.isPresent()
//        ? GIAGenerator.getNameOrError(target.orElseThrow())
//        : GIAGenerator.NAME_OF_TEMP_STATE;
//  }
// }
