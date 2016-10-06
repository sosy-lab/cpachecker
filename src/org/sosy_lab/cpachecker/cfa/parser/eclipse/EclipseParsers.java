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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;


/**
 * We load the parser in its own class loader, so both all Eclipse objects
 * and all Eclipse classes can be garbage collected when they are not needed anymore.
 * Without this, nothing could be garbage collected because all the parser objects
 * are referenced statically inside their classes.
 */
public class EclipseParsers {

  @Options(prefix = "cfa")
  public static class EclipseCParserOptions extends ParserOptions {

    @Option(
      secure = true,
      description =
          "Also initialize local variables with default values, or leave them uninitialized."
    )
    private boolean initializeAllVariables = false;

    @Option(
      secure = true,
      description = "Show messages when dead code is encountered during parsing."
    )
    private boolean showDeadCode = true;

    @Option(
      secure = true,
      description = "Allow then/else branches to be swapped in order to obtain simpler conditions."
    )
    private boolean allowBranchSwapping = true;

    @Option(
      secure = true,
      description =
          "simplify pointer expressions like s->f to (*s).f with this option "
              + "the cfa is simplified until at maximum one pointer is allowed for left- and rightHandSide"
    )
    private boolean simplifyPointerExpressions = false;

    @Option(secure = true, description = "simplify simple const expressions like 1+2")
    private boolean simplifyConstExpressions = true;

    public boolean initializeAllVariables() {
      return initializeAllVariables;
    }

    public boolean showDeadCode() {
      return showDeadCode;
    }

    public boolean allowBranchSwapping() {
      return allowBranchSwapping;
    }

    public boolean simplifyPointerExpressions() {
      return simplifyPointerExpressions;
    }

    public boolean simplifyConstExpressions() {
      return simplifyConstExpressions;
    }
  }

  private EclipseParsers() { }

  private static final Pattern OUR_CLASSES = Pattern.compile("^(org\\.eclipse|org\\.sosy_lab\\.cpachecker\\.cfa\\.parser\\.eclipse\\..*\\.*)\\..*");

  private static final String C_PARSER_CLASS    = "org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCParser";
  private static final String JAVA_PARSER_CLASS = "org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser";

  private static WeakReference<ClassLoader> loadedClassLoader = new WeakReference<>(null);

  private static WeakReference<Constructor<? extends CParser>> loadedCParser    = new WeakReference<>(null);
  private static WeakReference<Constructor<? extends Parser>>  loadedJavaParser = new WeakReference<>(null);

  private static final AtomicInteger loadingCount = new AtomicInteger(0);

  private static ClassLoader getClassLoader(LogManager logger) {
    ClassLoader classLoader = loadedClassLoader.get();
    if (classLoader != null) {
      return classLoader;
    }

    // garbage collected or first time we come here
    if (loadingCount.incrementAndGet() > 1) {
      logger.log(Level.INFO, "Repeated loading of Eclipse source parser");
    }

    classLoader = EclipseParsers.class.getClassLoader();
    if (classLoader instanceof URLClassLoader) {
      classLoader =
          Classes.makeExtendedURLClassLoader()
              .setParent(classLoader)
              .setUrls(((URLClassLoader) classLoader).getURLs())
              .setDirectLoadClasses(OUR_CLASSES)
              .build();
    }
    loadedClassLoader = new WeakReference<>(classLoader);
    return classLoader;
  }

  public static CParser getCParser(
      LogManager logger, EclipseCParserOptions options, MachineModel machine) {

    try {
      Constructor<? extends CParser> parserConstructor = loadedCParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends CParser> parserClass = (Class<? extends CParser>) classLoader.loadClass(C_PARSER_CLASS);
        parserConstructor =
            parserClass.getConstructor(
                new Class<?>[] {LogManager.class, EclipseCParserOptions.class, MachineModel.class});
        parserConstructor.setAccessible(true);
        loadedCParser = new WeakReference<>(parserConstructor);
      }

      return parserConstructor.newInstance(logger, options, machine);
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse CDT parser", e);
    }
  }

  public static Parser getJavaParser(LogManager logger, Configuration config) throws InvalidConfigurationException {

    try {
      Constructor<? extends Parser> parserConstructor = loadedJavaParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends CParser> parserClass = (Class<? extends CParser>) classLoader.loadClass(JAVA_PARSER_CLASS);
        parserConstructor = parserClass.getConstructor(new Class<?>[]{ LogManager.class, Configuration.class });
        parserConstructor.setAccessible(true);
        loadedJavaParser = new WeakReference<>(parserConstructor);
      }

      try {
        return parserConstructor.newInstance(logger, config);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof InvalidConfigurationException) {
          throw (InvalidConfigurationException)e.getCause();
        }
        throw e;
      }
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse Java parser", e);
    }
  }
}
