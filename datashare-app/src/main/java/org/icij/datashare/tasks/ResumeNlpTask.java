package org.icij.datashare.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.icij.datashare.Entity;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.cli.DatashareCli;
import org.icij.datashare.extract.DocumentCollectionFactory;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.datashare.text.nlp.Pipeline;
import org.icij.datashare.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.icij.datashare.cli.DatashareCliOptions.NLP_PIPELINE_OPT;

public class ResumeNlpTask extends PipelineTask<String> {
    Logger logger = LoggerFactory.getLogger(getClass());
    private final Pipeline.Type nlpPipeline;
    private final User user;
    private final String projectName;
    private final Indexer indexer;

    @Inject
    public ResumeNlpTask(final DocumentCollectionFactory<String> factory, final Indexer indexer,
                         @Assisted final User user, @Assisted final String queueName, @Assisted final Properties taskProperties) {
        super(DatashareCli.Stage.NLP, user, queueName, factory, new PropertiesProvider(taskProperties), String.class);
        this.indexer = indexer;
        this.nlpPipeline = Pipeline.Type.parse(taskProperties.getProperty(NLP_PIPELINE_OPT));
        this.user = user;
        this.projectName = ofNullable(taskProperties.getProperty("defaultProject")).orElse("local-datashare");
    }

    @Override
    public Long call() throws IOException {
        Indexer.Searcher searcher = indexer.search(singletonList(projectName), Document.class).without(nlpPipeline).withSource("rootDocument");
        logger.info("resuming NLP name finding for index {} and {} : {} documents found", projectName, nlpPipeline, searcher.totalHits());
        List<? extends Entity> docsToProcess = searcher.scroll().collect(toList());
        long totalHits = searcher.totalHits();

        do {
            docsToProcess.forEach(doc -> queue.add(doc.getId()));
            docsToProcess = searcher.scroll().collect(toList());
        } while (!docsToProcess.isEmpty());
        logger.info("enqueued into {} {} files without {} pipeline tags", queue.getName(), totalHits, nlpPipeline);
        searcher.clearScroll();

        return totalHits;
    }

    @Override
    public User getUser() { return user;}
}
