/**
 * @ClassName AbstractScope
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/6/19 8:54 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public abstract class AbstractScope implements Scope {
    private static final String SUFFIX_SEPARATOR = "__";
    protected final String currentFile;

    public AbstractScope(String currentFile) {
        this.currentFile = currentFile;
    }

    @Override
    public abstract boolean isGlobalScope();

    @Override
    public abstract boolean variableNameInUse(String name);

    @Override
    public abstract CSimpleDeclaration lookupVariable(String name);

    @Override
    public abstract CFunctionDeclaration lookupFunction(String name);

    /**
     * Look up {@link CComplexType}s by their name.
     * @param name The fully qualified name (e.g., "struct s").
     * @return The CComplexType instance or null.
     */
    @Override
    public abstract CComplexType lookupType(String name);

    /**
     * Look up {@link CType}s by the names of their typedefs.
     * This is basically needed to correctly search for anonymous complex types e.g.
     * <pre>
     * typedef struct { // The struct gets the tag __anon_type_0
     *    ...
     * } s_type;
     * </pre>
     * @param name typedef type name e.g. s_type
     * @return the type declared in typedef e.g. struct __anon_type_0
     */
    @Override
    public abstract CType lookupTypedef(String name);

    @Override
    public abstract void registerDeclaration(CSimpleDeclaration declaration);

    /**
     * Register a type, e.g., a new struct type.
     *
     * @return True if the type actually needs to be declared, False if the declaration can be omitted because the type is already known.
     */
    @Override
    public abstract boolean registerTypeDeclaration(CComplexTypeDeclaration declaration);

    /**
     * Take a name and return a name qualified with the current function
     * (if we are in a function).
     */
    @Override
    public abstract String createScopedNameOf(String name);

    /**
     * Returns the name for the type as it would be if it is renamed.
     */
    @Override
    public String getFileSpecificTypeName(String type) {
        if (currentFile.isEmpty() || isFileSpecificTypeName(type)) {
            return type;
        } else {
            return type + SUFFIX_SEPARATOR + currentFile;
        }
    }

    @Override
    public boolean isFileSpecificTypeName(String type) {
        return currentFile.isEmpty() || type.endsWith(SUFFIX_SEPARATOR + currentFile);
    }

    /**
     * This method removes the file specific part of a type name if there is one.
     *
     * @param type The type name where the original version should be found.
     * @return The type name without the filename suffix.
     */
    public String removeFileSpecificPartOfTypeName(String type) {
        if (currentFile.isEmpty() || !isFileSpecificTypeName(type)) {
            return type;
        } else {
            return type.replace(SUFFIX_SEPARATOR + currentFile, "");
        }
    }
}
