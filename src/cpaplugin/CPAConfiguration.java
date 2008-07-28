package cpaplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;

public class CPAConfiguration extends Properties{

    private static final long serialVersionUID = -5910186668866464153L;
    Object source;
    String fileName;

    // Delimiters to create string arrays
    static final String DELIMS = "[:;, ]+";

    // TODO use arguments later to change config values dynamically
    public CPAConfiguration(String[] args) {
        super(new CPAConfiguration());

        loadFileName(args);
        loadFile(args, this.fileName);

        if (args != null){
            try {
                processArgs(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //normalizeValues();
    }

    private CPAConfiguration() {

    }

    private void loadFileName(String[] args){
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(arg.equals("-config")){
                this.fileName = args[i+1];
                return;
            }
        }
	URL binDir = getClass().getProtectionDomain().getCodeSource().getLocation();
	String binDirString = binDir.getPath();
	this.fileName = binDirString + "../default.properties";
    }

    private void processArgs(String[] args) throws Exception {
        Vector<String> ret = new Vector<String>();

        for (int i = 0; i < args.length;) {
            String arg = args[i];
            if (arg.equals("-logpath")) {
                if (i+1 < args.length) {
                    this.setProperty("log.path", args[i+1]);
                    i += 2;
                } else {
                    throw new Exception("-logpath argument missing!");
                }
            } else if (arg.equals("-dotoutpath")) {
                if (i+1 < args.length) {
                    this.setProperty("dot.path", args[i+1]);
                    i += 2;
                } else {
                    throw new Exception("-dotoutpath argument missing!");
                }
            } else if (arg.equals("-predlistpath")) {
                if (i+1 < args.length) {
                    this.setProperty("predicates.path", args[i+1]);
                    i += 2;
                } else {
                    throw new Exception("-predlistpath argument missing!");
                }
            } else if (arg.equals("-entryfunction")) {
                if (i+1 < args.length) {
                    this.setProperty("analysis.entryFunction", args[i+1]);
                    i += 2;
                } else {
                    throw new Exception("-entryfunction argument missing!");
                }
            } else if (arg.equals("-dfs")) {
                this.setProperty("analysis.bfs", "false");
                ++i;
            } else if (arg.equals("-bfs")) {
                this.setProperty("analysis.bfs", "true");
                ++i;
            } else if (arg.equals("-nolog")) {
                this.setProperty("log.level", "false");
                ++i;
            } else if (arg.equals("-help")) {
                System.out.println("OPTIONS:");
                System.out.println(" -logpath");
                System.out.println(" -dotoutpath");
                System.out.println(" -predlistpath");
                System.out.println(" -entryfunction");
                System.out.println(" -dfs");
                System.out.println(" -bfs");
                System.out.println(" -nolog");
                System.out.println(" -help");
                System.exit(0);
            } else if (arg.equals("-config")) {
                // this has been processed earlier, in loadFileName
                i += 2;
            } else {
                ret.add(arg);
                ++i;
            }
        }

        String programNames = "";
        if(ret.size() > 0){
            programNames = programNames + ret.get(0);
            for (int i = 1; i < ret.size(); i++) {
                programNames = programNames + ", " + ret.get(i);
            }
            this.setProperty("analysis.programNames", programNames);
        }
    }

    boolean loadFile(String[] args, String fileName) {
        InputStream is = null;

        try {
            // first, try to load from a file
            File f = new File(fileName);
            if (!f.exists()) {}

            if (f.exists()) {
                source = f;
                is = new FileInputStream(f);
            }

            if (is != null) {
                load(is);
                return true;
            }
        } catch (IOException iex) {
            return false;
        }
        return false;
    }

    // TODO implement this when you get really bored
    //	void normalizeValues() {
    //	for (Enumeration<?> keys = propertyNames(); keys.hasMoreElements();) {
    //	String k = (String) keys.nextElement();
    //	String v = getProperty(k);

    //	// trim heading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
    //	String v0 = v;
    //	v = v.trim();
    //	if (!v.equals(v0)) {
    //	put(k, v);
    //	}      

    //	if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
    //	|| "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)) {
    //	put(k, "true");
    //	} else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
    //	|| "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)) {
    //	put(k, "false");
    //	}
    //	}
    //	}

    // to get arrays
    public String[] getPropertiesArray(String key){
        String s = getProperty(key);
        if (s != null) {
            return s.split(DELIMS);
        }
        return null;
    }

    // get boolean value for properties with only true, false value
    public boolean getBooleanValue(String key){
        String s = getProperty(key);
        if(s.equals("true")){
            return true;
        }
        else{
            assert(s.equals("false"));
            return false;
        }
    }
}
