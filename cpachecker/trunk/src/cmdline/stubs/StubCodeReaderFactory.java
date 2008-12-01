package cmdline.stubs;

import java.io.IOException;

import org.eclipse.cdt.core.dom.CDOM;
import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.IMacroCollector;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.ICodeReaderCache;

import cmdline.stubs.StubCodeReaderFactory;


public class StubCodeReaderFactory implements ICodeReaderFactory {

    private ICodeReaderCache cache = null;

    public static StubCodeReaderFactory getInstance()
    {
        return instance;
    }

    private static StubCodeReaderFactory instance = new StubCodeReaderFactory();

    private StubCodeReaderFactory()
    {
//        int size=0;
//        size = CodeReaderCache.DEFAULT_CACHE_SIZE_IN_MB;
//        cache = new CodeReaderCache(size);
    }
    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#getUniqueIdentifier()
     */
    public int getUniqueIdentifier() {
        return CDOM.PARSE_SAVED_RESOURCES;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#createCodeReaderForTranslationUnit(java.lang.String)
     */
    public CodeReader createCodeReaderForTranslationUnit(String path) {
//        return cache.get(path);
        try {
            return new CodeReader(path);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public CodeReader createCodeReaderForTranslationUnit(ITranslationUnit tu) {
        return new CodeReader(tu.getResource().getLocation().toOSString(), tu.getContents());
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#createCodeReaderForInclusion(org.eclipse.cdt.core.dom.ICodeReaderFactoryCallback, java.lang.String)
     */
    public CodeReader createCodeReaderForInclusion(IMacroCollector scanner, String path) {
        return cache.get(path);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ICodeReaderFactory#createCodeReaderForInclusion(java.lang.String)
     */
    public ICodeReaderCache getCodeReaderCache() {
        return cache;
    }
    public CodeReader createCodeReaderForInclusion(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
