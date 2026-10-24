package studytask.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import studytask.model.*;
import studytask.model.Priority;

/** Native JavaFX view. Observes the model, reads snapshots, never mutates it. */
public final class TaskView extends BorderPane implements TaskListener {
    private final TaskModel model;
    private final TableView<Task> table = new TableView<>();
    private final VBox rail = new VBox(22);
    private final VBox courseList = new VBox(5);
    private final ComboBox<String> courseFilter = new ComboBox<>();
    private final HBox compactCourse = new HBox(10);
    private final TextField search = new TextField();
    private final TextField title = new TextField();
    private final TextField course = new TextField();
    private final DatePicker deadline = new DatePicker();
    private final ComboBox<Priority> priority = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Label editorTitle = label("New task", "pane-title");
    private final Label editorHint = label("Make room for your next assignment.", "secondary");
    private final Label message = new Label();
    private final Label listSummary = label("", "secondary");
    private final Label count = label("", "secondary");
    private final Label emptyTitle = label("Your coursework starts here", "empty-title");
    private final Label emptyText = label("Add an assignment, exam or project.\nKeep its course and deadline in one place.", "secondary");
    private final Button newTask = button("New task", "primary", "new-task");
    private final Button save = button("Add task", "primary", "save-task");
    private final Button clear = button("Clear form", "secondary-button", "clear-form");
    private final Button delete = button("Delete task", "danger-button", "delete-task");
    private final Button status = button("Mark completed", "secondary-button", "change-status");
    private final Button applyPriority = button("Apply priority", "text-button", "apply-priority");
    private final Button examples = button("Load example tasks", "secondary-button", "load-examples");
    private final Button clearFilters = button("Clear filters", "text-button", "clear-filters");
    private final ToggleButton all = new ToggleButton("All");
    private final ToggleButton pending = new ToggleButton("Pending");
    private final ToggleButton completed = new ToggleButton("Completed");
    private final VBox empty = new VBox(12);
    private final BorderPane editorContainer;
    private TaskStatus statusFilter;
    private Integer editingId;
    private String selectedCourse;
    private boolean refreshing;
    private boolean demoLoaded;
    private Runnable filterAction = () -> {};

    public TaskView(TaskModel model) {
        this.model = model;
        getStyleClass().add("workspace");
        setId("workspace");
        setLeft(buildRail());
        setCenter(buildList());
        editorContainer = buildEditor();
        setRight(editorContainer);
        widthProperty().addListener((obs, oldWidth, width) -> adapt(width.doubleValue()));
        search.textProperty().addListener((o, a, b) -> filterAction.run());
        courseFilter.valueProperty().addListener((o, a, b) -> {
            if (!refreshing) { selectedCourse = "All courses".equals(b) ? null : b; filterAction.run(); }
        });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, task) -> {
            if (!refreshing && task != null) edit(task);
        });
        refresh();
        resetForm();
    }

    private Node buildRail() {
        rail.getStyleClass().add("rail");
        rail.setPrefWidth(188);
        rail.setMinWidth(188);
        Label brand = label("StudyTask", "brand");
        Label intro = label("A place for\nyour coursework.", "rail-intro");
        Label coursesLabel = label("Courses", "rail-heading");
        VBox navigation = new VBox(10, coursesLabel, courseList);
        Region spacer = new Region();
        VBox.setVgrow(spacer, PriorityAdapter.ALWAYS);
        Label storage = label("Saved on this device\nYour tasks return next time.", "rail-note");
        storage.setWrapText(true);
        rail.getChildren().addAll(brand, intro, navigation, spacer, storage);
        return rail;
    }

    private Node buildList() {
        VBox content = new VBox(18);
        content.getStyleClass().add("work-area");
        Label heading = label("Coursework", "page-title");
        Label today = label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)), "secondary");
        VBox titles = new VBox(4, heading, today);
        Region flex = new Region(); HBox.setHgrow(flex, PriorityAdapter.ALWAYS);
        HBox header = new HBox(12, titles, flex, newTask); header.setAlignment(Pos.CENTER_LEFT);
        search.setId("search"); search.setPromptText("Find a task or course"); search.setAccessibleText("Search tasks");
        search.setMaxWidth(Double.MAX_VALUE);
        compactCourse.setAlignment(Pos.CENTER_LEFT);
        compactCourse.getChildren().addAll(label("Course", "field-label"), courseFilter);
        courseFilter.setId("course-filter"); courseFilter.setAccessibleText("Filter by course");
        courseFilter.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(courseFilter, PriorityAdapter.ALWAYS);
        compactCourse.setVisible(false); compactCourse.setManaged(false);
        ToggleGroup group = new ToggleGroup();
        all.setId("filter-all"); pending.setId("filter-pending"); completed.setId("filter-completed");
        for (ToggleButton b : List.of(all, pending, completed)) { b.setToggleGroup(group); b.getStyleClass().add("status-tab"); }
        all.setSelected(true);
        all.setOnAction(e -> selectStatus(null));
        pending.setOnAction(e -> selectStatus(TaskStatus.PENDING));
        completed.setOnAction(e -> selectStatus(TaskStatus.COMPLETED));
        Region filterFlex = new Region(); HBox.setHgrow(filterFlex, PriorityAdapter.ALWAYS);
        HBox filters = new HBox(4, all, pending, completed, filterFlex, clearFilters); filters.setAlignment(Pos.CENTER_LEFT);
        configureTable();
        empty.setAlignment(Pos.CENTER); empty.setMaxWidth(420); empty.setPadding(new Insets(32));
        emptyText.setWrapText(true); emptyText.setAlignment(Pos.CENTER); emptyText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        empty.getChildren().addAll(emptyTitle, emptyText, examples);
        table.setPlaceholder(empty);
        VBox.setVgrow(table, PriorityAdapter.ALWAYS);
        count.setWrapText(true);
        VBox footer = new VBox(4, count, label("Saved automatically on this device.", "secondary"));
        content.getChildren().addAll(header, search, compactCourse, filters, listSummary, table, footer);
        return content;
    }

    private void configureTable() {
        table.setId("task-table"); table.setAccessibleText("Study tasks");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(68);
        TableColumn<Task, Task> taskCol = column("Task", 225);
        taskCol.setMinWidth(125);
        taskCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Task task, boolean blank) {
                super.updateItem(task, blank); setText(null); setGraphic(null);
                if (!blank && task != null) {
                    Label name = label(task.getTitle(), "task-name"); name.setMaxWidth(Double.MAX_VALUE);
                    name.setTooltip(new Tooltip(task.getTitle()));
                    Label subject = label(task.getCourse(), "secondary");
                    subject.setTooltip(new Tooltip(task.getCourse()));
                    VBox box = new VBox(4, name, subject); box.setAlignment(Pos.CENTER_LEFT);
                    box.maxWidthProperty().bind(widthProperty().subtract(20));
                    setGraphic(box);
                }
            }
        });
        TableColumn<Task, Task> dueCol = column("Deadline", 120); dueCol.setMinWidth(105); dueCol.setMaxWidth(150);
        dueCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Task task, boolean blank) {
                super.updateItem(task, blank); setText(null); setGraphic(null);
                if (!blank && task != null) {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());
                    String timing = task.getStatus() == TaskStatus.COMPLETED ? "Finished" : days < 0 ? Math.abs(days) + "d overdue" : days == 0 ? "Due today" : days == 1 ? "Tomorrow" : "In " + days + " days";
                    String style = task.getStatus() == TaskStatus.COMPLETED ? "success" : days < 0 ? "danger" : days <= 2 ? "due-soon" : "secondary";
                    VBox box = new VBox(4, label(task.getDeadline().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)), "date"), label(timing, style));
                    box.setAlignment(Pos.CENTER_LEFT); setGraphic(box);
                }
            }
        });
        TableColumn<Task, Task> priorityCol = column("Priority", 80); priorityCol.setMinWidth(75); priorityCol.setMaxWidth(95);
        priorityCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Task task, boolean blank) {
                super.updateItem(task, blank); setText(null); setGraphic(null);
                if (!blank && task != null) setGraphic(label(task.getPriority().toString(), task.getPriority() == studytask.model.Priority.HIGH ? "high-priority" : "secondary"));
            }
        });
        TableColumn<Task, Task> statusCol = column("Status", 100); statusCol.setMinWidth(93); statusCol.setMaxWidth(115);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Task task, boolean blank) {
                super.updateItem(task, blank); setText(null); setGraphic(null);
                if (!blank && task != null) setGraphic(label(task.getStatus().toString(), task.getStatus() == TaskStatus.COMPLETED ? "status-complete" : "status-pending"));
            }
        });
        table.getColumns().setAll(List.of(taskCol, dueCol, priorityCol, statusCol));
    }

    private TableColumn<Task, Task> column(String name, double width) {
        TableColumn<Task, Task> col = new TableColumn<>(name);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setPrefWidth(width); col.setSortable(false); col.setReorderable(false);
        return col;
    }

    private BorderPane buildEditor() {
        VBox pane = new VBox(14); pane.getStyleClass().add("editor");
        editorHint.setWrapText(true);
        VBox editorHeading = new VBox(6, editorTitle, editorHint);
        title.setId("task-title"); title.setPromptText("e.g. Database assignment");
        course.setId("task-course"); course.setPromptText("e.g. CS-321");
        deadline.setId("task-deadline"); deadline.setPromptText("yyyy-mm-dd"); deadline.setMaxWidth(Double.MAX_VALUE);
        deadline.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : date.toString(); }
            @Override public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                try { return LocalDate.parse(text.trim()); } catch (DateTimeParseException ex) { return null; }
            }
        });
        priority.setId("task-priority"); priority.setItems(FXCollections.observableArrayList(studytask.model.Priority.values())); priority.setMaxWidth(Double.MAX_VALUE);
        description.setId("task-description"); description.setPromptText("What needs to get done?"); description.setWrapText(true); description.setPrefRowCount(4);
        description.setMinHeight(100); description.setMaxHeight(130);
        applyPriority.setMaxWidth(Double.MAX_VALUE);
        VBox priorityField = new VBox(6, field("Priority", priority), applyPriority);
        message.setId("form-message"); message.setWrapText(true); message.setMinHeight(30); message.setMaxWidth(Double.MAX_VALUE);
        save.setMaxWidth(Double.MAX_VALUE); status.setMaxWidth(Double.MAX_VALUE); delete.setMaxWidth(Double.MAX_VALUE); clear.setMaxWidth(Double.MAX_VALUE);
        HBox secondaryActions = new HBox(8, clear, delete);
        HBox.setHgrow(clear, PriorityAdapter.ALWAYS); HBox.setHgrow(delete, PriorityAdapter.ALWAYS);
        VBox actions = new VBox(8, message, save, status, secondaryActions); actions.getStyleClass().add("editor-actions");
        pane.getChildren().addAll(editorHeading, field("Title", title), field("Course", course), field("Deadline", deadline), priorityField, field("Description (optional)", description));
        ScrollPane scroll = new ScrollPane(pane); scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("editor-scroll");
        BorderPane container = new BorderPane(scroll); container.setBottom(actions);
        container.setPrefWidth(310); container.setMinWidth(280); container.getStyleClass().add("editor-container");
        return container;
    }

    private VBox field(String text, Control control) {
        Label name = label(text, "field-label"); name.setLabelFor(control); control.setAccessibleText(text);
        return new VBox(7, name, control);
    }

    public void bindActions(Runnable newAction, Runnable saveAction, Runnable deleteAction,
                            Runnable statusAction, Runnable priorityAction, Runnable examplesAction,
                            Runnable clearAction, Runnable filterAction) {
        newTask.setOnAction(e -> newAction.run()); save.setOnAction(e -> saveAction.run());
        delete.setOnAction(e -> deleteAction.run()); status.setOnAction(e -> statusAction.run());
        applyPriority.setOnAction(e -> priorityAction.run()); examples.setOnAction(e -> examplesAction.run());
        clear.setOnAction(e -> clearAction.run()); this.filterAction = filterAction;
        clearFilters.setOnAction(e -> resetFilters());
    }

    public record TaskInput(String title, String course, LocalDate deadline, studytask.model.Priority priority, String description) {}

    public TaskInput input() {
        LocalDate date;
        try { date = LocalDate.parse(deadline.getEditor().getText().trim()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Choose a deadline or enter it as yyyy-mm-dd."); }
        return new TaskInput(title.getText(), course.getText(), date, priority.getValue(), description.getText());
    }

    public Integer editingId() { return editingId; }
    public studytask.model.Priority chosenPriority() { return priority.getValue(); }
    public void focusTitle() { title.requestFocus(); }

    public void resetForm() {
        editingId = null;
        refreshing = true; table.getSelectionModel().clearSelection(); refreshing = false;
        title.clear(); course.clear(); description.clear(); deadline.setValue(LocalDate.now().plusDays(1));
        priority.setValue(studytask.model.Priority.MEDIUM);
        editorTitle.setText("New task"); editorHint.setText("Make room for your next assignment.");
        save.setText("Add task"); clear.setText("Clear form");
        status.setDisable(true); delete.setDisable(true); applyPriority.setDisable(true); showMessage("", false);
    }

    public void edit(Task task) {
        editingId = task.getId();
        title.setText(task.getTitle()); course.setText(task.getCourse()); deadline.setValue(task.getDeadline());
        priority.setValue(task.getPriority()); description.setText(task.getDescription());
        editorTitle.setText("Task details"); editorHint.setText("Update the details, then save your changes.");
        save.setText("Save changes"); clear.setText("Cancel editing");
        status.setDisable(false); delete.setDisable(false); applyPriority.setDisable(false);
        status.setText(task.getStatus() == TaskStatus.COMPLETED ? "Mark pending" : "Mark completed");
        showMessage("", false);
    }

    @Override public void tasksChanged() { refresh(); }

    public void refresh() {
        refreshing = true;
        List<Task> tasks = model.getTasks();
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Task> visible = tasks.stream().filter(task -> matches(task, selectedCourse, statusFilter, query)).toList();
        Integer selectedId = table.getSelectionModel().getSelectedItem() == null ? null : table.getSelectionModel().getSelectedItem().getId();
        table.getItems().setAll(visible);
        if (selectedId != null) visible.stream().filter(t -> t.getId() == selectedId).findFirst().ifPresent(t -> table.getSelectionModel().select(t));
        List<String> courses = tasks.stream().map(Task::getCourse).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        courseFilter.getItems().setAll("All courses"); courseFilter.getItems().addAll(courses);
        if (selectedCourse != null && !courseFilter.getItems().contains(selectedCourse)) courseFilter.getItems().add(selectedCourse);
        courseFilter.setValue(selectedCourse == null ? "All courses" : selectedCourse);
        rebuildCourses(courses, tasks);
        long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        count.setText(tasks.size() + " total   /   " + (tasks.size() - completedCount) + " pending   /   " + completedCount + " completed" + (demoLoaded ? "   •   Example session" : ""));
        listSummary.setText((selectedCourse == null ? "All courses" : selectedCourse) + "   /   " + visible.size() + (visible.size() == 1 ? " task" : " tasks"));
        boolean filtered = selectedCourse != null || statusFilter != null || !query.isEmpty();
        clearFilters.setDisable(!filtered);
        emptyTitle.setText(filtered ? "No matching tasks" : "Your coursework starts here");
        emptyText.setText(filtered ? "Try another course or status, or clear your filters." : "Add an assignment, exam or project.\nKeep its course and deadline in one place.");
        examples.setVisible(tasks.isEmpty() && !demoLoaded && !filtered); examples.setManaged(examples.isVisible());
        refreshing = false;
    }

    /** Presentation predicate; never changes model state. */
    public static boolean matches(Task task, String course, TaskStatus status, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return (course == null || task.getCourse().equalsIgnoreCase(course.trim()))
            && (status == null || task.getStatus() == status)
            && (q.isEmpty() || (task.getTitle() + " " + task.getCourse() + " " + task.getDescription()).toLowerCase(Locale.ROOT).contains(q));
    }

    private void rebuildCourses(List<String> courses, List<Task> tasks) {
        courseList.getChildren().clear();
        addCourseButton("All courses", null, tasks.size());
        for (String name : courses) addCourseButton(name, name, (int) tasks.stream().filter(t -> t.getCourse().equals(name)).count());
    }

    private void addCourseButton(String name, String value, int total) {
        Button button = new Button(); button.setMaxWidth(Double.MAX_VALUE); button.getStyleClass().add("course-button");
        if (java.util.Objects.equals(selectedCourse, value)) button.getStyleClass().add("course-selected");
        Label nameLabel = label(name, "course-name"); nameLabel.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(nameLabel, PriorityAdapter.ALWAYS);
        HBox line = new HBox(8, nameLabel, label(Integer.toString(total), "course-count")); line.setAlignment(Pos.CENTER_LEFT); line.setMaxWidth(Double.MAX_VALUE);
        button.setGraphic(line); button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY); button.setAccessibleText(name + ", " + total + " tasks"); button.setTooltip(new Tooltip(name));
        button.setOnAction(e -> { selectedCourse = value; filterAction.run(); }); courseList.getChildren().add(button);
    }

    private void selectStatus(TaskStatus value) {
        statusFilter = value; all.setSelected(value == null); pending.setSelected(value == TaskStatus.PENDING); completed.setSelected(value == TaskStatus.COMPLETED); filterAction.run();
    }

    public void resetFilters() {
        selectedCourse = null; search.clear(); selectStatus(null); refresh();
    }

    public void markDemoLoaded() { demoLoaded = true; refresh(); }
    public void updateStatusAction(Task task) {
        status.setText(task.getStatus() == TaskStatus.COMPLETED ? "Mark pending" : "Mark completed");
    }
    public void showMessage(String text, boolean error) {
        message.setText(text); message.getStyleClass().setAll(error ? "form-error" : "form-success");
        message.setAccessibleText(text);
    }

    public boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.CANCEL, ButtonType.OK);
        alert.initOwner(getScene().getWindow()); alert.setTitle("StudyTask"); alert.setHeaderText("Confirm action");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/studytask.css").toExternalForm());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void adapt(double width) {
        boolean compact = width < 1150;
        rail.setManaged(!compact); rail.setVisible(!compact);
        compactCourse.setManaged(compact); compactCourse.setVisible(compact);
        editorContainer.setPrefWidth(compact ? 280 : 310);
    }

    private static Label label(String text, String style) { Label l = new Label(text); l.getStyleClass().add(style); return l; }
    private static Button button(String text, String style, String id) { Button b = new Button(text); b.getStyleClass().add(style); b.setId(id); return b; }
    // Avoid confusing layout Priority with the domain's Priority enum.
    private static final class PriorityAdapter { static final javafx.scene.layout.Priority ALWAYS = javafx.scene.layout.Priority.ALWAYS; }
}
