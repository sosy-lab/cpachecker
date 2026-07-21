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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/**
 * Wrapper for the Eclipse CDT parser component. Provides basic functionality to parse C code with
 * Eclipse CDT into an AST.
 *
 * <p>To parse C code to a CFA, use {@link EclipseCParser}.
 */
public class EclipseCdtWrapper {

  /**
   * As a workaround for missing CDT support for _Atomic, we use a macro that replaces _Atomic with
   * an attribute with this name. Cf. #1253
   */
  static final String ATOMIC_ATTRIBUTE = "__CPAchecker_Atomic__";

  // we don't use IASTName#getImageLocation(), so the parser doesn't need to create them
  private static final int PARSER_OPTIONS = ILanguage.OPTION_NO_IMAGE_LOCATIONS;

  private final ILanguage language;
  private final IParserLogService parserLog;
  private final IScannerInfo scannerInfo;

  private final ShutdownNotifier shutdownNotifier;

  public EclipseCdtWrapper(
      final ParserOptions pOptions,
      final MachineModel pMachineModel,
      final ShutdownNotifier pShutdownNotifier) {
    shutdownNotifier = pShutdownNotifier;
    parserLog = new ShutdownNotifierLogAdapter(shutdownNotifier);
    scannerInfo = new StubScannerInfo(pMachineModel);

    language =
        switch (pOptions.getDialect()) {
          case C99 -> new CLanguage(new ANSICParserExtensionConfiguration());
          case GNUC -> GCCLanguage.getDefault();
        };
  }

  static FileContent wrapCode(final Path pFileName, final String pCode) {
    return FileContent.create(
        pFileName.toString(), rewriteAtomicTypeSpecifiers(pCode).toCharArray());
  }

  private static final String ATOMIC_KEYWORD = "_Atomic";

  private enum AtomicTypeName {
    /** A plain type name such as {@code int}, {@code unsigned long}, or {@code struct s}. */
    PLAIN,
    /** A pointer type name whose {@code *}(s) are trailing, e.g. {@code int*} or {@code int **}. */
    TRAILING_POINTER,
    /**
     * A pointer type name with a parenthesized declarator, e.g. {@code int (*)(void)} (pointer to
     * function) or {@code int (*)[3]} (pointer to array).
     */
    PARENTHESIZED_POINTER,
    /** An unsupported type name: an array, function, or otherwise too complex type name. */
    UNSUPPORTED
  }

  /**
   * Rewrite each atomic type specifier {@code _Atomic ( type-name )} into the equivalent {@code
   * _Atomic}-qualifier form, which denotes the same type (cf. C23 § 6.7.3.5 and footnote 147,
   * #1667). CDT cannot parse the specifier form, but the qualifier form is handled via the {@link
   * #ATOMIC_ATTRIBUTE} macro (and, for pointers, via the recovery in {@code
   * ASTTypeConverter#findAtomicPointerOperators}, #1670).
   *
   * <ul>
   *   <li>a plain type name: {@code _Atomic(T)} becomes {@code _Atomic T} by blanking the
   *       parentheses;
   *   <li>a trailing-pointer type name: {@code _Atomic(T*)} becomes {@code T* _Atomic}, so the
   *       {@code _Atomic} applies to the pointer (the distinction is the point of the parentheses:
   *       {@code _Atomic(int) *} is a pointer to an atomic int, while {@code _Atomic(int*)} is an
   *       atomic pointer to int);
   *   <li>a parenthesized-pointer type name (function pointer or pointer to array): {@code
   *       _Atomic(int (*)(void)) f} becomes {@code typedef int (*NAME)(void); _Atomic NAME f} via a
   *       fresh typedef, because the qualifier form cannot otherwise be written without moving the
   *       declarator.
   * </ul>
   *
   * <p>The first two rewrites keep the total length so that all source offsets are preserved; the
   * typedef rewrite does not (but preserves the line structure). Array and function type names are
   * forbidden (C23 § 6.7.3.5 (3)) and left unchanged, as are occurrences inside strings, character
   * constants, or comments.
   */
  private static String rewriteAtomicTypeSpecifiers(final String pCode) {
    if (!pCode.contains(ATOMIC_KEYWORD)) {
      return pCode;
    }
    char[] code = pCode.toCharArray();
    StringBuilder out = new StringBuilder(code.length);
    int typedefCounter = 0;
    int i = 0;
    while (i < code.length) {
      char c = code[i];
      if (c == '"' || c == '\'') {
        int end = skipLiteral(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (c == '/' && i + 1 < code.length && code[i + 1] == '/') {
        int end = skipToLineEnd(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (c == '/' && i + 1 < code.length && code[i + 1] == '*') {
        int end = skipBlockComment(code, i);
        out.append(code, i, end - i);
        i = end;
      } else if (startsAtomicKeyword(code, i)) {
        int open = skipWhitespace(code, i + ATOMIC_KEYWORD.length());
        int close = open < code.length && code[open] == '(' ? matchingParenthesis(code, open) : -1;
        if (close < 0) {
          out.append(ATOMIC_KEYWORD);
          i += ATOMIC_KEYWORD.length();
          continue;
        }
        int start = open + 1;
        switch (classifyTypeName(code, start, close)) {
          case PLAIN ->
              // "_Atomic(T)" -> "_Atomic T " (blank the parentheses)
              out.append(code, i, open - i)
                  .append(' ')
                  .append(code, start, close - start)
                  .append(' ');
          case TRAILING_POINTER -> {
            // "_Atomic(T*)" -> "T* _Atomic" (append the qualifier after the pointer)
            int padding = (close + 1 - i) - (close - start) - ATOMIC_KEYWORD.length();
            out.append(code, start, close - start);
            appendRepeated(out, ' ', padding);
            out.append(ATOMIC_KEYWORD);
          }
          case PARENTHESIZED_POINTER -> {
            // introduce a fresh typedef for the type name and make that typedef atomic
            String name = "__CPAchecker_atomic_type_" + typedefCounter++;
            out.append("typedef ")
                .append(typedefDeclarator(code, start, close, name))
                .append("; ")
                .append(ATOMIC_KEYWORD)
                .append(' ')
                .append(name);
          }
          case UNSUPPORTED -> out.append(code, i, close + 1 - i); // leave unchanged
        }
        i = close + 1;
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static void appendRepeated(final StringBuilder pOut, final char pChar, final int pCount) {
    for (int k = 0; k < pCount; k++) {
      pOut.append(pChar);
    }
  }

  private static AtomicTypeName classifyTypeName(
      final char[] pCode, final int pStart, final int pEnd) {
    if (pStart >= pEnd) {
      return AtomicTypeName.UNSUPPORTED;
    }
    int firstParenthesis = -1;
    int firstBracket = -1;
    boolean hasPointer = false;
    for (int i = pStart; i < pEnd; i++) {
      char c = pCode[i];
      if (c == '(' && firstParenthesis < 0) {
        firstParenthesis = i;
      } else if (c == '[' && firstBracket < 0) {
        firstBracket = i;
      } else if (c == '*') {
        hasPointer = true;
      }
    }
    if (firstParenthesis >= 0 && (firstBracket < 0 || firstParenthesis < firstBracket)) {
      // A parenthesized declarator such as "(*)" in "int (*)(void)"; supported only if these
      // parentheses contain nothing but "*"(s).
      int declaratorEnd = matchingParenthesis(pCode, firstParenthesis);
      if (declaratorEnd < 0 || declaratorEnd >= pEnd) {
        return AtomicTypeName.UNSUPPORTED;
      }
      boolean starsOnly = false;
      for (int i = firstParenthesis + 1; i < declaratorEnd; i++) {
        char c = pCode[i];
        if (c == '*') {
          starsOnly = true;
        } else if (!Character.isWhitespace(c)) {
          return AtomicTypeName.UNSUPPORTED;
        }
      }
      return starsOnly ? AtomicTypeName.PARENTHESIZED_POINTER : AtomicTypeName.UNSUPPORTED;
    }
    if (firstBracket >= 0) {
      return AtomicTypeName.UNSUPPORTED; // array type name
    }
    if (!hasPointer) {
      return AtomicTypeName.PLAIN;
    }
    // A trailing-pointer type name has its "*"(s) at the end; anything else (e.g. the qualified
    // pointer "int * const") is a constraint violation that we do not rewrite.
    int last = pEnd - 1;
    while (last >= pStart && Character.isWhitespace(pCode[last])) {
      last--;
    }
    return pCode[last] == '*' ? AtomicTypeName.TRAILING_POINTER : AtomicTypeName.UNSUPPORTED;
  }

  /**
   * Build the declarator for a typedef of the parenthesized-pointer type name {@code [pStart,
   * pEnd)}, giving it the name {@code pName}. For example {@code int (*)(void)} with name {@code N}
   * yields {@code int (*N)(void)}.
   */
  private static String typedefDeclarator(
      final char[] pCode, final int pStart, final int pEnd, final String pName) {
    int parenthesis = pStart;
    while (pCode[parenthesis] != '(') {
      parenthesis++;
    }
    int declaratorEnd = matchingParenthesis(pCode, parenthesis);
    return new String(pCode, pStart, declaratorEnd - pStart)
        + pName
        + new String(pCode, declaratorEnd, pEnd - declaratorEnd);
  }

  private static boolean startsAtomicKeyword(final char[] pCode, final int pIndex) {
    if (pIndex + ATOMIC_KEYWORD.length() > pCode.length) {
      return false;
    }
    for (int k = 0; k < ATOMIC_KEYWORD.length(); k++) {
      if (pCode[pIndex + k] != ATOMIC_KEYWORD.charAt(k)) {
        return false;
      }
    }
    if (pIndex > 0 && isIdentifierPart(pCode[pIndex - 1])) {
      return false;
    }
    int after = pIndex + ATOMIC_KEYWORD.length();
    return after >= pCode.length || !isIdentifierPart(pCode[after]);
  }

  private static boolean isIdentifierPart(final char pChar) {
    return Character.isLetterOrDigit(pChar) || pChar == '_';
  }

  private static int skipWhitespace(final char[] pCode, final int pIndex) {
    int i = pIndex;
    while (i < pCode.length && Character.isWhitespace(pCode[i])) {
      i++;
    }
    return i;
  }

  private static int skipLiteral(final char[] pCode, final int pIndex) {
    char quote = pCode[pIndex];
    int i = pIndex + 1;
    while (i < pCode.length) {
      if (pCode[i] == '\\') {
        i += 2;
      } else if (pCode[i] == quote) {
        return i + 1;
      } else {
        i++;
      }
    }
    return i;
  }

  private static int skipToLineEnd(final char[] pCode, final int pIndex) {
    int i = pIndex + 2;
    while (i < pCode.length && pCode[i] != '\n') {
      i++;
    }
    return i;
  }

  private static int skipBlockComment(final char[] pCode, final int pIndex) {
    int i = pIndex + 2;
    while (i + 1 < pCode.length && !(pCode[i] == '*' && pCode[i + 1] == '/')) {
      i++;
    }
    return Math.min(i + 2, pCode.length);
  }

  private static int matchingParenthesis(final char[] pCode, final int pOpen) {
    int depth = 0;
    for (int i = pOpen; i < pCode.length; i++) {
      if (pCode[i] == '(') {
        depth++;
      } else if (pCode[i] == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Reads the content of a file located at the given {@code Path} and wraps it into a {@code
   * FileContent} object. The content is read using the system's default character set.
   *
   * @param pFileName The {@code Path} object representing the location of the file to read.
   * @return A new {@code FileContent} object containing the content of the file.
   * @throws IOException If an I/O error occurs reading from the file or a malformed or unmappable
   *     byte sequence is read.
   */
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
          pCode, scannerInfo, FileContentProvider.instance, null, PARSER_OPTIONS, parserLog);
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

    CLanguage(final ICParserExtensionConfiguration pParserConfig) {
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

    private final ImmutableMap<String, String> macros;

    private StubScannerInfo(final MachineModel pMachineModel) {
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

      // Parsing C and computing types can only be done while knowing the sizes of types,
      // and CDT's SizeofCalculator needs these macros for that.
      macrosBuilder.put("__SIZEOF_POINTER__", Integer.toString(pMachineModel.getSizeofPtr()));
      macrosBuilder.put("__SIZEOF_INT__", Integer.toString(pMachineModel.getSizeofInt()));
      macrosBuilder.put("__SIZEOF_LONG__", Integer.toString(pMachineModel.getSizeofLongInt()));
      macrosBuilder.put(
          "__SIZEOF_LONG_LONG__", Integer.toString(pMachineModel.getSizeofLongLongInt()));
      macrosBuilder.put("__SIZEOF_INT128__", Integer.toString(pMachineModel.getSizeofInt128()));
      macrosBuilder.put("__SIZEOF_SHORT__", Integer.toString(pMachineModel.getSizeofShortInt()));
      macrosBuilder.put("__SIZEOF_BOOL__", Integer.toString(pMachineModel.getSizeofBool()));
      macrosBuilder.put("__SIZEOF_FLOAT__", Integer.toString(pMachineModel.getSizeofFloat()));
      macrosBuilder.put("__SIZEOF_DOUBLE__", Integer.toString(pMachineModel.getSizeofDouble()));
      macrosBuilder.put(
          "__SIZEOF_LONG_DOUBLE__", Integer.toString(pMachineModel.getSizeofLongDouble()));

      macrosBuilder.put("_Atomic", "__attribute__((%s))".formatted(ATOMIC_ATTRIBUTE));

      macros = macrosBuilder.buildOrThrow();
    }

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return macros;
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
