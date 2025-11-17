import com.seedfinding.mccore.rand.seed.StructureSeed;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CavefinderForSwamp {
    //This class is for searching the lowest swamp huts at (512,512).
    private static final int THREAD_COUNT = 8; //Your computer's thread amount
    private static final int MAX_DAYS = 365;

    public static void main(String[] args) throws IOException {
        Path resultPath = Paths.get("./result.txt");
        if (Files.exists(resultPath)) {
            throw new IOException("File ./result.txt already exists. Aborting.");
        }
        long[] structureSeeds = Files.lines(Paths.get("./seed.txt"))
                //You need to make a list of low 48 bit seeds with potential swamp hut at (512,512) on cubiomes-viewer
                .mapToLong(Long::parseLong)
                .toArray();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger completedTasks = new AtomicInteger(0);
        int totalTasks = structureSeeds.length;
        AtomicInteger printedBasisPoints = new AtomicInteger(-1);
        try (BufferedWriter ignored = Files.newBufferedWriter(resultPath)) {
            for (long structureSeed : structureSeeds) {
                executor.execute(() -> {
                    processSeed(structureSeed);
                    int completed = completedTasks.incrementAndGet();
                    int currentBasisPoints = (int)((long)completed * 10000 / totalTasks);
                    int lastPrinted = printedBasisPoints.get();
                    if (currentBasisPoints > lastPrinted) {
                        if (printedBasisPoints.compareAndSet(lastPrinted, currentBasisPoints)) {
                            double percentage = currentBasisPoints / 100.0;
                            System.out.printf("%d/%d (%.2f%%)\n", completed, totalTasks, percentage);
                        }
                    }
                });
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(MAX_DAYS, TimeUnit.DAYS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    private static void processSeed(long structureSeed) {
        StructureSeed.getWorldSeeds(structureSeed).forEachRemaining(ws -> {
            if (check(ws, 515, 515)) {
                SeedChecker checker = new SeedChecker(ws, TargetState.NO_STRUCTURES, SeedCheckerDimension.OVERWORLD);
                Box box = new Box(515,-40,515,516,200,516);
                if (checker.getBlockCountInBox(Blocks.AIR, box) == 240) {
                    synchronized (System.out) {
                        System.out.println(ws);
                    }
                }
                checker.clearMemory();
            }
        });
    }
    public static boolean check(long seed, int x, int z) {
        NoiseCache cache = new NoiseCache(seed);
        double erosionSample = cache.erosion.sample((double)x/4, 0, (double)z/4);
        if (erosionSample < 0.55) {
            return false;
        }
        double temperature = cache.temperature.sample((double)x/4, 0, (double)z/4);
        if (temperature > 0.2||temperature<-0.45) {
            return false;
        }
        double ridge = cache.ridge.sample((double)x/4, 0, (double)z/4);
        if ((ridge > 0.4&&ridge<0.933)||(ridge<-0.4&&ridge>-0.933)) {
            return false;
        }
        if (Entrance(seed, x, 50, z) >= 0) {
            return false;
        }
        if (Entrance(seed, x, 60, z) >= 0) {
            return false;
        }
        if (Entrance2(seed, x, -40, z) >= 0 && Cheese(seed, x, -40, z) >= 0) {
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
        if (Entrance2(seed, x, -30, z) >= 0 && Cheese(seed, x, -30, z) >= 0) {
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
        for (int y = -40; y <= 60; y += 10) {
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