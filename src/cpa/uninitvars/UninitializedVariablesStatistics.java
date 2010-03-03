package cpa.uninitvars;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import common.Pair;
import common.Triple;

import cpa.common.ReachedElements;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractWrapperElement;
import cpa.common.interfaces.Statistics;

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
      StringBuffer b = new StringBuffer();

      //find all UninitializedVariablesElements and get their warnings
      for (AbstractElement reachedElement : pReached) {
        if (reachedElement instanceof AbstractWrapperElement) {
          UninitializedVariablesElement e = ((AbstractWrapperElement)reachedElement).retrieveWrappedElement(UninitializedVariablesElement.class);
          if (e != null) {
            Set<Triple<Integer, String, String>> s = e.getWarnings();
            //warnings are identified by line number and variable name
            Pair<Integer, String> warningIndex;
            for(Triple<Integer, String, String> t : s) {
              //check if a warning has already been displayed
              warningIndex = new  Pair<Integer, String>(t.getFirst(), t.getSecond());
              if (!warningsDisplayed.contains(warningIndex)) {
                warningsDisplayed.add(warningIndex);
                b.append(t.getThird() + "\n");
              }
            }
          }
        }
      }
      if (b.length() > 1) {
        pOut.println(b.substring(0, b.length() - 1));
      } else {
        pOut.println("No uninitialized variables found");
      }
    } else {
      pOut.println("Output deactivated by configuration option");
    }
  }
}
