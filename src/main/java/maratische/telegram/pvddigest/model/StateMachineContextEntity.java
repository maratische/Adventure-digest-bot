package maratische.telegram.pvddigest.model;

import jakarta.persistence.*;

@Entity
@Table(name = "state_machine_context")
public class StateMachineContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "machine_id", nullable = false, unique = true)
    private String machineId;

    @Column(name = "state", nullable = false)
    private String state;

    @Lob
    @Column(name = "context", nullable = false)
    private byte[] context;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public byte[] getContext() {
        return context;
    }

    public void setContext(byte[] context) {
        this.context = context;
    }
}

