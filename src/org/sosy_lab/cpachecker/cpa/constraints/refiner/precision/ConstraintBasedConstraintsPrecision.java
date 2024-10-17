// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintReader;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.ConstraintToTextVisitor;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment.Builder;
import org.sosy_lab.cpachecker.cpa.value.symbolic.util.SymbolicValues;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * ConstraintsPrecision that uses the code constraints represent.
 *
 * <p>Example: A ConstraintBasedConstraintsPrecision contains for location <code>N12</code> the
 * precision <code>{@code 'a > 5'}</code>. The ConstraintsState to adjust at this location is <code>
 * {@code 's1(a) > 5, s3(b) > 5'}</code>. After precision adjustment, the ConstraintsState only
 * consists of <code>{@code 's1(a) > 5'}</code>.
 */
final class ConstraintBasedConstraintsPrecision implements ConstraintsPrecision {

  private static enum ConstraintScope {
    FUNCTION,
    GLOBAL,
    LOCAL,
    NONE
  }

  private static final ConstraintBasedConstraintsPrecision EMPTY =
      new ConstraintBasedConstraintsPrecision();

  private SetMultimap<CFANode, Constraint> trackedLocally;
  private Multimap<String, Constraint> trackedInFunction;
  private Set<Constraint> trackedGlobally;

  public static ConstraintsPrecision getEmptyPrecision() {
    return EMPTY;
  }

  public static ConstraintsPrecision restorePrecisionFromFile(
      final Path pPrecisionFile, final LogManager pLogger, final CFA pCfa) {
    List<String> contents = null;
    try {
      contents = Files.readAllLines(pPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      pLogger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pPrecisionFile);
      return EMPTY;
    }

    Map<Integer, CFANode> idToCfaNode = CFAUtils.getMappingFromNodeIDsToCFANodes(pCfa);
    Builder incrBuilder = Increment.builder();

    CFANode location = null;
    String func = "";
    ConstraintScope currentScope = ConstraintScope.NONE;
    Optional<Constraint> constr;
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()
          || currentLine
              .trim()
              .equals(ConstraintBasedConstraintsPrecision.class.getCanonicalName())) {
        continue;

      } else if (currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":")).trim();
        if (scopeSelectors.equals("*")) {
          currentScope = ConstraintScope.GLOBAL;
        } else {
          Matcher matcher = CFAUtils.CFA_NODE_NAME_PATTERN.matcher(scopeSelectors);
          if (matcher.matches()) {
            currentScope = ConstraintScope.LOCAL;
            location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
          } else {
            currentScope = ConstraintScope.FUNCTION;
            func = scopeSelectors;
          }
        }
      } else {
        constr = ConstraintReader.parseConstraint(currentLine.trim());
        if (constr.isPresent()) {
          switch (currentScope) {
            case GLOBAL -> incrBuilder.globallyTracked(constr.orElseThrow());
            case FUNCTION -> incrBuilder.functionWiseTracked(func, constr.orElseThrow());
            case LOCAL -> incrBuilder.locallyTracked(location, constr.orElseThrow());
            case NONE -> throw new AssertionError("No scope for constraint selected");
          }
        }
      }
    }

    return EMPTY.withIncrement(incrBuilder.build());
  }

  /**
   * Creates a new <code>ConstraintBasedConstraintsPrecision</code> with the given constraints as
   * precision.
   */
  private ConstraintBasedConstraintsPrecision(
      final ConstraintBasedConstraintsPrecision pPrecision) {
    checkNotNull(pPrecision);

    trackedLocally = pPrecision.trackedLocally;
    trackedInFunction = pPrecision.trackedInFunction;
    trackedGlobally = pPrecision.trackedGlobally;
  }

  private ConstraintBasedConstraintsPrecision(
      final SetMultimap<CFANode, Constraint> pTrackedLocally,
      final Multimap<String, Constraint> pTrackedInFunction,
      final Set<Constraint> pTrackedGlobally) {
    trackedLocally = pTrackedLocally;
    trackedInFunction = pTrackedInFunction;
    trackedGlobally = pTrackedGlobally;
  }

  private ConstraintBasedConstraintsPrecision() {
    trackedLocally = HashMultimap.create();
    trackedInFunction = HashMultimap.create();
    trackedGlobally = new HashSet<>();
  }

  /** Returns whether the given <code>Constraint</code> is tracked by this precision. */
  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    for (Constraint c : trackedLocally.get(pLocation)) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    for (Constraint c : trackedInFunction.get(pLocation.getFunctionName())) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    for (Constraint c : trackedGlobally) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Joins this precision with the given one. The join of two precisions is their union.
   *
   * @param pOther the precision to join with this precision
   * @return the join of both precisions
   */
  @Override
  public ConstraintBasedConstraintsPrecision join(final ConstraintsPrecision pOther) {
    assert pOther instanceof ConstraintBasedConstraintsPrecision;

    ConstraintBasedConstraintsPrecision other = (ConstraintBasedConstraintsPrecision) pOther;
    SetMultimap<CFANode, Constraint> joinedLocal = HashMultimap.create(trackedLocally);
    Multimap<String, Constraint> joinedFunctionwise = HashMultimap.create(trackedInFunction);
    Set<Constraint> joinedGlobal = new HashSet<>(trackedGlobally);

    addNewLocalConstraints(joinedLocal, other.trackedLocally);
    addNewFunctionConstraints(joinedFunctionwise, other.trackedInFunction);
    addNewGlobalConstraints(joinedGlobal, other.trackedGlobally);

    return new ConstraintBasedConstraintsPrecision(joinedLocal, joinedFunctionwise, joinedGlobal);
  }

  private void addNewLocalConstraints(
      Multimap<CFANode, Constraint> pMapToAddTo, Multimap<CFANode, Constraint> pNewConstraints) {
    for (Entry<CFANode, Constraint> entry : pNewConstraints.entries()) {
      CFANode loc = entry.getKey();
      Constraint constraint = entry.getValue();

      if (!constraintWithSameMeaningExists(loc, constraint, pMapToAddTo)) {
        pMapToAddTo.put(loc, constraint);
      }
    }
  }

  private void addNewFunctionConstraints(
      Multimap<String, Constraint> pMapToAddTo, Multimap<String, Constraint> pNewConstraints) {
    for (Entry<String, Constraint> entry : pNewConstraints.entries()) {
      String function = entry.getKey();
      Constraint constraint = entry.getValue();

      if (!constraintWithSameMeaningExists(function, constraint, pMapToAddTo)) {
        pMapToAddTo.put(function, constraint);
      }
    }
  }

  private void addNewGlobalConstraints(
      Set<Constraint> pSetToAddTo, Set<Constraint> pNewConstraints) {
    for (Constraint c : pNewConstraints) {
      if (!constraintWithSameMeaningExists(c, pNewConstraints)) {
        pSetToAddTo.add(c);
      }
    }
  }

  private boolean constraintWithSameMeaningExists(
      final CFANode pLoc,
      final Constraint pConstraint,
      final Multimap<CFANode, Constraint> pTrackedConstraints) {

    if (pTrackedConstraints.containsKey(pLoc)) {
      final Collection<Constraint> constraintsOnLocation = pTrackedConstraints.get(pLoc);

      for (Constraint c : constraintsOnLocation) {
        if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean constraintWithSameMeaningExists(
      final String pFunctionName,
      final Constraint pConstraint,
      final Multimap<String, Constraint> pTrackedConstraints) {

    if (pTrackedConstraints.containsKey(pFunctionName)) {
      final Collection<Constraint> constraintsOnLocation = pTrackedConstraints.get(pFunctionName);

      for (Constraint c : constraintsOnLocation) {
        if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean constraintWithSameMeaningExists(
      final Constraint pConstraint, final Set<Constraint> pTrackedConstraints) {

    for (Constraint c : pTrackedConstraints) {
      if (SymbolicValues.representSameCCodeExpression(c, pConstraint)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void serialize(Writer pWriter) throws IOException {

    final ConstraintToTextVisitor toText = new ConstraintToTextVisitor();
    // header
    pWriter.write(this.getClass().getCanonicalName() + "\n\n");

    if (trackedGlobally != null && !trackedGlobally.isEmpty()) {
      pWriter.write(
          "*:\n"
              + Joiner.on("\n")
                  .join(from(trackedGlobally).transform(constraint -> constraint.accept(toText))));
      pWriter.write("\n\n");
    }

    if (trackedInFunction != null) {
      for (String function : trackedInFunction.keySet()) {
        pWriter.write(
            function
                + ":\n"
                + Joiner.on("\n")
                    .join(
                        from(trackedInFunction.get(function))
                            .transform(constraint -> constraint.accept(toText))));
        pWriter.write("\n\n");
      }
    }

    if (trackedLocally != null) {
      for (CFANode loc : trackedLocally.keySet()) {
        pWriter.write(
            loc
                + ":\n"
                + Joiner.on("\n")
                    .join(
                        from(trackedLocally.get(loc))
                            .transform(constraint -> constraint.accept(toText))));
        pWriter.write("\n\n");
      }
    }
  }

  @Override
  public ConstraintBasedConstraintsPrecision withIncrement(final Increment pIncrement) {
    ConstraintBasedConstraintsPrecision newPrecision =
        new ConstraintBasedConstraintsPrecision(this);

    newPrecision.trackedGlobally.addAll(pIncrement.getTrackedGlobally());
    newPrecision.trackedInFunction.putAll(pIncrement.getTrackedInFunction());
    newPrecision.trackedLocally.putAll(pIncrement.getTrackedLocally());

    return newPrecision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConstraintBasedConstraintsPrecision that = (ConstraintBasedConstraintsPrecision) o;

    return Objects.equals(trackedLocally, that.trackedLocally);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trackedLocally);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ConstraintBasedConstraintsPrecision[");

    sb.append("\nLocally tracked: {");
    if (!trackedLocally.keySet().isEmpty()) {
      sb.append("\n");
      // we always want the same node order
      for (CFANode n : ImmutableList.sortedCopyOf(trackedLocally.keySet())) {
        sb.append("\t").append(n).append(" -> ");

        // unfortunately, constraints aren't comparable, so we won't have a deterministic order.
        for (Constraint c : trackedLocally.get(n)) {
          sb.append(c.getRepresentation()).append(", ");
        }

        sb.append("\n");
      }
    }
    sb.append("} -> size: ").append(trackedLocally.size());

    sb.append("\nFunctionwise tracked: {");
    if (!trackedInFunction.keySet().isEmpty()) {
      // we always want the same function order
      List<String> functions = ImmutableList.sortedCopyOf(trackedInFunction.keySet());

      sb.append("\n");
      for (String f : functions) {
        sb.append("\t").append(f).append(" -> ");

        for (Constraint c : trackedInFunction.get(f)) {
          sb.append(c.getRepresentation()).append(", ");
        }

        sb.append("\n");
      }
    }
    sb.append("} -> size: ").append(trackedInFunction.size());

    sb.append("\nGlobally tracked: {");
    for (Constraint c : trackedGlobally) {
      sb.append(c).append(", ");
    }
    sb.append("} -> size: ").append(trackedGlobally.size());

    return sb.toString();
  }
}
