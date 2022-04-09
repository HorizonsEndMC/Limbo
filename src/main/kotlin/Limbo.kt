package net.horizonsend.limbo

import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import net.minestom.server.MinecraftServer.getBiomeManager
import net.minestom.server.MinecraftServer.getDimensionTypeManager
import net.minestom.server.MinecraftServer.getGlobalEventHandler
import net.minestom.server.MinecraftServer.getInstanceManager
import net.minestom.server.MinecraftServer.init
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode.ADVENTURE
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.extras.velocity.VelocityProxy.enable
import net.minestom.server.instance.Chunk.CHUNK_SIZE_X
import net.minestom.server.instance.Chunk.CHUNK_SIZE_Z
import net.minestom.server.instance.DynamicChunk
import net.minestom.server.instance.block.Block.BARRIER
import net.minestom.server.utils.NamespaceID.from
import net.minestom.server.world.DimensionType
import net.minestom.server.world.biomes.Biome
import net.minestom.server.world.biomes.BiomeEffects
import net.minestom.server.world.biomes.BiomeParticle
import net.minestom.server.world.biomes.BiomeParticle.NormalOption

const val LIMBO_SIZE = 16.0

fun main() {
	// Init Server
	val server = init()

	// Create the dimension
	val dimension = DimensionType.builder(from("minecraft:space"))
		.effects("minecraft:the_end")
		.minY(0)
		.height(16)
		.logicalHeight(16)
		.build()

	// Register the dimension
	getDimensionTypeManager().addDimension(dimension)

	// Create the biome
	val biome = Biome.builder()
		.name(from("minecraft:space"))
		.effects(
			BiomeEffects.builder()
				.biomeParticle(BiomeParticle(1f, NormalOption(from("minecraft:underwater"))))
				.build()
		)
		.build()

	// Register the biome
	getBiomeManager().addBiome(biome)

	// Create the instance
	val instance = getInstanceManager().createInstanceContainer(dimension)

	// Chunk supplier
	instance.setChunkSupplier { _, chunkX, chunkZ ->
		val chunk = DynamicChunk(instance, chunkX, chunkZ)

		for (x in 0 ..CHUNK_SIZE_X) for (z in 0 ..CHUNK_SIZE_Z) {
			chunk.setBlock(x, 0, z, BARRIER)

			for (y in 0 .. 15) chunk.setBiome(x, y, z, biome)
		}

		chunk
	}

	// Handle Player Spawns
	getGlobalEventHandler().addListener(PlayerLoginEvent::class.java) {
		it.player.gameMode = ADVENTURE

		it.setSpawningInstance(instance)
		it.player.respawnPoint = Pos(Random.nextDouble(-LIMBO_SIZE, LIMBO_SIZE), 1.0, Random.nextDouble(-LIMBO_SIZE, LIMBO_SIZE))
		it.player.lookAt(Pos(0.0, 1.0, 0.0))
	}

	// World wrap around
	getGlobalEventHandler().addListener(PlayerMoveEvent::class.java) {
		var newX = it.newPosition.x
		var newZ = it.newPosition.z

		if (-LIMBO_SIZE > newX || newX > LIMBO_SIZE) newX = -min(max(-LIMBO_SIZE, newX), LIMBO_SIZE)
		if (-LIMBO_SIZE > newZ || newZ > LIMBO_SIZE) newZ = -min(max(-LIMBO_SIZE, newZ), LIMBO_SIZE)

		it.newPosition = Pos(newX, it.newPosition.y, newZ, it.newPosition.yaw, it.newPosition.pitch)
	}

	// Init Velocity Support
	val secretPath = Path("velocitySecret")
	if (!secretPath.exists()) secretPath.createFile()

	val secret = secretPath.readText()

	enable(secret) // Enable Velocity

	server.start("0.0.0.0", 10000)
}