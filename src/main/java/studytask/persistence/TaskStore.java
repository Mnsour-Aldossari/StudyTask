package studytask.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import studytask.model.Priority;
import studytask.model.Task;
import studytask.model.TaskStatus;

/** Local XML storage. A complete replacement is written before the live model changes. */
public final class TaskStore {
    private static final String VERSION = "1";
    private final Path file;

    public record Snapshot(List<Task> tasks, long nextId) {}

    public TaskStore(Path file) { this.file = file.toAbsolutePath(); }

    public static TaskStore defaultStore() {
        String appData = System.getenv("APPDATA");
        Path base = appData == null || appData.isBlank()
            ? Path.of(System.getProperty("user.home"), ".studytask")
            : Path.of(appData, "StudyTask");
        return new TaskStore(base.resolve("tasks.xml"));
    }

    public Path file() { return file; }

    public Snapshot load() throws IOException {
        if (!Files.exists(file)) return new Snapshot(List.of(), 1);
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            values.loadFromXML(input);
            if (!VERSION.equals(values.getProperty("version"))) throw new IllegalArgumentException("Unsupported file version");
            int count = Integer.parseInt(required(values, "count"));
            long nextId = Long.parseLong(required(values, "nextId"));
            if (count < 0 || count > 1_000_000 || nextId < 1 || nextId > (long) Integer.MAX_VALUE + 1) {
                throw new IllegalArgumentException("Invalid task count or next ID");
            }
            List<Task> tasks = new ArrayList<>(count);
            Set<Integer> ids = new HashSet<>();
            int greatestId = 0;
            for (int index = 0; index < count; index++) {
                String prefix = "task." + index + ".";
                int id = Integer.parseInt(required(values, prefix + "id"));
                Task task = new Task(id, required(values, prefix + "title"), required(values, prefix + "course"),
                    LocalDate.parse(required(values, prefix + "deadline")),
                    Priority.valueOf(required(values, prefix + "priority")),
                    required(values, prefix + "description"),
                    TaskStatus.valueOf(required(values, prefix + "status")));
                if (!ids.add(id)) throw new IllegalArgumentException("Duplicate task ID");
                greatestId = Math.max(greatestId, id);
                tasks.add(task);
            }
            if (nextId <= greatestId) throw new IllegalArgumentException("Invalid next task ID");
            return new Snapshot(List.copyOf(tasks), nextId);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new IOException("Saved task data is invalid: " + file, ex);
        }
    }

    public void save(List<Task> tasks, long nextId) throws IOException {
        Properties values = new Properties();
        values.setProperty("version", VERSION);
        values.setProperty("count", Integer.toString(tasks.size()));
        values.setProperty("nextId", Long.toString(nextId));
        for (int index = 0; index < tasks.size(); index++) {
            Task task = tasks.get(index);
            String prefix = "task." + index + ".";
            values.setProperty(prefix + "id", Integer.toString(task.getId()));
            values.setProperty(prefix + "title", task.getTitle());
            values.setProperty(prefix + "course", task.getCourse());
            values.setProperty(prefix + "deadline", task.getDeadline().toString());
            values.setProperty(prefix + "priority", task.getPriority().name());
            values.setProperty(prefix + "description", task.getDescription());
            values.setProperty(prefix + "status", task.getStatus().name());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "tasks-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.storeToXML(output, "StudyTask data", "UTF-8");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException("Atomic saving is unavailable for " + file, ex);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
}
