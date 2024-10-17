// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment.Builder;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * {@link ConstraintsPrecision} that determines whether a {@link Constraint} is tracked based on the
 * memory locations occurring in it.
 */
final class LocationBasedConstraintsPrecision implements ConstraintsPrecision {

  private static final LocationBasedConstraintsPrecision EMPTY =
      new LocationBasedConstraintsPrecision();

  private Set<CFANode> trackedLocations;

  private LocationBasedConstraintsPrecision() {
    trackedLocations = new HashSet<>();
  }

  private LocationBasedConstraintsPrecision(final LocationBasedConstraintsPrecision pOther) {
    trackedLocations = pOther.trackedLocations;
  }

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

    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()
          || currentLine
              .trim()
              .equals(LocationBasedConstraintsPrecision.class.getCanonicalName())) {
        continue;

      } else if (currentLine.endsWith(":")) {
        String scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFAUtils.CFA_NODE_NAME_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          incrBuilder.locallyTracked(
              idToCfaNode.get(Integer.parseInt(matcher.group(1))),
              (Constraint) null); // we only need the node
        }
      }
    }

    return EMPTY.withIncrement(incrBuilder.build());
  }

  @Override
  public boolean isTracked(final Constraint pConstraint, final CFANode pLocation) {
    return trackedLocations.contains(pLocation);
  }

  @Override
  public LocationBasedConstraintsPrecision join(final ConstraintsPrecision pOther) {
    assert pOther instanceof LocationBasedConstraintsPrecision;

    LocationBasedConstraintsPrecision that = (LocationBasedConstraintsPrecision) pOther;

    LocationBasedConstraintsPrecision newPrec = new LocationBasedConstraintsPrecision(this);
    newPrec.trackedLocations.addAll(that.trackedLocations);

    return newPrec;
  }

  @Override
  public void serialize(final Writer pWriter) throws IOException {
    // header
    pWriter.write(this.getClass().getCanonicalName() + "\n\n");

    if (trackedLocations != null) {
      for (CFANode loc : trackedLocations) {
        pWriter.write(loc + ":\n");
      }
    }
  }

  @Override
  public LocationBasedConstraintsPrecision withIncrement(final Increment pIncrement) {
    assert pIncrement.getTrackedGlobally().isEmpty();
    assert pIncrement.getTrackedInFunction().isEmpty();

    LocationBasedConstraintsPrecision newPrec = new LocationBasedConstraintsPrecision(this);

    newPrec.trackedLocations.addAll(pIncrement.getTrackedLocally().keySet());

    return newPrec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LocationBasedConstraintsPrecision that = (LocationBasedConstraintsPrecision) o;

    return trackedLocations.equals(that.trackedLocations);
  }

  @Override
  public int hashCode() {
    return trackedLocations.hashCode();
  }

  @Override
  public String toString() {
    return "LocationBasedConstraintsPrecision{" + trackedLocations + "}";
  }
}
