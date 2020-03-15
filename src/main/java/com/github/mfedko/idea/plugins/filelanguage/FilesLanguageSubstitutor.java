package com.github.mfedko.idea.plugins.filelanguage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;

public class FilesLanguageSubstitutor extends LanguageSubstitutor {

    static final Key<Language> LANGUAGE_KEY = Key.create(FilesLanguageSubstitutor.class.getName() + ".language");

    @Nullable
    @Override
    public Language getLanguage(@NotNull VirtualFile virtualFile, @NotNull Project project) {
        return virtualFile.getUserData(LANGUAGE_KEY);
    }
}
