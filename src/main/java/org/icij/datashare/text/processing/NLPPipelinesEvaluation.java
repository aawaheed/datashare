package org.icij.datashare.text.processing;

import java.io.*;
import java.nio.file.*;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.*;

import org.icij.datashare.text.Language;
import org.icij.datashare.text.reading.DocumentParser;
import org.icij.datashare.text.reading.DocumentParserException;
import org.icij.datashare.util.io.FileSystemUtils;

import static org.icij.datashare.util.function.ThrowingFunctions.getProperty;
import static org.icij.datashare.util.function.ThrowingFunctions.path;
import static org.icij.datashare.util.function.ThrowingFunctions.splitComma;


/**
 * Created by julien on 3/11/16.
 */
public class NLPPipelinesEvaluation {

    private static final Logger logger = Logger.getLogger(NLPPipelinesEvaluation.class.getName());


    private Path corporaDir;

    private Map<String, String> corpora;

    private String sourceDir;

    private String resultDir;

    private List<String> entityTypes;

    private DocumentParser parser;

    private Level loggerLevel;

    private Properties properties;


    public Path getCorporaDir() {
        return corporaDir;
    }

    public Set<String> getCorpora() {
        return corpora.keySet();
    }


    public NLPPipelinesEvaluation() {
        try {
            initialize();
        } catch (IOException e) {
            logger.log(SEVERE, NLPPipeline.class.getName() + " failed to initialize.", e);
        }
    }

    private void initialize() throws IOException {

        properties = new Properties();
        properties.load( this.getClass().getResourceAsStream(this.getClass().getName() + ".properties") );

        String userDir = System.getProperty("user.dir");

        entityTypes   = getProperty("eval.entity.types", properties, splitComma).orElse(new ArrayList<>());
        corporaDir    = getProperty("eval.corpus.basedir", properties, path).orElse(Paths.get(userDir));
        sourceDir     = getProperty("eval.corpus.srcdir", properties, String::trim).orElse("");
        resultDir     = getProperty("eval.corpus.procdir", properties, String::trim).orElse("");
        corpora       = getProperty("eval.corpus.names", properties, splitComma).orElse(new ArrayList<>())
                .stream()
                .collect(Collectors.toMap(c -> c, c -> corporaDir + "/" + c));
        loggerLevel   = getProperty("eval.logger.level", properties, Level::parse).orElse(INFO);

        initializeLogger();

        logger.config( "Corpora directory: " + corporaDir  );
        logger.config( "Source files directory: " + sourceDir   );
        logger.config( "Result files directory: " + resultDir);
        logger.config( "Entity types: " + entityTypes);
    }

    private void initializeLogger() {
        for (Handler handler : logger.getHandlers()) { logger.removeHandler(handler); }
        Handler handler = new ConsoleHandler();
        handler.setLevel(loggerLevel);
        logger.addHandler(handler);
        logger.setLevel(loggerLevel);
    }

    public void run(String corpus) {
        if (corpus == null || corpus.isEmpty()) {
            logger.log(SEVERE, "Corpus is undefined. Aborting");
            return;
        }

        String corpusBaseDir = corpora.get(corpus);
        if (corpusBaseDir == null || corpusBaseDir.isEmpty()) {
            logger.log(SEVERE, "Undefined directory for " + corpus + ". Aborting");
            return;
        }

        Path corpusSourcePath = Paths.get(corpusBaseDir, sourceDir);
        logger.log(INFO, " |> Evaluating Corpus " + corpus);
        try {
            Map<String, NLPPipeline> extractors;
            NLPPipeline              opennlpNER;
            NLPPipeline              corenlpNER;

            // List and filter all files in corpus source directory to be processed
            for (Path sourceFilePath : FileSystemUtils.listFilesInDirectory(corpusSourcePath)) {
                Path resultFilePath = getProcessedFilePath(corpus, sourceFilePath);
                if (Files.exists(resultFilePath)) {
                    // Skip, processed file already exists
                    logger.log(FINE, "Skipped " + sourceFilePath);
                    logger.log(FINE, "        " + resultFilePath + " exists)");

                } else {
                    // Process file
                    Optional<String> content = parse(sourceFilePath);
                    if (content.isPresent()) {
                        String cont = content.get().replaceAll("[\\n\\s]+", " ");
                        cont = cont.substring(0, Math.min(cont.length(), 120));
                        logger.log(FINE, "Content excerpt: " + cont);

                        Optional<Language> lang = parser.getLanguage();
                        logger.log(FINE, "Detected language is " + lang.get().toString());

                        /*
                    opennlpNER = new OpenNLPNamedEntityExtractor(lang);
                    corenlpNER = new CoreNLPNamedEntityExtractor(lang);
                    extractors = new HashMap<>();
                    extractors.put(opennlpNER.getClass().getName(), opennlpNER);
                    extractors.put(corenlpNER.getClass().getName(), corenlpNER);

                    NamedEntityExtractor extractor;
                    Map<String, List<String>> result;
                    List<String> lines = new ArrayList<>();
                    for (String extractorName : extractors.keySet()) {
                        extractor = extractors.get(extractorName);
                        result = run(extractor, content);
                        lines.addAll( formatProcessed(corpus, sourceFilePath, extractorName, result) );
                    }
                    FileSystemUtils.write(resultFilePath, lines, StandardCharsets.UTF_8);
                    */
                    }
                }
            }
        } catch (IOException e) {
            logger.log(SEVERE, "Failed to process " + corpus, e);
        }
        logger.log(FINE, "< Corpus " + corpus + "Evaluated");
    }

    private Optional<Map<String, List<String>>> run(NLPPipeline extractor, Path filePath) {
        Optional<String> content = parse(filePath);
        if (content.isPresent())
            return Optional.of(run(extractor, content.get()));
        return Optional.empty();
    }

    private Map<String, List<String>> run(NLPPipeline extractor, String content) {
        try {
            // ! NOTHING RETURNED YET
            extractor.run(content);
            return new HashMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private Optional<String> parse(Path filePath) {
        Optional<DocumentParser> parserOpt = DocumentParser.create();
        if ( ! parserOpt.isPresent()) {
            logger.log(WARNING, "No valid parser set. Did not parse anything");
            return Optional.empty();
        }
        parser = parserOpt.get();
        logger.log(INFO, "|> Parsing " + filePath);
        Optional<String> parsed = parser.parse(filePath);
        logger.log(INFO, "<| Parsed " + filePath);
        return parsed;
    }

    private Path getProcessedFilePath(String corpus, Path sourceFilePath) {
        String fileName = String.join("-", corpus, sourceFilePath.getFileName().toString(), "eval.psv");
        return corporaDir.resolve(Paths.get(corpus, resultDir, fileName));
    }

    private List<String> formatProcessed(String corpus, Path sourceFilePath,
                                         String extractorName, Map<String, List<String>> extraction) {
        List<String> lines = new ArrayList<>();
        for (String type: entityTypes) {
            List<String> entities = new ArrayList<>();
            String key = "NER_" + type;
            if (extraction.containsKey(key)) {
                entities = extraction.get(key);
            }
            String extractedEntities = String.join(";", entities);
            List<String> fields = Arrays.asList(corpus, sourceFilePath.toString(), extractorName, type, extractedEntities);
            lines.add(String.join("|", fields));
        }
        return lines;
    }

}
