package com.github.mfedko.idea.plugins.filelanguage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.util.ObjectUtils;

// TODO: {@link migrate as of com.intellij.openapi.wm.impl.status.EncodingPanel}
class FileLanguagePanel extends EditorBasedStatusBarPopup {

    public static final String N_A = "n/a";

    FileLanguagePanel(@NotNull final Project project) {
        super(project, false);
    }

    @NotNull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        if (file == null) {
            return WidgetState.HIDDEN;
        }

        Pair<Language, String> check = FileLanguageUtil.getLanguageAndTheReasonTooltip(file);
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        final Language cachedLanguage = document == null ? null : FileLanguageManager.getCachedLanguageFromFile(document);
        String failReason = Pair.getSecond(check);
        Language language = Pair.getFirst(check) != null ? Pair.getFirst(check) : cachedLanguage;
        String languageName = ObjectUtils.notNull(language == null ? N_A : language.getDisplayName(), N_A);
        String toolTipText = failReason == null ? "File Language: " + languageName : StringUtil.capitalize(failReason) + ".";
        return new WidgetState(toolTipText, languageName, failReason == null);
    }

    @Nullable
    @Override
    protected ListPopup createPopup(DataContext context) {
        SetFileLanguageAction action = new SetFileLanguageAction();
        action.getTemplatePresentation().setText("File Language");
        return action.createPopup(context);
    }

    @Override
    protected void registerCustomListeners() {
        FileLanguageManager.getInstance().addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals(FileLanguageManager.PROP_LANGUAGE_CHANGED)) {
                Document document = evt.getSource() instanceof Document ? (Document)evt.getSource() : null;
                updateForDocument(document);
            }
        }, this);
    }

    @NotNull
    @Override
    protected StatusBarWidget createInstance(@NotNull Project project) {
        return new FileLanguagePanel(project);
    }

    @NotNull
    @Override
    public String ID() {
        return "FileLanguage";
    }
}
