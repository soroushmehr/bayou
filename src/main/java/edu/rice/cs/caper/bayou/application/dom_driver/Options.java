package edu.rice.cs.caper.bayou.application.dom_driver;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Options {

    static String HELP =
        "Configuration options for driver:\n" +
        "{                                     |\n" +
        "  `api-classes`: [                    | Classes that driver should\n" +
        "      `java.io.BufferedReader`,       | extract data on. Must be fully\n" +
        "      `java.util.Iterator`            | qualified class names.\n" +
        "  ],                                  |\n" +
        "  `api-packages`: [                   | Packages that the driver\n" +
        "      `java.io`,                      | should extract data on.\n" +
        "      `java.net`                      |\n" +
        "  ],                                  |\n" +
        "  `api-modules`: [                    | Modules (for lack of a better\n" +
        "      `java`,                         | word) that driver should extract\n" +
        "      `javax`                         | data on.\n" +
        "  ],                                  |\n" +
        "  `num-unrolls`: 1,                   | Max unroll of loops in sequences\n" +
        "  `max-seqs`: 10,                     | Max num of sequences in sketches\n" +
        "  `javadoc-type`: `summary`           | `summary` (only first line),\n" +
        "                                      | `full` (everything)\n" +
        "}                                     |";

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input Java program")
                .build());

        opts.addOption(Option.builder("c")
                .longOpt("config-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("configuration JSON file")
                .build());

        opts.addOption(Option.builder("o")
                .longOpt("output-file")
                .hasArg()
                .numberOfArgs(1)
                .desc("output DSL AST to file")
                .build());
    }

    CommandLine cmdLine;
    JsonObject config;

    public final List<String> API_CLASSES;
    public final List<String> API_PACKAGES;
    public final List<String> API_MODULES;
    public final Map<String, Boolean> KNOWN_CONSTANTS_BOOLEAN;
    public final Map<String, Float> KNOWN_CONSTANTS_NUMBER;
    public final Map<String, String> KNOWN_CONSTANTS_STRING;
    public final int NUM_UNROLLS;
    public final int MAX_SEQS;
    public final int MAX_SEQ_LENGTH;
    public final String JAVADOC_TYPE;

    public Options(String[] args) throws ParseException, IOException {
        this.cmdLine = readCommandLine(args);
        this.config = readConfigFile(cmdLine.getOptionValue("config-file"));

        // API_CLASSES
        List<String> classes = new ArrayList<>();
        if (this.config.has("api-classes"))
            for (JsonElement e : this.config.getAsJsonArray("api-classes"))
                classes.add(e.getAsString());
        this.API_CLASSES = Collections.unmodifiableList(classes);

        // API_PACKAGES
        List<String> packages = new ArrayList<>();
        if (this.config.has("api-packages"))
            for (JsonElement e : this.config.getAsJsonArray("api-packages"))
                packages.add(e.getAsString());
        this.API_PACKAGES = Collections.unmodifiableList(packages);

        // API_MODULES
        List<String> modules = new ArrayList<>();
        if (this.config.has("api-modules"))
            for (JsonElement e : this.config.getAsJsonArray("api-modules"))
                modules.add(e.getAsString());
        this.API_MODULES = Collections.unmodifiableList(modules);

        // KNOWN_CONSTANTS_BOOLEAN
        Map<String, Boolean> kb = new HashMap<>();
        if (this.config.has("known-constants-boolean")) {
            JsonObject o = this.config.getAsJsonObject("known-constants-boolean");
            for (Map.Entry<String, JsonElement> e : o.entrySet())
                kb.put(e.getKey(), e.getValue().getAsBoolean());
        }
        this.KNOWN_CONSTANTS_BOOLEAN = Collections.unmodifiableMap(kb);

        // KNOWN_CONSTANTS_NUMBER
        Map<String, Float> kn = new HashMap<>();
        if (this.config.has("known-constants-number")) {
            JsonObject o = this.config.getAsJsonObject("known-constants-number");
            for (Map.Entry<String, JsonElement> e : o.entrySet())
                kn.put(e.getKey(), e.getValue().getAsFloat());
        }
        this.KNOWN_CONSTANTS_NUMBER = Collections.unmodifiableMap(kn);

        // KNOWN_CONSTANTS_STRING
        Map<String, String> ks = new HashMap<>();
        if (this.config.has("known-constants-string")) {
            JsonObject o = this.config.getAsJsonObject("known-constants-string");
            for (Map.Entry<String, JsonElement> e : o.entrySet())
                ks.put(e.getKey(), e.getValue().getAsString());
        }
        this.KNOWN_CONSTANTS_STRING = Collections.unmodifiableMap(ks);

        // NUM_UNROLLS
        if (this.config.has("num-unrolls"))
            this.NUM_UNROLLS = this.config.getAsJsonPrimitive("num-unrolls").getAsInt();
        else
            this.NUM_UNROLLS = 1;

        // MAX_SEQS
        if (this.config.has("max-seqs"))
            this.MAX_SEQS = this.config.getAsJsonPrimitive("max-seqs").getAsInt();
        else
            this.MAX_SEQS = 10;

        // MAX_SEQS
        if (this.config.has("max-seq-length"))
            this.MAX_SEQ_LENGTH = this.config.getAsJsonPrimitive("max-seq-length").getAsInt();
        else
            this.MAX_SEQ_LENGTH = 10;

        // Javadoc only
        if (this.config.has("javadoc-type")) {
            this.JAVADOC_TYPE = this.config.getAsJsonPrimitive("javadoc-type").getAsString();
            if (! (this.JAVADOC_TYPE.equals("full") || this.JAVADOC_TYPE.equals("summary"))) {
                throw new IllegalArgumentException("javadoc-type must be \"full\" or \"summary\"");
            }
        }
        else
            this.JAVADOC_TYPE = "summary";
    }

    private CommandLine readCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            return parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("driver", HELP.replace('`', '\"'), clopts, "", true);
            throw e;
        }
    }

    private JsonObject readConfigFile(String file) throws IOException {
        JsonParser parser = new JsonParser();
        File configFile = new File(file);

        return parser.parse(FileUtils.readFileToString(configFile, "utf-8")).getAsJsonObject();
    }
}
