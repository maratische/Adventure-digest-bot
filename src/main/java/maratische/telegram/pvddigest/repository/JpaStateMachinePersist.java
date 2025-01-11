package maratische.telegram.pvddigest.repository;

//import maratische.telegram.pvddigest.model.StateMachineEntity;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.statemachine.StateMachineContext;
//import org.springframework.statemachine.StateMachinePersist;
//import org.springframework.statemachine.support.DefaultStateMachineContext;
//import org.springframework.stereotype.Component;

//@Component
//public class JpaStateMachinePersist implements StateMachinePersist<String, String, String> {
//
//    @Autowired
//    private StateMachineRepository stateMachineRepository;
//
//    @Override
//    public void write(StateMachineContext<String, String> context, String contextObj) throws Exception {
//        // Сохранение состояния в базу данных
//        StateMachineEntity entity = new StateMachineEntity();
//        entity.setMachineId(contextObj);
//        entity.setState(context.getState());
//        stateMachineRepository.save(entity);
//    }
//
//    @Override
//    public StateMachineContext<String, String> read(String contextObj) throws Exception {
//        // Чтение состояния из базы данных
//        StateMachineEntity entity = stateMachineRepository.findById(contextObj).orElse(null);
//        if (entity != null) {
//            return new DefaultStateMachineContext<>(entity.getState(), null, null, null);
//        }
//        return null;
//    }
//}
