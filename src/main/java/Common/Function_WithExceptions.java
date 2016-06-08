package Common;

@FunctionalInterface
public interface Function_WithExceptions<T, R, E extends Throwable> {
    R apply(T t) throws E;
}