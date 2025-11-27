# Result Type - Complete Usage Guide

## What is Result?

`Result<T, E>` is a type-safe way to handle operations that can fail. Instead of throwing exceptions or returning null, methods return either:
- `Ok(value)` - Success with a value of type `T`
- `Err(error)` - Failure with an error of type `E`

## Why Use Result?

 **Explicit error handling** - Errors are part of the type signature  
 **No null checks** - Type system guarantees a value or error  
 **Composable** - Chain operations easily with `map` and `flatMap`  
 **Safe** - Compile-time checks force you to handle errors  

---

## 1. Creating Results

### Basic Factory Methods

```java
public static Result<Integer, String> divide(int a, int b) {
    if (b == 0) {
        return Result.err("Division by zero");
    }
    return Result.ok(a / b);
}

Result<Integer, String> success = divide(10, 2);  // Ok(5)
Result<Integer, String> failure = divide(10, 0);  // Err("Division by zero")
```

### Checking Result State

```java
if (result.isOk()) {
    System.out.println("Success!");
}

if (result.isErr()) {
    System.out.println("Failed!");
}
```

---

## 2. Extracting Values Safely

###  Dangerous: `unwrap()`
Throws exception if error - **only use when you're certain it's Ok**

```java
Result<Integer, String> success = divide(10, 2);
int value = success.unwrap();  // Returns 5

Result<Integer, String> failure = divide(10, 0);
int bad = failure.unwrap();  // Throws IllegalStateException!
```

###  Safe: `orElse()`
Provides a default value for errors

```java
Result<Integer, String> failure = divide(10, 0);
int safe = failure.orElse(0);  // Returns 0 (default)
```

###  Lazy: `orElseGet()`
Computes default only if needed

```java
int lazy = failure.orElseGet(() -> {
    System.out.println("Computing default...");
    return computeExpensiveDefault();
});
```

###  With Custom Exception: `orElseThrow()`

```java
try {
    int value = result.orElseThrow(
        error -> new IllegalArgumentException("Math error: " + error)
    );
} catch (IllegalArgumentException e) {
    System.err.println(e.getMessage());
}
```

---

## 3. Transforming Values with `map()`

Transform the success value while preserving errors

```java
Result<Integer, String> result = divide(10, 2);  // Ok(5)

// Transform to string
Result<String, String> str = result.map(n -> "Result: " + n);
// Ok("Result: 5")

// Double the value
Result<Integer, String> doubled = result.map(n -> n * 2);
// Ok(10)

// Map on error does nothing
Result<Integer, String> error = divide(10, 0);
Result<String, String> still = error.map(n -> "Result: " + n);
// Still Err("Division by zero")
```

**Key Point:** `map()` only runs if the Result is Ok. Errors pass through unchanged.

---

## 4. Chaining Operations with `flatMap()`

Use `flatMap` when your operation returns another `Result`

```java
public static Result<User, String> findUser(int id) {
    if (id == 1) {
        return Result.ok(new User(1, "Alice", "alice@example.com"));
    }
    return Result.err("User not found");
}

public static Result<String, String> getEmail(User user) {
    if (user.email != null && !user.email.isEmpty()) {
        return Result.ok(user.email);
    }
    return Result.err("Email not available");
}

// Chain multiple operations
Result<String, String> email = findUser(1)
    .flatMap(user -> getEmail(user))    // Returns Result<String, String>
    .map(String::toLowerCase);          // Returns Result<String, String>

System.out.println(email.unwrap());  // "alice@example.com"
```

### Short-Circuit on Error

```java
// If findUser fails, getEmail and map never run
Result<String, String> notFound = findUser(999)
    .flatMap(user -> getEmail(user))
    .map(String::toLowerCase);

System.out.println(notFound.isErr());  // true
```

**Rule:** Use `map` when function returns `T`, use `flatMap` when function returns `Result<T, E>`

---

## 5. Pattern Matching

### Match with Return Value

```java
Result<Integer, String> result = divide(10, 2);

String message = result.match(
    value -> "Success: " + value,
    error -> "Error: " + error
);
System.out.println(message);  // "Success: 5"
```

### Match with Side Effects

```java
result.match(
    value -> System.out.println("Got value: " + value),
    error -> System.err.println("Got error: " + error)
);
```

**Key Point:** `match()` forces you to handle both Ok and Err cases.

**or use switch case**
```java
        switch (result) {
            case Result.Ok(User value) -> System.out.println("Got value: " + value);
            case Result.Err(String value) -> System.out.println("Got error: " + error);
        }
```

---

## 6. Side Effects with `peek()`

Run code without changing the Result

```java
Result<Integer, String> result = divide(10, 2)
    .peek(value -> System.out.println("Success: " + value))
    .peekErr(error -> System.err.println("Error: " + error))
    .map(n -> n * 2);

// Peek runs but doesn't modify the result
// Final result is Ok(10)
```

**Use Case:** Logging, debugging, or side effects while maintaining the chain.

---

## 7. Real-World Example: File Processing

```java
public static Result<String, String> readFile(String path) {
    try {
        String content = Files.readString(Path.of(path));
        return Result.ok(content);
    } catch (IOException e) {
        return Result.err("Failed to read file: " + e.getMessage());
    }
}

public static Result<Integer, String> parseNumber(String text) {
    try {
        return Result.ok(Integer.parseInt(text.trim()));
    } catch (NumberFormatException e) {
        return Result.err("Invalid number format");
    }
}

// Chain file operations
Result<Integer, String> result = readFile("config.txt")
    .flatMap(content -> parseNumber(content))
    .map(number -> number * 2)
    .filter(n -> n > 0, "Number must be positive");

result.match(
    value -> System.out.println("Processed value: " + value),
    error -> System.err.println("Processing failed: " + error)
);
```

**Advantages:**
- No try-catch blocks in calling code
- Clear error propagation
- Type-safe error handling
- Easy to compose operations

---

## 8. Filtering Results

```java
Result<Integer, String> age = Result.ok(25)
    .filter(a -> a >= 18, "Must be adult");
// Ok(25)

Result<Integer, String> tooYoung = Result.ok(15)
    .filter(a -> a >= 18, "Must be adult");
// Err("Must be adult")
```

---

## 9. Combining Multiple Results

### `and()` - Both Must Succeed

```java
Result<Integer, String> ageResult = validateAge(25);
Result<String, String> nameResult = validateName("Alice");

// Only Ok if both are Ok
Result<String, String> combined = ageResult.and(nameResult);
```

### `or()` - First Success Wins

```java
Result<Integer, String> age = validateAge(-5)
    .or(Result.ok(0));  // Fallback to 0 if validation fails
```

---

## 10. Converting to Optional

When you don't care about the error details

```java
Result<Integer, String> result = divide(10, 2);
Optional<Integer> optional = result.toOptional();
optional.ifPresent(System.out::println);  // 5

// Or get the error as Optional
Result<Integer, String> error = divide(10, 0);
Optional<String> errorOpt = error.toOptionalErr();
errorOpt.ifPresent(System.err::println);  // "Division by zero"
```

---

## Best Practices

###  DO

```java
// Use Result for operations that can fail
Result<User, DatabaseError> findUser(int id)

// Chain operations
user.flatMap(u -> validateUser(u))
    .flatMap(u -> saveUser(u))
    .map(u -> u.id)

// Handle both cases explicitly
result.match(
    value -> process(value),
    error -> logError(error)
)

// Use safe extraction
int value = result.orElse(0)
```

### DON'T

```java
// Don't unwrap without checking
result.unwrap()  // Might throw!

// Don't ignore errors
result.toOptional()  // Loses error information

// Don't nest Results
Result<Result<T, E>, E>  // Use flatMap instead

// Don't use for control flow
if (result.isOk()) {
    result.unwrap()  // Just use orElse or match
}
```

---

## Quick Reference

| Method | When to Use | Returns |
|--------|-------------|---------|
| `ok(value)` | Create success | `Result<T, E>` |
| `err(error)` | Create failure | `Result<T, E>` |
| `isOk()` | Check if success | `boolean` |
| `isErr()` | Check if failure | `boolean` |
| `unwrap()` | Get value (unsafe) | `T` or throws |
| `orElse(default)` | Get value safely | `T` |
| `orElseGet(supplier)` | Lazy default | `T` |
| `orElseThrow(mapper)` | Convert to exception | `T` or throws |
| `map(fn)` | Transform value | `Result<U, E>` |
| `flatMap(fn)` | Chain Results | `Result<U, E>` |
| `mapErr(fn)` | Transform error | `Result<T, F>` |
| `match(okFn, errFn)` | Handle both cases | `R` |
| `peek(fn)` | Side effect on Ok | `Result<T, E>` |
| `filter(predicate, err)` | Validate value | `Result<T, E>` |
| `and(other)` | Combine (both must succeed) | `Result<U, E>` |
| `or(other)` | Fallback | `Result<T, E>` |
| `toOptional()` | Convert to Optional | `Optional<T>` |

---

## Comparison with Traditional Approach

### Traditional (Exceptions)

```java
try {
    User user = findUser(id);
    String email = getEmail(user);
    String lower = email.toLowerCase();
    return lower;
} catch (UserNotFoundException e) {
    return "default@example.com";
} catch (EmailException e) {
    return "default@example.com";
}
```

### With Result

```java
return findUser(id)
    .flatMap(user -> getEmail(user))
    .map(String::toLowerCase)
    .orElse("default@example.com");
```

**Result is:**
- More concise
- Type-safe
- Easier to compose