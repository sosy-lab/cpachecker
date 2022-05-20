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

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentStack;
import org.sosy_lab.cpachecker.cpa.smg2.StackFrame;
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/** Class implementing joinSPC algorithm10 from FIT-TR-2013-4 (Appendix C.7) */
public class SMGJoinSPC extends SMGAbstractJoin {

  private final SymbolicProgramConfiguration inputSPC1;
  private final SymbolicProgramConfiguration inputSPC2;

  private Map<String, SMGObject> resultGolbalMapping = new TreeMap<>();
  private PersistentStack<StackFrame> resultStackMapping = PersistentStack.of();

  private SymbolicProgramConfiguration resultSPC;

  public SMGJoinSPC(SymbolicProgramConfiguration pSpc1, SymbolicProgramConfiguration pSpc2) {
    super(
        SMGJoinStatus.EQUAL,
        pSpc1.getSmg(),
        pSpc2.getSmg(),
        new SMG(pSpc1.getSmg().getSizeOfPointer()),
        new NodeMapping(),
        new NodeMapping());
    inputSPC1 = pSpc1;
    inputSPC2 = pSpc2;
    // assert that both variable region mappings contain the same variables
    checkVariableRanges();

    // step 2 and 3 loop over all variables and apply joinSubSMGS on
    // global heap mapping
    for (Map.Entry<String, SMGObject> variableAndObject :
        inputSPC1.getGolbalVariableToSmgObjectMap().entrySet()) {
      SMGObject destObject =
          joinVariable(
              variableAndObject.getValue(),
              inputSPC2.getGolbalVariableToSmgObjectMap().get(variableAndObject.getKey()));
      resultGolbalMapping.put(variableAndObject.getKey(), destObject);
      if (status.equals(SMGJoinStatus.INCOMPARABLE)) {
        return;
      }
    }
    // local stack mapping
    Iterator<StackFrame> smg1stackIterator = inputSPC1.getStackFrames().iterator();
    Iterator<StackFrame> smg2stackIterator = inputSPC1.getStackFrames().iterator();
    while (smg1stackIterator.hasNext() && smg2stackIterator.hasNext()) {
      StackFrame frameInSMG1 = smg1stackIterator.next();
      StackFrame frameInSMG2 = smg2stackIterator.next();
      Map<String, SMGObject> frameMapping = new TreeMap<>();
      for (Map.Entry<String, SMGObject> variableAndObject : frameInSMG1.getVariables().entrySet()) {
        SMGObject localInSMG1 = variableAndObject.getValue();
        SMGObject localInSMG2 = frameInSMG2.getVariable(variableAndObject.getKey());
        SMGObject destObject = joinVariable(localInSMG1, localInSMG2);
        frameMapping.put(variableAndObject.getKey(), destObject);
        if (status.equals(SMGJoinStatus.INCOMPARABLE)) {
          return;
        }
      }

      Optional<SMGObject> returnOptional = Optional.empty();
      /* Don't forget to join the return object */
      if (frameInSMG1.getReturnObject().isPresent()) {
        SMGObject returnObjectInSmg1 = frameInSMG1.getReturnObject().orElseThrow();
        SMGObject returnObjectInSmg2 = frameInSMG2.getReturnObject().orElseThrow();
        returnOptional = Optional.of(joinVariable(returnObjectInSmg1, returnObjectInSmg2));
        if (status.equals(SMGJoinStatus.INCOMPARABLE)) {
          return;
        }
      }
      resultStackMapping =
          resultStackMapping.pushAndCopy(frameInSMG1.copyWith(returnOptional, frameMapping));
    }
    // TODO join heap object mapping??

    // step 4 check for any cycle consisting of 0+ DLSs only in result, and if so, the algorithm
    // fails.
    if (resultDLSHaveNewCycles()) {
      status = status.updateWith(SMGJoinStatus.INCOMPARABLE);
      return;
    }

    // step 5
    resultSPC =
        SymbolicProgramConfiguration.of(
            destSMG,
            PathCopyingPersistentTreeMap.copyOf(resultGolbalMapping),
            resultStackMapping,
            PersistentSet.of(),
            PersistentSet.of(),
            PersistentBiMap.of());
  }

  /** Apply joinSubSMG on the two input SMG and the SMGObjects connected to a certain variable. */
  private SMGObject joinVariable(SMGObject obj1, SMGObject obj2) {
    // step 2-1 and 3-1 create fresh region
    SMGObject newObject = obj1.freshCopy();
    destSMG = destSMG.copyAndAddObject(newObject);
    // step 2-3 introduce new mappings
    mapping1.addMapping(obj1, newObject);
    mapping2.addMapping(obj2, newObject);
    // step 3 join on the newly created region
    SMGJoinSubSMGs joinSubSMGs =
        new SMGJoinSubSMGs(
            status, inputSMG1, inputSMG2, destSMG, mapping1, mapping2, obj1, obj2, newObject, 0);

    if (!joinSubSMGs.isDefined() || joinSubSMGs.isRecoverableFailur()) {
      // join failed
      status = status.updateWith(SMGJoinStatus.INCOMPARABLE);
    } else {
      // join successful
      copyJoinState(joinSubSMGs);
    }
    return newObject;
  }

  private void checkVariableRanges() {
    Set<String> spc1Variables = inputSPC1.getGolbalVariableToSmgObjectMap().keySet();
    Set<String> spc2Variables = inputSPC2.getGolbalVariableToSmgObjectMap().keySet();
    checkArgument(spc1Variables.containsAll(spc2Variables), "Variable ranges are not equal.");
  }

  public SymbolicProgramConfiguration getResult() {
    return resultSPC;
  }
}
