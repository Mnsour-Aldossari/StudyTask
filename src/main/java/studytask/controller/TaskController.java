package studytask.controller;

import java.time.LocalDate;
import java.util.function.Predicate;
import studytask.model.*;
import studytask.view.TaskView;
import studytask.persistence.StorageException;

/** Handles user input and model commands; domain validation stays in the model. */
public final class TaskController {
    private final TaskModel model;
    private final TaskView view;
    private final Predicate<String> confirm;

    public TaskController(TaskModel model, TaskView view) { this(model, view, view::confirm); }
    public TaskController(TaskModel model, TaskView view, Predicate<String> confirm) {
        this.model = model; this.view = view; this.confirm = confirm;
        view.bindActions(this::newTask, this::save, this::delete, this::toggleStatus,
            this::changePriority, this::loadExamples, this::newTask, view::refresh);
    }

    public void newTask() { view.resetForm(); view.focusTitle(); }

    public void save() {
        run(() -> {
            TaskView.TaskInput input = view.input();
            if (view.editingId() == null) {
                model.addTask(input.title(), input.course(), input.deadline(), input.priority(), input.description());
                view.resetForm(); view.showMessage("Task added. Active filters may hide it.", false);
            } else {
                Task updated = model.updateTask(view.editingId(), input.title(), input.course(), input.deadline(), input.priority(), input.description());
                view.edit(updated); view.showMessage("Changes saved.", false);
            }
        });
    }

    public void delete() {
        if (view.editingId() == null) return;
        run(() -> {
            Task task = model.getTask(view.editingId());
            if (confirm.test("Delete \"" + task.getTitle() + "\"? This cannot be undone.")) {
                model.deleteTask(task.getId()); view.resetForm(); view.showMessage("Task deleted.", false);
            }
        });
    }

    public void toggleStatus() {
        if (view.editingId() == null) return;
        run(() -> {
            Task task = model.getTask(view.editingId());
            Task updated = model.setStatus(task.getId(), task.getStatus() == TaskStatus.PENDING ? TaskStatus.COMPLETED : TaskStatus.PENDING);
            view.updateStatusAction(updated); view.showMessage("Marked " + updated.getStatus().toString().toLowerCase() + ".", false);
        });
    }

    public void changePriority() {
        if (view.editingId() == null) return;
        run(() -> {
            model.setPriority(view.editingId(), view.chosenPriority());
            view.showMessage("Priority updated.", false);
        });
    }

    public void loadExamples() {
        if (!model.getTasks().isEmpty()) return;
        run(this::addExamples);
    }

    private void addExamples() {
        LocalDate today = LocalDate.now();
        model.addTask("Finish the MVC implementation", "SE-4111", today.plusDays(2), Priority.HIGH, "Connect the task controller to the model. Check all seven functional requirements.");
        model.addTask("Review normalization exercises", "CS-321", today.plusDays(1), Priority.MEDIUM, "Review 1NF, 2NF and 3NF, then solve the practice questions.");
        model.addTask("Prepare the project demonstration", "SE-4111", today.plusDays(7), Priority.HIGH, "Show adding, editing, filtering and completing a task.");
        model.addTask("Read the networking chapter", "CS-342", today.plusDays(4), Priority.LOW, "Take notes on routing and addressing.");
        model.addTask("Submit the lab worksheet", "CS-321", today.minusDays(1), Priority.HIGH, "Check the final query results before submission.");
        Task done = model.addTask("Write the project requirements", "SE-4111", today.minusDays(2), Priority.MEDIUM, "List the seven requirements in the project report.");
        model.setStatus(done.getId(), TaskStatus.COMPLETED);
        view.markDemoLoaded();
    }

    private void run(Runnable operation) {
        try { operation.run(); }
        catch (IllegalArgumentException | StorageException ex) { view.showMessage(ex.getMessage(), true); }
    }
}
