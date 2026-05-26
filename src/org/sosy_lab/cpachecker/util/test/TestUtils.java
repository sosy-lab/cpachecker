// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.truth.TruthJUnit;
import com.google.errorprone.annotations.CheckReturnValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;

/**
 * Various utilities for tests, such as spezialized {@link Configuration} instances.
 *
 * <p>For a logger that is spezialized for tests, use {@link
 * org.sosy_lab.common.log.LogManager#createTestLogManager()}. For a {@link
 * org.sosy_lab.common.ShutdownNotifier} use {@link
 * org.sosy_lab.common.ShutdownNotifier#createDummy()}. For utilities for creating a CFA or parts of
 * it for tests, look at {@link TestCfaUtils}.
 */
public class TestUtils {

  /**
   * Flag for deciding whether extended/expensive tests should be executed.
   *
   * <p>Use <code>ant tests -DenableExtendedTests=true</code> to set this flag to true. The test
   * suite will then generate a much more exhaustive set of input values for the tested methods.
   */
  private static final boolean ENABLE_EXTENDED_TESTS =
      Boolean.parseBoolean(System.getProperty("enableExtendedTests"));

  /**
   * Automatically skips the current test if only the standard tests are desired. This can be called
   * by developers adding tests that are expensive and unlikely to fail when nothing in the
   * respective component changes. Extended can still be executed with <code>
   * ant tests -DenableExtendedTests=true</code>.
   *
   * <p>If a whole test class should be run only when extended tests are disabled, this method can
   * be called from a public static {@link org.junit.BeforeClass} method. Otherwise just call it at
   * the start of the test. {@link #shouldRunExtendedTests()} can be used if the test should not be
   * skipped completely by default but for example use a reduced set of test values.
   */
  public static void skipUnlessExtendedTestsEnabled() {
    TruthJUnit.assume()
        .withMessage(
            "Extended tests are disabled by default, "
                + "use 'ant tests -DenableExtendedTests=true' to enable such tests. "
                + "System property enableExtendedTests was false but")
        .that(ENABLE_EXTENDED_TESTS)
        .isTrue();
  }

  /**
   * Returns true if extended tests are desired (off by default). This can be called by developers
   * adding tests that are expensive and unlikely to fail when nothing in the respective component
   * changes. Extended can still be executed with <code>
   * ant tests -DenableExtendedTests=true</code>.
   *
   * <p>Calling {@link #skipUnlessExtendedTestsEnabled()} is recommended for most cases instead of
   * this method.
   */
  @CheckReturnValue
  public static boolean shouldRunExtendedTests() {
    return ENABLE_EXTENDED_TESTS;
  }

  /**
   * Create a configuration suitable for unit tests (writing output files is disabled).
   *
   * @return A {@link ConfigurationBuilder} which can be further modified and then can be used to
   *     {@link ConfigurationBuilder#build()} a {@link Configuration} object.
   */
  public static ConfigurationBuilder configurationForTest() throws InvalidConfigurationException {
    Configuration typeConverterConfig =
        Configuration.builder().setOption("output.disable", "true").build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(typeConverterConfig);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    return Configuration.builder().addConverter(FileOption.class, fileTypeConverter);
  }
}
