package com.github.mfedko.idea.plugins.filelanguage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.VolatileNotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IconDeferrer;
import com.intellij.util.ArrayUtilRt;
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
                    Icon defer;
                    if (virtualFile == null || virtualFile.isDirectory()) {
                        defer = null;
                    } else {
                        NotNullLazyValue<CharSequence> myText = VolatileNotNullLazyValue.createValue(() -> LoadTextUtil.loadText(virtualFile));
                        NotNullLazyValue<byte[]> myBytes = VolatileNotNullLazyValue.createValue(() -> {
                            try {
                                return virtualFile.contentsToByteArray();
                            } catch (IOException e1) {
                                return ArrayUtilRt.EMPTY_BYTE_ARRAY;
                            }
                        });
                        defer = IconDeferrer.getInstance().defer(null, Pair.create(virtualFile, language), pair -> {
                            VirtualFile myFile = pair.getFirst();
                            Language lang = pair.getSecond();
                            CharSequence text = myText.getValue();
                            byte[] bytes = myBytes.getValue();
                            FileLanguageUtil.Magic8 safeToReload = FileLanguageUtil.isSafeToReloadIn(myFile, text, bytes, lang);
                            FileLanguageUtil.Magic8 safeToConvert = FileLanguageUtil.Magic8.ABSOLUTELY;
                            if (safeToReload != FileLanguageUtil.Magic8.ABSOLUTELY) {
                                safeToConvert = FileLanguageUtil.Magic8.ABSOLUTELY;
                            }
                            return safeToReload == FileLanguageUtil.Magic8.ABSOLUTELY || safeToConvert == FileLanguageUtil.Magic8.ABSOLUTELY ? null :
                                    safeToReload == FileLanguageUtil.Magic8.WELL_IF_YOU_INSIST || safeToConvert == FileLanguageUtil.Magic8.WELL_IF_YOU_INSIST ?
                                            AllIcons.General.Warning : AllIcons.General.Error;
                        });
                    }
                    e.getPresentation().setIcon(defer);
                    e.getPresentation().setDescription(description);
                }
            };
            group.add(action);
        }
    }

    protected abstract void chosen(@Nullable VirtualFile virtualFile, @NotNull Language language);

    @NotNull
    protected DefaultActionGroup createFileTypeActionGroup(@Nullable String clearItemText,
                                                           @Nullable Language alreadySelected,
                                                           @NotNull Function<Language, String> languageFilter) {
        DefaultActionGroup group = new DefaultActionGroup();
        List<Language> favorites = new ArrayList<>(FileLanguageManager.getFavorites());
        favorites.sort(Comparator.comparing(Language::getDisplayName));
        Language defaultLanguage = myVirtualFile == null ? null : FileLanguageManager.languageFromFileTypeOrNull(myVirtualFile);
        favorites.remove(defaultLanguage);
        favorites.remove(alreadySelected);

        if (clearItemText != null) {
            String description = "Clear " + (myVirtualFile == null ? "default" : "file '" + myVirtualFile.getName() + "'") + " type.";
            group.add(new DumbAwareAction(clearItemText, description, null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    chosen(myVirtualFile, defaultLanguage);
                }
            });
        }
        if (favorites.isEmpty() && clearItemText == null) {
            fillFileTypeActions(group, myVirtualFile, Language.getRegisteredLanguages(), languageFilter);
        } else {
            fillFileTypeActions(group, myVirtualFile, favorites, languageFilter);

            DefaultActionGroup more = new DefaultActionGroup("more", true);
            group.add(more);
            fillFileTypeActions(more, myVirtualFile, Language.getRegisteredLanguages(), languageFilter);
        }
        return group;
    }
}
