package studytask.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TaskModelTest {
    private final LocalDate date = LocalDate.of(2026, 11, 1);
    private Task add(TaskModel model) { return model.addTask(" Assignment ", " SE-4111 ", date, Priority.HIGH, "Read the brief"); }

    @Test void addingNormalizesAndAllocatesIdsWithoutReusingDeletedIds() {
        TaskModel model = new TaskModel(); Task first = add(model);
        assertEquals("Assignment", first.getTitle()); assertEquals("SE-4111", first.getCourse());
        assertEquals(TaskStatus.PENDING, first.getStatus()); assertTrue(first.getId() > 0);
        model.deleteTask(first.getId()); Task second = add(model);
        assertTrue(second.getId() > first.getId()); assertEquals(List.of(second), model.getTasks());
    }

    @Test void invalidConstructionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Task(0,"A","B",date,Priority.LOW,"",TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1," ","B",date,Priority.LOW,"",TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1,"A",null,date,Priority.LOW,"",TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1,"A","B",null,Priority.LOW,"",TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1,"A","B",date,null,"",TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1,"A","B",date,Priority.LOW,null,TaskStatus.PENDING));
        assertThrows(IllegalArgumentException.class, () -> new Task(1,"A","B",date,Priority.LOW,"",null));
    }

    @Test void overdueDatesAndEmptyDescriptionsAreValid() {
        Task task = new Task(1,"A","B",LocalDate.of(2000,1,1),Priority.LOW,"",TaskStatus.PENDING);
        assertEquals("",task.getDescription());
    }

    @Test void snapshotsAndOldVersionsStayUnchangedAfterEditing() {
        TaskModel model = new TaskModel(); Task original = add(model); List<Task> before = model.getTasks();
        model.setStatus(original.getId(),TaskStatus.COMPLETED);
        Task changed = model.updateTask(original.getId(),"New title","CS-321",date.plusDays(1),Priority.LOW,"New description");
        assertNotSame(original,changed); assertEquals(original.getId(),changed.getId());
        assertEquals(TaskStatus.COMPLETED,changed.getStatus()); assertEquals("Assignment",original.getTitle());
        assertEquals(TaskStatus.PENDING,before.getFirst().getStatus());
        assertThrows(UnsupportedOperationException.class, () -> before.clear());
    }

    @Test void equalityAndHashSetMembershipUseIdentityAcrossVersions() {
        TaskModel model = new TaskModel(); Task first = add(model);
        HashSet<Task> selected = new HashSet<>(); selected.add(first);
        Task updated = model.setPriority(first.getId(),Priority.LOW);
        assertEquals(first,updated); assertEquals(first.hashCode(),updated.hashCode());
        assertTrue(selected.contains(updated)); assertFalse(selected.add(updated));
        assertNotEquals(first,add(model)); assertNotEquals(first,null); assertNotEquals(first,"1");
    }

    @Test void statusAndPriorityPreserveUnrelatedFields() {
        TaskModel model = new TaskModel(); Task initial = add(model);
        for (Priority priority : Priority.values()) {
            Task updated = model.setPriority(initial.getId(),priority);
            assertEquals(initial.getTitle(),updated.getTitle()); assertEquals(initial.getDeadline(),updated.getDeadline());
            assertEquals(priority,updated.getPriority());
        }
        for (TaskStatus status : TaskStatus.values()) {
            Task updated = model.setStatus(initial.getId(),status);
            assertEquals(status,updated.getStatus()); assertEquals(initial.getCourse(),updated.getCourse());
        }
    }

    @Test void observersSeeCommittedStateExactlyOnceAndCanUnsubscribe() {
        TaskModel model = new TaskModel(); AtomicInteger calls = new AtomicInteger();
        TaskListener listener = calls::incrementAndGet;
        model.addListener(listener); model.addListener(listener);
        Task task = add(model); assertEquals(1,calls.get());
        model.setStatus(task.getId(),TaskStatus.COMPLETED); assertEquals(2,calls.get());
        model.setStatus(task.getId(),TaskStatus.COMPLETED); assertEquals(2,calls.get());
        model.updateTask(task.getId(),task.getTitle(),task.getCourse(),date,task.getPriority(),task.getDescription());
        assertEquals(2,calls.get());
        model.removeListener(listener); model.deleteTask(task.getId()); assertEquals(2,calls.get());
        assertTrue(model.getTasks().isEmpty());
    }

    @Test void failedChangesDoNotMutateOrNotifyOrConsumeIds() {
        TaskModel model = new TaskModel(); AtomicInteger calls = new AtomicInteger(); model.addListener(calls::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> model.addTask("", "CS",date,Priority.HIGH,""));
        Task task = add(model); assertEquals(1,task.getId());
        assertThrows(IllegalArgumentException.class, () -> model.updateTask(task.getId(),"",task.getCourse(),date,Priority.HIGH,""));
        assertThrows(IllegalArgumentException.class, () -> model.setPriority(task.getId(),null));
        assertThrows(IllegalArgumentException.class, () -> model.deleteTask(999));
        assertThrows(IllegalArgumentException.class, () -> model.setStatus(999,TaskStatus.COMPLETED));
        assertSame(task,model.getTask(task.getId())); assertEquals(1,calls.get());
    }

    @Test void notificationUsesListenerSnapshot() {
        TaskModel model = new TaskModel(); AtomicInteger calls = new AtomicInteger();
        TaskListener[] selfRemoving = new TaskListener[1];
        selfRemoving[0] = () -> { assertFalse(model.getTasks().isEmpty()); calls.incrementAndGet(); model.removeListener(selfRemoving[0]); };
        model.addListener(selfRemoving[0]); add(model); add(model); assertEquals(1,calls.get());
    }
}
