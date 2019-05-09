# Terra
Terra is a voxel engine composed of multiple modules. Currently, those are:

* terra-core: public, safe and documented APIs
  * For manipulating blocks in the world, you only need this
* terra-meshgen: mesh generators to visualize data
  * Also, texture management utilies
  * Uses only safe terra-core APIs; you'll need to provide the renderer
* terra-offheap: implementation of terra-core that uses offheap memory
  * NOT A SAFE, PUBLIC API (memory corruption is *possible* if misused)
  * Direct usage in application code should be minimized
* terra-io-files: terra-offheap data saving and loading using files
* terra-net-*: networking server and client using Aeron with terra-offheap
  * Not a full solution for multiplayer voxel game
  
Terra dependency tree:
* terra-core
  * terra-meshgen
    * terra-offheap
        * TestGame repository
        * terra-io-files
        * terra-net-core
            * terra-net-server
            * terra-net-client
      
terra-testgame depends on everything but networking parts of Terra.
  * terra-offheap
    * terra-io-files
    * terra-net-core
      * terra-net-server
      * terra-net-client

## Other projects using Terra
**terra-test-game** Test game for Terra. Depends on everything but networking parts of Terra. Uses Weltschmerz and JavaMonkeyEngine.

**weltschmerz** Terrain generator for Terra.

## Instructions
**Step 1:**

```
git clone https://github.com/starandserpent/terra-test-game --recursive
```

**Step 2:**

Compile and run src/main/java/com/ritualsoftheold/testgame/TestGame.java

## License
Licensed under MIT. See LICENSE for more information.

## Questions, feedback, and contacting us

### Discord chat
https://discord.ritualsoftheold.com/

### Website, email, and more links
https://www.starandserpent.com

### Forums
https://community.ritualsoftheold.com/c/collaboration/terra
