// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser;

import com.google.common.collect.ImmutableSet;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.annotations.SuppressForbidden;
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
 * We load the parser in its own class loader, so both all Eclipse objects and all Eclipse classes
 * can be garbage collected when they are not needed anymore. Without this, nothing could be garbage
 * collected because all the parser objects are referenced statically inside their classes.
 */
@SuppressForbidden("reflection necessary")
public class Parsers {

  @Options(prefix = "cfa")
  public static class EclipseCParserOptions extends ParserOptions {

    @Option(
        secure = true,
        description =
            "Also initialize local variables with default values, or leave them uninitialized.")
    private boolean initializeAllVariables = false;

    @Option(
        secure = true,
        description = "Show messages when dead code is encountered during parsing.")
    private boolean showDeadCode = true;

    @Option(
        secure = true,
        description =
            "simplify pointer expressions like s->f to (*s).f with this option the cfa is"
                + " simplified until at maximum one pointer is allowed for left- and rightHandSide")
    private boolean simplifyPointerExpressions = false;

    @Option(secure = true, description = "simplify simple const expressions like 1+2")
    private boolean simplifyConstExpressions = true;

    @Option(
        secure = true,
        name = "nonReturningFunctions",
        description = "Which functions should be interpreted as never returning to their call site")
    private Set<String> noReturnFunctions = ImmutableSet.of("abort", "exit");

    public boolean initializeAllVariables() {
      return initializeAllVariables;
    }

    public boolean showDeadCode() {
      return showDeadCode;
    }

    public boolean simplifyPointerExpressions() {
      return simplifyPointerExpressions;
    }

    public boolean simplifyConstExpressions() {
      return simplifyConstExpressions;
    }

    /**
     * Returns whether the given function (by name) should be interpreted to never return to its
     * call site.
     */
    public boolean isNonReturningFunction(String functionName) {
      return noReturnFunctions.contains(functionName);
    }
  }

  private Parsers() {}

  private static final Pattern OUR_CLASSES =
      Pattern.compile(
          "^(org\\.eclipse|org\\.sosy_lab\\.cpachecker\\.cfa\\.parser\\.(eclipse\\..*|llvm)\\.*)\\..*");

  private static final String C_PARSER_CLASS =
      "org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCParser";
  private static final String JAVA_PARSER_CLASS =
      "org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser";
  private static final String LLVM_PARSER_CLASS =
      "org.sosy_lab.cpachecker.cfa.parser.llvm.LlvmParser";

  private static WeakReference<ClassLoader> loadedClassLoader = new WeakReference<>(null);

  private static WeakReference<Constructor<? extends CParser>> loadedCParser =
      new WeakReference<>(null);
  private static WeakReference<Constructor<? extends Parser>> loadedJavaParser =
      new WeakReference<>(null);
  private static WeakReference<Constructor<? extends Parser>> loadedLlvmParser =
      new WeakReference<>(null);

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

    classLoader = Parsers.class.getClassLoader();
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
      LogManager logger,
      EclipseCParserOptions options,
      MachineModel machine,
      ShutdownNotifier shutdownNotifier) {

    try {
      Constructor<? extends CParser> parserConstructor = loadedCParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends CParser> parserClass =
            (Class<? extends CParser>) classLoader.loadClass(C_PARSER_CLASS);
        parserConstructor =
            parserClass.getConstructor(
                LogManager.class,
                EclipseCParserOptions.class,
                MachineModel.class,
                ShutdownNotifier.class);
        parserConstructor.setAccessible(true);
        loadedCParser = new WeakReference<>(parserConstructor);
      }

      return parserConstructor.newInstance(logger, options, machine, shutdownNotifier);
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse CDT parser", e);
    }
  }

  public static Parser getJavaParser(LogManager logger, Configuration config, String entryMethod)
      throws InvalidConfigurationException {

    try {
      Constructor<? extends Parser> parserConstructor = loadedJavaParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(logger);

        @SuppressWarnings("unchecked")
        Class<? extends Parser> parserClass =
            (Class<? extends Parser>) classLoader.loadClass(JAVA_PARSER_CLASS);
        parserConstructor =
            parserClass.getConstructor(LogManager.class, Configuration.class, String.class);
        parserConstructor.setAccessible(true);
        loadedJavaParser = new WeakReference<>(parserConstructor);
      }

      try {
        return parserConstructor.newInstance(logger, config, entryMethod);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof InvalidConfigurationException) {
          throw (InvalidConfigurationException) e.getCause();
        }
        throw e;
      }
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create Eclipse Java parser", e);
    }
  }

  public static Parser getLlvmParser(final LogManager pLogger, final MachineModel pMachineModel)
      throws InvalidConfigurationException {
    try {
      Constructor<? extends Parser> parserConstructor = loadedLlvmParser.get();

      if (parserConstructor == null) {
        ClassLoader classLoader = getClassLoader(pLogger);

        @SuppressWarnings("unchecked")
        Class<? extends Parser> parserClass =
            (Class<? extends Parser>) classLoader.loadClass(LLVM_PARSER_CLASS);
        parserConstructor = parserClass.getConstructor(LogManager.class, MachineModel.class);
        parserConstructor.setAccessible(true);
        loadedLlvmParser = new WeakReference<>(parserConstructor);
      }

      try {
        return parserConstructor.newInstance(pLogger, pMachineModel);
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof InvalidConfigurationException) {
          throw (InvalidConfigurationException) e.getCause();
        }
        throw e;
      }
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to create LLVM parser", e);
    }
  }
}
