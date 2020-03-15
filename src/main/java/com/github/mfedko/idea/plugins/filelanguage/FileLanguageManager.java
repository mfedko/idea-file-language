package com.github.mfedko.idea.plugins.filelanguage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

public class FileLanguageManager {

    private static final Logger LOG = Logger.getInstance(FileLanguageManager.class);
    private static final Key<Language> CACHED_LANGUAGE_FOR_FILE = Key.create(FileLanguageManager.class.getName() + ".language");

    public static final String PROP_LANGUAGE_CHANGED = "cachedLanguage";

    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    @Nullable
    public static Language getCachedLanguageFromFile(@NotNull Document document) {
        return document.getUserData(CACHED_LANGUAGE_FOR_FILE);
    }

    public static void setLanguageFromFileType(@NotNull Document document, Language language) {
        document.putUserData(CACHED_LANGUAGE_FOR_FILE, language);
    }

    @Nullable("returns null if language set cannot be determined")
    static Language computeLanguageFromFile(@NotNull final VirtualFile virtualFile) {
        final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        Language cached = getCachedLanguageFromFile(document);
        if (cached != null) {
            return cached;
        }

        return ReadAction.compute(() -> {
            Language languageFromFile = FileLanguageManager.languageFromFileTypeOrNull(virtualFile);
            if (languageFromFile != null) {
                setLanguageFromFileType(document, languageFromFile);
            }
            return languageFromFile;
        });
    }

    public static Collection<Language> getFavorites() {
        Set<Language> result = new LinkedHashSet<>(widelyKnownLanguages());
//        TODO: get per-project favorites
//        Project[] projects = ProjectManager.getInstance().getOpenProjects();
//        for (Project project : projects) {
//            result.addAll(EncodingProjectManager.getInstance(project).getFavorites());
//        }
        result.remove(Language.ANY);

        return result;
    }

    private static Collection<Language> widelyKnownLanguages() {
        List<Language> languages = new ArrayList<>();
        Optional.ofNullable(Language.findLanguageByID("JSON")).ifPresent(languages::add);
        Optional.ofNullable(Language.findLanguageByID("Markdown")).ifPresent(languages::add);
        Optional.ofNullable(Language.findLanguageByID("yaml")).ifPresent(languages::add);
        languages.add(StdFileTypes.XML.getLanguage());
        languages.add(StdFileTypes.PROPERTIES.getLanguage());
        languages.add(StdFileTypes.PLAIN_TEXT.getLanguage());
        return languages;
    }

    @Nullable
    private static Project guessProject(@Nullable VirtualFile virtualFile) {
        return ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    }

    public static void setLanguage(@Nullable VirtualFile file, @Nullable Language language) {
        Project project = guessProject(file);
        final Document document = file == null ? null : FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        if (project != null) {
            FileLanguageManager.setLanguageFromFileType(document, language);
        }

        Language currentLanguage = file.getUserData(FilesLanguageSubstitutor.LANGUAGE_KEY);
        if (currentLanguage != null && language != null && currentLanguage.getID().equals(language.getID())) {
            return;
        }

        Application application = ApplicationManager.getApplication();
        application.runWriteAction(() -> {

            try {
                file.putUserData(FilesLanguageSubstitutor.LANGUAGE_KEY, language);
                FileTypeManagerEx.getInstanceEx().fireFileTypesChanged();
                getInstance().firePropertyChange(document, PROP_LANGUAGE_CHANGED, currentLanguage, language);
            } catch (Exception e1) {
                LOG.warn("Failed updating file type", e1);
            }

        });

    }

    public static Language languageFromFileTypeOrNull(VirtualFile file) {
        if (file == null) {
            return null;
        }
        final FileType fileType = file.getFileType();
        return fileType instanceof LanguageFileType ?
                ((LanguageFileType) fileType).getLanguage() : null;
    }

    @NotNull
    public static FileLanguageManager getInstance() {
        return ServiceManager.getService(FileLanguageManager.class);
    }


    public void addPropertyChangeListener(@NotNull final PropertyChangeListener listener, @NotNull Disposable parentDisposable) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
        Disposer.register(parentDisposable, () -> removePropertyChangeListener(listener));
    }

    private void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @SuppressWarnings("SameParameterValue")
    void firePropertyChange(@Nullable Document document, @NotNull String propertyName, final Object oldValue, final Object newValue) {
        Object source = document == null ? this : document;
        myPropertyChangeSupport.firePropertyChange(new PropertyChangeEvent(source, propertyName, oldValue, newValue));
    }

}
