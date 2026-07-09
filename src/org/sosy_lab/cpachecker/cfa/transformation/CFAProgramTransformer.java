// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.graph.Traverser;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/** Algorithm for performing all program transformations and adding them to the CFA. */
public class CFAProgramTransformer {

  public static MutableCFA applyTransformations(MutableCFA pCFA, List<ProgramTransformationEnum> pProgramTransformations) {
    // boolean finished = false;
    ImmutableList.Builder<ProgramTransformation> programTransformationsBuilder = ImmutableList.builder();
    for (ProgramTransformationEnum programTransformationEnum : pProgramTransformations) {
      switch (programTransformationEnum) {
        case LOOP_ACCELERATION:
          programTransformationsBuilder.add(new LoopAccelerationProgramTransformation());
          break;
        case TAIL_RECURSION_ELIMINATION:
          programTransformationsBuilder.add(new TailRecursionEliminationProgramTransformation());
          break;
        default:
          throw new IllegalArgumentException("Unknown program transformation enum: " + programTransformationEnum);
      }
    }
    ImmutableList<ProgramTransformation> programTransformations = programTransformationsBuilder.build();


    ImmutableList.Builder<ProgramTransformationInformation> newProgramTransformations =
        new ImmutableList.Builder<>();

    for (FunctionEntryNode functionEntryNode : pCFA.entryNodes()) {
      Iterable<CFANode> cfaNodeIterable = Traverser.forGraph(pCFA.asGraph()).breadthFirst(functionEntryNode);

      for (CFANode currentNode : cfaNodeIterable) {
        for (ProgramTransformation transformation : programTransformations) {
          Optional<ProgramTransformationInformation> transformationResult =
              transformation.transform(pCFA, currentNode);
          if (transformationResult.isPresent()) {
            newProgramTransformations.add(transformationResult.orElseThrow());
          }
        }
      }
    }

    for (ProgramTransformationInformation programTransformation :
        newProgramTransformations.build()) {
      // insert new nodes and edges
      programTransformation.subCFA().insertSubCFA(pCFA);
      // add new information to metadata
      ImmutableListMultimap.Builder<CFANode, ProgramTransformationInformation> newMapBuilder =
          ImmutableListMultimap.builder();
      newMapBuilder.putAll(pCFA.getMetadata().getNodesToProgramTransformations().orElse(ImmutableListMultimap.of()));
      newMapBuilder.put(
          programTransformation.subCFA().originalCFAEntryNode(), programTransformation);
      pCFA.setMetadata(pCFA.getMetadata().withNodesToProgramTransformations(newMapBuilder.build()));
    }

    return pCFA;
  }
}
