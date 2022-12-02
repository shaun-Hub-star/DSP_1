package org.dsp.Manager.Threads;

public interface Terminatable {

    void terminateSystem();
    void notifyTerminationMessageOccurred(); //occurred
    boolean systemTerminated();
    boolean receivedTerminationMessage();
}
