package com.github.mfedko.idea.plugins.filelanguage;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SetFileLanguageActionGroup extends ActionGroup {

    final private Supplier<AnAction[]> actionsSupplier = Suppliers.memoize(() -> {

        List<Language> languages = new ArrayList<>(Language.getRegisteredLanguages());
        Collections.sort(languages, (o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
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
