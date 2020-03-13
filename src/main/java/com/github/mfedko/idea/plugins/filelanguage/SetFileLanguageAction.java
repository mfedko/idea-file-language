package com.github.mfedko.idea.plugins.filelanguage;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.command.undo.GlobalUndoableAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;

// TODO: migrate as of com.intellij.openapi.wm.impl.status.EncodingPanel
public class SetFileLanguageAction extends AnAction implements DumbAware {

    private final boolean allowDirectories;

    public SetFileLanguageAction() {
        this(false);
    }

    public SetFileLanguageAction(boolean allowDirectories) {
        this.allowDirectories = allowDirectories;
    }

    public static boolean changeTo(Project project, @NotNull Document document,
                                   @NotNull VirtualFile virtualFile,
                                   @NotNull Language language
    ) {
        final Language oldLanguage = FileLanguageManager.getCachedLanguageFromFile(document);
        final Runnable undo;
        final Runnable redo;

        //change and forget
        undo = () -> FileLanguageManager.setLanguage(virtualFile, oldLanguage);
        redo = () -> FileLanguageManager.setLanguage(virtualFile, language);
        final UndoableAction action = new GlobalUndoableAction(virtualFile) {
            @Override
            public void undo() {
                // invoke later because changing document inside undo/redo is not allowed
                Application application = ApplicationManager.getApplication();
                application.invokeLater(undo, ModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
            }

            @Override
            public void redo() {
                // invoke later because changing document inside undo/redo is not allowed
                Application application = ApplicationManager.getApplication();
                application.invokeLater(redo, ModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
            }
        };

        redo.run();
        CommandProcessor.getInstance().executeCommand(project, () -> {
            UndoManager undoManager = project == null ? UndoManager.getGlobalInstance() : UndoManager.getInstance(project);
            undoManager.undoableActionPerformed(action);
        }, "Change file type for '" + virtualFile.getName() + "'", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);

        return true;
    }

    private boolean checkEnabled(@NotNull VirtualFile virtualFile) {
        if (allowDirectories && virtualFile.isDirectory()) return true;
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (document == null) return false;

        return FileLanguageUtil.checkCanConvert(virtualFile) == null
                || FileLanguageUtil.checkCanReload(virtualFile, null) == null;
    }

    @Override
    public final void actionPerformed(@NotNull final AnActionEvent e) {
        DataContext dataContext = e.getDataContext();

        ListPopup popup = createPopup(dataContext);
        if (popup != null) {
            popup.showInBestPositionFor(dataContext);
        }
    }

    @Nullable
    public ListPopup createPopup(@NotNull DataContext dataContext) {
        final VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (virtualFile == null) return null;
        boolean enabled = checkEnabled(virtualFile);
        if (!enabled) return null;
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        final Document document = documentManager.getDocument(virtualFile);
        if (!allowDirectories && virtualFile.isDirectory() || document == null && !virtualFile.isDirectory())
            return null;

        DefaultActionGroup group = createActionGroup(virtualFile, document, null);

        return JBPopupFactory.getInstance().createActionGroupPopup(getTemplatePresentation().getText(),
                group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    public DefaultActionGroup createActionGroup(@Nullable final VirtualFile myFile,
                                                final Document document,
                                                @Nullable final String clearItemText) {
        return new ChooseFileTypeAction(myFile) {
            @Override
            public void update(@NotNull final AnActionEvent e) {
            }

            @NotNull
            @Override
            protected DefaultActionGroup createPopupActionGroup(JComponent button) {
                return createFileTypeActionGroup(clearItemText, null, language -> "Change file type to '" + language.getDisplayName() + "'");
                // no 'clear'
            }

            @Override
            protected void chosen(@Nullable VirtualFile virtualFile, @NotNull Language charset) {
                SetFileLanguageAction.this.chosen(document, virtualFile, charset);
            }
        }
                .createPopupActionGroup(null);
    }

    // returns true if language was changed, false if failed
    protected boolean chosen(final Document document,
                             @Nullable final VirtualFile virtualFile,
                             @NotNull final Language charset) {
        if (virtualFile == null) return false;

        final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        return changeTo(project, document, virtualFile, charset);
    }

    @Override
    @Deprecated
    public void update(@NotNull AnActionEvent e) {
        VirtualFile myFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean enabled = myFile != null && checkEnabled(myFile);
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(myFile != null);
    }
}
