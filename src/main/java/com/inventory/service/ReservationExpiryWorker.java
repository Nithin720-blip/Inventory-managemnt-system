package com.inventory.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background worker thread that periodically sweeps and auto-expires overdue active reservations.
 * Automatically releases held inventory back to available stock.
 */
public class ReservationExpiryWorker {

    private final ReservationService reservationService;
    private ScheduledExecutorService scheduler;
    private Consumer<Integer> onExpiryCallback;

    public ReservationExpiryWorker(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Registers a callback invoked whenever one or more reservations are expired.
     * The argument passed is the count of expired reservations in that sweep.
     */
    public void setOnExpiryCallback(Consumer<Integer> callback) {
        this.onExpiryCallback = callback;
    }

    /**
     * Starts the periodic background sweeper daemon thread.
     *
     * @param intervalSeconds frequency in seconds to check for expired reservations
     */
    public synchronized void start(int intervalSeconds) {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "reservation-expiry-sweeper");
            thread.setDaemon(true); // Daemon thread ensures JVM shuts down gracefully
            return thread;
        });

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int expiredCount = reservationService.expireOverdueReservations();
                if (expiredCount > 0 && onExpiryCallback != null) {
                    onExpiryCallback.accept(expiredCount);
                }
            } catch (Exception e) {
                System.err.println("[ReservationExpiryWorker] Error during sweep: " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        System.out.println("[ReservationExpiryWorker] Background sweeper started. Running every " + intervalSeconds + "s.");
    }

    /**
     * Stops the background sweeper thread.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            System.out.println("[ReservationExpiryWorker] Background sweeper stopped.");
        }
    }

    public synchronized boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
