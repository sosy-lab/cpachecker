package cpaplugin.cmdline.stubs;

import java.util.Map;

import org.eclipse.cdt.core.dom.IParserConfiguration;
import org.eclipse.cdt.core.parser.IScannerInfo;


public class StubConfiguration implements IParserConfiguration {
    
    class StubScannerInfo implements IScannerInfo {

        
        public Map getDefinedSymbols() {
            // TODO Auto-generated method stub
            return null;
        }

        
        public String[] getIncludePaths() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    
    public String getParserDialect() {
        // TODO Auto-generated method stub
        return "C99";
    }

    
    public IScannerInfo getScannerInfo() {
        // TODO Auto-generated method stub
        return new StubScannerInfo();
    }

}
