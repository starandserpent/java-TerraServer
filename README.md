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
* terra-testgame: simple, ugly, singleplayer voxel world
  * Does not use nearly all Terra features
  * Did I mention it is ugly?
  
Dependency tree:
* terra-core
  * terra-meshgen
  * terra-offheap
    * terra-io-files
    * terra-net-core
      * terra-net-server
      * terra-net-client
      
terra-testgame depends on everything but networking parts of Terra.
