package studytask.model;

import java.time.LocalDate;

/**
 * Immutable ADT for one academic task.
 * RI: id > 0; title/course are trimmed and nonblank; all fields are non-null.
 * AF: the task identified by id, titled title, for course, due on deadline,
 * with priority and description, complete iff status is COMPLETED.
 * Rep safety: final class, private final fields, immutable field types, no setters.
 * Equality is logical identity across immutable versions and saved sessions.
 */
public final class Task {
    private final int id;
    private final String title;
    private final String course;
    private final LocalDate deadline;
    private final Priority priority;
    private final String description;
    private final TaskStatus status;

    public Task(int id, String title, String course, LocalDate deadline,
                Priority priority, String description, TaskStatus status) {
        if (id <= 0) throw new IllegalArgumentException("Task ID must be positive.");
        this.id = id;
        this.title = requiredText(title, "Title");
        this.course = requiredText(course, "Course");
        this.deadline = required(deadline, "Choose a deadline.");
        this.priority = required(priority, "Choose a priority.");
        this.description = required(description, "Description cannot be null.");
        this.status = required(status, "Choose a task status.");
    }

    private static String requiredText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required.");
        return value.trim();
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCourse() { return course; }
    public LocalDate getDeadline() { return deadline; }
    public Priority getPriority() { return priority; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }

    /** Used for no-op detection; equals intentionally compares only IDs. */
    boolean hasSameContent(Task other) {
        return equals(other) && title.equals(other.title) && course.equals(other.course)
            && deadline.equals(other.deadline) && priority == other.priority
            && description.equals(other.description) && status == other.status;
    }

    @Override public boolean equals(Object other) {
        return other instanceof Task task && id == task.id;
    }
    @Override public int hashCode() { return Integer.hashCode(id); }
}
