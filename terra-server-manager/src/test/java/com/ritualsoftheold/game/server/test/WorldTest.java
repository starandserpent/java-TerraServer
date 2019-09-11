package com.ritualsoftheold.game.server.test;

/**
 * Tests OffheapWorld.
 *
 */
/*
public class WorldTest {
    
    // FIXME broken, Palette16 requires functional materials to be available
    
    private static final Memory mem = OS.memory();
    
    private OffheapWorld world;

    @Before
    public void init() {
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);
        
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoaderInterface())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(new TestWorldGeneratorInterface())
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry())
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
        world.setLoadListener(new DummyLoadListener());
        
        world.addLoadMarker(world.createLoadMarker(0, 0, 0, 32, 32, 0));
        world.updateLoadMarkers().forEach((f) -> f.join());
    }
    
    @Test
    public void initTest() {
        // See init() above
    }
}
*/