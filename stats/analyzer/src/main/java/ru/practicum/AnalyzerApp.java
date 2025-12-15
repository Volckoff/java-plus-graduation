package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.starters.EventSimilarityStarter;
import ru.practicum.starters.UserActionStarter;

@EnableDiscoveryClient
@SpringBootApplication
public class AnalyzerApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApp.class, args);

        final UserActionStarter userActionStarter =
                context.getBean(UserActionStarter.class);
        final EventSimilarityStarter eventSimilarityStarter =
                context.getBean(EventSimilarityStarter.class);

        Thread userActionThread = new Thread(userActionStarter);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        eventSimilarityStarter.run();
    }
}
