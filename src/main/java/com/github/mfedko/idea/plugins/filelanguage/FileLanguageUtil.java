package com.github.mfedko.idea.plugins.filelanguage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;

public class FileLanguageUtil {

    @Nullable("null means enabled, notnull means disabled and contains error message")
    static FileLanguageUtil.FailReason checkCanConvert(@NotNull VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            return FileLanguageUtil.FailReason.IS_DIRECTORY;
        }

        return fileTypeDescriptionError(virtualFile);
    }

    @Nullable
    static FailReason checkCanReload(@NotNull VirtualFile virtualFile, @Nullable Ref<? super Language> current) {
        if (virtualFile.isDirectory()) {
            return FailReason.IS_DIRECTORY;
        }
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);
        if (document == null) return FailReason.IS_BINARY;
        Language languageFromFile = FileLanguageManager.computeLanguageFromFile(virtualFile);
        Language existing = FileLanguageManager.languageFromFileTypeOrNull(virtualFile);
        FailReason result;
        if (existing != null) {
            // no point changing encoding if it was auto-detected
            result = FailReason.BY_FILE;
        } else if (languageFromFile != null) {
            result = FailReason.BY_FILE;
            existing = languageFromFile;
        } else {
            result = fileTypeDescriptionError(virtualFile);
        }
        if (current != null) current.set(existing);
        return result;
    }

    @Nullable
    private static FailReason fileTypeDescriptionError(@NotNull VirtualFile virtualFile) {
        if (virtualFile.getFileType().isBinary()) return FailReason.IS_BINARY;

        return null;
    }

    @Nullable
    public static Pair<Language, String> getLanguageAndTheReasonTooltip(@NotNull VirtualFile file) {
        FailReason r1 = checkCanConvert(file);
        if (r1 == null) return null;
        Ref<Language> current = Ref.create();
        FailReason r2 = checkCanReload(file, current);
        if (r2 == null) return null;
        String errorDescription = r1 == r2 ? reasonToString(r1) : reasonToString(r1) + ", " + reasonToString(r2);
        return Pair.create(current.get(), errorDescription);
    }

    static String reasonToString(@NotNull FailReason reason) {
        switch (reason) {
            case IS_DIRECTORY:
                return "disabled for a directory";
            case IS_BINARY:
                return "disabled for a binary file";
            case BY_FILE:
                return "language is hard-coded in the file";
        }
        throw new AssertionError(reason);
    }

    enum FailReason {
        IS_DIRECTORY,
        IS_BINARY,
        BY_FILE
    }
}
