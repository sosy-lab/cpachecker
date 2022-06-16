// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.ANSICParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.InternalParserUtil;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;

/**
 * Wrapper for the Eclipse CDT parser component. Provides basic functionality to parse C code with
 * Eclipse CDT into an AST.
 *
 * <p>To parse C code to a CFA, use {@link EclipseCParser}.
 */
public class EclipseCdtWrapper {

  // we don't use IASTName#getImageLocation(), so the parser doesn't need to create them
  private static final int PARSER_OPTIONS = ILanguage.OPTION_NO_IMAGE_LOCATIONS;

  private final ILanguage language;
  private final IParserLogService parserLog;

  private final ShutdownNotifier shutdownNotifier;

  public EclipseCdtWrapper(final ParserOptions pOptions, final ShutdownNotifier pShutdownNotifier) {
    shutdownNotifier = pShutdownNotifier;
    parserLog = new ShutdownNotifierLogAdapter(shutdownNotifier);

    switch (pOptions.getDialect()) {
      case C99:
        language = new CLanguage(new ANSICParserExtensionConfiguration());
        break;
      case GNUC:
        language = GCCLanguage.getDefault();
        break;
      default:
        throw new IllegalArgumentException("Unknown C dialect");
    }
  }

  static FileContent wrapCode(final Path pFileName, final String pCode) {
    return FileContent.create(pFileName.toString(), pCode.toCharArray());
  }

  public static FileContent wrapFile(final Path pFileName) throws IOException {
    final String code = Files.readString(pFileName, Charset.defaultCharset());
    return wrapCode(pFileName, code);
  }

  /**
   * Constructs an AST in CDT format with the Eclipse CDT parser.
   *
   * @param pCode the code to be parsed
   * @return the AST for the provided code
   */
  public IASTTranslationUnit getASTTranslationUnit(final FileContent pCode)
      throws CFAGenerationRuntimeException, CoreException, InterruptedException {
    try {
      return language.getASTTranslationUnit(
          pCode,
          StubScannerInfo.instance,
          FileContentProvider.instance,
          null,
          PARSER_OPTIONS,
          parserLog);
    } finally {
      shutdownNotifier.shutdownIfNecessary();
    }
  }

  /**
   * Private class extending the Eclipse CDT class that is the starting point for using the parser.
   * Supports choice of parser dialect.
   */
  private static class CLanguage extends GCCLanguage {

    private final ICParserExtensionConfiguration parserConfig;

    public CLanguage(final ICParserExtensionConfiguration pParserConfig) {
      parserConfig = pParserConfig;
    }

    @Override
    protected ICParserExtensionConfiguration getParserExtensionConfiguration() {
      return parserConfig;
    }
  }

  /**
   * Private class that tells the Eclipse CDT scanner that no macros and include paths have been
   * defined externally.
   */
  private static class StubScannerInfo implements IScannerInfo {

    private static final ImmutableMap<String, String> MACROS;

    static {
      final ImmutableMap.Builder<String, String> macrosBuilder = ImmutableMap.builder();

      // _Static_assert(cond, msg) feature of C11
      macrosBuilder.put("_Static_assert(c, m)", "");
      // _Noreturn feature of C11
      macrosBuilder.put("_Noreturn", "");

      // These built-ins are defined as macros
      // in org.eclipse.cdt.core.dom.parser.GNUScannerExtensionConfiguration.
      // When the parser encounters their redefinition or
      // some non-trivial usage in the code, we get parsing errors.
      // So we redefine these macros to themselves in order to
      // parse them as functions.
      macrosBuilder.put("__builtin_constant_p", "__builtin_constant_p");
      macrosBuilder.put(
          "__builtin_types_compatible_p(t1,t2)",
          "__builtin_types_compatible_p(({t1 arg1; arg1;}), ({t2 arg2; arg2;}))");
      macrosBuilder.put("__offsetof__", "__offsetof__");
      macrosBuilder.put("__builtin_offsetof(t,f)", "__builtin_offsetof(((t){}).f)");
      macrosBuilder.put("__func__", "\"__func__\"");
      macrosBuilder.put("__FUNCTION__", "\"__FUNCTION__\"");
      macrosBuilder.put("__PRETTY_FUNCTION__", "\"__PRETTY_FUNCTION__\"");

      // For vararg handling there are some interesting macros that we could use available at
      // https://web.archive.org/web/20160801170919/http://research.microsoft.com/en-us/um/redmond/projects/invisible/include/stdarg.h.htm
      // However, without proper support in the analysis, these just make things worse.
      // Cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/711
      // We need size of smallest addressable unit:
      // macrosBuilder.put("_INTSIZEOF(n)", "((sizeof(n) + sizeof(int) - 1) & ~(sizeof(int) - 1))");
      // macrosBuilder.put("__builtin_va_start(ap,v)", "(ap = (va_list)&v + _INTSIZEOF(v))");
      // macrosBuilder.put("__builtin_va_arg(ap,t)", "*(t *)((ap += _INTSIZEOF(t)) -
      // _INTSIZEOF(t))");
      // macrosBuilder.put("__builtin_va_end(ap)", "(ap = (va_list)0)");

      // But for now we just make sure that code with varargs can be parsed
      macrosBuilder.put("__builtin_va_arg(ap,t)", "(t)__builtin_va_arg(ap)");

      // specifying a GCC version >= 4.7 enables handling of 128-bit types in
      // GCCScannerExtensionConfiguration
      macrosBuilder.put("__GNUC__", "4");
      macrosBuilder.put("__GNUC_MINOR__", "7");

      // Our version of CDT does not recognize _Float128 yet:
      // https://gitlab.com/sosy-lab/software/cpachecker/-/issues/471
      macrosBuilder.put("_Float128", "__float128");
      // https://gcc.gnu.org/onlinedocs/gcc/Floating-Types.html
      // https://code.woboq.org/userspace/glibc/bits/floatn-common.h.html
      macrosBuilder.put("_Float32", "float");
      macrosBuilder.put("_Float32x", "double");
      macrosBuilder.put("_Float64", "double");
      macrosBuilder.put("_Float64x", "long double");

      MACROS = macrosBuilder.buildOrThrow();
    }

    private static final IScannerInfo instance = new StubScannerInfo();

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return MACROS;
    }

    @Override
    public String[] getIncludePaths() {
      return new String[0];
    }
  }

  private static class FileContentProvider extends InternalFileContentProvider {

    private static final InternalFileContentProvider instance = new FileContentProvider();

    @Override
    public InternalFileContent getContentForInclusion(
        final String pFilePath, final IMacroDictionary pMacroDictionary) {
      return InternalParserUtil.createExternalFileContent(
          pFilePath, InternalParserUtil.SYSTEM_DEFAULT_ENCODING);
    }

    @Override
    public InternalFileContent getContentForInclusion(
        final IIndexFileLocation pIfl, final String pAstPath) {
      return InternalParserUtil.createFileContent(pIfl);
    }
  }
}
