package com.github.mfedko.idea.plugins.filelanguage;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.AppTopics;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;

class FileLanguagePanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {

    @NotNull
    private final TextPanel myComponent;

    private boolean myActionEnabled;

    FileLanguagePanel(@NotNull final Project project) {
        super(project);

        myComponent = new TextPanel.ExtraSize() {
            @Override
            protected void paintComponent(@NotNull final Graphics g) {
                super.paintComponent(g);
                if (myActionEnabled && getText() != null) {
                    final Rectangle r = getBounds();
                    final Insets insets = getInsets();
                    AllIcons.Ide.Statusbar_arrows.paintIcon(this, g, r.width - insets.right - AllIcons.Ide.Statusbar_arrows.getIconWidth() - 2,
                            r.height / 2 - AllIcons.Ide.Statusbar_arrows.getIconHeight() / 2);
                }
            }
        };

        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                update();
                showPopup(e);
                return true;
            }
        }.installOn(myComponent);
        myComponent.setBorder(WidgetBorder.WIDE);
    }

    private void update() {
        UIUtil.invokeLaterIfNeeded(() -> {

            VirtualFile file = getSelectedFile();
            myActionEnabled = true;
            Language language = null;
            String toolTipText = null;
            String panelText = null;

            if (file != null) {
                FileViewProvider viewProvider = PsiManager.getInstance(myProject).findViewProvider(file);
                language = viewProvider != null ? viewProvider.getBaseLanguage() : null;

                if (language != null) {
                    toolTipText = String.format("Language: %s", StringUtil.escapeLineBreak(language.getDisplayName()));
                    panelText = language.getDisplayName();
                }
            }

            if (language == null) {
                toolTipText = "No language";
                panelText = "n/a";
                myActionEnabled = false;
            }

            myComponent.resetColor();

            String toDoComment;

            if (myActionEnabled) {
                toDoComment = "Click to change";
                myComponent.setForeground(UIUtil.getActiveTextColor());
                myComponent.setTextAlignment(Component.LEFT_ALIGNMENT);
            } else {
                toDoComment = "";
                myComponent.setForeground(UIUtil.getInactiveTextColor());
                myComponent.setTextAlignment(Component.CENTER_ALIGNMENT);
            }

            myComponent.setToolTipText(String.format("%s%n%s", toolTipText, toDoComment));
            myComponent.setText(panelText);


            if (myStatusBar != null) {
                myStatusBar.updateWidget(ID());
            }
        });
    }

    private void showPopup(MouseEvent e) {
        if (!myActionEnabled) {
            return;
        }
        DataContext dataContext = getContext();
        AnAction group = ActionManager.getInstance().getAction("ChangeFileLanguage");
        if (!(group instanceof ActionGroup)) {
            return;
        }

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "Language",
                (ActionGroup) group,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
        );
        Dimension dimension = popup.getContent().getPreferredSize();
        Point at = new Point(0, -dimension.height);
        popup.show(new RelativePoint(e.getComponent(), at));
        Disposer.register(this, popup); // destroy popup on unexpected project close
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
            @Override
            public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {
                update();
            }
        });
    }

    @NotNull
    private DataContext getContext() {
        Editor editor = getEditor();
        DataContext parent = DataManager.getInstance().getDataContext((Component) myStatusBar);
        return SimpleDataContext.getSimpleContext(
                CommonDataKeys.VIRTUAL_FILE.getName(),
                new VirtualFile[] {getSelectedFile()},
                SimpleDataContext.getSimpleContext(CommonDataKeys.PROJECT.getName(),
                        getProject(),
                        SimpleDataContext.getSimpleContext(PlatformDataKeys.CONTEXT_COMPONENT.getName(),
                                editor == null ? null : editor.getComponent(), parent)
                ));
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Override
    public StatusBarWidget copy() {
        Project project = getProject();
        return project == null ? null : new FileLanguagePanel(project);
    }

    @NotNull
    @Override
    public String ID() {
        return "FileLanguage";
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return null;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }
        update();
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        update();
    }

    void doUpdate() {
        update();
    }
}
