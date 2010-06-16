package org.sosy_lab.cpachecker.plugin.eclipse.editors.propertieseditor;

import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

public class Property {
	private static final String PREFERENCES_SEPERATOR = "propEditor.seperator";
	private static String seperator = null;
	private static final String PREFERENCES_PREFIX = "propEditor.key.";
	private String key;
	private String value;
	public Property(String pKey, String pValue) {
		super();
		this.key = pKey.trim();
		this.value = pValue.trim();
		if (seperator == null) {
			String tmp = CPAclipse.getPlugin().getPreferenceStore().getString(PREFERENCES_SEPERATOR).trim();
			if (tmp.equals("")) {
				seperator = ";";
			} else {
				seperator = tmp;
			}
		}
	}
	public String getKey() {
		return key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String pValue) {
		this.value = pValue.trim();
	}
	public void setKey(String pKey) {
		this.key = pKey.trim();
	}
	public boolean isComment() {
		return this.key.trim().startsWith("#") || this.key.trim().startsWith("!");
	}
	public String getFileRepresentation() {
		if (this.key.equals("")) {
			return "";
		} else if (isComment()) {
			return key + value;
		} else {
			// property
			if (value == "") {
				return "";
			} else {
				return key + " = " + value;
			}
		}
	}
	
	private boolean isKnownKey() {
		String name = PREFERENCES_PREFIX + this.key;
		return CPAclipse.getPlugin().getPreferenceStore().contains(name);
	}
	public String getDescription() {
		if (! isKnownKey()) {
			return "";
		} else {
			String name = PREFERENCES_PREFIX + this.key + ".description";
			return CPAclipse.getPlugin().getPreferenceStore().getString(name).trim();
			// will also return "" if no description is given
		}
	}
	/** default value is first. a "" indicates, a "free text"
	 * @return
	 */
	public String[] getPossibleValues() {
		if (! isKnownKey()) {
			return new String[0];
		} else {
			String name = PREFERENCES_PREFIX + this.key;
			String valuesStr = CPAclipse.getPlugin().getPreferenceStore().getString(name).trim();
			String[] valuesArray = valuesStr.split(seperator, -1); // include trailing ";;"
			return valuesArray;
		}
	}
	public String getToolTip() {
		if (! isKnownKey()) {
			return "Key unknown";
		} else {
			String tip = "Possible values: \n";
			String[] possibleValues = getPossibleValues();
			for (int i = 0; i < possibleValues.length; i++) {
				if (i == 0) {
					tip = tip + possibleValues[i] + " (default)\n";
				} else {
					tip = tip + possibleValues[i] + "\n";
				}
			}
			return tip;
		}
	}
	
	
}
