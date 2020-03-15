package com.github.mfedko.idea.plugins.filelanguage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.ui.EmptyIcon;

public abstract class ChooseFileTypeAction extends ComboBoxAction {
    private final VirtualFile myVirtualFile;

    protected ChooseFileTypeAction(@Nullable VirtualFile virtualFile) {
        myVirtualFile = virtualFile;
    }

    @Override
    public abstract void update(@NotNull final AnActionEvent e);

    private void fillFileTypeActions(@NotNull DefaultActionGroup group,
                                     @Nullable final VirtualFile virtualFile,
                                     @NotNull Collection<Language> languages,
                                     @NotNull final Function<Language, String> languageFilter
    ) {
        for (final Language language : languages) {
            AnAction action = new DumbAwareAction(language.getDisplayName(), null, EmptyIcon.ICON_16) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    chosen(virtualFile, language);
                }

                @Override
                public void update(@NotNull AnActionEvent e) {
                    super.update(e);
                    String description = languageFilter.fun(language);
                    e.getPresentation().setDescription(description);
                }
            };
            group.add(action);
        }
    }

    protected abstract void chosen(@Nullable VirtualFile virtualFile, @NotNull Language language);

    @NotNull
    protected DefaultActionGroup createFileTypeActionGroup(@NotNull Function<Language, String> languageFilter) {
        DefaultActionGroup group = new DefaultActionGroup();
        final List<Language> favorites = new ArrayList<>(FileLanguageManager.getFavorites());
        final List<Language> allLanguages = new ArrayList<>(Language.getRegisteredLanguages());
        allLanguages.remove(Language.ANY);
        allLanguages.sort(Comparator.comparing(Language::getDisplayName));
        if (favorites.isEmpty()) {
            fillFileTypeActions(group, myVirtualFile, allLanguages, languageFilter);
        } else {
            fillFileTypeActions(group, myVirtualFile, favorites, languageFilter);

            DefaultActionGroup more = new DefaultActionGroup("more", true);
            group.add(more);
            fillFileTypeActions(more, myVirtualFile, allLanguages, languageFilter);
        }
        return group;
    }
}
