// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains utility classes for program slicing.
 *
 * @see org.sosy_lab.cpachecker.util.dependencegraph
 */
package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.SMGVariable;
import org.sosy_lab.cpachecker.util.smg.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * Class implementing joinSPC algorithm10 from FIT-TR-2013-4 (Appendix C.7)
 */
public class SMGJoinSPC extends SMGAbstractJoin {

  private final SymbolicProgramConfiguration inputSPC1;
  private final SymbolicProgramConfiguration inputSPC2;

  private Map<SMGVariable, SMGObject> resultMapping = new TreeMap<>();

  private SymbolicProgramConfiguration resultSPC;

  public SMGJoinSPC(SymbolicProgramConfiguration pSpc1, SymbolicProgramConfiguration pSpc2) {
    super(
        SMGJoinStatus.EQUAL,
        pSpc1.getSmg(),
        pSpc2.getSmg(),
        new SMG(),
        new NodeMapping(),
        new NodeMapping());
    inputSPC1 = pSpc1;
    inputSPC2 = pSpc2;
    // assert that both variable region mappings contain the same variables
    checkVariableRanges();

    // step 2 and 3 loop over all variables and apply joinSubSMGS on
    for (SMGVariable variable : inputSPC1.getVariableToSmgObjectMap().keySet()) {
      joinVariable(variable);
      if (status.equals(SMGJoinStatus.INCOMPARABLE)) {
        return;
      }
    }

    // step 4 check for any cycle consisting of 0+ DLSs only in result, and if so, the algorithm
    // fails.
    if (resultDLSHaveNewCycles()) {
      status = status.updateWith(SMGJoinStatus.INCOMPARABLE);
      return;
    }

    // step 5
    resultSPC =
        SymbolicProgramConfiguration
            .of(destSMG, PathCopyingPersistentTreeMap.copyOf(resultMapping));
  }



  /**
   * Apply joinSubSMG on the two input SMG and the SMGObjects connected to a certain variable.
   *
   * @param pVar - the variable both SMGs should be joined on.
   */
  private void joinVariable(SMGVariable pVar) {
    // step 2-1 and 3-1 create fresh region
    SMGObject obj1 = inputSPC1.getVariableToSmgObjectMap().get(pVar);
    SMGObject obj2 = inputSPC2.getVariableToSmgObjectMap().get(pVar);
    SMGObject newObject = obj1.freshCopy();
    destSMG = destSMG.copyAndAddObject(newObject);
    // step 2-3 introduce new mappings
    mapping1.addMapping(obj1, newObject);
    mapping2.addMapping(obj2, newObject);
    resultMapping.put(pVar, newObject);
    // step 3 join on the newly created region
    SMGJoinSubSMGs joinSubSMGs =
        new SMGJoinSubSMGs(
            status,
            inputSMG1,
            inputSMG2,
            destSMG,
            mapping1,
            mapping2,
            obj1,
            obj2,
            newObject,
            0);

    if (!joinSubSMGs.isDefined() || joinSubSMGs.isRecoverableFailur()) {
      // join failed
      status = status.updateWith(SMGJoinStatus.INCOMPARABLE);
    } else {
      // join successful
      copyJoinState(joinSubSMGs);
    }
  }

  private void checkVariableRanges() {
    Set<SMGVariable> spc1Variables = inputSPC1.getVariableToSmgObjectMap().keySet();
    Set<SMGVariable> spc2Variables = inputSPC2.getVariableToSmgObjectMap().keySet();
    checkArgument(
        spc1Variables.containsAll(spc2Variables),
        "Variable ranges are not equal.");
  }


  public SymbolicProgramConfiguration getResult() {
    return resultSPC;
  }

}
