/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DepPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/*
 * Important Information: This analysis depends on the class org.sosy_lab.cpachecker.cpa.interval. If the Interval class is changed, this analysis can break.
 */

/**
 * QuantitativeInformationFlowState
 *
 * <p>The state stores information about the Entropy, the probability, the intervals and the
 * mappings
 */
public class MultiIntervalState
    implements Serializable,
        LatticeAbstractState<MultiIntervalState>,
        AbstractQueryableState,
        Graphable {

  // ================================================================================
  // Work in Progress:
  // 1. working with dead paths. everything needs to be erased, so they cant influence the outcome
  // -->
  // Done!
  // 2. Contract information in states
  // Done!
  // 3. Support IF
  // Done
  // 4. Support WHILE
  // can analyse While <--Check
  // can count each single WHILE <--Check!
  // Bounded ModelChecking with WHILE <-- Check!
  // innerwhile <-- check
  // tidy up code <-- check
  // WIP combine if and while (atm endless loop) <--- DONE!
  // TODO
  // 5. Support Methods/Pointer/structs...
  // ================================================================================

  // ================================================================================
  // Known Bugs:
  // ================================================================================
  // ----------  if we use nested while with a restriction on loopcount and this count is reached
  // then the analysis will result in an endless loop.

  // ================================================================================
  // enums
  // ================================================================================
  public enum exitStatus {
    IDLE,
    COLLECT_T,
    COLLECT_F,
    EXIT,
    ERROR; // not yet used
  }

  // ================================================================================
  // Vars
  // ================================================================================

  private static final long serialVersionUID = 1L;
  private LogManager logger;
  private StringBuilder s = new StringBuilder();
  MultiIntervalMinMaxMapping minimax;

  // statecounter gname: global number, name: number for the current state set in the constructor.
  // Mostly for bugfinding purpose
  private static int gname = 0;
  private int name;

  // public because this should be easily accessed and changed
  public exitStatus eS = exitStatus.IDLE;

  // Variable for loops
  private boolean loopstart;
  private boolean innerloop;
  private TreeMap<CFANode, Integer> loopc =
      new TreeMap<>(); // counter which counts how often the loopstart has been visited

  // Warning: mostly null
  private CFANode thisNode;

  // the map of minEntropy values. Set on the last state
  HashMap<Variable, Double> hMap;

  /** the intervals of the element */
  private TreeMap<Variable, Range> ranges;

  // the nodedepencies of this state
  private SortedSet<CFANode> dependencies;

  // after assumptions no join should be executed
  private boolean assumeState = false;

  // flag for the last state. A state can be last state even if it has a successor.
  // The last state computation is calculated in strengthen(), but after that there can be a join
  // which resulting state isnt handled by the strengthen method any more.
  // So the lastState flagg has to be transferred to the joined state.
  // We also have to save some additional Informations.
  private boolean lastState = false;

  // ================================================================================
  // configuration
  // ================================================================================

  public int maxLoops = 30;

  // lets keep a minimum of 5 iterations per loop.
  {
    assert (maxLoops >= 5) : "We keeep a minimum of 5 iterations per loop";
  }

  // ================================================================================
  // Constructor
  // ================================================================================

  /** Contstructor of an QuantitativeInformationFlowState */
  public MultiIntervalState(LogManager logger, int maxLoops) {
    this.logger = logger;
    ranges = new TreeMap<>();
    this.maxLoops = maxLoops;
    name = gname;
    gname++;
    // Log.Log("StateCreated");
  }


  // ================================================================================
  // Getter and Setter
  // ================================================================================
  public void setCFANode(CFANode node) {
    this.thisNode = node;
  }

  public CFANode getCFANode() {
    return thisNode;
  }

  public void setDep(SortedSet<CFANode> rContexts) {
    dependencies = rContexts;
  }

  public SortedSet<CFANode> getDep() {
    return dependencies;
  }

  /**
   * Add a single loop
   *
   * @param n Starnode of the Loop
   */
  public void addLoop(CFANode n) {
    if (loopc.containsKey(n)) {
      loopc.put(n, loopc.get(n) + 1);
    } else {
      loopc.put(n, 1);
    }
  }

  /**
   * Leave the loop and remove the Loopnode and counter
   *
   * @param n the loopnode to remove
   */
  public void resetLoop(CFANode n) {
    assert loopc.containsKey(n);
    loopc.remove(n);
  }

  public TreeMap<CFANode, Integer> getLopC() {
    return loopc;
  }

  /**
   * Checks if we have to exit the loop in the next round
   *
   * @return true if yes else false
   */
  public boolean hasToExitNow() {
    for (Entry<CFANode, Integer> ent : loopc.entrySet()) {
      if (ent.getValue() + 1 >= maxLoops) {
        return true;
      }
    }
    return false;
  }
  /**
   * Checks if we have to exit the loop now
   *
   * @return true if yes else false
   */
  public boolean hasToBreakNow() {
    for (Entry<CFANode, Integer> ent : loopc.entrySet()) {
      if (ent.getValue() >= maxLoops) {
        return true;
      }
    }
    return false;
  }

  public void setLoopstart() {
    loopstart = true;

  }

  public int getName() {
    return name;
  }


  public void setInnterloop(boolean inner) {
    innerloop = inner;
  }


  public boolean isInner() {
    return innerloop;

  }

  public boolean isLoopstart() {
    return loopstart;
  }

  public void setAssume() {
    assumeState = true;
  }
  public boolean isAssume() {
    return assumeState;
  }

  public void setLast() {
    lastState = true;
  }

  /**
   * adds another Treemap to the current Range values
   *
   * @param other the other map which will be added
   */
  public void combineMaps(TreeMap<Variable, Range> other) {
    ranges.putAll(other);
  }

  public void addRange(Variable var, Range r) {
    ranges.put(var, r);
  }
  //  public void setMinEntropyMaps(MultiIntervalMinMaxMapping minimax,HashMap<Variable, Double>
  // hMap) {
  //    this.minimax = minimax;
  //    this.hMap  = hMap;
  //  }

  public void removeKey(Variable key) {
    ranges.remove(key);
  }

  public TreeMap<Variable, Range> getRangeMap() {
    return ranges;
  }

  public boolean contains(String str) {
    return contains(new Variable(str));
  }

  /**
   * checks if we have stored already an Interval for the variable v
   *
   * @param v the Variable to check
   * @return true or false
   */
  public boolean contains(Variable v) {
    return ranges.containsKey(v);
  }


  public Range getRange(Variable v) {
    return ranges.get(v);
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public String getCPAName() {
    return "qMultiInterval";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  // ================================================================================
  //  Visualisation
  // ================================================================================
  /**
   * Lets you display some text on the ARG created by CPAchecker. Useful for knowing where we are at
   * certain points and situations in the analysis.
   *
   * @param c the Message we want to display
   */
  public void SendMessage(Object c) {

    s.append(c);
  }

  // ================================================================================
  //  Methods
  // ================================================================================

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("\n");
    for (Entry<Variable, Range> entry : ranges.entrySet()) {
      sb.append("Var:" + entry.getKey() + ",   Range: " + entry.getValue() + "\n");
    }
    sb.append(name + ". TestState like all");

    if (hMap != null) {
      sb.append("\n MinEntropy: \n");
      for (Variable v : hMap.keySet()) {
        sb.append(
            "Min-Entropy for "
                + v
                + " is: "
                + hMap.get(v)
                + " allowed is max "
                + minimax.getMinMinEntMap().get(v)
                + "\n");
      }
    }
    sb.append("\n Loopstart: " + loopstart);
    sb.append("\n innerloop: " + innerloop);
    sb.append("\n LoopNodes" + loopc);
    sb.append("\n this CFA: " + thisNode);
    sb.append("\n Status: " + eS);
    sb.append("\n Dependencies: " + dependencies);
    sb.append("\n Additional:" + s.toString());

    return sb.toString();
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  @SuppressWarnings("unchecked")
  @Override
  public MultiIntervalState join(MultiIntervalState pOther)
      throws CPAException, InterruptedException {
    MultiIntervalState joinState = new MultiIntervalState(logger, maxLoops);

    // if both of the states are in a loop the resulting state is also in a loop (with the same
    // loopcounter, see the last comment in this method)

    // info: maybe take as condition for join the dependencies additionally to the loopcountermap

    // This is a merge with a loopstart, so we have a special merge
    if (isLoopstart() && name < pOther.name) {
      joinState.setLoopstart();
      joinState.loopc = (TreeMap<CFANode, Integer>) pOther.getLopC().clone();
      joinState.combineMaps(pOther.getRangeMap());
      assert getCFANode() != null;
      joinState.setCFANode(pOther.getCFANode());
      joinState.eS = pOther.eS;
      return joinState;

    } else if (pOther.isLoopstart() && name > pOther.name) {
      joinState.setLoopstart();
      joinState.loopc = (TreeMap<CFANode, Integer>) loopc.clone();
      joinState.combineMaps(ranges);
      assert pOther.getCFANode() != null;
      joinState.setCFANode(getCFANode());
      joinState.eS = eS;
      return joinState;
    }

    if (innerloop || pOther.isInner()) {
      joinState.setInnterloop(true);
      if (!loopc.equals(pOther.loopc)) {
        throw new CPATransferException("Non existent Transfer");
      }
    }
    // else we just merge the maps
    joinState.combineMaps(TreeMultimapOperations.easySumm(ranges, pOther.getRangeMap()));

    // this works because if the loopcounter of the two states are different lessOrEqual will be
    // false and the states wont be merged

    joinState.loopc = (TreeMap<CFANode, Integer>) loopc.clone();

    // special case where two last states got merged.
    if (lastState && pOther.lastState) {
      lastState = true;
    }
    return joinState;
    }


  @Override
  public boolean isLessOrEqual(MultiIntervalState pOther)
      throws CPAException, InterruptedException {

    if (pOther == null) {
      return false;
    }

    // No join after an assume edge. Unpredictable behaviour
    if (assumeState || pOther.isAssume()) {
      return false;
    }
    if (!(loopc.equals(pOther.loopc))) {
      return false;
    }
    // forceJoin if one of them is a Loopstart
    if (isLoopstart() || pOther.isLoopstart()) {
      // Log.Log2("--------LoopStart------Return TRUE");
      return true;
    }

    // handle leq in Loops
    if ((innerloop && pOther.isInner())) {
      if (loopc.equals(pOther.loopc)) {
        TreeMap<Variable, Range> tempTM = pOther.getRangeMap();
        Set<Entry<Variable, Range>> th = ranges.entrySet();
        for (Entry<Variable, Range> ent : th) {
          if (!(tempTM.get(ent.getKey()).contains(ent.getValue()))) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    // normal leq
    TreeMap<Variable, Range> tempTM = pOther.getRangeMap();
    Set<Entry<Variable, Range>> th = ranges.entrySet();
    for (Entry<Variable, Range> ent : th) {
      if (!(tempTM.get(ent.getKey()).contains(ent.getValue()))) {
        return false;
      }
    }
    return true;
    }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof MultiIntervalState)) {
      return false;
    }
    return (ranges.equals(((MultiIntervalState) pOther).getRangeMap()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public MultiIntervalState clone() {
    MultiIntervalState clone = new MultiIntervalState(logger, maxLoops);
    clone.combineMaps(cloneTM(ranges));
    clone.setInnterloop(innerloop);


    //    clone.loopcounter = loopcounter;
    clone.loopc = (TreeMap<CFANode, Integer>) loopc.clone();

    //    if (assumeState) {
    //      clone.setAssume();
    //    }

    clone.eS = eS;
    return clone;
  }


  /**
   * @param toClone TreeMultimap to clone
   * @return a shallow copy of the Multimap
   */
  public TreeMap<Variable, Range> cloneTM(TreeMap<Variable, Range> toClone) {
    TreeMap<Variable, Range> temp = new TreeMap<>();
    for (Entry<Variable, Range> entry : toClone.entrySet()) {
      temp.put(entry.getKey(), entry.getValue());
    }
    return temp;
  }

  /**
   * Calculates the Min-Entropy in this state. It uses the Information of the DependencyTracker for
   * the classes ("high" and "low") of the variables. The Min-Entropy is like: "How much Information
   * can I display with all "low" variables?" (e.g var vlow has the Interval [0,7]. It can express 3
   * Bit of information. If vlow is dependent on a "high" variable 3 Bit Entropy are subtracted from
   * the initially entropy of the "high" variable).
   *
   * @param mapping The mapping of the allowed Minimum Min-Entropy values
   * @param depMap the mapping of the dependencies of the individual variables
   * @param imap the mapping of the standard intervals
   * @param prec the mapping of high and low
   */
  public void calculateMinEntropy(
      MultiIntervalMinMaxMapping mapping,
      TreeMap<Variable, SortedSet<Variable>> depMap,
      TreeMap<Variable, IntervalExt> imap,
      DepPrecision prec) {
    // TreeMap<Variable, Double> minE = new TreeMap<>();

    // We need:

    // allowed remaining Entropy of vars         <-- Done! (mapping)
    // dependencies to confidential variables    <-- Done ! (tM)
    // their size                                <-- Done! (size())

    // calculate minEntropy

    // create map for initial Entropy values
    hMap = new HashMap<>();
    minimax = mapping;

    for (Entry<Variable, SortedSet<Variable>> ent : depMap.entrySet()) {
      if (mapping.getMinMinEntMap().containsKey(ent.getKey())) {
        hMap.put(ent.getKey(), Math.log(imap.get(ent.getKey()).size()) / Math.log(2));
      }
    }
    //    Log.Log2(depMap);
    //    Log.Log2(hMap);

    for (Entry<Variable, SortedSet<Variable>> ent : depMap.entrySet()) {
      // the currenty handled variable
      Variable curvar = ent.getKey();

      //      Log.Log2(curvar + " viol " + prec.isViolable(curvar));

      // if curvar is a low Variable then check if it depends on a high variable. If so subtract the
      // bits displayable by the low from the initial Entropy of the high variable
      if (prec.isViolable(curvar)) {
        SortedSet<Variable> deps = ent.getValue();
        deps.remove(curvar);
        if (!deps.isEmpty()) {
          for (Variable high : deps) {
            //            Log.Log2(
            //                "intervals: " + intervals + " deps: " + deps + " high: " + high + "cur
            // " + curvar);
            //            Log.Log2("Intervals.gethigh" + intervals.get(high));

            // is a high variable;)
            if (!prec.isViolable(high)) {
            hMap.put(
                high,
                Math.max(hMap.get(high) - Math.log(ranges.get(curvar).size()) / Math.log(2), 0));
            }
          }
        }
      }
    }
    // Log.Log2(hMap);

    // sendMessage to this state.
  }

  @Override
  public boolean checkProperty(String property) throws InvalidQueryException {

    // Do an Min-EntropyCheck
    if (property.equals("MinEntropyCheck")) {
      // minmapping <-- Map for Min-Entropy
      // return true for Error;
      // Map for comparison

      logger.logf(Level.FINER, "Checking property: %s", property);
      // Log.Log("===============Checking property:" + property + "================");
      TreeMap<Variable, Double> minminEnt = minimax.getMinMinEntMap();

      boolean violation = false;

      for (Variable v : hMap.keySet()) {
        if (minminEnt.containsKey(v) && hMap.get(v) < minminEnt.get(v)) {
          logger.logf(
              Level.FINE,
              "Min-Entropy of %s with  %f is lesser than the given minimum of Min-Entropy %f",
              v,
              hMap.get(v),
              minminEnt.get(v));

          violation = true;
        }
      }

      if (violation) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }


}
