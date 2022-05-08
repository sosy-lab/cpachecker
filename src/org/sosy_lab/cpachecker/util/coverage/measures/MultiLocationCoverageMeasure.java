// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

import com.google.common.collect.Multiset;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiLocationCoverageMeasure extends LocationCoverageMeasure {
  private final Multiset<Integer> alternativeCoveredLocations;

  static final String DEFAULT_CONSIDERED_COLOR = "#ff6e6e";

  public MultiLocationCoverageMeasure(
      Multiset<Integer> pCoveredLocations,
      Multiset<Integer> pAlternativeCoveredLocations,
      double pMaxCount) {
    super(pCoveredLocations, pMaxCount);
    alternativeCoveredLocations = pAlternativeCoveredLocations;
  }

  public Set<Integer> getIntersectionLocations() {
    Set<Integer> intersect = new HashSet<>(getCoveredLocations().elementSet());
    intersect.removeAll(alternativeCoveredLocations.elementSet());
    return intersect;
  }

  public Set<Integer> getAlternativeCoveredSet() {
    return alternativeCoveredLocations.elementSet();
  }

  public Map<Integer, Float> getLocationFrequencyValueMap() {
    Map<Integer, Float> frequencyMap = new HashMap<>();
    for (Integer location : alternativeCoveredLocations.elementSet()) {
      frequencyMap.put(
          location,
          alternativeCoveredLocations.count(location) / (float) getMaxLocationFrequencyCount());
    }
    return frequencyMap;
  }

  public Map<Integer, String> getLocationFrequencyColorMap() {
    Map<Integer, String> frequencyColorMap = new HashMap<>();
    for (Integer location : alternativeCoveredLocations.elementSet()) {
      String gradientColor = chooseColorFromGradient(getLocationFrequencyValueMap().get(location));
      frequencyColorMap.put(location, gradientColor);
    }
    return frequencyColorMap;
  }

  @Override
  public String getColor(Integer location) {
    if (getIntersectionLocations().contains(location)) {
      return DEFAULT_CONSIDERED_COLOR;
    } else if (getAlternativeCoveredSet().contains(location)) {
      return getLocationFrequencyColorMap().get(location);
    } else {
      return DEFAULT_LOCATION_COLOR;
    }
  }

  private int getMaxLocationFrequencyCount() {
    int max = 0;
    for (var x : alternativeCoveredLocations.elementSet()) {
      int candidate = alternativeCoveredLocations.count(x);
      if (candidate > max) {
        max = candidate;
      }
    }
    return max;
  }

  private String chooseColorFromGradient(float gradient) {
    Color color1 = new Color(26, 148, 49);
    Color color2 = new Color(152, 251, 152);

    Color rgbGradient = colorGradient(color1, color2, gradient);

    return rgbToHex(rgbGradient);
  }

  private Color colorGradient(Color color1, Color color2, float weight) {
    float inverseWeight = 1 - weight;
    return new Color(
        Math.round(color1.getRed() * weight + color2.getRed() * inverseWeight),
        Math.round(color1.getGreen() * weight + color2.getGreen() * inverseWeight),
        Math.round(color1.getBlue() * weight + color2.getBlue() * inverseWeight));
  }

  private String rgbToHex(Color color) {
    String hex = Integer.toHexString(color.getRGB() & 0xffffff);
    if (hex.length() < 6) {
      hex = "0" + hex;
    }
    return "#" + hex;
  }
}
