package io.github.ganyuke.invitation.ports;

public interface SchedulerPort {
    void runAsync(Runnable task);

    void runSync(Runnable task);
}
