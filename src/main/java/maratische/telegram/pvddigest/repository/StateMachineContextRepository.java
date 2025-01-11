package maratische.telegram.pvddigest.repository;

import maratische.telegram.pvddigest.model.StateMachineContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateMachineContextRepository extends JpaRepository<StateMachineContextEntity, Long> {

    @Query("SELECT s FROM StateMachineContextEntity s WHERE s.state = :state")
    List<StateMachineContextEntity> findByState(@Param("state") String state);
}

