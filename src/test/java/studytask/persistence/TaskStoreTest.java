package studytask.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studytask.model.Priority;
import studytask.model.Task;
import studytask.model.TaskModel;
import studytask.model.TaskStatus;

class TaskStoreTest {
    @TempDir Path directory;
    private static final LocalDate DEADLINE = LocalDate.of(2026, 11, 1);

    @Test void tasksAndChangesReturnAfterRestart() throws IOException {
        TaskStore store = new TaskStore(directory.resolve("tasks.xml"));
        TaskModel firstRun = new TaskModel(store);
        Task created = firstRun.addTask("مراجعة الفصل", "برمجة", DEADLINE, Priority.HIGH, "اقرأ الملاحظات");
        firstRun.updateTask(created.getId(), "مراجعة نهائية", "برمجة", DEADLINE.plusDays(1), Priority.HIGH, "أكمل الملخص");
        firstRun.setPriority(created.getId(), Priority.LOW);
        firstRun.setStatus(created.getId(), TaskStatus.COMPLETED);

        TaskModel secondRun = new TaskModel(store);
        assertEquals(1, secondRun.getTasks().size());
        Task restored = secondRun.getTasks().getFirst();
        assertEquals(created.getId(), restored.getId());
        assertEquals("مراجعة نهائية", restored.getTitle());
        assertEquals("برمجة", restored.getCourse());
        assertEquals(DEADLINE.plusDays(1), restored.getDeadline());
        assertEquals(Priority.LOW, restored.getPriority());
        assertEquals("أكمل الملخص", restored.getDescription());
        assertEquals(TaskStatus.COMPLETED, restored.getStatus());

        secondRun.deleteTask(restored.getId());
        TaskModel thirdRun = new TaskModel(store);
        assertTrue(thirdRun.getTasks().isEmpty());
        assertTrue(thirdRun.addTask("Another", "CS", DEADLINE, Priority.MEDIUM, "").getId() > created.getId());
    }

    @Test void invalidFileIsPreservedAndDoesNotStartAnEmptyModel() throws IOException {
        Path file = directory.resolve("tasks.xml");
        String invalid = "not valid XML";
        Files.writeString(file, invalid);
        assertThrows(IOException.class, () -> new TaskModel(new TaskStore(file)));
        assertEquals(invalid, Files.readString(file));
    }

    @Test void failedSaveLeavesModelAndObserversUnchanged() throws IOException {
        Path parentFile = directory.resolve("not-a-directory");
        Files.writeString(parentFile, "keep this file");
        TaskModel model = new TaskModel(new TaskStore(parentFile.resolve("tasks.xml")));
        AtomicInteger notifications = new AtomicInteger();
        model.addListener(notifications::incrementAndGet);
        assertThrows(StorageException.class,
            () -> model.addTask("Unsaved", "CS", DEADLINE, Priority.HIGH, ""));
        assertTrue(model.getTasks().isEmpty());
        assertEquals(0, notifications.get());
        assertEquals("keep this file", Files.readString(parentFile));
    }
}
