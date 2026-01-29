package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.starters.SimilarityProcessor;
import ru.practicum.starters.ActionProcessor;

@EnableDiscoveryClient
@ConfigurationPropertiesScan
@SpringBootApplication
public class AnalyzerApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApp.class, args);
        SimilarityProcessor similarityProcessor = context.getBean(SimilarityProcessor.class);
        ActionProcessor actionProcessor = context.getBean(ActionProcessor.class);

        Runtime.getRuntime().addShutdownHook(new Thread(similarityProcessor::stop));
        Runtime.getRuntime().addShutdownHook(new Thread(actionProcessor::stop));

        Thread similarityProcessorThread = new Thread(similarityProcessor);
        similarityProcessorThread.setName("SimilarityProcessorThread");
        similarityProcessorThread.start();

        Thread actionProcessorThread = new Thread(actionProcessor);
        actionProcessorThread.setName("ActionProcessorThread");
        actionProcessorThread.start();
    }
}
