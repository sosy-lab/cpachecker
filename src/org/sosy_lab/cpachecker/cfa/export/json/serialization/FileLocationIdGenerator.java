// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.FileLocationMixin;

/**
 * A custom generator for generating unique IDs for {@link FileLocation}s.
 *
 * <p>It combines a counter with a fixed prefix to generate the ID.
 *
 * @see CfaJsonExport
 * @see FileLocationMixin
 */
public class FileLocationIdGenerator extends AbstractStringIdGenerator {
  private static final long serialVersionUID = 1296028592781872635L;

  private int counter;

  /* Jackson requires a default constructor for serialization. */
  public FileLocationIdGenerator() {
    this(Object.class);
  }

  /**
   * Constructs a new {@link FileLocationIdGenerator}.
   *
   * <p>It initializes the {@link #counter} to 0.
   *
   * @param pScope The class scope for which the ID generator is created.
   */
  protected FileLocationIdGenerator(Class<?> pScope) {
    super(pScope);
    this.counter = 0;
  }

  /**
   * Creates a new instance of {@link FileLocationIdGenerator} with the specified scope.
   *
   * @param pScope The class scope for the new instance.
   * @return a new instance of {@link FileLocationIdGenerator}.
   */
  @Override
  protected AbstractStringIdGenerator newInstance(Class<?> pScope) {
    return new FileLocationIdGenerator(pScope);
  }

  /**
   * Generates a unique ID for the given object.
   *
   * @param pForObject The object to generate an ID for, must be an instance of {@link
   *     FileLocation}.
   * @return a unique ID string for the given object in the format "FileLocation_<counter>".
   * @throws NullPointerException if the provided object is null.
   * @throws IllegalArgumentException if the provided object is not an instance of {@link
   *     FileLocation}.
   */
  @Override
  public String generateId(Object pForObject) {
    checkNotNull(pForObject, "The object to generate an ID for must not be null");

    if (!(pForObject instanceof FileLocation)) {
      throw new IllegalArgumentException(
          "The object to generate an ID for must be an instance of FileLocation");
    }

    int value = counter;
    counter++;

    return "FileLocation_" + value;
  }
}
