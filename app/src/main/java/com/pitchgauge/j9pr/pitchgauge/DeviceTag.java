package com.pitchgauge.j9pr.pitchgauge;

import java.util.Objects;

public class DeviceTag {
    String uuid;
    String address;
    String name;
    int pos;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "DeviceTag{" +
                "address='" + address + '\'' +
                ", name='" + name + '\'' +
                ", pos=" + pos +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceTag deviceTag = (DeviceTag) o;
        return Objects.equals(getAddress(), deviceTag.getAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress());
    }
}
