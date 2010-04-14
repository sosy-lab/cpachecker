package cpa.uninitvars;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import common.Pair;
import common.Triple;

import cpa.common.ReachedElements;
import cpa.common.CPAcheckerResult.Result;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractWrapperElement;
import cpa.common.interfaces.Statistics;

/**
 * @author Gregor Endler
 * 
 * Statistics for UninitializedVariablesCPA.
 * Displays warnings about all uninitialized variables found. 
 */
public class UninitializedVariablesStatistics implements Statistics {

  private boolean printWarnings;

  public UninitializedVariablesStatistics(String printWarnings) {
    super();
    this.printWarnings = Boolean.parseBoolean(printWarnings);
  }

  @Override
  public String getName() {
    return "UninitializedVariablesCPA";
  }

  @Override
  public void printStatistics(PrintWriter pOut, Result pResult, ReachedElements pReached) {

    if (printWarnings) {

      Set<Pair<Integer, String>> warningsDisplayed = new HashSet<Pair<Integer, String>>();
      StringBuffer buffer = new StringBuffer();

      //find all UninitializedVariablesElements and get their warnings
      for (AbstractElement reachedElement : pReached) {
        if (reachedElement instanceof AbstractWrapperElement) {
          UninitializedVariablesElement uninitElement = 
            ((AbstractWrapperElement)reachedElement).retrieveWrappedElement(UninitializedVariablesElement.class);
          if (uninitElement != null) {
            Set<Triple<Integer, String, String>> warnings = uninitElement.getWarnings();
            //warnings are identified by line number and variable name
            Pair<Integer, String> warningIndex;
            for(Triple<Integer, String, String> warning : warnings) {
              //check if a warning has already been displayed
              warningIndex = new  Pair<Integer, String>(warning.getFirst(), warning.getSecond());
              if (!warningsDisplayed.contains(warningIndex)) {
                warningsDisplayed.add(warningIndex);
                buffer.append(warning.getThird() + "\n");
              }
            }
          }
        }
      }
      if (buffer.length() > 1) {
        pOut.println(buffer.substring(0, buffer.length() - 1));
      } else {
        pOut.println("No uninitialized variables found");
      }
    } else {
      pOut.println("Output deactivated by configuration option");
    }
  }
}
