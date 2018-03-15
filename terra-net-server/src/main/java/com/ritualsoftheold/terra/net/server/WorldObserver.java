package com.ritualsoftheold.terra.net.server;

import com.ritualsoftheold.terra.world.LoadMarker;
import com.starandserpent.venom.UdpConnection;

/**
 * Something that can observe a world over network.
 *
 */
public class WorldObserver {
    
    private LoadMarker marker;
    
    private UdpConnection conn;
    
    public WorldObserver(LoadMarker marker, UdpConnection conn) {
        this.marker = marker;
        this.conn = conn;
    }
    
    public LoadMarker getLoadMarker() {
        return marker;
    }
    
    public UdpConnection getConnection() {
        return conn;
    }
}
