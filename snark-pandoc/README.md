## Examples
### Simple converting
Convert from INPUT_FILE to OUTPUT_FILE:
```java
PandocWrapper wrapper = new PandocWrapper();
wrapper.use(p -> {
        var command = new PandocCommandBuilder(List.of(INPUT_FILE), OUTPUT_FILE);
        PandocWrapper.execute(command);
        });
```
Equal to:
```
pandoc --output=OUTPUT_FILE INPUT_FILE
```
### Convert and set formats
Convert from INPUT_FILE to OUTPUT_FILE and set INPUT_FORMAT and OUTPUT_FORMAT:
```java
PandocWrapper wrapper = new PandocWrapper();
wrapper.use(p -> {
        var command = new PandocCommandBuilder(List.of(INPUT_FILE), OUTPUT_FILE);
        command.formatForm(INPUT_FORMAT);
        command.formatTo(OUTPUT_FORMAT);
        PandocWrapper.execute(command);
        });
```
Equal to:
```
pandoc --output=OUTPUT_FILE --from=INPUT_FORMAT --to=OUTPUT_FORMAT INPUT_FILE
```
### Converting with options
Convert from INPUT_FILE to standalone OUTPUT_FILE and set variable KEY to VALUE :
```java
PandocWrapper wrapper = new PandocWrapper();
wrapper.use(p -> {
        var command = new PandocCommandBuilder(List.of(INPUT_FILE), OUTPUT_FILE);
        command.standalone();
        command.setVariable(KEY, VALUE);
        PandocWrapper.execute(command);
        });
```
Equal to:
```
pandoc --output=OUTPUT_FILE --standalone --variable=KEY:VALUE INPUT_FILE
```

### Write output from pandoc to file
Receive possible input formats in OUTPUT_FILE:
```java
PandocWrapper wrapper = new PandocWrapper();
wrapper.use(p -> {
        var command = new PandocCommandBuilder();
        command.getInputFormats();
        PandocWrapper.execute(command, OUTPUT_FILE);
        });
```
Then in OUTPUT_FILE will be a list supported input formats, one per line.

### Write errors from pandoc to file
Receive all from error stream and exit code in ERROR_FILE and output in OUTPUT_FILE:
```java
PandocWrapper wrapper = new PandocWrapper();
wrapper.use(p -> {
        var command = new PandocCommandBuilder(List.of(INPUT_FILE), OUTPUT_FILE);
        PandocWrapper.execute(command, OUTPUT_FILE, ERROR_FILE);
        });
```