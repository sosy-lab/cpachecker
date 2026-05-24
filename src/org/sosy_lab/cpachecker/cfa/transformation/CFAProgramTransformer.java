// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.graph.Traverser;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * Algorithm for performing all program transformations and adding them to the CFA.
 */
public class CFAProgramTransformer {

  public static MutableCFA applyTransformations(MutableCFA pCFA) {
    //boolean finished = false;
    ImmutableList<ProgramTransformationEnum> selectedProgramTransformations = (new ImmutableList.Builder<ProgramTransformationEnum>().add(ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION)).build();

    ImmutableList.Builder<SubCFA> newSubCFAs = new Builder<>();

    for (FunctionEntryNode functionEntryNode : pCFA.entryNodes()) {
      Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
      Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(functionEntryNode);

      for (CFANode currentNode : cfaNodeIterable) {
        if (selectedProgramTransformations.contains(ProgramTransformationEnum.JUMP_THREADING)) {
          Optional<SubCFA> transformationResult = new JumpThreadingProgramTransformation().transform(pCFA, currentNode);
          if (transformationResult.isPresent()) {
            newSubCFAs.add(transformationResult.get());
          }
        }
        if (selectedProgramTransformations.contains(ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION)) {
          Optional<SubCFA> transformationResult = new TailRecursionEliminationProgramTransformation().transform(pCFA, currentNode);
          if (transformationResult.isPresent()) {
            newSubCFAs.add(transformationResult.get());
          }
        }
      }
    }

    for (SubCFA subCFA : newSubCFAs.build()) {
      subCFA.insertSubCFA(pCFA);
    }

    return pCFA;
  }
}
