package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface Refiner {
  
  /**
   * Perform refinement, if possible.
   * 
   * @param pReached The reached set.
   * @return Whether the refinement was successful.
   * @throws CPAException If an error occured during refinement.
   */
  public boolean performRefinement(ReachedElements pReached) throws CPAException;

}
