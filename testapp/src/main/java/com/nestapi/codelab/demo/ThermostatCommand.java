package com.nestapi.codelab.demo;

import com.firebase.client.Firebase;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

public abstract class ThermostatCommand implements Serializable, Firebase.CompletionListener {
    public Date date;

    public ThermostatCommand() {
        date = new Date();
    }
    public abstract void run();

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(date);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.date = (Date) in.readObject();
    }
}
