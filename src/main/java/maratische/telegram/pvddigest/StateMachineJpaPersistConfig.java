package maratische.telegram.pvddigest;

import maratische.telegram.pvddigest.model.PostEvents;
import maratische.telegram.pvddigest.model.PostStatuses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;

@Configuration
public class StateMachineJpaPersistConfig {

    @Bean
    public StateMachineRuntimePersister<PostStatuses, PostEvents, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }

    @Bean
    public StateMachineConfigurerAdapter<PostStatuses, PostEvents> stateMachineConfig(
            StateMachineRuntimePersister<PostStatuses, PostEvents, String> stateMachineRuntimePersister) {
        return new StateMachineConfigurerAdapter<>() {
            @Override
            public void configure(StateMachineConfigurationConfigurer<PostStatuses, PostEvents> config) throws Exception {
                config
                        .withPersistence()
                        .runtimePersister(stateMachineRuntimePersister);
            }
        };
    }
}

