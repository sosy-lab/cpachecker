/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

public abstract class AbstractARTBasedRefiner implements Refiner {

  private final ARTCPA mArtCpa;
  private final LogManager logger;

  protected AbstractARTBasedRefiner(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    if (pCpa instanceof AbstractSingleWrapperCPA) {
      mArtCpa = ((AbstractSingleWrapperCPA) pCpa).retrieveWrappedCpa(ARTCPA.class);
    } else {
      throw new InvalidConfigurationException("ART CPA needed for refinement");
    }
    if (mArtCpa == null) {
      throw new InvalidConfigurationException("ART CPA needed for refinement");
    }
    this.logger = mArtCpa.getLogger();
  }

  protected final ARTCPA getArtCpa() {
    return mArtCpa;
  }

  private static final Function<Pair<ARTElement, CFAEdge>, String> pathToFunctionCalls
        = new Function<Pair<ARTElement, CFAEdge>, String>() {
    @Override
    public String apply(Pair<ARTElement,CFAEdge> arg) {

      if (arg.getSecond() instanceof FunctionCallEdge) {
        FunctionCallEdge funcEdge = (FunctionCallEdge)arg.getSecond();
        return "line " + funcEdge.getLineNumber() + ":\t" + funcEdge.getRawStatement();
      } else {
        return null;
      }
    }
  };

  @Override
  public final boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting ART based refinement");

    assert checkART(pReached);

    AbstractElement lastElement = pReached.getLastElement();
    assert lastElement instanceof ARTElement;
    assert ((ARTElement)lastElement).isTarget();

    Path path = computePath((ARTElement)lastElement, pReached);

    if (logger.wouldBeLogged(Level.ALL) && path != null) {
      logger.log(Level.ALL, "Error path:\n", path);
      logger.log(Level.ALL, "Function calls on Error path:\n",
          Joiner.on("\n ").skipNulls().join(Collections2.transform(path, pathToFunctionCalls)));
    }

    boolean result;
    try {
      result = performRefinement(new ARTReachedSet(pReached, mArtCpa), path);
    } catch (RefinementFailedException e) {
      if (e.getErrorPath() == null) {
        e.setErrorPath(path);
      }

      // set the path from the exception as the target path
      // so it can be used for debugging
      mArtCpa.setTargetPath(e.getErrorPath());
      throw e;
    }

    assert checkART(pReached);

    if (!result) {
      Path targetPath = getTargetPath(path);

      // new targetPath must contain root and error node
      assert targetPath.getFirst().getFirst() == path.getFirst().getFirst();
      assert targetPath.getLast().getFirst()  == path.getLast().getFirst();

      mArtCpa.setTargetPath(targetPath);
    }

    logger.log(Level.FINEST, "ART based refinement finished, result is", result);

    return result;
  }


  /**
   * Perform refinement.
   * @param pReached
   * @param pPath
   * @return whether the refinement was successful
   * @throws InterruptedException
   */
  protected abstract boolean performRefinement(ARTReachedSet pReached, Path pPath)
            throws CPAException, InterruptedException;

  /**
   * This method is intended to be overwritten if the implementation is able to
   * provide a better target path than ARTCPA. This is probably the case when the
   * ART is a DAG and not a tree.
   *
   * This method is called after {@link #performRefinement(ARTReachedSet, Path)}
   * and only if the former method returned false. This method should then return
   * the error path belonging to the latest call to performRefinement.
   *
   * @param pPath The target path.
   * @return A path from the root node to the target node.
   */
  protected Path getTargetPath(Path pPath) {
    return pPath;
  }

  /**
   * This method may be overwritten if the standard behavior of <code>ARTUtils.getOnePathTo()</code> is not
   * appropriate in the implementations context.
   *
   * TODO: Currently this function may return null.
   *
   * @param pLastElement Last ARTElement of the given reached set
   * @param pReached ReachedSet
   * @see org.sosy_lab.cpachecker.cpa.art.ARTUtils
   * @return
   * @throws InterruptedException
   */
  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  private static boolean checkART(ReachedSet pReached) {
    Set<? extends AbstractElement> reached = pReached.getReached();

    Deque<AbstractElement> workList = new ArrayDeque<AbstractElement>();
    Set<ARTElement> art = new HashSet<ARTElement>();

    workList.add(pReached.getFirstElement());
    while (!workList.isEmpty()) {
      ARTElement currentElement = (ARTElement)workList.removeFirst();
      for (ARTElement parent : currentElement.getParents()) {
        assert parent.getChildren().contains(currentElement);
      }
      for (ARTElement child : currentElement.getChildren()) {
        assert child.getParents().contains(currentElement);
      }

      // check if (e \in ART) => (e \in Reached ^ e.isCovered())
      assert reached.contains(currentElement) ^ currentElement.isCovered();

      if (art.add(currentElement)) {
        workList.addAll(currentElement.getChildren());
      }
    }

    // check if (e \in Reached) => (e \in ART)
    assert art.containsAll(reached) : "Element in reached but not in ART";

    return true;
  }
}
