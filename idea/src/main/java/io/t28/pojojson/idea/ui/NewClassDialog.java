package io.t28.pojojson.idea.ui;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.uiDesigner.core.GridConstraints;
import io.t28.pojojson.idea.Type;
import io.t28.pojojson.idea.commands.SetTextCommand;
import io.t28.pojojson.idea.validator.NullValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.lambda.tuple.Tuple;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class NewClassDialog extends DialogWrapper {
    private static final String EMPTY_TEXT = "";

    private final Project project;
    private final Application application;
    private final EditorFactory editorFactory;

    private final InputValidator nameValidator;
    private final InputValidator typeValidator;
    private final InputValidator jsonValidator;
    private final ActionListener actionListener;

    private final Action formatAction;

    private JPanel centerPanel;
    private JTextField nameTextField;
    private JComboBox<Type> styleComboBox;
    private JComponent jsonEditorComponent;
    private Editor jsonEditor;
    private Document jsonDocument;

    private NewClassDialog(@NotNull Builder builder) {
        super(builder.project, true);
        this.project = builder.project;
        this.application = builder.application;
        this.editorFactory = builder.editorFactory;
        this.nameValidator = builder.nameValidator;
        this.typeValidator = builder.typeValidator;
        this.jsonValidator = builder.jsonValidator;
        this.actionListener = builder.actionListener;

        this.formatAction = new FormatAction();

        setTitle("Create New Class from JSON");
        setResizable(true);
        init();
    }

    @NotNull
    public static Builder builder(@NotNull Project project) {
        return new Builder(project);
    }

    @NotNull
    @CheckReturnValue
    public String getClassName() {
        final String text = nameTextField.getText();
        return Strings.nullToEmpty(text).trim();
    }

    @NotNull
    @CheckReturnValue
    public String getClassStyle() {
        final Object selected = styleComboBox.getSelectedItem();
        return Strings.nullToEmpty((String) selected).trim();
    }

    @NotNull
    @CheckReturnValue
    public String getJson() {
        return jsonDocument.getText().trim();
    }

    public void setJson(@NotNull String json) {
        final Runnable command = SetTextCommand.builder()
                .text(json)
                .editor(jsonEditor)
                .document(jsonDocument)
                .build();
        ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(project, command, null, null, UndoConfirmationPolicy.DEFAULT, jsonDocument));
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        jsonDocument = editorFactory.createDocument(EMPTY_TEXT);
        jsonEditor = editorFactory.createEditor(jsonDocument, project, JsonFileType.INSTANCE, false);
        final EditorSettings settings = jsonEditor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setAdditionalColumnsCount(0);
        settings.setAdditionalLinesCount(0);
        settings.setRightMarginShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);

        final EditorColorsScheme colorsScheme = jsonEditor.getColorsScheme();
        colorsScheme.setColor(EditorColors.CARET_ROW_COLOR, null);

        final GridConstraints constraints = new GridConstraints(
                2, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                3,
                3,
                new Dimension(480, 300),
                null,
                null
        );
        jsonEditorComponent = jsonEditor.getComponent();
        jsonEditor.getContentComponent().setFocusable(true);
        ((JPanel) centerPanel.getComponent(0)).add(jsonEditorComponent, constraints);
        return centerPanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return ImmutableList
                .of(
                        Tuple.tuple(getClassName(), nameValidator, nameTextField),
                        Tuple.tuple(getClassStyle(), typeValidator, styleComboBox),
                        Tuple.tuple(getJson(), jsonValidator, jsonEditorComponent)
                )
                .stream()
                .filter(tuple -> {
                    final String text = tuple.v1();
                    final InputValidator validator = tuple.v2();
                    return validator.checkInput(text);
                })
                .filter(tuple -> {
                    final String text = tuple.v1();
                    final InputValidator validator = tuple.v2();
                    return !validator.canClose(text);
                })
                .map(tuple -> {
                    final String text = tuple.v1();
                    final InputValidator validator = tuple.v2();

                    String message = null;
                    if (validator instanceof InputValidatorEx) {
                        message = ((InputValidatorEx) validator).getErrorText(text);
                    }
                    return new ValidationInfo(message, tuple.v3());
                })
                .findFirst()
                .orElseGet(() -> super.doValidate());
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        actionListener.onOk(this);
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{formatAction};
    }

    class FormatAction extends DialogWrapperAction {
        protected FormatAction() {
            super("Format");
        }

        @Override
        protected void doAction(@NotNull ActionEvent event) {
            if (isEnabled()) {
            }
            actionListener.onFormat(NewClassDialog.this);
        }
    }

    public interface ActionListener {
        default void onOk(@NotNull NewClassDialog dialog) {
        }

        default void onCancel(@NotNull NewClassDialog dialog) {
        }

        default void onFormat(@NotNull NewClassDialog dialog) {
        }
    }

    public static class Builder {
        private final Project project;
        private Application application;
        private EditorFactory editorFactory;
        private InputValidator nameValidator;
        private InputValidator typeValidator;
        private InputValidator jsonValidator;
        private ActionListener actionListener;

        private Builder(@NotNull Project project) {
            this.project = project;
            this.application = ApplicationManager.getApplication();
            this.editorFactory = EditorFactory.getInstance();
            this.nameValidator = new NullValidator();
            this.typeValidator = new NullValidator();
            this.jsonValidator = new NullValidator();
            this.actionListener = new ActionListener() {
            };
        }

        @NotNull
        @CheckReturnValue
        public Builder application(@Nonnull Application application) {
            this.application = application;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public Builder editorFactory(@NotNull EditorFactory editorFactory) {
            this.editorFactory = editorFactory;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public Builder nameValidator(@NotNull InputValidator nameValidator) {
            this.nameValidator = nameValidator;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public Builder typeValidator(@NotNull InputValidator typeValidator) {
            this.typeValidator = typeValidator;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public Builder jsonValidator(@NotNull InputValidator jsonValidator) {
            this.jsonValidator = jsonValidator;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public Builder actionListener(@NotNull ActionListener actionListener) {
            this.actionListener = actionListener;
            return this;
        }

        @NotNull
        @CheckReturnValue
        public NewClassDialog build() {
            return new NewClassDialog(this);
        }
    }
}
