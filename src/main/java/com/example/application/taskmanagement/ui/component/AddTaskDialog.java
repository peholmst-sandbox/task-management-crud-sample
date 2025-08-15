package com.example.application.taskmanagement.ui.component;

import com.example.application.security.UserInfoLookup;
import com.example.application.taskmanagement.domain.Task;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;

public class AddTaskDialog extends Dialog {

    private final SerializableConsumer<Task> onSaveCallback;
    private final TaskForm form;

    public AddTaskDialog(UserInfoLookup userInfoLookup, SerializableSupplier<Task> formDataObjectFactory,
            SerializableConsumer<Task> onSaveCallback) {
        this.onSaveCallback = onSaveCallback;

        form = new TaskForm(userInfoLookup, formDataObjectFactory.get());

        var saveBtn = new Button("Save", event -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelBtn = new Button("Cancel", event -> close());

        setHeaderTitle("Add Task");
        add(form);
        getFooter().add(cancelBtn, saveBtn);
    }

    private void save() {
        form.getFormDataObject().ifPresent(project -> {
            onSaveCallback.accept(project);
            close();
        });
    }
}
