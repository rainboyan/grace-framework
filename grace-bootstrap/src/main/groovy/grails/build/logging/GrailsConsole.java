/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.build.logging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import jline.Terminal;
import jline.TerminalFactory;
import jline.UnixTerminal;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.internal.Log;
import jline.internal.ShutdownHooks;
import jline.internal.TerminalLineSettings;
import org.apache.tools.ant.BuildException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.springframework.util.ReflectionUtils;

import grails.util.Environment;

import org.grails.build.interactive.CandidateListCompletionHandler;
import org.grails.build.logging.GrailsConsoleErrorPrintStream;
import org.grails.build.logging.GrailsConsolePrintStream;

/**
 * Utility class for delivering console output in a nicely formatted way.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsole implements ConsoleLogger {

    private static GrailsConsole instance;

    public static final String ENABLE_TERMINAL = "grails.console.enable.terminal";

    public static final String ENABLE_INTERACTIVE = "grails.console.enable.interactive";

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static final String CATEGORY_SEPARATOR = "|";

    public static final String PROMPT = "grace> ";

    public static final String SPACE = " ";

    public static final String ERROR = "Error";

    public static final String WARNING = "Warning";

    public static final String HISTORYFILE = ".grails_history";

    public static final String STACKTRACE_FILTERED_MESSAGE = " (NOTE: Stack trace has been filtered. Use --verbose to see entire trace.)";

    public static final String STACKTRACE_MESSAGE = " (Use --stacktrace to see the full trace)";

    public static final Character SECURE_MASK_CHAR = '*';

    private PrintStream originalSystemOut;

    private PrintStream originalSystemErr;

    private StringBuilder maxIndicatorString;

    private int cursorMove;

    private Thread shutdownHookThread;

    private Character defaultInputMask = null;

    /**
     * Whether to enable verbose mode
     */
    private boolean verbose = Boolean.getBoolean("grails.verbose");

    /**
     * Whether to show stack traces
     */
    private boolean stacktrace = Boolean.getBoolean("grails.show.stacktrace");

    private boolean progressIndicatorActive = false;

    /**
     * The progress indicator to use
     */
    String indicator = ".";

    /**
     * The last message that was printed
     */
    String lastMessage = "";

    Ansi lastStatus = null;

    /**
     * The reader to read info from the console
     */
    ConsoleReader reader;

    Terminal terminal;

    PrintStream out;

    PrintStream err;

    History history;

    /**
     * The category of the current output
     */
    Stack<String> category = new Stack<String>() {

        @Override
        public String toString() {
            if (size() == 1) {
                return peek() + CATEGORY_SEPARATOR;
            }
            return DefaultGroovyMethods.join((Iterable) this, CATEGORY_SEPARATOR) + CATEGORY_SEPARATOR;
        }

    };

    /**
     * Whether ANSI should be enabled for output
     */
    private boolean ansiEnabled = true;

    /**
     * Whether user input is currently active
     */
    private boolean userInputActive;

    public void addShutdownHook() {
        if (!Environment.isFork()) {
            this.shutdownHookThread = new Thread(this::beforeShutdown);
            Runtime.getRuntime().addShutdownHook(this.shutdownHookThread);
        }
    }

    public void removeShutdownHook() {
        if (this.shutdownHookThread != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHookThread);
        }
    }

    protected GrailsConsole() throws IOException {
        this.cursorMove = 1;

        initialize(System.in, System.out, System.err);

        // bit of a WTF this, but see no other way to allow a customization indicator
        this.maxIndicatorString = new StringBuilder(this.indicator)
                .append(this.indicator).append(this.indicator).append(this.indicator).append(this.indicator);
    }

    /**
     * Use in testing when System.out, System.err or System.in change
     * @throws IOException
     */
    public void reinitialize(InputStream systemIn, PrintStream systemOut, PrintStream systemErr) throws IOException {
        if (this.reader != null) {
            this.reader.close();
        }
        initialize(systemIn, systemOut, systemErr);
    }

    protected void initialize(InputStream systemIn, PrintStream systemOut, PrintStream systemErr) throws IOException {
        bindSystemOutAndErr(systemOut, systemErr);

        redirectSystemOutAndErr(true);

        System.setProperty(ShutdownHooks.JLINE_SHUTDOWNHOOK, "false");

        if (isInteractiveEnabled()) {
            this.reader = createConsoleReader(systemIn);
            this.reader.setBellEnabled(false);
            this.reader.setCompletionHandler(new CandidateListCompletionHandler());
            if (isActivateTerminal()) {
                this.terminal = createTerminal();
            }

            this.history = prepareHistory();
            if (this.history != null) {
                this.reader.setHistory(this.history);
            }
        }
        else if (isActivateTerminal()) {
            this.terminal = createTerminal();
        }
    }

    protected void bindSystemOutAndErr(PrintStream systemOut, PrintStream systemErr) {
        this.originalSystemOut = unwrapPrintStream(systemOut);
        this.out = wrapInPrintStream(this.originalSystemOut);
        this.originalSystemErr = unwrapPrintStream(systemErr);
        this.err = wrapInPrintStream(this.originalSystemErr);
    }

    private PrintStream unwrapPrintStream(PrintStream printStream) {
        if (printStream instanceof GrailsConsolePrintStream) {
            return ((GrailsConsolePrintStream) printStream).getTargetOut();
        }
        if (printStream instanceof GrailsConsoleErrorPrintStream) {
            return ((GrailsConsoleErrorPrintStream) printStream).getTargetOut();
        }
        return printStream;
    }

    private PrintStream wrapInPrintStream(PrintStream printStream) {
        OutputStream ansiWrapped = ansiWrap(printStream);
        if (ansiWrapped instanceof PrintStream) {
            return (PrintStream) ansiWrapped;
        }
        else {
            return new PrintStream(ansiWrapped, true);
        }
    }

    public PrintStream getErr() {
        return this.err;
    }

    public void setErr(PrintStream err) {
        this.err = err;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public boolean isInteractiveEnabled() {
        return readPropOrTrue(ENABLE_INTERACTIVE);
    }

    private boolean isActivateTerminal() {
        return readPropOrTrue(ENABLE_TERMINAL);
    }

    private boolean readPropOrTrue(String prop) {
        String property = System.getProperty(prop);
        return property == null || Boolean.parseBoolean(property);
    }

    protected ConsoleReader createConsoleReader(InputStream systemIn) throws IOException {
        // need to swap out the output to avoid logging during init
        PrintStream nullOutput = new PrintStream(new ByteArrayOutputStream());
        PrintStream originalOut = Log.getOutput();
        try {
            Log.setOutput(nullOutput);
            ConsoleReader consoleReader = new ConsoleReader(systemIn, this.out);
            consoleReader.setExpandEvents(false);
            return consoleReader;
        }
        finally {
            Log.setOutput(originalOut);
        }
    }

    /**
     * Creates the instance of Terminal used directly in GrailsConsole. Note that there is also
     * another terminal instance created implicitly inside of ConsoleReader. That instance
     * is controlled by the jline.terminal system property.
     */
    protected Terminal createTerminal() {
        this.terminal = TerminalFactory.create();
        if (isWindows()) {
            this.terminal.setEchoEnabled(true);
        }
        return this.terminal;
    }

    public void resetCompleters() {
        ConsoleReader reader = getReader();
        if (reader != null) {
            Collection<Completer> completers = reader.getCompleters();
            for (Completer completer : completers) {
                reader.removeCompleter(completer);
            }

            // for some unknown reason / bug in JLine you have to iterate over twice to clear the completers (WTF)
            completers = reader.getCompleters();
            for (Completer completer : completers) {
                reader.removeCompleter(completer);
            }
        }
    }

    /**
     * Prepares a history file to be used by the ConsoleReader. This file
     * will live in the home directory of the user.
     */
    protected History prepareHistory() throws IOException {
        File file = new File(System.getProperty("user.home"), HISTORYFILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException ignored) {
                // can't create the file, so no history for you
            }
        }
        return file.canWrite() ? new FileHistory(file) : null;
    }

    /**
     * Hook method that allows controlling whether or not output streams should be wrapped by
     * AnsiConsole.wrapOutputStream. Unfortunately, Eclipse consoles will look to the AnsiWrap
     * like they do not understand ansi, even if we were to implement support in Eclipse to'
     * handle it and the wrapped stream will not pass the ansi chars on to Eclipse).
     */
    protected OutputStream ansiWrap(OutputStream out) {
        return new PrintStream(out, true); //AnsiConsole.wrapOutputStream(out);
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static synchronized GrailsConsole getInstance() {
        if (instance == null) {
            try {
                GrailsConsole console = createInstance();
                console.addShutdownHook();
                setInstance(console);
            }
            catch (IOException e) {
                throw new RuntimeException("Cannot create grails console: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public static synchronized void removeInstance() {
        if (instance != null) {
            instance.removeShutdownHook();
            instance.restoreOriginalSystemOutAndErr();
            if (instance.getReader() != null) {
                instance.getReader().close();
            }
            instance = null;
        }
    }

    public void beforeShutdown() {
        persistHistory();
        restoreTerminal();
    }

    protected void restoreTerminal() {
        try {
            this.terminal.restore();
        }
        catch (Exception ignored) {
        }
        if (this.terminal instanceof UnixTerminal) {
            // workaround for GRAILS-11494
            try {
                TerminalLineSettings.getSettings(TerminalLineSettings.DEFAULT_TTY).set("sane");
            }
            catch (Exception ignored) {
            }
        }
    }

    protected void persistHistory() {
        if (this.history instanceof Flushable) {
            try {
                ((Flushable) this.history).flush();
            }
            catch (Throwable ignored) {
            }
        }
    }

    public static void setInstance(GrailsConsole newConsole) {
        instance = newConsole;
        instance.redirectSystemOutAndErr(false);
    }

    protected void redirectSystemOutAndErr(boolean force) {
        if (force || !(System.out instanceof GrailsConsolePrintStream)) {
            System.setOut(new GrailsConsolePrintStream(this.out));
        }
        if (force || !(System.err instanceof GrailsConsoleErrorPrintStream)) {
            System.setErr(new GrailsConsoleErrorPrintStream(this.err));
        }
    }

    @SuppressWarnings("unchecked")
    public static GrailsConsole createInstance() throws IOException {
        String className = System.getProperty("grails.console.class");
        if (className != null) {
            try {
                Class<? extends GrailsConsole> klass = (Class<? extends GrailsConsole>) Class.forName(className);
                return ReflectionUtils.accessibleConstructor(klass).newInstance();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new GrailsConsole();
    }

    public void setAnsiEnabled(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
    }

    /**
     * @param verbose Sets whether verbose output should be used
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            // enable big traces in verbose mode
            // note - can't use StackTraceFilterer#SYS_PROP_DISPLAY_FULL_STACKTRACE as it is in grails-core
            System.setProperty("grails.full.stacktrace", "true");
        }
        this.verbose = verbose;
    }

    /**
     * @param stacktrace Sets whether to show stack traces on errors
     */
    public void setStacktrace(boolean stacktrace) {
        this.stacktrace = stacktrace;
    }

    /**
     * @return Whether verbose output is being used
     */
    public boolean isVerbose() {
        return this.verbose;
    }

    /**
     *
     * @return Whether to show stack traces
     */
    public boolean isStacktrace() {
        return this.stacktrace;
    }

    /**
     * @return The input stream being read from
     */
    public InputStream getInput() {
        assertAllowInput();
        return this.reader.getInput();
    }

    private void assertAllowInput() {
        assertAllowInput(null);
    }

    private void assertAllowInput(String prompt) {
        if (this.reader == null) {
            String msg = "User input is not enabled, cannot obtain input stream";
            if (prompt != null) {
                msg = msg + " - while trying: " + prompt;
            }

            throw new IllegalStateException(msg);
        }
    }

    /**
     * @return The last message logged
     */
    public String getLastMessage() {
        return this.lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public ConsoleReader getReader() {
        return this.reader;
    }

    public Terminal getTerminal() {
        return this.terminal;
    }

    public PrintStream getOut() {
        return this.out;
    }

    public Stack<String> getCategory() {
        return this.category;
    }

    /**
     * Indicates progress with the default progress indicator
     */
    @Override
    public void indicateProgress() {
        verifySystemOut();
        this.progressIndicatorActive = true;
        if (isAnsiEnabled()) {
            if (this.lastMessage != null && this.lastMessage.length() > 0) {
                if (!this.lastMessage.contains(this.maxIndicatorString)) {
                    updateStatus(this.lastMessage + this.indicator);
                }
            }
        }
        else {
            this.out.print(this.indicator);
        }
    }

    /**
     * Indicate progress for a number and total
     *
     * @param number The current number
     * @param total  The total number
     */
    @Override
    public void indicateProgress(int number, int total) {
        this.progressIndicatorActive = true;
        String currMsg = this.lastMessage;
        try {
            updateStatus(currMsg + ' ' + number + " of " + total);
        }
        finally {
            this.lastMessage = currMsg;
        }
    }

    /**
     * Indicates progress as a percentage for the given number and total
     *
     * @param number The number
     * @param total  The total
     */
    @Override
    public void indicateProgressPercentage(long number, long total) {
        verifySystemOut();
        this.progressIndicatorActive = true;
        String currMsg = this.lastMessage;
        try {
            int percentage = Math.round(NumberMath.multiply(NumberMath.divide(number, total), 100).floatValue());

            if (!isAnsiEnabled()) {
                this.out.print("..");
                this.out.print(percentage + '%');
            }
            else {
                updateStatus(currMsg + ' ' + percentage + '%');
            }
        }
        finally {
            this.lastMessage = currMsg;
        }
    }

    /**
     * Indicates progress by number
     *
     * @param number The number
     */
    @Override
    public void indicateProgress(int number) {
        verifySystemOut();
        this.progressIndicatorActive = true;
        String currMsg = this.lastMessage;
        try {
            if (isAnsiEnabled()) {
                updateStatus(currMsg + ' ' + number);
            }
            else {
                this.out.print("..");
                this.out.print(number);
            }
        }
        finally {
            this.lastMessage = currMsg;
        }
    }

    /**
     * Updates the current state message
     *
     * @param msg The message
     */
    @Override
    public void updateStatus(String msg) {
        outputMessage(msg, 1);
    }

    private void outputMessage(String msg, int replaceCount) {
        verifySystemOut();
        if (msg == null || msg.trim().length() == 0) {
            return;
        }
        try {
            if (isAnsiEnabled()) {
                if (replaceCount > 0) {
                    this.out.print(erasePreviousLine(CATEGORY_SEPARATOR));
                }
                this.lastStatus = outputCategory(Ansi.ansi(), CATEGORY_SEPARATOR)
                        .fg(Color.DEFAULT).a(msg).reset();
                this.out.println(this.lastStatus);
                if (!this.userInputActive) {
                    this.cursorMove = replaceCount;
                }
            }
            else {
                if (this.lastMessage != null && this.lastMessage.equals(msg)) {
                    return;
                }

                if (this.progressIndicatorActive) {
                    this.out.println();
                }

                this.out.print(CATEGORY_SEPARATOR);
                this.out.println(msg);
            }
            this.lastMessage = msg;
        }
        finally {
            postPrintMessage();
        }
    }

    private Ansi moveDownToSkipPrompt() {
        return Ansi.ansi()
                .cursorDown(1)
                .cursorLeft(PROMPT.length());
    }

    private void postPrintMessage() {
        this.progressIndicatorActive = false;
        this.appendCalled = false;
        if (this.userInputActive) {
            showPrompt();
        }
    }

    /**
     * Keeps doesn't replace the status message
     *
     * @param msg The message
     */
    @Override
    public void addStatus(String msg) {
        outputMessage(msg, 0);
        this.lastMessage = "";
    }

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    @Override
    public void error(String msg) {
        error(ERROR, msg);
    }

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    @Override
    public void warning(String msg) {
        error(WARNING, msg);
    }

    /**
     * Prints a warn message
     *
     * @param msg The message
     */
    @Override
    public void warn(String msg) {
        warning(msg);
    }

    private void logSimpleError(String msg) {
        verifySystemOut();
        if (this.progressIndicatorActive) {
            this.out.println();
        }
        this.out.println(CATEGORY_SEPARATOR);
        this.out.println(msg);
    }

    public boolean isAnsiEnabled() {
        return Ansi.isEnabled() && (this.terminal != null && this.terminal.isAnsiSupported()) && this.ansiEnabled;
    }

    /**
     * Use to log an error
     *
     * @param msg The message
     * @param error The error
     */
    @Override
    public void error(String msg, Throwable error) {
        try {
            if ((this.verbose || this.stacktrace) && error != null) {
                printStackTrace(msg, error);
                error(ERROR, msg);
            }
            else {
                error(ERROR, msg + STACKTRACE_MESSAGE);
            }
        }
        finally {
            postPrintMessage();
        }
    }

    /**
     * Use to log an error
     *
     * @param error The error
     */
    @Override
    public void error(Throwable error) {
        printStackTrace(null, error);
    }

    private void printStackTrace(String message, Throwable error) {
        if ((error instanceof BuildException) && error.getCause() != null) {
            error = error.getCause();
        }
        if (!isVerbose() && !Boolean.getBoolean("grails.full.stacktrace")) {
            StackTraceUtils.deepSanitize(error);
        }

        StringWriter sw = new StringWriter();
        PrintWriter ps = new PrintWriter(sw);
        message = message == null ? error.getMessage() : message;
        if (!isVerbose()) {
            message = message + STACKTRACE_FILTERED_MESSAGE;
        }
        ps.println(message);
        error.printStackTrace(ps);
        error(sw.toString());
    }

    /**
     * Logs a message below the current status message
     *
     * @param msg The message to log
     */
    @Override
    public void log(String msg) {
        verifySystemOut();
        PrintStream printStream = this.out;
        try {
            if (this.userInputActive) {
                erasePrompt(printStream);
            }
            if (msg.endsWith(LINE_SEPARATOR)) {
                printStream.print(msg);
            }
            else {
                printStream.println(msg);
            }
            this.cursorMove = 0;
        }
        finally {
            printStream.flush();
            postPrintMessage();
        }
    }

    private void erasePrompt(PrintStream printStream) {
        printStream.print(Ansi.ansi()
                .eraseLine(Ansi.Erase.BACKWARD).cursorLeft(PROMPT.length()));
    }

    /**
     * Logs a message below the current status message
     *
     * @param msg The message to log
     */
    private boolean appendCalled = false;

    public void append(String msg) {
        verifySystemOut();
        PrintStream printStream = this.out;
        try {
            if (this.userInputActive && !this.appendCalled) {
                printStream.print(moveDownToSkipPrompt());
                this.appendCalled = true;
            }
            if (msg.endsWith(LINE_SEPARATOR)) {
                printStream.print(msg);
            }
            else {
                printStream.println(msg);
            }
            this.cursorMove = 0;
        }
        finally {
            this.progressIndicatorActive = false;
        }
    }

    /**
     * Synonym for #log
     *
     * @param msg The message to log
     */
    @Override
    public void info(String msg) {
        log(msg);
    }

    @Override
    public void verbose(String msg) {
        verifySystemOut();
        try {
            if (this.verbose) {
                this.out.println(msg);
                this.cursorMove = 0;
            }
        }
        finally {
            postPrintMessage();
        }
    }

    /**
     * Replays the last status message
     */
    public void echoStatus() {
        if (this.lastStatus != null) {
            updateStatus(this.lastStatus.toString());
        }
    }

    /**
     * Replacement for AntBuilder.input() to eliminate dependency of
     * GrailsScriptRunner on the Ant libraries. Prints a message and
     * returns whatever the user enters (once they press &lt;return&gt;).
     * @param msg The message/question to display.
     * @return The line of text entered by the user. May be a blank
     * string.
     */
    public String userInput(String msg) {
        return doUserInput(msg, false);
    }

    /**
     * Like {@link #userInput(String)} except that the user's entered characters will be replaced with '*' on the CLI,
     * masking the input (i.e. suitable for capturing passwords etc.).
     *
     * @param msg The message/question to display.
     * @return The line of text entered by the user. May be a blank
     * string.
     */
    public String secureUserInput(String msg) {
        return doUserInput(msg, true);
    }

    private String doUserInput(String msg, boolean secure) {
        // Add a space to the end of the message if there isn't one already.
        if (!msg.endsWith(" ") && !msg.endsWith("\t")) {
            msg += ' ';
        }

        this.lastMessage = "";
        msg = isAnsiEnabled() ? outputCategory(Ansi.ansi(), ">").fg(Color.DEFAULT).a(msg).reset().toString() : msg;
        try {
            return readLine(msg, secure);
        }
        finally {
            this.cursorMove = 0;
        }
    }

    /**
     * Shows the prompt to request user input
     * @param prompt The prompt to use
     * @return The user input prompt
     */
    private String showPrompt(String prompt) {
        verifySystemOut();
        this.cursorMove = 0;
        if (!this.userInputActive) {
            return readLine(prompt, false);
        }

        this.out.print(prompt);
        this.out.flush();
        return null;
    }

    private String readLine(String prompt, boolean secure) {
        assertAllowInput(prompt);
        this.userInputActive = true;
        try {
            Character inputMask = secure ? SECURE_MASK_CHAR : this.defaultInputMask;
            return this.reader.readLine(prompt, inputMask);
        }
        catch (IOException e) {
            throw new RuntimeException("Error reading input: " + e.getMessage());
        }
        finally {
            this.userInputActive = false;
        }
    }

    /**
     * Shows the prompt to request user input
     * @return The user input prompt
     */
    public String showPrompt() {
        String prompt = isAnsiEnabled() ? ansiPrompt(PROMPT).toString() : PROMPT;
        return showPrompt(prompt);
    }

    private Ansi ansiPrompt(String prompt) {
        return Ansi.ansi()
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(Color.YELLOW)
                .a(prompt)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .fg(Color.DEFAULT);
    }

    public String userInput(String message, List<String> validResponses) {
        return userInput(message, validResponses.toArray(new String[0]));
    }

    /**
     * Replacement for AntBuilder.input() to eliminate dependency of
     * GrailsScriptRunner on the Ant libraries. Prints a message and
     * list of valid responses, then returns whatever the user enters
     * (once they press &lt;return&gt;). If the user enters something
     * that is not in the array of valid responses, the message is
     * displayed again and the method waits for more input. It will
     * display the message a maximum of three times before it gives up
     * and returns <code>null</code>.
     * @param message The message/question to display.
     * @param validResponses An array of responses that the user is
     * allowed to enter. Displayed after the message.
     * @return The line of text entered by the user, or <code>null</code>
     * if the user never entered a valid string.
     */
    public String userInput(String message, String[] validResponses) {
        if (validResponses == null) {
            return userInput(message);
        }

        String question = createQuestion(message, validResponses);
        String response = userInput(question);
        for (String validResponse : validResponses) {
            if (validResponse.equalsIgnoreCase(response)) {
                return response;
            }
        }
        this.cursorMove = 0;
        return userInput("Invalid input. Must be one of ", validResponses);
    }

    private String createQuestion(String message, String[] validResponses) {
        return message + "[" + DefaultGroovyMethods.join(validResponses, ",") + "] ";
    }

    private Ansi outputCategory(Ansi ansi, String categoryName) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(Color.YELLOW)
                .a(categoryName)
                .a(SPACE)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF);
    }

    private Ansi outputErrorLabel(Ansi ansi, String label) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(Color.RED)
                .a(CATEGORY_SEPARATOR)
                .a(SPACE)
                .a(label)
                .a(" ")
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .fg(Color.DEFAULT);
    }

    private Ansi erasePreviousLine(String categoryName) {
        int cursorMove = this.cursorMove;
        if (this.userInputActive) {
            cursorMove++;
        }
        if (cursorMove > 0) {
            int moveLeftLength = categoryName.length() + this.lastMessage.length();
            if (this.userInputActive) {
                moveLeftLength += PROMPT.length();
            }
            return Ansi.ansi()
                    .cursorUp(cursorMove)
                    .cursorLeft(moveLeftLength)
                    .eraseLine(Ansi.Erase.FORWARD);

        }
        return Ansi.ansi();
    }

    @Override
    public void error(String label, String message) {
        verifySystemOut();
        if (message == null) {
            return;
        }

        this.cursorMove = 0;
        try {
            if (isAnsiEnabled()) {
                Ansi ansi = outputErrorLabel(this.userInputActive ? moveDownToSkipPrompt() : Ansi.ansi(), label).a(message).reset();

                if (message.endsWith(LINE_SEPARATOR)) {
                    this.out.print(ansi);
                }
                else {
                    this.out.println(ansi);
                }
            }
            else {
                this.out.print(label);
                this.out.print(" ");
                logSimpleError(message);
            }
        }
        finally {
            postPrintMessage();
        }
    }

    private void verifySystemOut() {
        // something bad may have overridden the system out
        redirectSystemOutAndErr(false);
    }

    public void restoreOriginalSystemOutAndErr() {
        System.setOut(this.originalSystemOut);
        System.setErr(this.originalSystemErr);
    }

    public void cleanlyExit(int status) {
        flush();
        System.exit(status);
    }

    /**
     * Makes sure that the console has been reset to the default state and that
     * the out stream has been flushed.
     */
    public void flush() {
        if (isAnsiEnabled()) {
            this.out.print(Ansi.ansi().reset().toString());
        }
        this.out.flush();
    }

    public Character getDefaultInputMask() {
        return this.defaultInputMask;
    }

    public void setDefaultInputMask(Character defaultInputMask) {
        this.defaultInputMask = defaultInputMask;
    }

}
