package com.nestapi.codelab.demo;


import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nestapi.lib.API.Listener;
import com.nestapi.lib.API.Structure;
import com.nestapi.lib.API.Thermostat;

import java.util.ArrayList;

public class ThermostatAdapter extends BaseAdapter implements Listener.StructureListener, Listener.ThermostatListener {

    private static final String TAG = "ThermostatAdapter";

    private int layout;
    private final ListView drawerList;
    private LayoutInflater inflater;
    private final MainActivity mainActivity;

    private ArrayList<Thermostat> thermostatList;
    private ArrayList<Structure> structureList;

    public ThermostatAdapter(MainActivity mainActivity, int layout, ListView drawerList) {
        this.mainActivity = mainActivity;
        this.layout = layout;
        this.drawerList = drawerList;
        inflater = mainActivity.getLayoutInflater();

        thermostatList = new ArrayList<Thermostat>();
        structureList = new ArrayList<Structure>();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(layout, viewGroup, false);
        }

        // Call out to subclass to marshall this model into the provided view
        populateView(view, thermostatList.get(i));
        return view;
    }

    protected void populateView(View view, Thermostat thermostat) {
        // Map a Chat object to an entry in our listview
        TextView titleText = (TextView)view.findViewById(R.id.title_TextView);
        titleText.setText(thermostat.getName());
        titleText.setTag(thermostat);
    }

    @Override
    public int getCount() {
        return thermostatList.size();
    }

    @Override
    public Object getItem(int i) {
        return thermostatList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return thermostatList.get(i).hashCode();
    }

    @Override
    public void onThermostatUpdated(@NonNull Thermostat thermostat) {
        Log.v(TAG, String.format("Thermostat (%s) updated.", thermostat.getDeviceID()));

        boolean found = false;
        for(int i = 0; i < thermostatList.size(); i++) {
            if (thermostatList.get(i).getDeviceID().equals(thermostat.getDeviceID())) {
                thermostatList.set(i, thermostat);
                found = true;
                break;
            }
        }
        if (!found) {
            thermostatList.add(thermostat);
        }
    }

    @Override
    public void onStructureUpdated(@NonNull Structure structure) {
        Log.v(TAG, String.format("Structure (%s) updated.", structure.getStructureID()));

        boolean found = false;
        for(int i = 0; i < structureList.size(); i++) {
            if (structureList.get(i).getStructureID().equals(structure.getStructureID())) {
                structureList.set(i, structure);
                found = true;
                break;
            }
        }
        if (!found) {
            structureList.add(structure);
        }

        if ( drawerList.getAdapter() == null) {
            drawerList.setAdapter(this);
            // Set the list's click listener
            drawerList.setOnItemClickListener(mainActivity);
        }
    }
}
