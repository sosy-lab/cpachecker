package cpaplugin.cmdline.stubs;

import java.util.Map;

import org.eclipse.cdt.core.dom.IParserConfiguration;
import org.eclipse.cdt.core.parser.IScannerInfo;


public class StubConfiguration implements IParserConfiguration {
    
    class StubScannerInfo implements IScannerInfo {

        @Override
        public Map getDefinedSymbols() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String[] getIncludePaths() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    @Override
    public String getParserDialect() {
        // TODO Auto-generated method stub
        return "C99";
    }

    @Override
    public IScannerInfo getScannerInfo() {
        // TODO Auto-generated method stub
        return new StubScannerInfo();
    }

}
