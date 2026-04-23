package com.mycompany.mavenproject1.store;

import com.mycompany.mavenproject1.model.Room;
import com.mycompany.mavenproject1.model.Sensor;
import com.mycompany.mavenproject1.model.SensorReading;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStore {

    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readingsBySensor = new ConcurrentHashMap<>();

    private InMemoryStore() {
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Sensor getSensor(String sensorId) {
        return sensors.get(sensorId);
    }

    public List<SensorReading> getReadings(String sensorId) {
        return readingsBySensor.computeIfAbsent(sensorId,
                key -> Collections.synchronizedList(new ArrayList<>()));
    }

    public void addReading(String sensorId, SensorReading reading) {
        List<SensorReading> readings = getReadings(sensorId);
        synchronized (readings) {
            readings.add(reading);
        }
    }
}
