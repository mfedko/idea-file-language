package com.github.mfedko.idea.plugins.filelanguage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetProvider;

import static com.intellij.openapi.wm.StatusBar.Anchors.after;

public class FileLanguageWidgetProvider implements StatusBarWidgetProvider {

    @Nullable
    @Override
    public StatusBarWidget getWidget(@NotNull Project project) {
        return new FileLanguagePanel(project);
    }

    @NotNull
    @Override
    public String getAnchor() {
        return after(StatusBar.StandardWidgets.ENCODING_PANEL);
    }
}
