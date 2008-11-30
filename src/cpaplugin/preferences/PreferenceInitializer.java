package cpaplugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import cpaplugin.PreferencesActivator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = PreferencesActivator.getDefault().getPreferenceStore();
		store.setDefault(cpaplugin.preferences.PreferenceConstants.P_PATH, getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
	}

}
