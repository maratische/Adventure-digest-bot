package maratische.telegram.pvddigest;

import maratische.telegram.pvddigest.model.PostEvents;
import maratische.telegram.pvddigest.model.PostStatuses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;

import static maratische.telegram.pvddigest.model.PostEvents.*;
import static maratische.telegram.pvddigest.model.PostStatuses.*;

@Configuration
@EnableJpaRepositories//("org.springframework.statemachine.data.jpa")
//@EntityScan("org.springframework.statemachine.data.jpa")
//@EnableStateMachine
@EnableStateMachineFactory
public class PostStateMachineConfig extends StateMachineConfigurerAdapter<PostStatuses, PostEvents> {


    @Autowired
    private JpaStateMachineRepository stateMachineRepository;

    @Bean
    public StateMachinePersist<String, String, String> stateMachinePersist() {
        return new JpaStateMachinePersist<>(stateMachineRepository);
    }

    @Bean
    public StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister(
            StateMachinePersist<String, String, String> stateMachinePersist) {
        return new DefaultStateMachineRuntimePersister<>(stateMachinePersist);
    }

    @Override
    public void configure(StateMachineStateConfigurer<PostStatuses, PostEvents> states) throws Exception {
        states
                .withStates()
                .initial(DRAFT)
                .state(MODERATING)
                .state(REJECTED)
                .state(PUBLISHED)
                .end(CLOSED);    // Завершён
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PostStatuses, PostEvents> transitions) throws Exception {
        transitions
                .withExternal()
                .source(DRAFT).target(MODERATING).event(MODERATE).action(action())
                .and()
                .withExternal()
                .source(MODERATING).target(REJECTED).event(REJECT).guard(тут гуард)
                .and()
                .withExternal()
                .source(MODERATING).target(PUBLISHED).event(PUBLISH).secured(проверить секурити ?)
                .and()
                .withExternal()
                .source(PUBLISHED).target(CLOSED).event(CLOSE).timer(таймер повесить !);
    }

    @Bean
    public Action<PostStatuses, PostEvents> action() {
        return new Action<PostStatuses, PostEvents>() {

            @Override
            public void execute(StateContext<PostStatuses, PostEvents> context) {
                // do something
                System.out.println("action " + context);
            }
        };
    }
}
