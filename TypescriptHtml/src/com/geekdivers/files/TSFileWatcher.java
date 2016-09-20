package com.geekdivers.files;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class TSFileWatcher implements Runnable {

    /**
     * the watchService that is passed in from above
     */
    private WatchService myWatcher;

    public TSFileWatcher(WatchService myWatcher) {
        this.myWatcher = myWatcher;
    }

    /**
     * In order to implement a file watcher, we loop forever ensuring requesting
     * to take the next item from the file watchers queue.
     */
    @Override
    public void run() {
        try {
            // get the first event before looping
            System.out.println("started...");
            for (;;) {
                WatchKey key = myWatcher.take();
                while (key != null) {
                    // we have a polled event, now we traverse it and 
                    // receive all the states from it
                    for (WatchEvent event : key.pollEvents()) {
                        System.out.printf("Received %s event for file: %s\n",
                                event.kind(), event.context());
                    }
                    key.reset();
                    key = myWatcher.take();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping thread");
    }
}
