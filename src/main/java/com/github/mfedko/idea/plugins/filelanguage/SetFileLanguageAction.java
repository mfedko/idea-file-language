package com.github.mfedko.idea.plugins.filelanguage;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

public class SetFileLanguageAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(SetFileLanguageAction.class);

    private final Language language;

    public SetFileLanguageAction(@NotNull Language language) {
        super(language.getDisplayName());
        this.language = language;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        DataContext dataContext = e.getDataContext();

        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return;
        }

        final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
        final VirtualFile file = virtualFiles != null && virtualFiles.length == 1 ? virtualFiles[0] : null;
        if (file == null) {
            return;
        }

        Language currentLanguage = file.getUserData(FilesLanguageSubstitutor.LANGUAGE_KEY);
        if (currentLanguage != null && currentLanguage.getID().equals(language.getID())) {
            return;
        }

        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {

            try {
                file.putUserData(FilesLanguageSubstitutor.LANGUAGE_KEY, language);
                FileTypeManagerEx.getInstanceEx().fireFileTypesChanged();
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                FileLanguagePanel fileLanguage = (FileLanguagePanel) (statusBar != null ? statusBar.getWidget("FileLanguage") : null);
                if (fileLanguage != null) {
                    fileLanguage.doUpdate();
                }
            } catch (Exception e1) {
                LOG.warn(e1);
            }

        });
    }
}
