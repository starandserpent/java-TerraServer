package com.ritualsoftheold.terra.core.client.test;

import static org.junit.Assert.assertEquals;

import com.ritualsoftheold.terra.server.manager.util.IntFlushList;
import org.junit.Before;
import org.junit.Test;

public class FlushListTest {

    private IntFlushList list;
    
    @Before
    public void init() {
        list = new IntFlushList(10, 2);
    }
    
    @Test
    public void addTest() {
        // This will also force list to enlarge backing array
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        
        // Test that they were put there correctly
        int[] array = list.getArray();
        for (int i = 0; i < 100; i++) {
            assertEquals(i, array[i]);
        }
    }
}
