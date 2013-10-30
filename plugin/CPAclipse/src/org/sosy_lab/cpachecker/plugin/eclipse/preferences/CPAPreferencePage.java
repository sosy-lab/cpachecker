/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.plugin.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class CPAPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public CPAPreferencePage() {
		super(GRID);
		setPreferenceStore(CPAclipse.getPlugin().getPreferenceStore());
		setDescription("Preference Page for the CPAcheckerPlugin");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		
		BooleanFieldEditor editor = new BooleanFieldEditor(
				PreferenceConstants.P_STATS, "&Print Statistics",
				getFieldEditorParent());
		addField(editor);

		/*DirectoryFieldEditor dirFieldEd = new DirectoryFieldEditor(
				PreferenceConstants.P_RESULT_DIR,
				"Directory where CPAchecker stores the Task Results (WS-relative)",
				getFieldEditorParent());
		dirFieldEd.setEmptyStringAllowed(false);*/
		/*
		 * fieldEditor = new FileFieldEditor(PreferenceConstants.P_PATH,
		 * "&Directory preference:", getFieldEditorParent()); 
		 * String[] ext =
		 * {"*.properties"}; 
		 * fieldEditor.setFileExtensions(ext);
		 * addField(fieldEditor);
		 */
	}

	@Override
	protected void checkState() {
		super.checkState();
		/*
		 * boolean valid = fieldEditor.getStringValue().endsWith(".properties");
		 * if(!valid) { setErrorMessage("Not a .properties file!");
		 * setValid(false); return; } setErrorMessage(null);
		 */
		setValid(true);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		/*
		 * if(event.getProperty().equals(FieldEditor.VALUE)) { checkState(); }
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}