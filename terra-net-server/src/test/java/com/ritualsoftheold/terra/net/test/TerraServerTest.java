package com.ritualsoftheold.terra.net.test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ForkJoinPool;

import org.junit.Before;

import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.net.TerraMessages;
import com.ritualsoftheold.terra.net.server.SendingLoadListener;
import com.ritualsoftheold.terra.net.server.WorldObserver;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler.PanicResult;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;
import com.starandserpent.venom.UdpConnection;
import com.starandserpent.venom.ConnectionStateListener.DisconnectReason;
import com.starandserpent.venom.flow.FrameManager;
import com.starandserpent.venom.flow.MessageSender;
import com.starandserpent.venom.hook.VenomHook;
import com.starandserpent.venom.listeners.ListenerMessageHandler;
import com.starandserpent.venom.listeners.Listeners;
import com.starandserpent.venom.message.PartialSentMessage;
import com.starandserpent.venom.message.SentMessage;
import com.starandserpent.venom.server.UdpServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class TerraServerTest {
    
    private OffheapWorld world;
    private UdpServer server;
    
    public void launch() {
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);
        
        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt");
        mod.newMaterial().name("grass");
        MaterialRegistry reg = new MaterialRegistry();
        mod.registerMaterials(reg);
        
        WorldGenerator<?> gen = new TestWorldGenerator();
        gen.setup(0, reg);
        
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(gen)
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(reg) // TODO server sends this
                .memorySettings(10000000, 10000000, new MemoryPanicHandler() {
                    
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        
        SendingLoadListener listener = new SendingLoadListener(world, ByteBufAllocator.DEFAULT);
        world.setLoadListener(listener);
        
        server = new UdpServer.Builder()
                .handlerProvider((address) -> {
                    return (conn, msg, flags) -> {
                        LoadMarker marker = world.createLoadMarker(0, 10, 0, 32, 32, 0);
                        world.addLoadMarker(marker);
                        
                        WorldObserver observer = new WorldObserver(marker, conn, 10);
                        listener.addObserver(observer);
                        conn.setHook(new VenomHook() {
                            
                            @Override
                            public boolean writeRequest(int amount) {
                                //System.out.println("request: " + amount);
                                return true;
                            }
                            
                            @Override
                            public boolean reliableReceived(int msgId, boolean partial, boolean urgent,
                                    int index) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean reliableConfirmed(boolean partial, int status, int msgId) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean reliableConfirmationWrite(ByteBuf buf) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean reliableCheck(int id, SentMessage msg, boolean resend) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean packetWrite(ByteBuf packet) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean packetReceived(byte type, ByteBuf data) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean nextFrame(FrameManager manager, MessageSender sender) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public void messageWritten(SentMessage msg) {
                                
                            }
                            
                            @Override
                            public boolean messageWrite(SentMessage msg, boolean urgent) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean messageReceived(byte flags, int msgId, ByteBuf data) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean messagePartReceived(byte flags, int msgId, int index,
                                    ByteBuf data) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public boolean flowStateChange(int state) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public void flowPingAnalysis(int longPing, int shortPing) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                            @Override
                            public boolean flowControlFrame(int ping, int id, int pressure) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                            
                            @Override
                            public void disconnected(UdpConnection deadConn, DisconnectReason reason) {
                                // TODO Auto-generated method stub
                                
                            }
                            
                            @Override
                            public boolean connected(InetSocketAddress address) {
                                // TODO Auto-generated method stub
                                return true;
                            }
                        });
                        
                        world.updateLoadMarkers();
                    };
                })
                .build();
        try {
            server.listen(1234);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String... args) {
        new TerraServerTest().launch();
    }
}
