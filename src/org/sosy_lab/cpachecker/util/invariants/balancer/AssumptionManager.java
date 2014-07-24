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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionRelation;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionStack.Frame;


public class AssumptionManager {

  private LogManager logger;
  private Deque<Matrix> currentMatrixQueue;
  private Matrix currentMatrix;
  private AssumptionStack stack;
  private AssumptionSet currentAssumptionSet;

  public AssumptionManager(List<Matrix> ml, LogManager lm) {
    logger = lm;
    makeMatrixQueueCopy(ml);
    stack = new AssumptionStack();
    currentAssumptionSet = new AssumptionSet();
  }

  /*
   * Return the next matrix in the queue, or null if there aren't any more.
   * Also, a pointer to the matrix returned by this method will be stored locally
   * as currentMatrix.
   */
  Matrix nextMatrix() {
    currentMatrix = currentMatrixQueue.pollFirst();
    return currentMatrix;
  }

  void addPossiblyUnnecessaryAssumption(Assumption a) throws BadAssumptionsException {
    AssumptionRelation rel = matchAgainst(a);
    switch (rel) {
    case CONTRADICTS:
      logger.log(Level.ALL, "Tried to add contradictory assumption!",a);
      throw new BadAssumptionsException();
    case WEAKENS:
    case ISSAMEAS:
      logger.log(Level.ALL, "Assumption",a,"already implied by existing set. We leave the set unchanged.");
      return;
    default:
      logger.log(Level.ALL, "Adding new possibly unnecessary assumption, ",a);
      // In this case we must create a new stack frame, and then apply any substitutions resulting from a.
      // The assumption stack will automatically create a FRESH COPY of everything we send it.
      stack.addNewFrame(currentAssumptionSet, allCurrentMatrices(), a);
      // Now that a fresh copy of the assumption set and the matrices as they stand right now have
      // been preserved, we can move on.
      currentAssumptionSet.add(a);
      logger.log(Level.ALL, "Current assumption set is now:\n",currentAssumptionSet);
      zeroSubsCurrent(a);
    }
  }

  private List<Matrix> allCurrentMatrices() {
    List<Matrix> ml = new Vector<>(currentMatrixQueue.size()+1);
    if (currentMatrix != null) {
      ml.add(currentMatrix);
    }
    ml.addAll(currentMatrixQueue);
    return ml;
  }

  void addNecessaryAssumption(Assumption a) throws BadAssumptionsException {
    AssumptionSet as = new AssumptionSet();
    as.add(a);
    addNecessaryAssumptions(as);
  }

  /*
   * Add assumptions that follow necessarily from those we have assumed so far. So there
   * are no options here. Either add, or fail.
   */
  public void addNecessaryAssumptions(AssumptionSet na) throws BadAssumptionsException {
    // Add the assumptions to the current assumption set, and check whether this
    // gave rise to an immediate contradiction.
    logger.log(Level.ALL, "Adding necessary assumptions:",na);
    boolean consistent = currentAssumptionSet.addAll(na);
    if (!consistent) {
      // Then there is a contradiction, so we throw an exception.
      logger.log(Level.ALL, "There was a contradiction!");
      throw new BadAssumptionsException();
    } else {
      // Then there was no immediate contradiction. So we proceed to apply these assumptions
      // as substitutions to both the current matrix, and also all matrices yet to be processed.
      logger.log(Level.ALL, "There was no immediate contradiction.",
          "Assumption set is now:\n",currentAssumptionSet,
          "\nWe now apply substitutions if possible.");
      zeroSubsCurrent(na);
    }
  }

  public AssumptionRelation matchAgainst(Assumption a) {
    return currentAssumptionSet.matchAgainst(a);
  }

  /*
   * Must return false when out of branches.
   */
  public boolean nextBranch() {
    Boolean result = null;
    // This loop must eventually obtain a boolean value for 'result', since we must eventually
    // run out of stack frames.
    while (result == null) {
      try {
        result = nextFrame();
      } catch (BadAssumptionsException e) {}
    }
    return result.booleanValue();
  }

  private boolean nextFrame() throws BadAssumptionsException {
    Frame f = stack.popFrame();
    if (f == null) {
      // We are out of frames, so we are out of options. The matrix balancing operation has failed.
      logger.log(Level.ALL, "The assumption manager has no more stack frames. We are out of options.");
      return false;
    } else {
      // There is a frame to backtrack to. So we set our data to be what was preserved in the frame.
      currentAssumptionSet = f.getAssumptionSet();
      currentMatrixQueue = new ArrayDeque<>(f.getMatrices());
      currentMatrix = null;
      logger.log(Level.ALL, "Restoring assumption set to:\n",currentAssumptionSet);
      logger.log(Level.ALL, "Restoring matrix queue to:\n",currentMatrixQueue);
      // Get the assumption that is to be applied to these data.
      // This is the alternative to the assumption that we made the last time we were at this juncture.
      Assumption a = f.getAssumption();
      logger.log(Level.ALL, "We now apply assumption",a,"whose negation we applied last time we were at this juncture.");
      // Now, if a is going to cause a contradition when added, this is something we really should
      // have realized last time we were at this juncture. Had we realized it then, then we would not
      // have added ~a as a possibly unnecessary assumption, but instead as a necessary one.
      // Still, just in case a causes a contradiction, we have to catch that.
      // In that (unlikely) case, we need to proceed to the /next/ branch after this one, and continue
      // this way until either we run out of frames, or we get one that is noncontradictory.
      // The following method call will throw an exception iff a is contradictory.
      addNecessaryAssumption(a);
      return true;
    }
  }

  /*
   * Make the currentMatrixQueue into a deque containing a COPY of each matrix in the passed collection.
   */
  public void makeMatrixQueueCopy(Collection<Matrix> c) {
    currentMatrixQueue = new ArrayDeque<>();
    for (Matrix m : c) {
      currentMatrixQueue.add(m.copy());
    }
  }

  public AssumptionSet getCurrentAssumptionSet() {
    return currentAssumptionSet;
  }

  public void setCurrentAssumptionSet(AssumptionSet a) {
    currentAssumptionSet = a;
  }

  void zeroSubsCurrent(Assumption a) {
    AssumptionSet as = new AssumptionSet();
    as.add(a);
    zeroSubsCurrent(as);
  }

  /*
   * Make substitutions in the current matrices on the basis of polynomials assumed to be zero.
   */
  public void zeroSubsCurrent(AssumptionSet aset) {
    // Prepare the list of all current matrices.
    List<Matrix> ml = allCurrentMatrices();

    // Compute substitutions based on the assumptions.
    List<Substitution> subs = new Vector<>();
    for (Assumption a : aset) {
      // We can only use assumptions of type ZERO.
      if (a.getAssumptionType() != AssumptionType.ZERO) {
        continue;
      }
      Polynomial num = a.getNumerator();
      Substitution s = num.linearIsolateFirst();
      if (s != null) {
        subs.add(s);
      }
    }
    // If we didn't get any, then quit.
    if (subs.size() == 0) {
      return;
    }
    // So we got one or more linear substitutions.
    // The SubstitutionManager applies them to the matrix intelligently,
    // so that we get the maximum
    // possible simplification. (In particular, we should eliminate as many variables
    // as possible.)
    for (Matrix m : ml) {
      // First make a fresh copy of the substitutions.
      List<Substitution> ss = new Vector<>(subs.size());
      for (Substitution s : subs) {
        ss.add(s.copy());
      }
      // Form a substitution manager.
      SubstitutionManager sman = new SubstitutionManager(ss, logger);
      // Apply the substitutions.
      logger.log(Level.ALL, "Applying substitutions to matrix:","\n"+m.toString());
      sman.applyAll(m); // (The matrix is modified in-place.)
      logger.log(Level.ALL, "Matrix is now:","\n"+m.toString());
    }

  }

}
