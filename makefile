TARGET_DIR := target
SOURCES    := $(shell find src -name "*.java")
# Trick: track a single timestamp file
TIMESTAMP  := $(TARGET_DIR)/.compiled

run: $(TIMESTAMP)
	@java -cp $(TARGET_DIR) Main

# Rebuild only if any source is newer than timestamp
$(TIMESTAMP): $(SOURCES) | $(TARGET_DIR)
	@javac -sourcepath src -d $(TARGET_DIR) $(SOURCES)
	@touch $@

$(TARGET_DIR):
	mkdir -p $@

clean:
	rm -rf $(TARGET_DIR)