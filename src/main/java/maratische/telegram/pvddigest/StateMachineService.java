package maratische.telegram.pvddigest;

import maratische.telegram.pvddigest.model.PostEvents;
import maratische.telegram.pvddigest.model.PostStatuses;
import maratische.telegram.pvddigest.model.StateMachineContextEntity;
import maratische.telegram.pvddigest.repository.StateMachineContextRepository;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StateMachineService {

    private final StateMachineRuntimePersister<PostStatuses, PostEvents, String> stateMachineRuntimePersister;
    private final StateMachineFactory<PostStatuses, PostEvents> stateMachineFactory;
    private final StateMachineContextRepository stateMachineContextRepository;

    public StateMachineService(StateMachineRuntimePersister<PostStatuses, PostEvents, String> stateMachineRuntimePersister,
                               StateMachineFactory<PostStatuses, PostEvents> stateMachineFactory,
                               StateMachineContextRepository stateMachineContextRepository) {
        this.stateMachineRuntimePersister = stateMachineRuntimePersister;
        this.stateMachineFactory = stateMachineFactory;
        this.stateMachineContextRepository = stateMachineContextRepository;
    }

    public List<StateMachine<PostStatuses, PostEvents>> getMachinesInState(String state) {
        // Получаем контексты машин из базы
        List<StateMachineContextEntity> contexts = stateMachineContextRepository.findByState(state);

        // Восстанавливаем машины состояний
        return contexts.stream()
                .map(context -> {
                    StateMachine<PostStatuses, PostEvents> stateMachine = stateMachineFactory.getStateMachine(context.getMachineId());
//                    stateMachineRuntimePersister.restore(stateMachine, context.getMachineId());
                    return stateMachine;
                })
                .collect(Collectors.toList());
    }
}

