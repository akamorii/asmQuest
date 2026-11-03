package asmquest.vm;

public final class AsmException extends Exception {
    private final int line;

    public AsmException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
