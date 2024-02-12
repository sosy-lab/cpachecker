// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains all the classes representing a YAML witness format internally in
 * CPAchecker. The classes are used for exporting and validating YAML witnesses. The classes
 * represent the structure of the YAML witness as defined in the format specification.
 *
 * <p>Currently a YAML correctness witness contains a list of sets, each set containing multiple
 * entries and a metadata record. Each entry usually contains some records pertaining to the
 * information it contains.
 *
 * <p>While a YAML violation witness contains a list of violation sequences, each sequence
 * containing multiple segments and a metadata record. Each segment contains a list of waypoints.
 */
package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;
