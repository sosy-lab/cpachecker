// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

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
