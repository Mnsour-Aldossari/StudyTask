package studytask.model;

import java.time.LocalDate;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import studytask.persistence.StorageException;
import studytask.persistence.TaskStore;

/**
 * Mutable MVC model, confined to its caller thread (the FX thread in the app).
 * RI: keys equal task IDs, values satisfy Task RI, no null values/listeners,
 * unique listeners, nextId > every previously assigned ID and nextId > 0.
 * AF: current academic tasks in insertion order.
 * No JavaFX dependency; snapshots never expose the mutable representation.
 */
public final class TaskModel {
    private final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private final Set<TaskListener> listeners = new LinkedHashSet<>();
    private final TaskStore store;
    private long nextId = 1;

    /** In-memory model for isolated use and tests. */
    public TaskModel() { store = null; }

    /** Load the previously saved state before presenting the application. */
    public TaskModel(TaskStore store) throws IOException {
        if (store == null) throw new IllegalArgumentException("Task store is required.");
        this.store = store;
        TaskStore.Snapshot snapshot = store.load();
        for (Task task : snapshot.tasks()) tasks.put(task.getId(), task);
        nextId = snapshot.nextId();
    }

    public List<Task> getTasks() { return List.copyOf(tasks.values()); }

    public Task addTask(String title, String course, LocalDate deadline, Priority priority, String description) {
        if (nextId > Integer.MAX_VALUE) throw new IllegalArgumentException("No more task IDs are available.");
        Task task = new Task((int) nextId, title, course, deadline, priority, description, TaskStatus.PENDING);
        Map<Integer, Task> updated = new LinkedHashMap<>(tasks);
        updated.put(task.getId(), task);
        persist(updated, nextId + 1);
        tasks.put(task.getId(), task);
        nextId++;
        notifyListeners();
        return task;
    }

    public Task updateTask(int id, String title, String course, LocalDate deadline, Priority priority, String description) {
        Task old = getTask(id);
        return replace(new Task(id, title, course, deadline, priority, description, old.getStatus()));
    }

    public Task setStatus(int id, TaskStatus status) {
        Task old = getTask(id);
        return replace(new Task(id, old.getTitle(), old.getCourse(), old.getDeadline(), old.getPriority(), old.getDescription(), status));
    }

    public Task setPriority(int id, Priority priority) {
        Task old = getTask(id);
        return replace(new Task(id, old.getTitle(), old.getCourse(), old.getDeadline(), priority, old.getDescription(), old.getStatus()));
    }

    public void deleteTask(int id) {
        getTask(id);
        Map<Integer, Task> updated = new LinkedHashMap<>(tasks);
        updated.remove(id);
        persist(updated, nextId);
        tasks.remove(id);
        notifyListeners();
    }

    public Task getTask(int id) {
        Task task = tasks.get(id);
        if (task == null) throw new IllegalArgumentException("This task no longer exists. Select another task.");
        return task;
    }

    public void addListener(TaskListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null.");
        listeners.add(listener);
    }
    public void removeListener(TaskListener listener) { listeners.remove(listener); }

    private Task replace(Task replacement) {
        Task old = getTask(replacement.getId());
        if (old.hasSameContent(replacement)) return old;
        Map<Integer, Task> updated = new LinkedHashMap<>(tasks);
        updated.put(replacement.getId(), replacement);
        persist(updated, nextId);
        tasks.put(replacement.getId(), replacement);
        notifyListeners();
        return replacement;
    }

    private void notifyListeners() {
        for (TaskListener listener : List.copyOf(listeners)) listener.tasksChanged();
    }

    private void persist(Map<Integer, Task> updated, long nextIdAfterChange) {
        if (store == null) return;
        try {
            store.save(List.copyOf(updated.values()), nextIdAfterChange);
        } catch (IOException ex) {
            throw new StorageException("Could not save tasks to " + store.file() + ". Check that the folder is writable.", ex);
        }
    }
}
