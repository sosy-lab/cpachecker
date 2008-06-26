package cpaplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class Config extends Properties{

	private static final long serialVersionUID = -5910186668866464153L;
	Object source;
	boolean gotProperties;
	String fileName;

	// TODO use arguments later to determine domains dynamically
	public Config(String[] args, String fileName) {
		super(new Config(args));

		this.fileName = fileName;
		gotProperties = loadFile(args, fileName);

		if (args != null){
			//processArgs(args);
		}
		normalizeValues();
	}

	private Config(String[] args) {
		gotProperties = loadFile(args, "default.properties");
		normalizeValues();
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

	void normalizeValues() {
		for (Enumeration<?> keys = propertyNames(); keys.hasMoreElements();) {
			String k = (String) keys.nextElement();
			String v = getProperty(k);

			// trim heading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
			String v0 = v;
			v = v.trim();
			if (!v.equals(v0)) {
				put(k, v);
			}      

			if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
					|| "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)) {
				put(k, "true");
			} else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
					|| "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)) {
				put(k, "false");
			}
		}
	}
	
	
	
}
