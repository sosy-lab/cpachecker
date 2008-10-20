package cpaplugin.preferences;

import java.io.File;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import cpaplugin.PreferencesActivator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class CPAPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	private FileFieldEditor fieldEditor;
	public CPAPreferencePage() {
		super(GRID);
		setPreferenceStore(PreferencesActivator.getDefault().getPreferenceStore());
		setDescription("Preference Page for CPAPlugin");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		fieldEditor = new FileFieldEditor(PreferenceConstants.P_PATH, 
				"&Directory preference:", getFieldEditorParent());
		String[] ext = {"*.properties"};
		fieldEditor.setFileExtensions(ext);
		addField(fieldEditor);
	}
	protected void checkState()
	{
		super.checkState();
		boolean valid = fieldEditor.getStringValue().endsWith(".properties");
		if(!valid)
		{
			setErrorMessage("Not a .properties file!");
			setValid(false);
			return;
		}
		setErrorMessage(null);
		setValid(true);
	}
	public void propertyChange(PropertyChangeEvent event)
	{
		super.propertyChange(event);
		if(event.getProperty().equals(FieldEditor.VALUE))
		{
			checkState();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}