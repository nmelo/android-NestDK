package com.nestapi.codelab.demo;

import android.content.Context;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The command queue can be used to calculate rate limitations, it also holds commands for {{holdTime}}
 * before they get sent to Firebase
 */
public class CommandQueue implements Serializable {
    private static final String TAG = "Command Queue";
    private ArrayList<ThermostatCommand> commands;
    private Timer commandTimer;
    private TimerTask commandTimerTask;
    private long holdTime;
    private String name;

    private boolean limit_reached;
    private Date limit_reached_date;

    // ******************************************************************************************

    public CommandQueue() {
        commands = new ArrayList<ThermostatCommand>();
        commandTimer = new Timer("commandTimer_" + name, true);
        holdTime = 5000;
    }

    public void initialize() {
        commandTimer = new Timer("commandTimer_" + name, true);
        holdTime = 5000;
    }

    public boolean acceptsCommands() {
        if(limit_reached) {
            if(new Date().getTime() - limit_reached_date.getTime() < 60000) {
                return false;
            }
            else {
                limit_reached = false;
                return true;
            }
        }

        purgeQueue();
        if (commands.size() <= 60) {
            return true;
        }
        else {
            return false;
        }
    }

    public void addCommand(final ThermostatCommand command) {
        //Check if we can accept commands
        if (!acceptsCommands()) {
            Log.i(TAG, "Not accepting commands right now");
            return;
        }

        // Cancel the previous command if there was one, and it hasn't executed
        if(commandTimerTask != null && commandTimerTask.scheduledExecutionTime() < new Date().getTime()) {
            Log.i(TAG, "Cancelling previous command");
            commandTimerTask.cancel();
        }
        else {
            if(commandTimerTask == null) {
                Log.i(TAG, "There were no pending commands");
            }
            else {
                Log.i(TAG, "NOT Cancelling previous command");
            }
        }
        //Then purge the queue
        if(commandTimer != null) {
            commandTimer.purge();
        }
        else {
            Log.e(TAG, "Command timer was null");
        }

        //Now add a new command to the queue
        commandTimerTask = new TimerTask() {
            @Override
            public void run() {
                command.run();
                commands.add(command);
            }
        };
        if(commandTimer != null) {
            commandTimer.schedule(commandTimerTask, holdTime);
        }
        else {
            Log.e(TAG, "Command timer was null");
        }
    }

    private void purgeQueue() {
        // Removes commands older than one hour
        int purge_count = 0;
        for(int i = 0; i < commands.size(); i++) {
            if (new Date().getTime() - commands.get(i).date.getTime() > 3600000) {
                commands.remove(i);
                purge_count += 1;
            }
        }
        Log.i(TAG, String.format("Purged %d commands", purge_count));
    }

    public void cancel() {
        commandTimer.cancel();
        commands.clear();
    }

    // ******************************************************************************************

    public long getHoldTime() {
        return holdTime;
    }

    public void setHoldTime(long holdTime) {
        this.holdTime = holdTime;
    }

    // ******************************************************************************************

    public static CommandQueue readQueue(Context context, String instanceName) {
        if(instanceName == null || instanceName.isEmpty()) {
            return null;
        }

        LocalPersistence persistence = new LocalPersistence();
        CommandQueue queue = (CommandQueue)persistence.readObjectFromFile(context, "command_queue_" + instanceName);

        // if nothing came back, just create an empty queue
        if(queue == null) {
            queue = new CommandQueue();
        }

        queue.initialize();
        queue.name = instanceName;

        Log.i(TAG,  String.format("Got queue: %s with %d messages.", queue.name, queue.commands.size()));
        if (queue.commands.size() > 0) {
            Log.i(TAG, String.format("First message in queue: %s.", queue.commands.get(0).toString()));
        }
        return queue;
    }
    public static void writeQueue(Context context, CommandQueue queue) {
        LocalPersistence persistence = new LocalPersistence();
        if(queue != null && queue.name != null && !queue.name.isEmpty()) {
            persistence.witeObjectToFile(context, queue, "command_queue_" + queue.name);
            Log.i(TAG, String.format("Storing queue: %s with %d messages.", queue.name, queue.commands.size()));
        }
    }

    // ******************************************************************************************

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLimit_reached() {
        return limit_reached;
    }

    public void setLimit_reached(boolean limit_reached) {
        this.limit_reached = limit_reached;
    }

    public Date getLimit_reached_date() {
        return limit_reached_date;
    }

    public void setLimit_reached_date(Date limit_reached_date) {
        this.limit_reached_date = limit_reached_date;
    }

    // ******************************************************************************************

    public int size() {
        return commands.size();
    }

    // ******************************************************************************************

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        ArrayList<Date> dates = new ArrayList<Date>(commands.size());
        for (ThermostatCommand command: commands) {
            dates.add(command.date);
        }
        out.writeObject(dates);
        out.writeObject(name);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        ArrayList<Date> dates = (ArrayList<Date>) in.readObject();
        this.commands = new ArrayList<ThermostatCommand>(dates.size());
        for (Date date: dates) {
            ThermostatCommand command = new ThermostatCommand() {
                @Override
                public void run() {

                }

                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {

                }
            };
            command.date = date;
            commands.add(command);
        }

        this.name = (String) in.readObject();
    }
}
