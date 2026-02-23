// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.MutableCFA;

/**
 * Algorithm for performing all program transformations and adding them to the CFA.
 */
public class CFAProgramTransformer {

  public static MutableCFA applyTransformations(MutableCFA pCFA) {
    MutableCFA modifiedCFA = MutableCFA.copyOf(pCFA, null, null);
    boolean finished = false;

    while (!finished) {

      Optional<ProgramTransformationInformation> info = new TailRecursionEliminationProgramTransformation().canBeApplied(pCFA);
      if (info.isPresent()){
        SubCFA subCFAToBeAdded =  SubCFA.createSubCFA(pCFA, info.get());
        modifiedCFA = subCFAToBeAdded.insertSubCFA(pCFA);
      } else {
        finished = true;
      }
    }

    return modifiedCFA;
  }
}
