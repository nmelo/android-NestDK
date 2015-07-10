package com.nestapi.codelab.demo;

import com.nestapi.lib.API.NestAPI;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

public abstract class ThermostatCommand implements Serializable, NestAPI.CompletionListener{
    public Date date;

    public ThermostatCommand() {
        date = new Date();
    }
    public abstract void run();
    public abstract void onComplete();
    public abstract void onError(int errorCode, String message);

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(date);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.date = (Date) in.readObject();
    }
}
