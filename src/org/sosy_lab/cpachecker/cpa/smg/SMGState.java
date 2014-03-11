/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGConsistencyVerifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.ReadableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.WritableSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

public class SMGState implements AbstractQueryableState, Targetable {
  static boolean targetMemoryErrors = true;
  static boolean unknownOnUndefined = true;

  static private final AtomicInteger id_counter = new AtomicInteger(0);

  private WritableSMG heap;
  private final LogManager logger;
  private final int id;

  private static SMGRuntimeCheck runtimeCheckLevel = SMGRuntimeCheck.NONE;

  //TODO These flags are not enough, they should contain more about the nature of the error.
  private boolean invalidWrite = false;
  private boolean invalidRead = false;
  private boolean invalidFree = false;
  private boolean memoryLeak = false;

  /*
   * If a property is violated by this state, this member is set
   */
  private ViolatedProperty violatedProperty = null;

  private void issueMemoryLeakMessage() {
    issueMemoryError("Memory leak found", false);
  }
  private void issueInvalidReadMessage() {
    issueMemoryError("Invalid read found", true);
  }
  private void issueInvalidWriteMessage() {
    issueMemoryError("Invalid write found", true);
  }
  private void issueInvalidFreeMessage() {
    issueMemoryError("Invalid free found", true);
  }

  private void issueMemoryError(String pMessage, boolean pUndefinedBehavior) {
    if (targetMemoryErrors) {
      logger.log(Level.WARNING, pMessage);
    } else if (pUndefinedBehavior) {
      logger.log(Level.WARNING, pMessage );
      logger.log(Level.WARNING, "Non-target undefined behavior detected. The verification result is unreliable.");
    }
  }

  static public void setTargetMemoryErrors(boolean pV) {
    targetMemoryErrors = pV;
  }

  static public void setUnknownOnUndefined(boolean pV) {
    unknownOnUndefined = pV;
  }

  /**
   * Constructor.
   *
   * Keeps consistency: yes
   *
   * @param pLogger A logger to log any messages
   * @param pMachineModel A machine model for the underlying SMGs
   */
  public SMGState(LogManager pLogger, MachineModel pMachineModel) {
    heap = SMGFactory.createWritableSMG(pMachineModel);
    logger = pLogger;
    id = id_counter.getAndIncrement();
  }

  /**
   * Copy constructor.
   *
   * Keeps consistency: yes
   *
   * @param pOriginalState Original state. Will be the predecessor of the
   * new state
   * @throws SMGInconsistentException
   */
  public SMGState(SMGState pOriginalState) {
    heap = SMGFactory.createWritableCopy(pOriginalState.heap);
    logger = pOriginalState.logger;
    id = id_counter.getAndIncrement();
  }

  public SMGState(LogManager pLogger, WritableSMG pSmg) {
    heap = pSmg;
    logger = pLogger;
    id = id_counter.getAndIncrement();
  }

  /**
   * Sets a level of runtime checks performed.
   *
   * Keeps consistency: yes
   *
   * @param pLevel One of {@link SMGRuntimeCheck.NONE},
   * {@link SMGRuntimeCheck.HALF} or {@link SMGRuntimeCheck.FULL}
   * @throws SMGInconsistentException
   */
  static final public void setRuntimeCheck(SMGRuntimeCheck pLevel) {
    runtimeCheckLevel = pLevel;
  }

  /* ********************************************* */
  /* Non-modifying functions: getters and the like */
  /* ********************************************* */

  /**
   * Constant.
   *
   * @return The ID of this SMGState
   */
  final public int getId() {
    return id;
  }

  /**
   * Based on the current setting of runtime check level, it either performs
   * a full consistency check or not. If the check is performed and the
   * state is deemed inconsistent, a {@link SMGInconsistentException} is thrown.
   *
   * Constant.
   *
   * @param pLevel A level of the check request. When e.g. HALF is passed, it
   * means "perform the check if the setting is HALF or finer.
   * @throws SMGInconsistentException
   */
  final public void performConsistencyCheck(SMGRuntimeCheck pLevel) throws SMGInconsistentException {
    if (SMGState.runtimeCheckLevel.isFinerOrEqualThan(pLevel)) {
      if ( ! CLangSMGConsistencyVerifier.verifyCLangSMG(logger, heap) ) {
        throw new SMGInconsistentException("SMG was found inconsistent during a check");
      }
    }
  }

  /**
   * @return A string representation of the SMGState.
   */
  @Override
  public String toString() {
    return "SMGState [" + getId() + "]\n" + heap.toString();
  }

  public void setInvalidRead() {
    invalidRead  = true;
  }

  /**
   * Marks that an invalid write operation was performed on this smgState.
   *
   */
  public void setInvalidWrite() {
    invalidWrite = true;
  }

  /**
   * Computes the join of this abstract State and the reached abstract State.
   *
   * @param reachedState the abstract state this state will be joined to.
   * @return the join of the two states.
   */
  public SMGState join(SMGState reachedState) {
    // Not necessary if merge_SEP and stop_SEP is used.
    return null;
  }

  /**
   * Computes whether this abstract state is covered by the given abstract state.
   * A state is covered by another state, if the set of concrete states
   * a state represents is a subset of the set of concrete states the other
   * state represents.
   *
   * If this state contains a memory leak and the given does not. The given state
   * does not cover this state even if covering relation (join operation) claim that
   * this state is covered by the other state.
   *
   *
   * @param reachedState already reached state, that may cover this state already.
   * @return True, if this state is covered by the given state, false otherwise.
   * @throws SMGInconsistentException
   */
  public boolean isLessOrEqual(SMGState reachedState) throws SMGInconsistentException {
    SMGJoin join = new SMGJoin(reachedState.heap, heap);
    if (join.isDefined() &&
        (join.getStatus() == SMGJoinStatus.LEFT_ENTAIL || join.getStatus() == SMGJoinStatus.EQUAL)){

      // check memory leaks
      // if reached does NOT contain memory leak and this DOES
      //   this shouldn't be drop
      heap.pruneUnreachable();
      if (heap.hasMemoryLeaks()){
        reachedState.heap.pruneUnreachable();
        if (!reachedState.heap.hasMemoryLeaks()){
          return false;
        }
        // else return true
      }

      return true;
    }
    return false;
  }

  @Override
  public String getCPAName() {
    return "SMGCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    // SMG Properties:
    // has-leaks:boolean

    switch (pProperty) {
      case "has-leaks":
        if (memoryLeak || heap.hasMemoryLeaks()) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_MEMTRACK;
          issueMemoryLeakMessage();
          return true;
        }
        return false;
      case "has-invalid-writes":
        if (invalidWrite) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_DEREF;
          issueInvalidWriteMessage();
          return true;
        }
        return false;
      case "has-invalid-reads":
        if (invalidRead) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_DEREF;
          issueInvalidReadMessage();
          return true;
        }
        return false;
      case "has-invalid-frees":
        if (invalidFree) {
          //TODO: Give more information
          violatedProperty = ViolatedProperty.VALID_FREE;
          issueInvalidFreeMessage();
          return true;
        }
        return false;
      default:
        throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
    }
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return checkProperty(pProperty);
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    // TODO Auto-generated method stub
  }

  public void setMemLeak() {
    memoryLeak = true;
  }

  /**
   *  Signals an invalid free call.
   */
  public void setInvalidFree() {
    invalidFree = true;
  }

  @Nullable
  public MemoryLocation resolveMemLoc(SMGAddress pValue, String pFunctionName) {
    SMGObject object = pValue.getObject();
    long offset = pValue.getOffset().getAsLong();

    if (heap.isGlobalObject(object) || heap.isHeapObject(object)) {
      return MemoryLocation.valueOf(object.getLabel(), offset);
    } else {
      return MemoryLocation.valueOf(pFunctionName, object.getLabel(), offset);
    }
  }

  /**
   * Signals a dereference of a pointer or array
   *  which could not be resolved.
   */
  public void setUnknownDereference() {
    //TODO: This can actually be an invalid read too
    //      The flagging mechanism should be improved

    invalidWrite = true;
  }

  @Override
  public boolean isTarget() {
    return violatedProperty != null;
  }

  @Override
  public ViolatedProperty getViolatedProperty() throws IllegalStateException {
    return violatedProperty;
  }

  public void attemptAbstraction() throws SMGInconsistentException {
    SMGAbstractionManager manager = new SMGAbstractionManager(heap);
    heap = SMGFactory.createWritableCopy(manager.execute());
  }

  public ReadableSMG getSMG() {
    return heap;
  }

  // TODO: This is just a temporary solution for making the refactoring: SMGState should not allow changing of its
  //       inner state in the future.
  public WritableSMG getWritableSMG() {
    return heap;
  }
}
