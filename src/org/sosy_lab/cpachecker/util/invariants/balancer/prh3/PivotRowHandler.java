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
package org.sosy_lab.cpachecker.util.invariants.balancer.prh3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AbstractBalancer;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionRelation;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.BadAssumptionsException;
import org.sosy_lab.cpachecker.util.invariants.balancer.Matrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;
import org.sosy_lab.cpachecker.util.invariants.balancer.SubstitutionManager;


public class PivotRowHandler {

  private final AbstractBalancer mBalancer;
  private final AssumptionManager amgr;
  private final LogManager logger;
  private final Matrix matx;
  private int rowNum, colNum, augStart;

  public PivotRowHandler(Matrix m, AssumptionManager am, AbstractBalancer mb, LogManager lm) {
    mBalancer = mb;
    amgr = am;
    logger = lm;
    matx = m;
  }

  public void solve() throws BadAssumptionsException {
    logger.log(Level.ALL, "Solving pivot rows for matrix:","\n"+matx.toString());
    // Build the initial frame.
    StackFrame F = buildInitialFrame();
    logger.log(Level.ALL, "Building initial frame.");
    // If it has an impossible row, then it is an immediate fail.
    if (F.hasAnImpossibleRow()) {
      logger.log(Level.ALL, "Frame has an impossible row. Aborting.");
      throw new BadAssumptionsException();
    }
    // Else, initialize the stack.
    Stack<StackFrame> stack = new Stack<>();
    logger.log(Level.ALL, "Pushing initial frame onto stack.");
    stack.push(F);
    // Enter main loop.
    while (true) {
      // If the stack is empty, then we are out of options, and it's a fail.
      if (stack.empty()) {
        logger.log(Level.ALL, "Stack empty. Aborting.");
        throw new BadAssumptionsException();
      }
      // Else, investigate the top frame.
      F = stack.peek();
      logger.log(Level.ALL, "Considering top stack frame:\n",F);
      // Does it have any challenges?
      if (!F.hasChallenges()) {
        // If not, then it is time finish up.
        logger.log(Level.ALL, "Frame has no challenges left.");
        // Has the assumption set changed?
        if (!F.aset.equals(amgr.getCurrentAssumptionSet())) {
          // If so, then we check for consistency if its size is at least two.
          if (F.aset.size() >= 2 && !mBalancer.isSatisfiable(F.aset)) {
            // The set was unsatisfiable. Give up on this frame, and return to the top of the loop.
            logger.log(Level.ALL, "The assumption set was unsatisfiable.",
                "We pop the current frame off the stack.");
            stack.pop();
          } else {
            // Set is consistent.
            // Adopt this frame's assumption set, apply any substitutions it yields, and break.
            logger.log(Level.ALL, "Matrix solvable.");
            amgr.setCurrentAssumptionSet(F.aset);
            amgr.zeroSubsCurrent(F.aset);
            break;
          }
        } else {
          logger.log(Level.ALL, "Matrix solvable.");
          logger.log(Level.ALL, "Assumption set is unchanged.");
          break;
        }

      } else {
        // Else there is at least one challenge in F.
        logger.log(Level.ALL, "Frame has one or more challenges to meet.");

        // Loop on possible next moves.
        while (true) {
          // Are we out of moves?
          if (F.nextmv.empty()) {
            // If so, then give up on frame F, and return to the top of the main loop.
            logger.log(Level.ALL, "Frame has no moves left. Popping it off stack.");
            stack.pop();
            break;
          } else {
            // Otherwise, try the next move.
            StackFrame E = F.buildNextFrame();
            if (E != null) {
              // If we got a new frame, push it onto the stack, and return to the top of the main loop.
              logger.log(Level.ALL, "Frame construction succeeded. Pushing frame onto stack.");
              stack.push(E);
              break;
            } else {
              // Construction of new frame didn't work.
              logger.log(Level.ALL, "Frame construction failed.");
            }
          }
        }

      }
    }
  }

  public String printCodes(int[][] codes) {
    String s = "";
    for (int i = 0; i < codes.length; i++) {
      s += "[ ";
      for (int j = 0; j < codes[0].length; j++) {
        if (j > 0) {
          s += " ";
        }
        if (j == augStart) {
          s += "| ";
        }
        String t = Integer.toString(codes[i][j]);
        if (t.length() == 1) {
          s += " ";
        }
        s += t;
      }
      s += " ]\n";
    }
    return s;
  }

  public String printRationalFunctions(RationalFunction[][] rfs) {
    Matrix m = new Matrix(rfs);
    m.setAugStart(augStart);
    return m.toString();
  }

  private class StackFrame {
    AssumptionSet aset;
    RationalFunction[][] entries;
    int[][] codes;
    List<Integer> active;
    DependencyGraph depgr;
    List<Integer> pchal;
    List<Integer> qchal;
    Stack<Move> nextmv;

    String writeActiveStars(List<Integer> act) {
      String s = "";
      for (int j = 0; j < colNum; j++) {
        if (j == 0) {
          s += " ";
        }
        if (j == augStart) {
          s += "  ";
        }
        s += "  ";
        if (act.contains(j)) {
          s += "*";
        } else {
          s += " ";
        }
      }
      return s;
    }

    @Override
    public String toString() {
      String s = "";
      s += "Frame\n";
      s += "=======\n";
      s += "Assumption Set: "+aset.toString()+"\n\n";
      s += "Entries:\n"+printRationalFunctions(entries)+"\n\n";
      s += "Codes and active columns:\n"+printCodes(codes);
      s += writeActiveStars(active)+"\n\n";
      s += "Dependency graph:\n";
      s += depgr.toString()+"\n\n";
      s += "P-challenges: "+pchal.toString()+"\n";
      s += "Q-challenges: "+qchal.toString()+"\n";
      s += "Next Moves:\n";
      s += nextmv.toString();
      return s;
    }

    void deleteRows(List<Integer> rows) {
      // TODO: Implement this.
      // We don't actually need to delete immune rows. It's just an optimization.
    }

    /*
     * Say whether we have any rows in which all the codes are 0, 1, 10, and
     * there is a 1 in an active column. If so, this matrix has no possible solution
     * in nonnegative numbers.
     */
    boolean hasAnImpossibleRow() {
      boolean ans = false;
      for (int i = 0; i < codes.length; i++) {
        boolean all0110 = true;
        boolean hasActive1 = false;
        for (int j = 0; j < codes[i].length; j++) {
          int c = codes[i][j];
          if (c == 1 && active.contains(j)) {
            hasActive1 = true;
          }
          if (c != 0 && c != 1 && c != 10) {
            all0110 = false;
            break;
          }
        }
        if (all0110 && hasActive1) {
          ans = true;
          break;
        }
      }
      return ans;
    }

    boolean hasChallenges() {
      return pchal.size() > 0 || qchal.size() > 0;
    }

    void computeChallenges() {
      // Initialize empty lists of challenges.
      pchal = new Vector<>();
      qchal = new Vector<>();
      // Now go through the rows one by one.
      for (int r = 0; r < codes.length; r++) {
        // If r has an out edge, then it is immune.
        if (depgr.hasForwardEdge(r)) {
          continue;
        }
        // Else we check for active codes that might be positive,
        // i.e. for codes 1, 10, 2, 31 in active columns.
        for (Integer c : active) {
          int code = codes[r][c];
          if (code == 1) {
            // An active 1 yields a P-challenge.
            pchal.add(r);
            break;
          } else if (code == 10 || code == 2 || code == 31) {
            // An active 10, 2, 31 yields a Q-challenge.
            qchal.add(r);
            break;
          }
        }
      }
    }

    void computeNextMoves() {
      // Start with an empty stack of next moves.
      nextmv = new Stack<>();
      // For both P- and Q-challenges, we find moves that work by relying on an active 3.
      List<Integer> PUQ = new Vector<>(pchal);
      PUQ.addAll(qchal);
      for (Integer r : PUQ) {
        // Consider all and only the non-augmentation columns.
        for (int j = 0; j < augStart; j++) {
          int code = codes[r][j];
          // Does the code represent an entry that could potentially be negative?
          if (code == 3 || code == 2 || code == 30 || code == 31) {
            // Then we get a move.
            Move move = new Move();
            // What assumption set does the move use?
            // If the code is 3, then none.
            if (code == 3) {
              move.newaset = null;
            }
            // Else, it uses one assumption, stating that this entry is negative.
            else {
              RationalFunction f = entries[r][j];
              AssumptionType atype = AssumptionType.NEGATIVE;
              Assumption a = new Assumption(f, atype);
              move.newaset = new AssumptionSet();
              move.newaset.add(a);
            }
            // If the column is not yet active, then we say to activate it.
            if (!active.contains(j)) {
              move.newcol = j;
            } else {
              move.newcol = null;
            }
            // Finally, we state that this row should rely on this column.
            move.newedge = new ForwardEdge(r, j);
            // Add the move to the stack.
            nextmv.push(move);
          }
        }
      }

      // For just Q-challenges, we find moves that work by nullifying the challenge.
      for (Integer r : qchal) {
        Move move = new Move();
        move.newaset = new AssumptionSet();
        for (int j = 0; j < colNum; j++) {
          // If column j is not active, then it does not pose a threat.
          if (!active.contains(j)) {
            continue;
          }
          // Otherwise the column is active.
          int code = codes[r][j];
          // If code says that the entry is possibly but not definitely positive, then
          // we add an assumption saying that that entry is not positive.
          if (code == 2 || code == 10 || code == 31) {
            RationalFunction f = entries[r][j];
            AssumptionType atype = null;
            switch (code) {
            case 2:
              atype = AssumptionType.NONPOSITIVE;
              break;
            case 10:
              atype = AssumptionType.ZERO;
              break;
            case 31:
              atype = AssumptionType.NEGATIVE;
              break;
            }
            Assumption a = new Assumption(f, atype);
            move.newaset.add(a);
          }
        }
        // That is all. We do not activate a column, and we do not rely on a column.
        nextmv.push(move);
      }
    }

    /*
     * Try to build the next stack frame. Return null if it fails for any reason.
     */
    StackFrame buildNextFrame() {

      // Do we have a next move?
      if (nextmv.empty()) {
        // If not, then return null.
        return null;
      }

      // Otherwise, we do have a next move. Pop it off the stack.
      Move M = nextmv.pop();
      logger.log(Level.ALL, "Attempting to build the next frame, using move:\n",M);

      // We try to build the next stack frame.
      StackFrame E = new StackFrame();

      // Build the assumption set.
      // Copy the existing one.
      E.aset = new AssumptionSet(this.aset);
      logger.log(Level.ALL, "Copied last frame's assumption set",this.aset);
      // Add any new assumptions from the Move.
      if (M.newaset != null) {
        boolean consistent = E.aset.addAll(M.newaset);
        logger.log(Level.ALL, "Added new assumptions",M.newaset);
        // Is the new assumption set immediately contradictory?
        if (!consistent) {
          // If so, then give up.
          logger.log(Level.ALL, "New assumption set is immediately contadictory. Abort.");
          return null;
        }
        logger.log(Level.ALL, "New assumption set:",E.aset);
      }

      // entries
      // Make a copy of the current entries, and apply any substitutions
      // yielded by the new assumption set.
      SubstitutionManager subman = new SubstitutionManager(E.aset, logger);
      logger.log(Level.ALL, "Copying rational function entries, and applying any substitutions.");
      E.entries = subman.applyAll(this.entries);
      logger.log(Level.ALL, "New entries:","\n"+printRationalFunctions(E.entries));

      // active
      // Copy the old list.
      E.active = new Vector<>(this.active);
      // Add any new column named in the move.
      if (M.newcol != null) {
        E.active.add(M.newcol);
        logger.log(Level.ALL, "Activating column",M.newcol);
      }

      // codes
      E.codes = computeCodes(E.entries, E.aset);
      logger.log(Level.ALL, "New codes and active columns:",
          "\n"+printCodes(E.codes)+"\n"+writeActiveStars(E.active));

      // Check for impossible rows.
      if (E.hasAnImpossibleRow()) {
        logger.log(Level.ALL, "The new frame has an impossible row. Aborting.");
        return null;
      }

      // depgr
      // Create a new dependency graph.
      E.depgr = new DependencyGraph();
      // For each active, non-augmentation column, recompute its outgoing edges.
      for (Integer c : E.active) {
        if (c < augStart) {
          // In this case c is an active, non-augmentation column
          // We add one "backward" edge for each of its entries of codes 1, 10, 2, 31.
          for (int i = 0; i < rowNum; i++) {
            int code = E.codes[i][c];
            if (code == 1 || code == 10 || code == 2 || code == 31) {
              // Is there any assumption we could make to break the edge?
              Assumption a;
              if (code == 1) {
                // There is no way to break this edge.
                a = null;
              } else {
                // Otherwise there is a way.
                RationalFunction f = E.entries[i][c];
                AssumptionType atype = null;
                switch (code) {
                case 10:
                  atype = AssumptionType.ZERO;
                  break;
                case 2:
                  atype = AssumptionType.NONPOSITIVE;
                  break;
                case 31:
                  atype = AssumptionType.NEGATIVE;
                  break;
                }
                a = new Assumption(f, atype);
              }
              // Add the edge.
              E.depgr.addBackEdge(c, i, a);
            }
          }
        }
      }
      // Copy the old "forward" edges.
      Map<Integer, Integer> forward = this.depgr.getForwardEdges();
      for (Integer r : forward.keySet()) {
        E.depgr.addForwardEdge(r, forward.get(r));
      }
      // Add any new reliance specified in the move.
      if (M.newedge != null) {
        int r = M.newedge.row;
        int c = M.newedge.col;
        E.depgr.addForwardEdge(r, c);
      }
      logger.log(Level.ALL, "Computed new dependency graph:\n",E.depgr);

      // Check for cycles in the new dependency graph.
      logger.log(Level.ALL, "Checking for cycles in new dependency graph.");
      List<Assumption> breakers;
      for (int i = 0; i < rowNum; i++) {
        breakers = E.depgr.findRowtoRowLoop(i);
        if (breakers != null) {
          logger.log(Level.ALL, "Found cycle based at row",i);
          // We found a loop.
          // Are there any ways to break it?
          if (breakers.size() == 0) {
            // If not, then abort. We cannot build this frame.
            logger.log(Level.ALL, "Cycle is unbreakable. Aborting frame construction.");
            return null;
          } else {
            // If there is a way to break the loop, then create a new move
            // that will be the same as the current one, plus an assumption that
            // breaks the loop. Push that onto the nextmv stack, and abort the
            // current frame construction.
            // But if none of the possible assumptions is consistent with the
            // assumption set already in the current move, then we must simply
            // give up on this move altogether (so we do not push it back on the stack).
            logger.log(Level.ALL, "Loop can be broken by assumptions:",breakers);
            for (Assumption a : breakers) {
              AssumptionRelation rel = E.aset.matchAgainst(a);
              if (rel != AssumptionRelation.CONTRADICTS) {
                logger.log(Level.ALL, "Assumption",a,"appears consistent with new frame's assumption set.",
                    "We add the assumption to the move, push the move back on the stack, and abort.",
                    "Next construction attempt will use the augmented move.");
                M.newaset.add(a);
                this.nextmv.push(M);
                return null;
              }
            }
            logger.log(Level.ALL, "None of these assumptions is consistent with the new frame's assumption set.",
                "Aborting frame construction.");
            return null;
          }
        }
      }
      logger.log(Level.ALL, "No cycles found.");

      // Compute challenges, and next moves.
      logger.log(Level.ALL, "Computing challenges and next moves for new frame.");
      E.computeChallenges();
      E.computeNextMoves();

      logger.log(Level.ALL, "New frame computation successful.");
      return E;
    }

  }

  private class Move {
    AssumptionSet newaset;
    Integer newcol;
    ForwardEdge newedge;

    @Override
    public String toString() {
      String s = "";
      s += "(";
      if (newaset == null) {
        s += "--";
      } else {
        s += newaset.toString();
      }
      s += ", ";
      if (newcol == null) {
        s += "--";
      } else {
        s += newcol.toString();
      }
      s += ", ";
      if (newedge == null) {
        s += "--";
      } else {
        s += "r"+Integer.toString(newedge.row)+" --> c"+Integer.toString(newedge.col);
      }
      s += ")\n";
      return s;
    }
  }

  private class ForwardEdge {
    // Forward means it points from a row to a column.
    int row;
    int col;

    ForwardEdge(int r, int c) {
      row = r; col = c;
    }
  }

  private StackFrame buildInitialFrame() {
    StackFrame F = new StackFrame();

    // Get a copy of the current assumption set.
    F.aset = new AssumptionSet(amgr.getCurrentAssumptionSet());

    // Extract the entries from the matrix.
    F.entries = extractEntries();

    // Compute the codes.
    F.codes = computeCodes(F.entries, F.aset);

    // Find and delete any "immune" rows.
    List<Integer> immune = findImmuneRows(F.codes);
    F.deleteRows(immune);

    // Set initially active columns.
    F.active = initialActivation(F.codes);
    // Initial dependency graph is always empty.
    F.depgr = new DependencyGraph();
    // Get free columns, and the rows that they serve.
    Map<Integer, List<Integer>> freeCols = findFreeCols(F.codes);
    // Activate these columns, and set the rows they serve to rely on them.
    if (freeCols.keySet().size() > 0) {
      F.active.addAll(freeCols.keySet());
      for (Integer c : freeCols.keySet()) {
        List<Integer> rows = freeCols.get(c);
        for (Integer r : rows) {
          F.depgr.addForwardEdge(r, c);
        }
      }
    }

    // Compute challenges, and next moves.
    F.computeChallenges();
    F.computeNextMoves();

    return F;
  }

  Map<Integer, List<Integer>> findFreeCols(int[][] codes) {
    Map<Integer, List<Integer>> fc = new HashMap<>();
    for (int j = 0; j < augStart; j++) {
      // Is column j filled with only codes 0, 3, 30?
      // If so, note the rows in which it has 3's.
      List<Integer> threes = new Vector<>();
      boolean all0330 = true;
      for (int i = 0; i < rowNum; i++) {
        int c = codes[i][j];
        if (c == 3) {
          threes.add(i);
        }
        if (c != 0 && c != 3 && c != 30) {
          all0330 = false;
          break;
        }
      }
      if (all0330) {
        fc.put(j, threes);
      }
    }
    return fc;
  }

  /*
   * We assume matx is in RREF.
   * Starting from the entries in matx, we
   * (1) chop off zero rows at the bottom, and set rowNum accordingly
   * (2) chop off the identity matrix on the left, and set colNum and augStart accordingly
   * (3) negate all augmentation entries
   */
  private RationalFunction[][] extractEntries() {
    RationalFunction[][] given = matx.getEntries();
    int n = matx.getColNum();
    int a = n - matx.getNumAugCols();
    int p = matx.getNumPivotRows();
    // The submatrix we want is (0 <= i < p) X (p <= j < n).
    RationalFunction[][] entries = new RationalFunction[p][n-p];
    for (int i = 0; i < p; i++) {
      for (int j = p; j < n; j++) {
        if (j < a) {
          entries[i][j-p] = given[i][j];
        } else {
          entries[i][j-p] = RationalFunction.makeNegative(given[i][j]);
        }
      }
    }
    rowNum = p;
    colNum = n-p;
    augStart = a-p;
    return entries;
  }

  /*
   * We represent each entry f of the matrix by a code:
   *  0: f = 0
   *  1: f > 0
   *  2: f is variable
   *  3: f < 0
   * 10: f >= 0
   * 30: f <= 0
   * 31: f <> 0
   * We use the assumption set to make the codes as accurate as we can.
   */
  private int[][] computeCodes(RationalFunction[][] entries, AssumptionSet aset) {
    // How many rows does entries have?
    int m = entries.length;
    // If none, then quit.
    if (m == 0) {
      return new int[0][0];
    }
    // Otherwise entries has at least one row.
    // How many columns?
    int n = entries[0].length;
    // If none, then quit.
    if (n == 0) {
      return new int[0][0];
    }
    // Otherwise there is at least one column.
    int[][] codes = new int[m][n];
    RationalFunction f;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        f = entries[i][j];
        if (f.isConstant()) {
          // First, if f is constant, then we simply check its sign.
          if (f.isZero()) {
            // f is identically zero
            codes[i][j] = 0;
          } else if (f.isPositive()) {
            // f is a positive constant
            codes[i][j] = 1;
          } else {
            // f is a negative constant
            codes[i][j] = 3;
          }
        } else {
          // If f is not constant, then we attempt to constrain its sign using the
          // assumption set.
          AssumptionType at = aset.query(f);
          int[] lookUp = {2, 0, 1, 10, 3, 30, 31, 2};
          int b = at.getCode();
          int c = lookUp[b];
          codes[i][j] = c;
        }
      }
    }
    return codes;
  }

  /*
   * We look for rows in which all codes are among 0, 3, 30.
   * Such a row is "immune", i.e. cannot be challenged by any column.
   */
  private List<Integer> findImmuneRows(int[][] codes) {
    List<Integer> immune = new Vector<>();
    for (int i = 0; i < codes.length; i++) {
      // Is row i immune?
      boolean imm = true;
      for (int j = 0; j < codes[i].length; j++) {
        int c = codes[i][j];
        if (c != 0 && c != 3 && c != 30) {
          // Then this row is /not/ immune.
          imm = false;
          break;
        }
      }
      if (imm) {
        // If imm is still true, then row i is immune.
        immune.add(i);
      }
    }
    return immune;
  }

  private List<Integer> initialActivation(int[][] codes) {
    List<Integer> active = new Vector<>();
    // All augmentation columns are automatically active.
    for (int j = augStart; j < colNum; j++) {
      active.add(j);
    }
    return active;
  }

  private class DependencyGraph {

    // 'forward' maps row indices to column indices.
    // So this will give you the one column that a row points to, if it
    // points to any.
    private Map<Integer, Integer> forward;

    // 'back' takes as argument a column index j.
    // If column j points to rows i1, i2, i3, then the map f that
    // back.get(j) returns will have {i1, i2, i3} as its domain, and
    // for each i, f(i) will be the assumption you could make to break
    // that backward edge, if any, or null if there is no such assumption.
    private Map<Integer, Map<Integer, Assumption>> back;

    DependencyGraph() {
      forward = new HashMap<>();
      back = new HashMap<>();
    }

    @Override
    public String toString() {
      String s = "";
      s += "Forward (rows to columns):\n";
      s += "--------------------------\n";
      for (Integer r : forward.keySet()) {
        Integer c = forward.get(r);
        s += "r"+r.toString()+" --> c"+c.toString()+"\n";
      }
      if (forward.size() == 0) {
        s += "none";
      }
      s += "\n";
      s += "Back (columns to rows):\n";
      s += "-----------------------\n";
      for (Integer c : back.keySet()) {
        //s += "from c"+c.toString()+":\n";
        Map<Integer, Assumption> edges = back.get(c);
        for (Integer r : edges.keySet()) {
          s += "c"+c.toString()+" --> r"+r.toString()+", ";
          Assumption a = edges.get(r);
          if (a == null) {
            s += "unbreakable\n";
          } else {
            s += "broken by "+a.toString()+"\n";
          }
        }
        s += "\n";
      }
      if (back.size() == 0) {
        s += "none";
      }
      return s;
    }

    /*
     * Say whether row r has an out edge.
     */
    boolean hasForwardEdge(Integer r) {
      return forward.containsKey(r);
    }

    Map<Integer, Integer> getForwardEdges() {
      return forward;
    }

    /*
     * Use this method when row row points to column col.
     */
    void addForwardEdge(Integer row, Integer col) {
      forward.put(row, col);
    }

    /* Use this method when:
     * column col has an edge to row row, and the assumption breaker could
     * break this edge (let breaker be null if no assumption could do).
     */
    void addBackEdge(Integer col, Integer row, Assumption breaker) {
      // Get the map that col points to.
      Map<Integer, Assumption> edges;
      if (!back.containsKey(col)) {
        // If none yet, then create it.
        edges = new HashMap<>();
        // And tell col to point to it.
        back.put(col, edges);
      } else {
        // Otherwise retrieve it.
        edges = back.get(col);
      }
      // Add the new edge.
      edges.put(row, breaker);
    }

    /*
     * Convenience method.
     */
    List<Assumption> findRowtoRowLoop(Integer i) {
      return findRowtoRowPath(i, i);
    }

    /*
     * We search for a path (of positive length) from row i to row k.
     * If we find one, we return the list of all assumptions we could make
     * in order to break the path. The list will be of size 0 if there is a
     * path, but there aren't any such assumptions. It will be null if there
     * isn't even a path.
     */
    List<Assumption> findRowtoRowPath(Integer i, Integer k) {
      // Does ri even have an outgoing edge?
      if (!forward.containsKey(i)) {
        // If not, then return null. There is no (positive length) path from ri to rk.
        return null;
      }
      // Otherwise, get the column that ri points to.
      int col = forward.get(i);
      // And retrieve its back edges, and the assumptions that could break them.
      Map<Integer, Assumption> backedges = back.get(col);
      if (backedges == null) {
        // If col's back edges haven't even been initialized, then certainly there is
        // no path through col.
        return null;
      }

      // Prepare the return value.
      List<Assumption> ri2rk = null;

      // If rk is among the rows that col points back to, then we are done.
      if (backedges.containsKey(k)) {
        // So we have found a path of length 2: ri --> col --> rk.
        ri2rk = new Vector<>();
        Assumption a = backedges.get(k);
        // If there is an assumption that could break the edge col --> rk, then we add it
        // to the return value, making it a list of length 1.
        if (a != null) {
          ri2rk.add(a);
        }
        // Else we simply leave the return value a list of length 0.
      }
      // If rk is not among the rows that col points back to, then recurse.
      else {
        // For each row rj that col points back to, see if there is a path from rj to rk.
        List<Assumption> rj2rk = null;
        for (Integer j : backedges.keySet()) {
          rj2rk = findRowtoRowPath(j, k);
          if (rj2rk != null) {
            // We found a path from rj to rk.
            // Do we have an assumption to add, which could break the path from
            // ri to rj?
            Assumption a = backedges.get(j);
            if (a != null) {
              // If so, then add it. Otherwise, leave the return value untouched.
              rj2rk.add(a);
            }
            // In any case, break out of the for-loop, since we have already found a path.
            break;
          }
        }
        // At this point, rj2rk is still null if there was no path from any rj to rk.
        // But if there was such a path, then rj2rk contains all the assumptions that
        // could break the path ri --> col --> rj --> ... --> rk.
        ri2rk = rj2rk;
      }

      return ri2rk;
    }

  }

}
