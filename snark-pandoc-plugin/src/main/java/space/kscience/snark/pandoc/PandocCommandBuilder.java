package space.kscience.snark.pandoc;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class PandocCommandBuilder {
    private List<String> options = new ArrayList<>();
    private List<Path> inputFiles = new ArrayList<>();

    /**
     * For converting from inputFiles to outputFile
     * @param inputFiles
     * @param outputFile
     */
    public PandocCommandBuilder(List<Path> inputFiles, Path outputFile) {
        this.inputFiles = inputFiles;
        setOutputFile(outputFile);
    }

    public PandocCommandBuilder() {}

    /**
     * @return commands to run `pandoc commands[]...`
     */
    public List<String> build() {
        options.add(0, PandocWrapper.getPandocPath());
        if (inputFiles.isEmpty()) {
            // case pandoc -v; etc
            return options;
        }
        for (var file : inputFiles) {
            options.add(file.toString());
        }
        return options;
    }

    /**
     * set files to convert from
     * @param inputFiles
     */
    public PandocCommandBuilder setInputFile(List<Path> inputFiles) {
        this.inputFiles = inputFiles;
        return this;
    }
    /**
     * Specify input format
     */
    public PandocCommandBuilder formatFrom(String format) {
        options.add("--from=" + format);
        return this;
    }

    /**
     * Specify output format
     */
    public PandocCommandBuilder formatTo(String format) {
        options.add("--to=" + format);
        return this;
    }

    /**
     * Set file output. Without it output == stdout
     */
    public PandocCommandBuilder setOutputFile(Path outputFile) {
        options.add("--output=" + outputFile);
        return this;
    }

    /**
     * Specify the user data directory to search for pandoc data files. If this option is not specified, the default
     * user data directory will be used. On *nix and macOS systems this will be the pandoc subdirectory of the XDG
     * data directory (by default, $HOME/.local/share, overridable by setting the XDG_DATA_HOME environment variable).
     * If that directory does not exist and $HOME/.pandoc exists, it will be used (for backwards compatibility).
     * On Windows the default user data directory is %APPDATA%\pandoc. You can find the default user data directory on
     * your system by looking at the output of pandoc --version. Data files placed in this directory
     * (for example, reference.odt, reference.docx, epub.css, templates) will override pandoc’s normal defaults.
     */
    public PandocCommandBuilder dataDir(Path dataDir) {
        options.add("--data-dir=" + dataDir.toAbsolutePath());
        return this;
    }

    /**
     * Specify a set of default option settings. file is a YAML file whose fields correspond to command-line option
     * settings. All options for document conversion, including input and output files, can be set using a defaults file.
     * The file will be searched for first in the working directory, and then in the defaults subdirectory of the user
     * data directory (see PandocOptionsBuilder#dataDir). The .yaml extension may be omitted.
     * Settings from the defaults file may be overridden or extended by subsequent options on the command line.
     */
    public PandocCommandBuilder defaultSettings(Path file) {
        options.add("--defaults=" + file);
        return this;
    }

    /**
     * Generate a bash completion script. To enable bash completion with pandoc, add this to your .bashrc:
     * eval "$(pandoc --bash-completion)"
     */
    public PandocCommandBuilder bashCompletion() {
        options.add("--bash-completion");
        return this;
    }

    /**
     * Give verbose debugging output.
     */
    public PandocCommandBuilder verbose() {
        options.add("--verbose");
        return this;
    }

    /**
     * Suppress warning messages.
     */
    public PandocCommandBuilder quiet() {
        options.add("--quiet");
        return this;
    }

    /**
     * Exit with error status if there are any warnings.
     */
    public PandocCommandBuilder fallIfWarnings() {
        options.add("--fail-if-warnings");
        return this;
    }

    /**
     * Write log messages in machine-readable JSON format to file. All messages above DEBUG level will be written,
     * regardless of verbosity settings (--verbose, --quiet).
     * @param file
     */
    public PandocCommandBuilder logFile(Path file) {
        options.add("--log=" + file);
        return this;
    }

    /**
     * List supported input formats, one per line.
     */
    public PandocCommandBuilder getInputFormats() {
        options.add("--list-input-formats");
        return this;
    }

    /**
     * List supported output formats, one per line.
     */
    public PandocCommandBuilder getOutputFormats() {
        options.add("--list-output-formats");
        return this;
    }

    /**
     * List supported extensions for format, one per line, preceded by a + or - indicating whether it is enabled by
     * default in format. If FORMAT is not specified, defaults for pandoc’s Markdown are given.
     * @param format
     */
    public PandocCommandBuilder getExtensions(String format) {
        if (format == null) {
            options.add("--list-extensions");
        } else {
            options.add("--list-extensions=" + format);
        }
        return this;
    }

    /**
     * List supported languages for syntax highlighting, one per line.
     */
    public PandocCommandBuilder getHighlightLanguages() {
        options.add("--list-highlight-languages");
        return this;
    }

    /**
     * List supported styles for syntax highlighting, one per line.
     */
    public PandocCommandBuilder getHighlightStyles() {
        options.add("--list-highlight-styles");
        return this;
    }

    /**
     * Print version.
     */
    public PandocCommandBuilder getVersion() {
        options.add("--version");
        return this;
    }

    /**
     * Show usage message.
     */
    public PandocCommandBuilder help() {
        options.add("--help");
        return this;
    }

    /**
     * Shift heading levels by a positive or negative integer. For example, with --shift-heading-level-by=-1, level 2
     * headings become level 1 headings, and level 3 headings become level 2 headings. Headings cannot have a level less
     * than 1, so a heading that would be shifted below level 1 becomes a regular paragraph.
     * Exception: with a shift of -N, a level-N heading at the beginning of the document replaces the metadata title.
     * --shift-heading-level-by=-1 is a good choice when converting HTML or Markdown documents
     * that use an initial level-1 heading for the document title and level-2+ headings for sections.
     * --shift-heading-level-by=1 may be a good choice for converting Markdown documents that use level-1 headings for
     * sections to HTML, since pandoc uses a level-1 heading to render the document title.
     */
    public PandocCommandBuilder shiftHeadingLevelBy(int number) {
        options.add("--shift-heading-level-by=" + number);
        return this;
    }

    /**
     * Specify classes to use for indented code blocks–for example, perl,numberLines or haskell.
     * Multiple classes may be separated by spaces or commas.
     */
    public PandocCommandBuilder indentedCodeClasses(String classes) {
        options.add("--indented-code-classes=" + classes);
        return this;
    }

    /**
     * Specify a default extension to use when image paths/URLs have no extension. This allows you to use
     * the same source for formats that require different kinds of images.
     * Currently, this option only affects the Markdown and LaTeX readers.
     */
    public PandocCommandBuilder defaultImageExtension(String extension) {
        options.add("--default-image-extension=" + extension);
        return  this;
    }

    /**
     * Parse each file individually before combining for multifile documents. This will allow footnotes in different
     * files with the same identifiers to work as expected. If this option is set, footnotes and links will not
     * work across files. Reading binary files (docx, odt, epub) implies --file-scope.
     * If two or more files are processed using --file-scope, prefixes based on the filenames will be added to
     * identifiers in order to disambiguate them, and internal links will be adjusted accordingly. For example,
     * a header with identifier foo in subdir/file1.txt will have its identifier changed to subdir__file1.txt__foo.
     * In addition, a Div with an identifier based on the filename will be added around the file’s content,
     * so that internal links to the filename will point to this Div’s identifier.
     */
    public PandocCommandBuilder fileScope() {
        options.add("--file-scope");
        return this;
    }

    /**
     * Specify an executable to be used as a filter transforming the pandoc AST after the input is parsed and before
     * the output is written. The executable should read JSON from stdin and write JSON to stdout. The JSON must be
     * formatted like pandoc’s own JSON input and output. The name of the output format will be passed to the filter
     * as the first argument. Hence,
     * pandoc --filter ./caps.py -t latex
     * is equivalent to
     * pandoc -t json | ./caps.py latex | pandoc -f json -t latex
     * The latter form may be useful for debugging filters.
     * Filters may be written in any language. Text.Pandoc.JSON exports toJSONFilter to facilitate writing filters
     * in Haskell. Those who would prefer to write filters in python can use the module pandocfilters,
     * installable from PyPI. There are also pandoc filter libraries in PHP, perl, and JavaScript/node.js.
     * In order of preference, pandoc will look for filters in
     * 1. a specified full or relative path (executable or non-executable),
     * 2. $DATADIR/filters (executable or non-executable) where $DATADIR is the user data directory (see --data-dir, above),
     * 3. $PATH (executable only).
     * Filters, Lua-filters, and citeproc processing are applied in the order specified on the command line.
     */
    public PandocCommandBuilder filter(Path program) {
        options.add("-F " + program);
        return this;
    }

    /**
     * Transform the document in a similar fashion as JSON filters (see --filter), but use pandoc’s built-in Lua
     * filtering system. The given Lua script is expected to return a list of Lua filters which will be applied in order.
     * Each Lua filter must contain element-transforming functions indexed by the name of the AST element on which the
     * filter function should be applied.
     * The pandoc Lua module provides helper functions for element creation.
     * It is always loaded into the script’s Lua environment.
     * See the Lua filters documentation for further details.
     * In order of preference, pandoc will look for Lua filters in
     * 1. a specified full or relative path,
     * 2. $DATADIR/filters where $DATADIR is the user data directory (see --data-dir, above).
     * Filters, Lua filters, and citeproc processing are applied in the order specified on the command line.
     */
    public PandocCommandBuilder luaFilter(Path program) {
        options.add("-L " + program);
        return this;
    }

    /**
     * Set the metadata field KEY to the value VAL. A value specified on the command line overrides a value specified in
     * the document using YAML metadata blocks. Values will be parsed as YAML boolean or string values. If no value is
     * specified, the value will be treated as Boolean true. Like --variable, --metadata causes template variables
     * to be set. But unlike --variable, --metadata affects the metadata of the underlying document (which is accessible
     * from filters and may be printed in some output formats) and metadata values will be escaped
     * when inserted into the template.
     */
    public PandocCommandBuilder metadata(String key, String value) {
        if (value == null) {
            options.add("--metadata=" + key);
            return this;
        }
        options.add("--metadata=" + key + ":" + value);
        return this;
    }

    /**
     * Read metadata from the supplied YAML (or JSON) file. This option can be used with every input format, but string
     * scalars in the metadata file will always be parsed as Markdown. (If the input format is Markdown or a Markdown
     * variant, then the same variant will be used to parse the metadata file; if it is a non-Markdown format, pandoc’s
     * default Markdown extensions will be used.) This option can be used repeatedly to include multiple metadata files;
     * values in files specified later on the command line will be preferred over those specified in earlier files.
     * Metadata values specified inside the document, or by using -M, overwrite values specified with this option.
     * The file will be searched for first in the working directory, and then in the metadata subdirectory of the user
     * data directory (see --data-dir).
     */
    public PandocCommandBuilder metadataFile(Path file) {
        options.add("--metadata-file=" + file);
        return this;
    }

    /**
     * Preserve tabs instead of converting them to spaces. (By default, pandoc converts tabs to spaces before parsing
     * its input.) Note that this will only affect tabs in literal code spans and code blocks.
     * Tabs in regular text are always treated as spaces.
     */
    public PandocCommandBuilder preserveTabs() {
        options.add("--preserve-tabs");
        return this;
    }

    /**
     * Specify the number of spaces per tab (default is 4).
     */
    public  PandocCommandBuilder tabStop(int number) {
        options.add("--tab-stop=" + number);
        return this;
    }

    /**
     * Specifies what to do with insertions, deletions, and comments produced by the MS Word “Track Changes” feature.
     * accept (the default) processes all the insertions and deletions. reject ignores them. Both accept and reject
     * ignore comments. all includes all insertions, deletions, and comments, wrapped in spans with insertion, deletion,
     * comment-start, and comment-end classes, respectively. The author and time of change is included. all is useful
     * for scripting: only accepting changes from a certain reviewer, say, or before a certain date. If a paragraph is
     * inserted or deleted, track-changes=all produces a span with the class paragraph-insertion/paragraph-deletion
     * before the affected paragraph break. This option only affects the docx reader.
     */
    public PandocCommandBuilder trackChanges(String option) {
        options.add("--track-changes=" + option);
        return this;
    }

    /**
     * Extract images and other media contained in or linked from the source document to the path DIR, creating it
     * if necessary, and adjust the images references in the document so they point to the extracted files. Media are
     * downloaded, read from the file system, or extracted from a binary container (e.g. docx), as needed.
     * The original file paths are used if they are relative paths not containing ...
     * Otherwise, filenames are constructed from the SHA1 hash of the contents.
     */
    public PandocCommandBuilder extractMediaDirectory(Path dir) {
        options.add("--extract-media=" + dir);
        return this;
    }

    /**
     * Specifies a custom abbreviations file, with abbreviations one to a line. If this option is not specified, pandoc
     * will read the data file abbreviations from the user data directory or fall back on a system default.
     * To see the system default, use pandoc --print-default-data-file=abbreviations. The only use pandoc makes of this
     * list is in the Markdown reader. Strings found in this list will be followed by a nonbreaking space, and
     * the period will not produce sentence-ending space in formats like LaTeX. The strings may not contain spaces.
     */
    public PandocCommandBuilder abbreviationsFile(Path file) {
        options.add("--abbreviations=" + file);
        return this;
    }

    /**
     * Print diagnostic output tracing parser progress to stderr. This option is intended for use by developers
     * in diagnosing performance issues.
     */
    public PandocCommandBuilder trace() {
        options.add("--trace");
        return this;
    }

    /**
     * Produce output with an appropriate header and footer (e.g. a standalone HTML, LaTeX, TEI, or RTF file,
     * not a fragment). This option is set automatically for pdf, epub, epub3, fb2, docx, and odt output.
     * For native output, this option causes metadata to be included; otherwise, metadata is suppressed.
     */
    public PandocCommandBuilder standalone() {
        options.add("--standalone");
        return this;
    }

    /**
     * Use the specified file as a custom template for the generated document. Implies --standalone. See Templates,
     * below, for a description of template syntax. If no extension is specified, an extension corresponding to
     * the writer will be added, so that --template=special looks for special.html for HTML output. If the template
     * is not found, pandoc will search for it in the templates subdirectory of the user data directory (see --data-dir).
     * If this option is not used, a default template appropriate for the output format will be used
     * (see -D/--print-default-template).
     */
    public PandocCommandBuilder setTemplate(Path file) {
        options.add("--template=" + file);
        return this;
    }

    public PandocCommandBuilder setTemplate(URL url) {
        options.add("--template=" + url);
        return this;
    }

    /**
     * Set the template variable KEY to the value VAL when rendering the document in standalone mode.
     * If no VAL is specified, the key will be given the value true.
     */
    public PandocCommandBuilder setVariable(String key, String variable) {
        if (variable == null) {
            options.add("--variable=" + key);
            return this;
        }
        options.add("--variable=" + key + ":" + variable);
        return this;
    }

    /**
     * Run pandoc in a sandbox, limiting IO operations in readers and writers to reading the files specified on
     * the command line. Note that this option does not limit IO operations by filters or in the production of PDF
     * documents. But it does offer security against, for example, disclosure of files through the use of include
     * directives. Anyone using pandoc on untrusted user input should use this option.
     * Note: some readers and writers (e.g., docx) need access to data files. If these are stored on the file system,
     * then pandoc will not be able to find them when run in --sandbox mode and will raise an error.
     * For these applications, we recommend using a pandoc binary compiled with the embed_data_files option,
     * which causes the data files to be baked into the binary instead of being stored on the file system
     */
    public PandocCommandBuilder sandbox() {
        options.add("--sandbox");
        return this;
    }

    /**
     * Print the system default template for an output FORMAT. (See -t for a list of possible FORMATs.) Templates
     * in the user data directory are ignored. This option may be used with -o/--output to redirect output to a file,
     * but -o/--output must come before --print-default-template on the command line.
     * Note that some of the default templates use partials, for example styles.html. To print the partials,
     * use --print-default-data-file: for example, --print-default-data-file=templates/styles.html.
     */
    public PandocCommandBuilder printDefaultFormat(String format) {
        options.add("--print-default-template=" + format);
        return this;
    }

    /**
     * Print a system default data file. Files in the user data directory are ignored. This option may be used with
     * -o/--output to redirect output to a file, but -o/--output must come before --print-default-data-file
     * on the command line.
     */
    public PandocCommandBuilder printDefaultDataFile(Path file) {
        options.add("--print-default-data-file=" + file);
        return this;
    }

    /**
     * Manually specify line endings: crlf (Windows), lf (macOS/Linux/UNIX), or native (line endings appropriate
     * to the OS on which pandoc is being run). The default is native.
     */
    public PandocCommandBuilder setLineEndings(String option) {
        options.add("--eol=" + option);
        return this;
    }

    /**
     * Specify the default dpi (dots per inch) value for conversion from pixels to inch/centimeters and vice versa.
     * (Technically, the correct term would be ppi: pixels per inch.) The default is 96dpi. When images contain
     * information about dpi internally, the encoded value is used instead of the default specified by this option.
     */
    public PandocCommandBuilder setDotsPerInch(int number) {
        options.add("--dpi=" + number);
        return this;
    }

    /**
     * Determine how text is wrapped in the output (the source code, not the rendered version). With auto (the default),
     * pandoc will attempt to wrap lines to the column width specified by --columns (default 72). With none,
     * pandoc will not wrap lines at all. With preserve, pandoc will attempt to preserve the wrapping from the source
     * document (that is, where there are nonsemantic newlines in the source, there will be nonsemantic newlines
     * in the output as well). In ipynb output, this option affects wrapping of the contents of markdown cells.
     */
    public PandocCommandBuilder wrapStrategy(String strategy) {
        options.add("--wrap=" + strategy);
        return this;
    }

    /**
     * Specify length of lines in characters. This affects text wrapping in the generated source code (see --wrap).
     * It also affects calculation of column widths for plain text tables (see Tables below).
     */
    public PandocCommandBuilder lengthOfLines(int number) {
        options.add("--columns=" + number);
        return this;
    }

    /**
     * Include an automatically generated table of contents (or, in the case of latex, context, docx, odt, opendocument,
     * rst, or ms, an instruction to create one) in the output document. This option has no effect unless
     * -s/--standalone is used, and it has no effect on man, docbook4, docbook5, or jats output.
     * Note that if you are producing a PDF via ms, the table of contents will appear at the beginning of the document,
     * before the title. If you would prefer it to be at the end of the document, use the option
     * --pdf-engine-opt=--no-toc-relocation.
     */
    public PandocCommandBuilder addTable() {
        options.add("--table-of-contents");
        return this;
    }

    /**
     * Specify the number of section levels to include in the table of contents. The default is 3
     * (which means that level-1, 2, and 3 headings will be listed in the contents).
     */
    public PandocCommandBuilder setTableLevels(int numb) {
        options.add("--toc-depth=" + numb);
        return this;
    }

    /**
     * Strip out HTML comments in the Markdown or Textile source, rather than passing them on to Markdown, Textile or
     * HTML output as raw HTML. This does not apply to HTML comments inside raw HTML blocks
     * when the markdown_in_html_blocks extension is not set.
     */
    public PandocCommandBuilder stripComments() {
        options.add("--strip-comments");
        return this;
    }

    /**
     * Disables syntax highlighting for code blocks and inlines, even when a language attribute is given.
     */
    public PandocCommandBuilder disableHighlight() {
        options.add("--no-highlight");
        return this;
    }

    /**
     * Specifies the coloring style to be used in highlighted source code. Options are pygments (the default),
     * kate, monochrome, breezeDark, espresso, zenburn, haddock, and tango. For more information on syntax highlighting
     * in pandoc, see Syntax highlighting, below. See also --list-highlight-styles.
     * To generate the JSON version of an existing style, use --print-highlight-style.
     */
    public PandocCommandBuilder highlightStyle(String style) {
        options.add("--highlight-style=" + style);
        return this;
    }

    /**
     * Specifies the coloring style to be used in highlighted source code.
     * Param is a JSON file with extension .theme. This will be parsed as a
     * KDE syntax highlighting theme and (if valid) used as the highlighting style.
     * To generate the JSON version of an existing style, use --print-highlight-style.
     */
    public PandocCommandBuilder highlightStyle(Path style) {
        options.add("--highlight-style=" + style);
        return this;
    }

    /**
     * Prints a JSON version of a highlighting style, which can be modified, saved with a .theme extension, and
     * used with --highlight-style. This option may be used with -o/--output to redirect output to a file,
     * but -o/--output must come before --print-highlight-style on the command line.
     */
    public PandocCommandBuilder printHighlightStyle(String style) {
        options.add("--print-highlight-style=" + style);
        return this;
    }

    /**
     * Prints a JSON version of a highlighting style, which can be modified, saved with a .theme extension, and
     * used with --highlight-style. This option may be used with -o/--output to redirect output to a file,
     * but -o/--output must come before --print-highlight-style on the command line.
     */
    public PandocCommandBuilder printHighlightStyle(Path file) {
        options.add("--print-highlight-style=" + file);
        return this;
    }

    /**
     * Instructs pandoc to load a KDE XML syntax definition file, which will be used for syntax highlighting of
     * appropriately marked code blocks. This can be used to add support for new languages or to use altered syntax
     * definitions for existing languages. This option may be repeated to add multiple syntax definitions.
     */
    public PandocCommandBuilder syntaxDefinition(Path file) {
        options.add("--syntax-definition=" + file);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the end of the header. This can be used, for example, to include special
     * CSS or JavaScript in HTML documents. This option can be used repeatedly to include multiple files in the header.
     * They will be included in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeInHeader(Path file) {
        options.add("--include-in-header=" + file);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the end of the header. This can be used, for example, to include special
     * CSS or JavaScript in HTML documents. This option can be used repeatedly to include multiple files in the header.
     * They will be included in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeInHeader(URL url) {
        options.add("--include-in-header=" + url);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the beginning of the document body (e.g. after the <body> tag in HTML,
     * or the \begin{document} command in LaTeX). This can be used to include navigation bars or
     * banners in HTML documents. This option can be used repeatedly to include multiple files. They will be included
     * in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeBeforeBody(Path file) {
        options.add("--include-before-body=" + file);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the beginning of the document body (e.g. after the <body> tag in HTML,
     * or the \begin{document} command in LaTeX). This can be used to include navigation bars or
     * banners in HTML documents. This option can be used repeatedly to include multiple files. They will be included
     * in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeBeforeBody(URL url) {
        options.add("--include-before-body=" + url);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the end of the document body (before the </body> tag in HTML,
     * or the \end{document} command in LaTeX). This option can be used repeatedly to include multiple files.
     * They will be included in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeAfterBody(Path file) {
        options.add("--include-after-body=" + file);
        return this;
    }

    /**
     * Include contents of FILE, verbatim, at the end of the document body (before the </body> tag in HTML,
     * or the \end{document} command in LaTeX). This option can be used repeatedly to include multiple files.
     * They will be included in the order specified. Implies --standalone.
     */
    public PandocCommandBuilder includeAfterBody(URL url) {
        options.add("--include-after-body=" + url);
        return this;
    }

    /**
     * List of paths to search for images and other resources. The paths should be separated by : on Linux, UNIX,
     * and macOS systems, and by ; on Windows. If --resource-path is not specified, the default resource path
     * is the working directory. Note that, if --resource-path is specified, the working directory must be
     * explicitly listed or it will not be searched. For example: --resource-path=.:test will search the working
     * directory and the test subdirectory, in that order. This option can be used repeatedly. Search path components
     * that come later on the command line will be searched before those that come earlier, so --resource-path foo:bar
     * --resource-path baz:bim is equivalent to --resource-path baz:bim:foo:bar.
     */
    public PandocCommandBuilder resourcePath(String path) {
        options.add("--resource-path=" + path);
        return this;
    }

    /**
     * Set the request header NAME to the value VAL when making HTTP requests (for example, when a URL is given
     * on the command line, or when resources used in a document must be downloaded). If you’re behind a proxy,
     * you also need to set the environment variable http_proxy to http://....
     */
    public PandocCommandBuilder requestHeader(String name, String val) {
        options.add("--request-header=" + name + ":" + val);
        return this;
    }

    /**
     * Disable the certificate verification to allow access to unsecure HTTP resources
     * (for example when the certificate is no longer valid or self-signed).
     */
    public PandocCommandBuilder noCheckCertificate() {
        options.add("--no-check-certificate");
        return this;
    }

    /**
     * Produce a standalone HTML file with no external dependencies, using data: URIs to incorporate the contents of
     * linked scripts, stylesheets, images, and videos. The resulting file should be “self-contained,” in the sense
     * that it needs no external files and no net access to be displayed properly by a browser. This option works only
     * with HTML output formats, including html4, html5, html+lhs, html5+lhs, s5, slidy, slideous, dzslides, and
     * revealjs. Scripts, images, and stylesheets at absolute URLs will be downloaded; those at relative URLs will be
     * sought relative to the working directory (if the first source file is local) or relative to the base URL
     * (if the first source file is remote). Elements with the attribute data-external="1" will be left alone;
     * the documents they link to will not be incorporated in the document. Limitation: resources that are loaded
     * dynamically through JavaScript cannot be incorporated; as a result, fonts may be missing when --mathjax is used,
     * and some advanced features (e.g. zoom or speaker notes) may not work in an offline “self-contained” reveal.js
     * slide show.
     */
    public PandocCommandBuilder embedResources() {
        options.add("--embed-resources");
        return this;
    }

    /**
     * Use <q> tags for quotes in HTML.
     * (This option only has an effect if the smart extension is enabled for the input format used.)
     */
    public PandocCommandBuilder htmlQTags() {
        options.add("--html-q-tags");
        return this;
    }

    /**
     * Use only ASCII characters in output. Currently supported for XML and HTML formats (which use entities instead of
     * UTF-8 when this option is selected), CommonMark, gfm, and Markdown (which use entities), roff man and ms
     * (which use hexadecimal escapes), and to a limited degree LaTeX
     * (which uses standard commands for accented characters when possible).
     */
    public PandocCommandBuilder useAscii() {
        options.add("--ascii");
        return this;
    }

    /**
     * Use reference-style links, rather than inline links, in writing Markdown or reStructuredText.
     * By default, inline links are used.The placement of link references is affected by the --reference-location option
     */
    public PandocCommandBuilder referenceLinks() {
        options.add("--reference-links");
        return this;
    }

    /**
     * Specify whether footnotes (and references, if reference-links is set) are placed at the end of the current
     * (top-level) block, the current section, or the document. The default is document. Currently, this option only
     * affects the markdown, muse, html, epub, slidy, s5, slideous, dzslides, and revealjs writers. In slide formats,
     * specifying --reference-location=section will cause notes to be rendered at the bottom of a slide.
     * @param option : block|section|document
     */
    public PandocCommandBuilder referenceLocation(String option) {
        options.add("--reference-location=" + option);
        return this;
    }

    /**
     * Specify whether to use ATX-style (#-prefixed) or Setext-style (underlined) headings for level 1 and 2 headings
     * in Markdown output. (The default is atx.) ATX-style headings are always used for levels 3+. This option also
     * affects Markdown cells in ipynb output.
     * @param option : setext|atx
     */
    public PandocCommandBuilder markdownHeadings(String option) {
        options.add("--markdown-headings=" + option);
        return this;
    }

    /**
     * Render tables as list tables in RST output.
     */
    public PandocCommandBuilder listTables() {
        options.add("--list-tables");
        return this;
    }

    /**
     * Treat top-level headings as the given division type in LaTeX, ConTeXt, DocBook, and TEI output. The hierarchy
     * order is part, chapter, then section; all headings are shifted such that the top-level heading becomes
     * the specified type. The default behavior is to determine the best division type via heuristics: unless other
     * conditions apply, section is chosen. When the documentclass variable is set to report, book, or memoir
     * (unless the article option is specified), chapter is implied as the setting for this option. If beamer is
     * the output format, specifying either chapter or part will cause top-level headings to become \part{..},
     * while second-level headings remain as their default type.
     * @param option : default|section|chapter|part
     */
    public PandocCommandBuilder topLevelDivision(String option) {
        options.add("--top-level-division=" + option);
        return  this;
    }

    /**
     * Number section headings in LaTeX, ConTeXt, HTML, Docx, ms, or EPUB output. By default, sections are not numbered.
     * Sections with class unnumbered will never be numbered, even if --number-sections is specified.
     */
    public PandocCommandBuilder numberSections() {
        options.add("--number-sections");
        return this;
    }

    /**
     * Offset for section headings in HTML output (ignored in other output formats). The first number is added to the
     * section number for top-level headings, the second for second-level headings, and so on. So, for example,
     * if you want the first top-level heading in your document to be numbered “6”, specify --number-offset=5.
     * If your document starts with a level-2 heading which you want to be numbered “1.5”, specify --number-offset=1,4.
     * Offsets are 0 by default. Implies --number-sections.
     * @param numbs
     */
    public PandocCommandBuilder numberOffset(List<Integer> numbs) {
        if (numbs.isEmpty()) {
            return this;
        }
        StringBuilder sb = new StringBuilder(numbs.get(0).toString());
        if (numbs.size() > 1) {
            for (int i = 1; i < numbs.size(); ++i) {
                sb.append(",");
                sb.append(numbs.get(i));
            }
        }
        options.add("--number-offset=" + sb);
        return this;
    }

    /**
     * Use the listings package for LaTeX code blocks. The package does not support multibyte encoding for source code.
     * To handle UTF-8 you would need to use a custom template. This issue is fully documented here:
     * See <a href="https://en.wikibooks.org/wiki/LaTeX/Source_Code_Listings#Encoding_issue">
     *     Encoding issue with the listings package
     *      </a>
     */
    public PandocCommandBuilder listings() {
        options.add("--listings");
        return this;
    }

    /**
     * Make list items in slide shows display incrementally (one by one).
     * The default is for lists to be displayed all at once.
     */
    public PandocCommandBuilder incremental() {
        options.add("--incremental");
        return this;
    }

    /**
     * Specifies that headings with the specified level create slides (for beamer, s5, slidy, slideous, dzslides).
     * Headings above this level in the hierarchy are used to divide the slide show into sections; headings below this
     * level create subheads within a slide. Valid values are 0-6. If a slide level of 0 is specified,
     * slides will not be split automatically on headings, and horizontal rules must be used to indicate
     * slide boundaries. If a slide level is not specified explicitly, the slide level will be set automatically
     * based on the contents of the document.
     * See <a href="https://pandoc.org/MANUAL.html#structuring-the-slide-show">Structuring the slide show.</a>
     */
    public PandocCommandBuilder slideLevel(int number) {
        options.add("--slide-level=" + number);
        return this;
    }

    /**
     * Wrap sections in <section> tags (or <div> tags for html4), and attach identifiers to the enclosing <section>
     * (or <div>) rather than the heading itself.
     * See <a href="https://pandoc.org/MANUAL.html#heading-identifiers">Heading identifiers</a>
     */
    public PandocCommandBuilder sectionDivs() {
        options.add("--section-divs");
        return this;
    }

    /**
     * Specify a method for obfuscating mailto: links in HTML documents. none leaves mailto: links as they are.
     * javascript obfuscates them using JavaScript. references obfuscates them by printing their letters as decimal or
     * hexadecimal character references. The default is none.
     * @param option : none|javascript|references
     */
    public PandocCommandBuilder emailObfuscation(String option) {
        options.add("--email-obfuscation=" + option);
        return this;
    }

    /**
     * Specify a prefix to be added to all identifiers and internal links in HTML and DocBook output, and to footnote
     * numbers in Markdown and Haddock output. This is useful for preventing duplicate identifiers when generating
     * fragments to be included in other pages.
     */
    public PandocCommandBuilder idPrefix(String option) {
        options.add("--id-prefix=" + option);
        return this;
    }

    /**
     * Specify STRING as a prefix at the beginning of the title that appears in the HTML header (but not in the title
     * as it appears at the beginning of the HTML body). Implies --standalone.
     */
    public PandocCommandBuilder titlePrefix(String option) {
        options.add("--title-prefix=" + option);
        return this;
    }

    /**
     * Link to a CSS style sheet. This option can be used repeatedly to include multiple files. They will be included in
     * the order specified. This option only affects HTML (including HTML slide shows) and EPUB output. It should be
     * used together with -s/--standalone, because the link to the stylesheet goes in the document header.
     * A stylesheet is required for generating EPUB. If none is provided using this option (or the css or stylesheet
     * metadata fields), pandoc will look for a file epub.css in the user data directory (see --data-dir).
     * If it is not found there, sensible defaults will be used.
     */
    public PandocCommandBuilder linkToCss(URL url) {
        options.add("--css=" + url);
        return this;
    }

    /**
     * Use the specified file as a style reference in producing a docx or ODT file.
     * See <a href="https://pandoc.org/MANUAL.html#options-affecting-specific-writers">
     *          Options affecting specific writers
     *     </a>
     */
    public PandocCommandBuilder referenceDoc(Path file) {
        options.add("--reference-doc=" + file);
        return this;
    }

    /**
     * Use the specified file as a style reference in producing a docx or ODT file.
     * See <a href="https://pandoc.org/MANUAL.html#options-affecting-specific-writers">
     *          Options affecting specific writers
     *     </a>
     */
    public PandocCommandBuilder referenceDoc(URL url) {
        options.add("--reference-doc=" + url);
        return this;
    }

    /**
     * Specify the heading level at which to split an EPUB or chunked HTML document into separate files. The default is
     * to split into chapters at level-1 headings. In the case of EPUB, this option only affects the internal
     * composition of the EPUB, not the way chapters and sections are displayed to users. Some readers may be slow
     * if the chapter files are too large, so for large documents with few level-1 headings, one might want to use
     * a chapter level of 2 or 3. For chunked HTML, this option determines how much content goes in each “chunk.”
     */
    public PandocCommandBuilder splitLevel(int number) {
        options.add("--split-level=" + number);
        return this;
    }

    /**
     * Specify a template for the filenames in a chunkedhtml document. In the template, %n will be replaced by the chunk
     * number (padded with leading 0s to 3 digits), %s with the section number of the chunk, %h with the heading text
     * (with formatting removed), %i with the section identifier. For example, %section-%s-%i.html might be resolved to
     * section-1.1-introduction.html. The characters / and \ are not allowed in chunk templates and will be ignored.
     * The default is %s-%i.html.
     * @param pathTemplate : by default is %s-%i.html
     */
    public PandocCommandBuilder chunkTemplate(String pathTemplate) {
        options.add("--chunk-template=" + pathTemplate);
        return this;
    }

    /**
     * Use the specified image as the EPUB cover. It is recommended that the image be less than 1000px in width and
     * height. Note that in a Markdown source document you can also specify cover-image in a YAML metadata block
     * (see EPUB Metadata, below).
     */
    public PandocCommandBuilder epubCoverImage(Path file) {
        options.add("--epub-cover-image=" + file);
        return this;
    }

    /**
     * Determines whether a title page is included in the EPUB (default is true).
     */
    public PandocCommandBuilder epubTitlePage(Boolean addTitle) {
        options.add("--epub-title-page=" + addTitle);
        return this;
    }

    /**
     *Look in the specified XML file for metadata for the EPUB. The file should contain a series of Dublin Core elements
     *  For example:
     *  <dc:rights>Creative Commons</dc:rights>
     *  <dc:language>es-AR</dc:language>
     * By default, pandoc will include the following metadata elements:
     * <dc:title> (from the document title),
     * <dc:creator> (from the document authors),
     * <dc:date> (from the document date, which should be in ISO 8601 format),
     * <dc:language> (from the lang variable, or, if is not set, the locale),
     * and <dc:identifier id="BookId"> (a randomly generated UUID).
     * Any of these may be overridden by elements in the metadata file.
     * Note: if the source document is Markdown, a YAML metadata block in the document can be used instead.
     * See <a href="https://pandoc.org/MANUAL.html#epub-metadata">EPUB Metadata</a>.
     */
    public PandocCommandBuilder epubMetadata(Path file) {
        options.add("--epub-metadata=" + file);
        return this;
    }

    /**
     * Embed the specified font in the EPUB. This option can be repeated to embed multiple fonts. Wildcards can also be
     * used: for example, DejaVuSans-*.ttf. However, if you use wildcards on the command line, be sure to escape them
     * or put the whole filename in single quotes, to prevent them from being interpreted by the shell.
     * To use the embedded fonts, you will need to add declarations like the following to your CSS
     * (see {@link #linkToCss(URL)}):
     * {@code
     * @font-face {
     *    font-family: DejaVuSans;
     *    font-style: normal;
     *    font-weight: normal;
     *    src:url("../fonts/DejaVuSans-Regular.ttf");
     * }
     * @font-face {
     *    font-family: DejaVuSans;
     *    font-style: normal;
     *    font-weight: bold;
     *    src:url("../fonts/DejaVuSans-Bold.ttf");
     * }
     * @font-face {
     *    font-family: DejaVuSans;
     *    font-style: italic;
     *    font-weight: normal;
     *    src:url("../fonts/DejaVuSans-Oblique.ttf");
     * }
     * @font-face {
     *    font-family: DejaVuSans;
     *    font-style: italic;
     *    font-weight: bold;
     *    src:url("../fonts/DejaVuSans-BoldOblique.ttf");
     * }
     * body { font-family: "DejaVuSans"; }
     * }
     */
    public PandocCommandBuilder epubEmbedFont(String file) {
        options.add("--epub-embed-font=" + file);
        return this;
    }

    /**
     * Specify the subdirectory in the OCF container that is to hold the EPUB-specific contents. The default is EPUB.
     * To put the EPUB contents in the top level, use an empty string.
     */
    public PandocCommandBuilder epubSubdirectory(String dir) {
        options.add("--epub-subdirectory=" + dir);
        return this;
    }

    /**
     * Determines how ipynb output cells are treated.
     * all means that all the data formats included in the original are preserved.
     * none means that the contents of data cells are omitted.
     * best causes pandoc to try to pick the richest data block in each output cell that is compatible
     * with the output format.
     * The default is best.
     * @param option : all|none|best
     */
    public PandocCommandBuilder ipynbOutput(String option) {
        options.add("--ipynb-output=" + option);
        return this;
    }

    /**
     * Use the specified engine when producing PDF output. Valid values are pdflatex, lualatex, xelatex, latexmk,
     * tectonic, wkhtmltopdf, weasyprint, pagedjs-cli, prince, context, pdfroff, and typst.
     * If the engine is not in your PATH, the full path of the engine may be specified here.
     * If this option is not specified, pandoc uses the following defaults depending on the output format
     * specified using {@link #formatTo(String)}:
     * latex or none: pdflatex (other options: xelatex, lualatex, tectonic, latexmk)
     * context: context
     * html: wkhtmltopdf (other options: prince, weasyprint, pagedjs-cli; see print-css.rocks for a good introduction
     *      to PDF generation from HTML/CSS)
     * ms: pdfroff
     * typst: typst
     * @param program : pdflatex, lualatex, xelatex, latexmk, tectonic, wkhtmltopdf, weasyprint, pagedjs-cli, prince,
     *                context, pdfroff, and typst
     */
    public PandocCommandBuilder pdfEngine(String program) {
        options.add("--pdf-engine=" + program);
        return this;
    }

    /**
     * Use the given string as a command-line argument to the pdf-engine (see {@link #pdfEngine(String)}).
     * For example, to use a persistent directory foo for latexmk’s auxiliary files, use --pdf-engine-opt=-outdir=foo.
     * Note that no check for duplicate options is done
     */
    public PandocCommandBuilder pdfEngineOption(String option) {
        options.add("--pdf-engine-opt=" + option);
        return this;
    }

    /**
     * Process the citations in the file, replacing them with rendered citations and adding a bibliography. Citation
     * processing will not take place unless bibliographic data is supplied, either through an external file specified
     * using the {@link #bibliography(Path)} or the bibliography field in metadata, or via a references section in
     * metadata containing a list of citations in CSL YAML format with Markdown formatting. The style is controlled by
     * a CSL stylesheet specified using the {@link #csl(Path)} option or the csl field in metadata.
     * (If no stylesheet is specified, the chicago-author-date style will be used by default.)
     * The citation processing transformation may be applied before or after filters or Lua filters
     * (see {@link #filter(Path)}, {@link #luaFilter(Path)}):
     * these transformations are applied in the order they appear on the command line.
     * For more information, see <a href="https://pandoc.org/MANUAL.html#citations">Citations</a>
     */
    public PandocCommandBuilder citationProcessing() {
        options.add("--citeproc");
        return this;
    }

    /**
     * Set the bibliography field in the document’s metadata to FILE, overriding any value set in the metadata.
     * If you supply this argument multiple times, each FILE will be added to bibliography.
     * If FILE is not found relative to the working directory,
     * it will be sought in the resource path (see {@link #resourcePath(String)}).
     */
    public PandocCommandBuilder bibliography(Path file) {
        options.add("--bibliography=" + file);
        return this;
    }

    /**
     * Set the bibliography field in the document’s metadata to URL, overriding any value set in the metadata.
     * If you supply this argument multiple times, each URL will be added to bibliography.
     * File will be fetched via HTTP.
     * it will be sought in the resource path (see {@link #resourcePath(String)}).
     */
    public PandocCommandBuilder bibliography(URL url) {
        options.add("--bibliography=" + url);
        return this;
    }

    /**
     * Set the csl field in the document’s metadata to FILE, overriding any value set in the metadata. 
     * (This is equivalent to #metadata(csl, file)) 
     * If FILE is not found relative to the working directory, it will be sought in the resource path 
     * (see {@link #resourcePath(String)}) and finally in the csl subdirectory of the pandoc user data directory.
     */
    public PandocCommandBuilder csl(Path file) {
        options.add("--csl=" + file);
        return this;
    }

    /**
     * Set the csl field in the document’s metadata to URL, overriding any value set in the metadata.
     * (This is equivalent to #metadata(csl, url)). File will be fetched via HTTP
     * It will be sought in the resource path (see {@link #resourcePath(String)})
     * and finally in the csl subdirectory of the pandoc user data directory.
     */
    public PandocCommandBuilder csl(URL url) {
        options.add("--csl=" + url);
        return this;
    }

    /**
     * Set the citation-abbreviations field in the document’s metadata to FILE, overriding any value set in the metadata
     * (This is equivalent to #metadata(citation-abbreviations, url))
     * If FILE is not found relative to the working directory, it will be sought in the resource path
     * (see {@link #resourcePath(String)}) and finally in the csl subdirectory of the pandoc user data directory.
     */
    public PandocCommandBuilder citationAbbreviation(Path file) {
        options.add("--citation-abbreviations=" + file);
        return this;
    }

    /**
     * Set the citation-abbreviations field in the document’s metadata to FILE, overriding any value set in the metadata
     * (This is equivalent to #metadata(citation-abbreviations, url)) File will be fetched via HTTP
     * File will be sought in the resource path (see {@link #resourcePath(String)})
     * and finally in the csl subdirectory of the pandoc user data directory.
     */
    public PandocCommandBuilder citationAbbreviation(URL url) {
        options.add("--citation-abbreviations=" + url);
        return this;
    }

    /**
     *Use natbib for citations in LaTeX output. This option is not for use with the {@link #citationProcessing()} option
     *  or with PDF output. It is intended for use in producing a LaTeX file that can be processed with bibtex.
     */
    public PandocCommandBuilder natlib() {
        options.add("--natlib");
        return this;
    }

    /**
     * Use biblatex for citations in LaTeX output. This option is not for use with the {@link #citationProcessing()}
     * or with PDF output. It is intended for use in producing a LaTeX file that can be processed with bibtex or biber.
     */
    public PandocCommandBuilder biblatex() {
        options.add("--biblatex");
        return this;
    }

    /**
     * Use MathJax to display embedded TeX math in HTML output. TeX math will be put between \(...\) (for inline math)
     * or \[...\] (for display math) and wrapped in <span> tags with class math. Then the MathJax JavaScript will
     * render it. The URL should point to the MathJax.js load script. If a URL is not provided,
     * a link to the Cloudflare CDN will be inserted.
     */
    public PandocCommandBuilder mathJax(Optional<URL> url) {
        if (url.isEmpty()) {
            options.add("--mathjax");
        } else {
            options.add("--mathjax=" + url.get());
        }
        return this;
    }

    /**
     * Convert TeX math to MathML (in epub3, docbook4, docbook5, jats, html4 and html5). This is the default
     * in odt output. MathML is supported natively by the main web browsers and select e-book readers.
     */
    public PandocCommandBuilder mathML() {
        options.add("--mathml");
        return this;
    }

    /**
     * Convert TeX formulas to <img> tags that link to an external script that converts formulas to images. The formula
     * will be URL-encoded and concatenated with the URL provided. For SVG images you can for example use
     * --webtex https://latex.codecogs.com/svg.latex?. If no URL is specified, the CodeCogs URL generating PNGs will be
     * used (https://latex.codecogs.com/png.latex?). Note: the --webtex option will affect Markdown output as well as
     * HTML, which is useful if you’re targeting a version of Markdown without native math support.
     */
    public PandocCommandBuilder webTex(Optional<URL> url) {
        if (url.isEmpty()) {
            options.add("--webtex");
        } else {
            options.add("--webtex " + url.get());
        }
        return this;
    }

    /**
     * Use KaTeX to display embedded TeX math in HTML output. The URL is the base URL for the KaTeX library.
     * That directory should contain a katex.min.js and a katex.min.css file.
     * If a URL is not provided, a link to the KaTeX CDN will be inserted.
     */
    public PandocCommandBuilder kaTex(Optional<URL> url) {
        if (url.isEmpty()) {
            options.add("--katex");
        } else {
            options.add("--katex " + url.get());
        }
        return this;
    }

    /**
     * Enclose TeX math in <eq> tags in HTML output. The resulting HTML can then be processed by GladTeX to produce
     * SVG images of the typeset formulas and an HTML file with these images embedded.
     * pandoc -s --gladtex input.md -o myfile.htex
     * gladtex -d image_dir myfile.htex
     * # produces myfile.html and images in image_dir
     */
    public PandocCommandBuilder gladtex() {
        options.add("--gladtex");
        return this;
    }

    /**
     * Print information about command-line arguments to stdout, then exit. This option is intended primarily for use
     * in wrapper scripts. The first line of output contains the name of the output file specified with the -o option,
     * or - (for stdout) if no output file was specified. The remaining lines contain the command-line arguments,
     * one per line, in the order they appear. These do not include regular pandoc options and their arguments,
     * but do include any options appearing after a -- separator at the end of the line.
     */
    public PandocCommandBuilder dumpArgs() {
        options.add("--dump-args");
        return this;
    }

    /**
     * Ignore command-line arguments (for use in wrapper scripts). Regular pandoc options are not ignored. For example,
     * pandoc --ignore-args -o foo.html -s foo.txt -- -e latin1
     * is equivalent to
     * pandoc -o foo.html -s
     */
    public PandocCommandBuilder ignoreArgs() {
        options.add("--ignore-args");
        return this;
    }
}

