package cpaplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JOptionPane;


/**
 * CPA Checker properties file. Processes the properties file and save them as strings.
 * If -config is not among program arguments, reads properties file from a default
 * location. If properties are modified via command line arguments, they are processed
 * and related properties keys are modified by this class.
 * @author erkan
 *
 */
public class CPAConfiguration extends Properties{

    private static final long serialVersionUID = -5910186668866464153L;
    private String fileName;
    public boolean validConfig = true;
    /** Delimiters to create string arrays */
    static final String DELIMS = "[;, ]+";

    // TODO use arguments later to change config values dynamically
    /**
     * Class constructor to process arguments and load file.
     * @param args arguments to change values dynamically
     */
    public CPAConfiguration(String[] args) {
        super(new CPAConfiguration());

        // get the file name
        loadFileName(args);
        // load the file
        validConfig = loadFile(this.fileName);
        // if there are some commandline arguments, process them
        if (args != null){
            try {
                processArgs(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //normalizeValues();
    }

    /**
     * Class constructor.
     */
    private CPAConfiguration() {

    }

    /**
     * if -config is specified in arguments, loads this properties file,
     * otherwise loads the file from a default location. Default properties file is
     * $CPACheckerMain/default.properties
     * @param args commandline arguments
     */
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
        int index = binDirString.lastIndexOf("\\")+1;
        if(index < binDirString.lastIndexOf("/")+1)
        {
            index = binDirString.lastIndexOf("/")+1;
            binDirString = binDirString.substring(0, index);
            this.fileName = binDirString + "../default.properties";
        }
        else{
            binDirString = binDirString.substring(0, index);
            this.fileName = binDirString + "..\\default.properties";
        }
    }

    /**
     * Reads the arguments and process them. If a corresponding key is found, the property 
     * is updated
     * @param args commandline arguments
     * @throws Exception if an option is set but no value for the option is found
     */
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
                this.setProperty("log.level", "off");
                ++i;
            } else if (arg.equals("-setprop")) {
                if (i+1 < args.length) {
                    String[] bits = args[i+1].split("=");
                    if (bits.length != 2) {
                        throw new Exception(
                                "-setprop argument must be a key=value pair!");
                    }
                    this.setProperty(bits[0], bits[1]);
                    i += 2;
                } else {
                    throw new Exception("-setprop argument missing!");
                }
            } else if (arg.equals("-help")) {
                System.out.println("OPTIONS:");
                System.out.println(" -logpath");
                System.out.println(" -dotoutpath");
                System.out.println(" -predlistpath");
                System.out.println(" -entryfunction");
                System.out.println(" -dfs");
                System.out.println(" -bfs");
                System.out.println(" -nolog");
                System.out.println(" -setprop");
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

        // arguments with non-specified options are considered as file names
        String programNames = "";
        if(ret.size() > 0){
            programNames = programNames + ret.get(0);
            for (int i = 1; i < ret.size(); i++) {
                programNames = programNames + ", " + ret.get(i);
            }
            this.setProperty("analysis.programNames", programNames);
        }
    }

    /**
     * Load the file as property file see {@link Properties}
     * @param fileName name of the property file
     * @return true if file is loaded successfully
     */
    private boolean loadFile(String fileName) {
        InputStream is = null;
        try {
            // first, try to load from a file
            File f = new File(fileName);
            if (!f.exists()) {
            	String path = cpaplugin.PreferencesActivator.getDefault().getPreferenceStore().getString(cpaplugin.preferences.PreferenceConstants.P_PATH);
            	if(path.endsWith(".properties") == false)
            		return false;
            	f = new File(path);
            }
	            if (f.exists()) {
	                is = new FileInputStream(f);
	            }
	            if (is != null) {
	                load(is);
	                return true;
	            }
        } catch (IOException iex) 
        {
        	
            return false;
        }
        JOptionPane.showMessageDialog(null, "Could not find default.properties, set path in window>preferences>CPAPlugin", "Missing Default Properties", JOptionPane.ERROR_MESSAGE);
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

    
    /**
     * If there are a number of properties for a given key, this method will split them
     * using {@link CPAConfiguration#DELIMS} and return the array of properties
     * @param key the key for the property
     * @return array of properties
     */
    public String[] getPropertiesArray(String key){
        String s = getProperty(key);
        if (s != null) {
            return s.split(DELIMS);
        }
        return null;
    }


    /**
     * A shortcut for properties which has only true, false value
     * @param key the key for the property
     * @return the boolean value of the property, if the key is not present in
     * the properties file false 
     */
    public boolean getBooleanValue(String key){
        String s = getProperty(key);
        if (s == null) {
            return false;
        } else if (s.equals("true")) {
            return true;
        } else {
            assert(s.equals("false"));
            return false;
        }
    }
}

