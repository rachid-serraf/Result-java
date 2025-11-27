package utils.result;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import utils.result.annotations.IfError;
import utils.result.annotations.Log;
import utils.result.logic.LoggerLogic;

/**
 * <h2>Result&lt;T, E&gt; – A type-safe, functional alternative to exceptions
 * and nulls</h2>
 *
 * <p>
 * Inspired by Rust's {@code Result<T, E>} and Haskell's {@code Either}, this
 * sealed interface
 * forces explicit handling of success ({@link Ok}) and failure ({@link Err})
 * cases,
 * eliminating unchecked exceptions and surprise {@code NullPointerException}s.
 * </p>
 *
 * <p>
 * Use {@code Result} whenever a computation can fail in a meaningful way:
 * </p>
 * <ul>
 * <li>IO operations (file, network, database)</li>
 * <li>Parsing / validation</li>
 * <li>External API calls</li>
 * <li>Any method that currently throws checked exceptions or returns
 * {@code null}</li>
 * </ul>
 *
 * <pre>{@code
 * ```
 * Result<User, ApiError> result = UserRepository.findById(id);
 *
 * // Functional, composable style
 * String greeting = result
 *         .map(User::name)
 *         .map(name -> "Hello " + name)
 *         .orElse("Hello Guest");
 *
 * // Pattern-matching style (cleanest)
 * String message = result.match(
 *         user -> "Welcome back, " + user.name(),
 *         err -> "Login failed: " + err.message());
 * }</pre>
 *
 * <p>
 * <strong>Never use {@code unwrap()} in production code</strong> – it defeats
 * the purpose.
 * Prefer {@code map}, {@code flatMap}, {@code match}, or {@code orElse*}.
 * </p>
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error value (usually an exception or custom error
 *            type)
 *
 * @since 1.0
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {
    /**
     * Success variant – contains a non-null value of type {@code T}.
     */
    record Ok<T, E>(T value) implements Result<T, E> {
        /**
         * Prevents {@code null} values – use {@code Err} instead of {@code Ok(null)}.
         */
        public Ok {
            Objects.requireNonNull(value, "Ok value cannot be null");
            checkAnnotation("Ok(" + value + ")");
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
    }

    /**
     * Failure variant – contains a non-null error of type {@code E}.
     */
    record Err<T, E>(E error) implements Result<T, E> {
        /** Prevents {@code null} errors – forces meaningful error objects. */
        public Err {
            Objects.requireNonNull(error, "Error value cannot be null");
            checkAnnotation("Err(" + error + ")");
        }

        @Override
        public String toString() {
            return "Err(" + error + ")";
        }
    }

    default void checkAnnotation(String value) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        walker.forEach(frame -> {
            Class<?> declaringClass = frame.getDeclaringClass();
            String methodName = frame.getMethodName();

            if ("<init>".equals(methodName))
                return;

            try {
                Method method = findMethod(declaringClass, methodName, frame);
                if (method != null) {
                    Log log = method.getAnnotation(Log.class);
                    IfError ifErr = method.getAnnotation(IfError.class);
                    if (log != null) {
                        LoggerLogic.print(log, frame, value);
                    }
                    if (ifErr != null && value.startsWith("Err")) {
                        Method methocall = findMethod(declaringClass, ifErr.value(), frame);
                        methocall.invoke(null);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private Method findMethod(Class<?> clazz, String name, StackFrame frame) {
        var ms = clazz.getDeclaredMethods();
        for (var i = 0; i < ms.length; i++) {
            Method m = ms[i];
            if (m.getName().equals(name)) {
                if (m.getParameterCount() == frame.getMethodType().parameterCount()) {
                    return m;
                }
            }
        }
        return null;
    }

    // ======================================================================
    // Factory methods
    // ======================================================================

    /**
     * Creates a successful {@code Result}.
     *
     * @param value the success value (must not be {@code null})
     * @param <T>   success type
     * @param <E>   error type
     * @return {@code Ok(value)}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    /**
     * Creates a failed {@code Result}.
     *
     * @param error the error value (must not be {@code null})
     * @param <T>   success type
     * @param <E>   error type
     * @return {@code Err(error)}
     * @throws NullPointerException if {@code error} is {@code null}
     */
    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    // ======================================================================
    // State checking
    // ======================================================================

    /** @return {@code true} if this is {@code Ok} */
    default boolean isOk() {
        return this instanceof Ok;
    }

    /** @return {@code true} if this is {@code Err} */
    default boolean isErr() {
        return this instanceof Err;
    }

    // ======================================================================
    // Value extraction (use sparingly)
    // ======================================================================

    /**
     * Returns the success value or throws.
     *
     * <p>
     * <strong>Only use in tests or when you are 100% sure it's {@code Ok}.</strong>
     * </p>
     *
     * @return the contained value
     * @throws IllegalStateException if this is {@code Err}
     */
    default T unwrap() {
        return switch (this) {
            case Ok(var value) -> value;
            case Err(var error) ->
                throw new IllegalStateException(
                        "Called unwrap() on an Err value: " + error);
        };
    }

    /**
     * Returns the error value or throws.
     *
     * @return the contained error
     * @throws IllegalStateException if this is {@code Ok}
     */
    default E unwrapErr() {
        return switch (this) {
            case Ok(var value) -> throw new IllegalStateException(
                    "Called unwrapErr() on an Ok value: " + value);
            case Err(var error) -> error;
        };
    }

    /**
     * Returns the value if {@code Ok}, otherwise {@code defaultValue}.
     *
     * @param defaultValue fallback value
     * @return value or fallback
     */
    default T orElse(T defaultValue) {
        return switch (this) {
            case Ok(var value) -> value;
            case Err(var error) -> defaultValue;
        };
    }

    /**
     * Returns the value if {@code Ok}, otherwise the result of {@code supplier}.
     *
     * @param supplier lazy fallback
     * @return value or supplied fallback
     */
    default T orElseGet(Supplier<? extends T> supplier) {
        return switch (this) {
            case Ok(var value) -> value;
            case Err(var error) -> supplier.get();
        };
    }

    /**
     * Returns the value or throws a {@code RuntimeException}.
     *
     * @return the value
     * @throws RuntimeException wrapping the error
     */
    default T orElseThrow() {
        return switch (this) {
            case Ok(var value) -> value;
            case Err(var error) -> throw new RuntimeException(
                    "Result contained error: " + error);
        };
    }

    /**
     * Returns the value or throws a custom exception created from the error.
     *
     * @param <X>             exception type
     * @param exceptionMapper maps {@code E} → {@code X}
     * @return the value
     * @throws X if this is {@code Err}
     */
    default <X extends Throwable> T orElseThrow(
            Function<? super E, ? extends X> exceptionMapper) throws X {
        return switch (this) {
            case Ok(var value) -> value;
            case Err(var error) -> throw exceptionMapper.apply(error);
        };
    }

    // ======================================================================
    // Transformations
    // ======================================================================

    /**
     * Applies {@code mapper} to the success value if present.
     * Errors are propagated unchanged.
     *
     * @param mapper function {@code T → U}
     * @param <U>    new success type
     * @return new {@code Result<U, E>}
     */
    default <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Ok(var value) -> new Ok<>(mapper.apply(value));
            case Err(var error) -> new Err<>(error);
        };
    }

    /**
     * Applies {@code mapper} to the error value if present.
     * Success values are propagated unchanged.
     *
     * @param mapper function {@code E → F}
     * @param <F>    new error type
     * @return new {@code Result<T, F>}
     */
    default <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
        return switch (this) {
            case Ok(var value) -> new Ok<>(value);
            case Err(var error) -> new Err<>(mapper.apply(error));
        };
    }

    /**
     * Monadic bind – chains operations that return {@code Result}.
     *
     * @param mapper function {@code T → Result<U, E>}
     * @param <U>    new success type
     * @return mapped result (error short-circuits)
     */
    default <U> Result<U, E> flatMap(
            Function<? super T, ? extends Result<U, E>> mapper) {
        return switch (this) {
            case Ok(var value) -> mapper.apply(value);
            case Err(var error) -> new Err<>(error);
        };
    }

    // ======================================================================
    // Side effects
    // ======================================================================

    /**
     * Executes {@code consumer} if {@code Ok}. Returns {@code this} for chaining.
     *
     * @param consumer side-effect on success value
     * @return this
     */
    default Result<T, E> ifOk(Consumer<? super T> consumer) {
        if (this instanceof Ok(var value)) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Executes {@code consumer} if {@code Err}. Returns {@code this} for chaining.
     *
     * @param consumer side-effect on error
     * @return this
     */
    default Result<T, E> ifErr(Consumer<? super E> consumer) {
        if (this instanceof Err(var error)) {
            consumer.accept(error);
        }
        return this;
    }

    /** Alias for {@link #ifOk}. */
    default Result<T, E> peek(Consumer<? super T> consumer) {
        return ifOk(consumer);
    }

    /** Alias for {@link #ifErr}. */
    default Result<T, E> peekErr(Consumer<? super E> consumer) {
        return ifErr(consumer);
    }

    // ======================================================================
    // Pattern matching (the cleanest way to handle Result)
    // ======================================================================

    /**
     * <strong>Preferred way</strong> to consume a {@code Result} – eliminates
     * branching.
     *
     * @param onOK  maps success to {@code R}
     * @param onErr maps error to {@code R}
     * @param <R>   common return type
     * @return unified value
     */
    default <R> R match(
            Function<? super T, ? extends R> onOK,
            Function<? super E, ? extends R> onErr) {
        return switch (this) {
            case Ok(var value) -> onOK.apply(value);
            case Err(var error) -> onErr.apply(error);
        };
    }

    /**
     * Void version for side effects only (logging, metrics, etc.).
     *
     * @param okConsumer  action on success
     * @param errConsumer action on error
     */
    default void match(
            Consumer<? super T> okConsumer,
            Consumer<? super E> errConsumer) {
        switch (this) {
            case Ok(var value) -> okConsumer.accept(value);
            case Err(var error) -> errConsumer.accept(error);
        }
    }

    // ======================================================================
    // Conversions
    // ======================================================================

    /** Converts to {@code Optional<T>}, discarding the error. */
    default Optional<T> toOptional() {
        return switch (this) {
            case Ok(var value) -> Optional.of(value);
            case Err(var error) -> Optional.empty();
        };
    }

    /** Converts to {@code Optional<E>}, discarding the success value. */
    default Optional<E> toOptionalErr() {
        return switch (this) {
            case Ok(var value) -> Optional.empty();
            case Err(var error) -> Optional.of(error);
        };
    }

    // ======================================================================
    // Utility methods
    // ======================================================================

    /**
     * Validates the success value. If predicate fails → {@code Err}.
     * Lazy error creation via {@code Supplier} (recommended).
     *
     * @param predicate     condition that must hold
     * @param errorSupplier supplies error only if needed
     * @return this if {@code Ok} and predicate true, otherwise new {@code Err}
     */
    default Result<T, E> filter(
            Predicate<? super T> predicate,
            Supplier<? extends E> errorSupplier) {
        return switch (this) {
            case Ok(var value) -> predicate.test(value)
                    ? this
                    : new Err<>(errorSupplier.get());
            case Err(var error) -> this;
        };
    }

    // ======================================================================
    // Combining results
    // ======================================================================

    /**
     * Returns {@code other} if this is {@code Ok}, otherwise propagates error.
     * Useful for sequencing independent operations.
     *
     * @param other next result (ignored if this is {@code Err})
     * @param <U>   new success type
     * @return {@code other} or this error
     */
    default <U> Result<U, E> and(Result<U, E> other) {
        return switch (this) {
            case Ok(var value) -> other;
            case Err(var error) -> new Err<>(error);
        };
    }

    /**
     * Returns this if {@code Ok}, otherwise {@code other}.
     * Like fallback / default result.
     *
     * @param other fallback result
     * @return this or fallback
     */
    default Result<T, E> or(Result<T, E> other) {
        return switch (this) {
            case Ok(var value) -> this;
            case Err(var error) -> other;
        };
    }
}