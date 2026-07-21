package io.github.ganyuke.invitation.core.ports;

public interface SchedulerPort {
    void runAsync(Runnable task);

    void runSync(Runnable task);
}
