import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.SwampHut;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;
import nl.jellejurre.seedchecker.SeedChecker;
import nl.jellejurre.seedchecker.SeedCheckerDimension;
import nl.jellejurre.seedchecker.TargetState;
import nl.kallestruik.noisesampler.minecraft.NoiseColumnSampler;
import nl.kallestruik.noisesampler.minecraft.NoiseParameterKey;
import nl.kallestruik.noisesampler.minecraft.Xoroshiro128PlusPlusRandom;
import nl.kallestruik.noisesampler.minecraft.noise.LazyDoublePerlinNoiseSampler;
import nl.kallestruik.noisesampler.minecraft.util.MathHelper;
import nl.kallestruik.noisesampler.minecraft.util.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LowestSwampHutOnASeed {
    //This class is for searching the lowest swamp huts on a specific seed.
    private static final long seed=4020992490420041415L; //Your seed
    private static final SwampHut swampHut = new SwampHut(MCVersion.v1_21);
    private static final int threadCount=8; //Your computer's thread amount

    public static void main(String[] args) {
        int minX = -58594;//world border
        int maxX = 58593;
        int minZ = -58594;
        int maxZ = 58593;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int totalX = maxX - minX;
        int chunkSize = Math.max(1, totalX / threadCount);
        for (int i = 0; i < threadCount; i++) {
            int startX = minX + i * chunkSize;
            int endX = (i == threadCount - 1) ? maxX : startX + chunkSize;
            executor.execute(new RegionChecker(startX, endX, minZ, maxZ));
        }
        executor.shutdown();
    }
    static class RegionChecker implements Runnable {
        private final int startX;
        private final int endX;
        private final int minZ;
        private final int maxZ;
        private final ChunkRand rand;

        public RegionChecker(int startX, int endX, int minZ, int maxZ) {
            this.startX = startX;
            this.endX = endX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.rand = new ChunkRand();
        }
        @Override
        public void run() {
            for (int x = startX; x < endX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    CPos pos = swampHut.getInRegion(seed, x, z, rand);
                    if (check(seed, 16 * pos.getX() + 3, 16 * pos.getZ() + 3)) {
                        SeedChecker checker = new SeedChecker(seed, TargetState.NO_STRUCTURES, SeedCheckerDimension.OVERWORLD);
                        Box box = new Box(16 * pos.getX() + 3, -30, 16 * pos.getZ() + 3,
                                16 * pos.getX() + 4, 200, 16 * pos.getZ() + 4);
                        if (checker.getBlockCountInBox(Blocks.AIR, box) == 230) {
                            synchronized (System.out) {
                                System.out.printf("%d %d\n", 16 * pos.getX(), 16 * pos.getZ());
                            }
                        }
                        checker.clearMemory();
                    }
                }
            }
        }
    }
    public static boolean check(long seed, int x, int z) {
        NoiseCache cache = new NoiseCache(seed);
        double erosionSample = cache.erosion.sample((double)x/4, 0,(double)z/4);
        if (erosionSample < 0.55) {
            return false;
        }
        double temperature = cache.temperature.sample((double)x/4, 0, (double)z/4);
        if (temperature > 0.2||temperature<-0.45) {
            return false;
        }
        double ridge = cache.ridge.sample((double)x/4, 0, (double)z/4);
        if ((ridge > 0.46&&ridge<0.88)||(ridge<-0.46&&ridge>-0.88)) {
            return false;
        }
        if (Entrance(seed, x, 50, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 60, z) >= 0) {
            return false;
        }
        if (Entrance2(seed, x, -30, z) >= 0 && Cheese(seed, x, -30, z) >= 0) {
            return false;
        }
        if (Entrance2(seed, x, 0, z) >= 0 && Cheese(seed, x, 0, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 40, z) >= 0 && Cheese(seed, x, 40, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 30, z) >= 0 && Cheese(seed, x, 30, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 20, z) >= 0 && Cheese(seed, x, 20, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 10, z) >= 0 && Cheese(seed, x, 10, z) >= 0) {
            return false;
        }
        if (Entrance2(seed, x, -10, z) >= 0 && Cheese(seed, x, -10, z) >= 0) {
            return false;
        }
        if (Entrance2(seed, x, -20, z) >= 0 && Cheese(seed, x, -20, z) >= 0) {
            return false;
        }
        LazyDoublePerlinNoiseSampler continentalnessNoise = LazyDoublePerlinNoiseSampler.createNoiseSampler(
                new Xoroshiro128PlusPlusRandom(seed).createRandomDeriver(),
                NoiseParameterKey.CONTINENTALNESS
        );
        if (continentalnessNoise.sample((double)x/4, 0, (double)z/4) < -0.11) {
            return false;
        }
        LazyDoublePerlinNoiseSampler aquiferNoise = LazyDoublePerlinNoiseSampler.createNoiseSampler(
                new Xoroshiro128PlusPlusRandom(seed).createRandomDeriver(),
                NoiseParameterKey.AQUIFER_FLUID_LEVEL_FLOODEDNESS
        );
        for (int y = -30; y <= 60; y += 10) {
            if (aquiferNoise.sample(x, y * 0.67, z) > 0.41) {
                return false;
            }
        }
        return true;
    }
    private static class NoiseCache {
        final LazyDoublePerlinNoiseSampler caveEntrance;
        final LazyDoublePerlinNoiseSampler spaghettiRarity;
        final LazyDoublePerlinNoiseSampler spaghettiThickness;
        final LazyDoublePerlinNoiseSampler spaghetti3D1;
        final LazyDoublePerlinNoiseSampler spaghetti3D2;
        final LazyDoublePerlinNoiseSampler spaghettiRoughnessModulator;
        final LazyDoublePerlinNoiseSampler spaghettiRoughness;
        final LazyDoublePerlinNoiseSampler erosion;
        final LazyDoublePerlinNoiseSampler temperature;
        final LazyDoublePerlinNoiseSampler ridge;
        NoiseCache(long worldseed) {
            Xoroshiro128PlusPlusRandom random = new Xoroshiro128PlusPlusRandom(worldseed);
            var deriver = random.createRandomDeriver();
            caveEntrance = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.CAVE_ENTRANCE);
            spaghettiRarity = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_3D_RARITY);
            spaghettiThickness = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_3D_THICKNESS);
            spaghetti3D1 = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_3D_1);
            spaghetti3D2 = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_3D_2);
            spaghettiRoughnessModulator = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_ROUGHNESS_MODULATOR);
            spaghettiRoughness = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.SPAGHETTI_ROUGHNESS);
            erosion = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.EROSION);
            temperature = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.TEMPERATURE);
            ridge = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.RIDGE);
        }
    }
    private static class CheeseNoiseCache {
        final LazyDoublePerlinNoiseSampler caveLayer;
        final LazyDoublePerlinNoiseSampler caveCheese;
        CheeseNoiseCache(long worldseed) {
            Xoroshiro128PlusPlusRandom random = new Xoroshiro128PlusPlusRandom(worldseed);
            var deriver = random.createRandomDeriver();
            caveLayer = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.CAVE_LAYER);
            caveCheese = LazyDoublePerlinNoiseSampler.createNoiseSampler(deriver, NoiseParameterKey.CAVE_CHEESE);
        }
    }
    public static double Entrance(long worldseed, int x, int y, int z) {
        NoiseCache cache = new NoiseCache(worldseed);
        double c = cache.caveEntrance.sample(x * 0.75, y * 0.5, z * 0.75) + 0.37 +
                MathHelper.clampedLerp(0.3, 0.0, (10 + (double)y) / 40.0);
        double d = cache.spaghettiRarity.sample(x * 2, y, z * 2);
        double e = NoiseColumnSampler.CaveScaler.scaleTunnels(d);
        double h = Util.lerpFromProgress(cache.spaghettiThickness, x, y, z, 0.065, 0.088);
        double l = NoiseColumnSampler.sample(cache.spaghetti3D1, x, y, z, e);
        double m = Math.abs(e * l) - h;
        double n = NoiseColumnSampler.sample(cache.spaghetti3D2, x, y, z, e);
        double o = Math.abs(e * n) - h;
        double p = MathHelper.clamp(Math.max(m, o), -1.0, 1.0);
        double q = (-0.05 + (-0.05 * cache.spaghettiRoughnessModulator.sample(x, y, z))) *
                (-0.4 + Math.abs(cache.spaghettiRoughness.sample(x, y, z)));
        return Math.min(c, p + q);
    }
    public static double Cheese(long worldseed, int x, int y, int z) {
        CheeseNoiseCache cache = new CheeseNoiseCache(worldseed);
        double a = 4 * cache.caveLayer.sample(x, y * 8, z) * cache.caveLayer.sample(x, y * 8, z);
        double b = MathHelper.clamp((0.27 + cache.caveCheese.sample(x, y * 0.6666666666666666, z)), -1, 1);
        return a + b;//Actually there still need to add a function about sloped_cheese, but sloped_cheese is too complex and IDK how to calculate it.
    }
    public static double Entrance2(long worldseed, int x, int y, int z) {
        NoiseCache cache = new NoiseCache(worldseed);
        double d = cache.spaghettiRarity.sample(x * 2, y, z * 2);
        double e = NoiseColumnSampler.CaveScaler.scaleTunnels(d);
        double h = Util.lerpFromProgress(cache.spaghettiThickness, x, y, z, 0.065, 0.088);
        double l = NoiseColumnSampler.sample(cache.spaghetti3D1, x, y, z, e);
        double m = Math.abs(e * l) - h;
        double n = NoiseColumnSampler.sample(cache.spaghetti3D2, x, y, z, e);
        double o = Math.abs(e * n) - h;
        double p = MathHelper.clamp(Math.max(m, o), -1.0, 1.0);
        double q = (-0.05 + (-0.05 * cache.spaghettiRoughnessModulator.sample(x, y, z))) *
                (-0.4 + Math.abs(cache.spaghettiRoughness.sample(x, y, z)));
        return p + q;
    }
}