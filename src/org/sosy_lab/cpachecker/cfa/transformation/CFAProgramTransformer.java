// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Algorithm for performing all program transformations and adding them to the CFA.
 */
public class CFAProgramTransformer {

  public static MutableCFA applyTransformations(MutableCFA pCFA) {
    //boolean finished = false;
    ArrayList<ProgramTransformationEnum> selectedProgramTransformations = new ArrayList<ProgramTransformationEnum>();
    selectedProgramTransformations.add(ProgramTransformationEnum.JUMP_THREADING);
    selectedProgramTransformations.add(ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION);
    Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
    Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pCFA.getMainFunction());

    ArrayList<SubCFA> newSubCFAs = new ArrayList<>();

    for(CFANode currentNode : cfaNodeIterable) {
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

    for (SubCFA subCFA : newSubCFAs) {
      subCFA.insertSubCFA(pCFA);
    }

    return pCFA;
  }
}
