package studytask.model;

/** Observer of committed task collection changes. Callbacks run synchronously. */
@FunctionalInterface
public interface TaskListener {
    void tasksChanged();
}
