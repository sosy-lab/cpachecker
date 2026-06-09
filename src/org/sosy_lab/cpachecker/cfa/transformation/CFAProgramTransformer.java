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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.graph.Traverser;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/** Algorithm for performing all program transformations and adding them to the CFA. */
public class CFAProgramTransformer {

  public static MutableCFA applyTransformations(MutableCFA pCFA) {
    // boolean finished = false;
    ImmutableList<ProgramTransformationEnum> selectedProgramTransformations =
        new ImmutableList.Builder<ProgramTransformationEnum>()
            .add(ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION)
            .build();

    ImmutableList.Builder<ProgramTransformationInformation> newProgramTransformations =
        new ImmutableList.Builder<>();

    for (FunctionEntryNode functionEntryNode : pCFA.entryNodes()) {
      Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
      Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(functionEntryNode);

      for (CFANode currentNode : cfaNodeIterable) {
        if (selectedProgramTransformations.contains(ProgramTransformationEnum.JUMP_THREADING)) {
          Optional<ProgramTransformationInformation> transformationResult =
              new JumpThreadingProgramTransformation().transform(pCFA, currentNode);
          if (transformationResult.isPresent()) {
            newProgramTransformations.add(transformationResult.orElseThrow());
          }
        }
        if (selectedProgramTransformations.contains(
            ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION)) {
          Optional<ProgramTransformationInformation> transformationResult =
              new TailRecursionEliminationProgramTransformation().transform(pCFA, currentNode);
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
      ImmutableMultimap<CFANode, ProgramTransformationInformation> nodeToProgramTransformation =
          pCFA.getMetadata().getNodesToProgramTransformations().isEmpty()
              ? ImmutableListMultimap.of()
              : pCFA.getMetadata().getNodesToProgramTransformations().orElseThrow();
      ImmutableListMultimap.Builder<CFANode, ProgramTransformationInformation> newMapBuilder =
          ImmutableListMultimap.builder();
      newMapBuilder.putAll(nodeToProgramTransformation);
      newMapBuilder.put(
          programTransformation.subCFA().originalCFAEntryNode(), programTransformation);
      pCFA.setMetadata(pCFA.getMetadata().withNodesToProgramTransformations(newMapBuilder.build()));
    }

    return pCFA;
  }
}
