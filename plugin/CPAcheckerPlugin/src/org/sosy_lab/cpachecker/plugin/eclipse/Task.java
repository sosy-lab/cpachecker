/**
 * 
 */
package org.sosy_lab.cpachecker.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public class Task {
	private static final String DEFAULT_OUTPUT_DIR = "test/output/";
	private String name;
	private ITranslationUnit sourceTranslationUnit = null;
	private IFile configFile = null;
	//private Configuration config = null;
	private boolean isDirty = true;
	private Result lastResult = Result.UNKNOWN;

	/**
	 * One of the parameters configFile and source may be null.
	 * 
	 * @param taskName
	 * @param configFile
	 * @param source
	 */
	public Task(String taskName, IFile configFile, ITranslationUnit source) {
		this.name = createUniqueName(taskName);
		this.configFile = configFile;
		this.sourceTranslationUnit = source;
	}

	public void setLastResult(Result result) {
		this.lastResult = result;
	}

	public Result getLastResult() {
		return this.lastResult;
	}

	public Task(String taskName, IFile configFile) {
		this.name = createUniqueName(taskName);
		this.configFile = configFile;
	}

	public Task(String taskName, ITranslationUnit source) {
		this.name = createUniqueName(taskName);
		this.sourceTranslationUnit = source;
	}

	private static String createUniqueName(String preferredName) {
		List<Task> tasks = CPAclipse.getPlugin().getTasks();
		Set<String> takenNames = new HashSet<String>();
		for (Task t : tasks) {
			takenNames.add(t.getName());
		}
		if (takenNames.contains(preferredName)) {
			int i = 1;
			while (true) {
				if (takenNames.contains(preferredName + " (" + i + ")")) {
					i++;
				} else {
					return preferredName + " (" + i + ")";
				}
			}
		} else {
			return preferredName;
		}
	}
	
	public Configuration createConfig() throws IOException, CoreException {
			Configuration config = new Configuration(configFile.getContents(),
					Collections.<String, String> emptyMap());
			String projectRoot = this.configFile.getProject().getLocation()
					.toPortableString() + "/";
			String property = config.getProperty("automatonAnalysis.inputFile");
			// TODO: handle multiple automaton files
			if (property != null) {
				File file = new File(property);
				if (!file.isAbsolute()) {
					config.setProperty("automatonAnalysis.inputFile",
							projectRoot + property);
				}
			}
			config.setProperty("output.path",  
					this.getOutputDirectory(false).getLocation().toPortableString());
		return config;
	}

	public String getName() {
		return name;
	}

	public IFile getConfigFile() {
		return this.configFile;
	}

	public String getConfigFilePathProjRelative() {
		return configFile.getProjectRelativePath().toPortableString();
	}

	/**
	 * PreRunError == an error that does not allow the task to be run.
	 * 
	 * @return
	 */
	public boolean hasPreRunError() {
		try {
			if (configFile == null || createConfig() == null) {
				return true;
			} else if (this.getTranslationUnit() == null) {
				return true;
			}
		} catch (IOException e) {
			return true;
		} catch (CoreException e) {
			return true;
		}
		return false;
	}

	public String getErrorMessage() {
		if (this.configFile == null) {
			return "No configuration file was associated with this task!";
		} else if (this.getTranslationUnit() == null) {
			return "No Source file was associated with this Task!";
		} else {
			return "";
		}
	}

	public ITranslationUnit getTranslationUnit() {
		return this.sourceTranslationUnit;
	}

	public boolean hasConfigurationFile() {
		return this.configFile != null;
	}

	public void setName(String newName) {
		this.name = createUniqueName(newName);
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void setDirty(boolean b) {
		this.isDirty = b;
	}

	public void setConfigFile(IFile member) {
		this.configFile = member;
	}

	public void setTranslationUnit(ITranslationUnit tu) {
		this.sourceTranslationUnit = tu;
	}
	
	/**Returns the File (a Directory) where the results of this Task should be saved.
	 * @return
	 */
	public IFolder getOutputDirectory(boolean create) {
		/*IPreferencesService service = Platform.getPreferencesService();
		String value = service.getString(CPAcheckerPlugin.PLUGIN_ID,
				PreferenceConstants.P_RESULT_DIR,
				PreferenceConstants.P_RESULT_DIR_DEFAULT_VALUE, null);
				*/
		Properties properties = new Properties();
	    try {
			properties.load(this.configFile.getContents());
		} catch (IOException e1) {
			CPAclipse.logError("Could not load the configFile", e1);
		} catch (CoreException e1) {
			CPAclipse.logError("Could not load the configFile", e1);
		}
		String projectRelativePath = properties.getProperty("output.path", DEFAULT_OUTPUT_DIR);
		
		IFolder outDir = ResourcesPlugin.getWorkspace().getRoot().getFolder(
				this.configFile.getProject().getFullPath().append(projectRelativePath));
		assert outDir.exists() : "OutputDirectory of CPAchecker does not exist! Could not create Task Output dir.";
		outDir = outDir.getFolder(this.getName());
		if (create) {
			try {
				outDir.create(true, true, null);
			} catch (CoreException e) {
				CPAclipse.logError("Could not create Result Directory at path " 
						+ outDir.getFullPath().toPortableString(), e);
			}
		}
		// TODO: as this will be a directory we should escape characters that can not occur in directory paths
		
		return outDir;
		/*
		File file = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString() + "/.metadata/.plugins/" 
				+ CPAcheckerPlugin.PLUGIN_ID + "/results/" + this.getName() + "/");
		file.mkdirs();
		return file;
		*/
	}
	
}