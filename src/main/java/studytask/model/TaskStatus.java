package studytask.model;

public enum TaskStatus {
    PENDING("Pending"), COMPLETED("Completed");
    private final String label;
    TaskStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
