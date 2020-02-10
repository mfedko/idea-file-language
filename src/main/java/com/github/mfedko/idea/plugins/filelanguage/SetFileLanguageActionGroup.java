package com.github.mfedko.idea.plugins.filelanguage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class SetFileLanguageActionGroup extends ActionGroup {

    final private Supplier<AnAction[]> actionsSupplier = Suppliers.memoize(() -> {

        List<Language> languages = new ArrayList<>(Language.getRegisteredLanguages());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        AnAction[] actions = new AnAction[languages.size()];
        int i = 0;
        for (Language language : languages) {
            actions[i] = new SetFileLanguageAction(language);
            ++i;
        }
        return actions;
    });

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {

        return actionsSupplier.get();
    }
}
