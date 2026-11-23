
package com.smartfactory;

public class MachineData {

    private String name;
    private boolean running;
    private boolean fault;
    private double speed;
    private double temperature;
    private double load;

    public MachineData(String name) {
        this.name = name;
        this.running = false;
        this.fault = false;
        this.speed = 0;
        this.temperature = 35;
        this.load = 0;
    }

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isFault() {
        return fault;
    }

    public void setFault(boolean fault) {
        this.fault = fault;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }

    public String getStatus() {
        if (fault) {
            return "FAULT";
        }

        if (running) {
            return "RUNNING";
        }

        return "STOPPED";
    }
}
