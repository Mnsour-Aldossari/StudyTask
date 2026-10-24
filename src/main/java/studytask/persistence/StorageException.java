package studytask.persistence;

/** The requested task change was not committed because disk storage failed. */
public final class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
