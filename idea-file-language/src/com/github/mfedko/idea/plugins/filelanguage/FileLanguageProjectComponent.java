package com.github.mfedko.idea.plugins.filelanguage;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

public class FileLanguageProjectComponent implements ProjectComponent {

    private Project myProject;

    public FileLanguageProjectComponent(Project project) {

        myProject = project;
    }

    @Override
    public void projectOpened() {

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
        if (statusBar != null) {
            statusBar.addWidget(new FileLanguagePanel(myProject));
        }
    }

    @Override
    public void projectClosed() {

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
        if (statusBar != null) {
            statusBar.removeWidget("FileLanguage");
        }
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "FileLanguageProjectComponent";
    }
}
