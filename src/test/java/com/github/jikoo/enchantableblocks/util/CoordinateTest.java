package com.github.jikoo.enchantableblocks.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;
import org.bukkit.util.NumberConversions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Feature: Convert coordinates between block, chunk, and region")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoordinateTest {

    @DisplayName("Conversion back and forth should produce consistent results.")
    @ParameterizedTest
    @MethodSource("getScenarios")
    void testConversion(int value, double ratio, IntUnaryOperator convert, IntUnaryOperator revert) {
        int expectedConvert = NumberConversions.floor(value * ratio);
        assertThat("Value converts as expected", convert.applyAsInt(value), is(expectedConvert));
        int expectedRevert = NumberConversions.floor(expectedConvert / ratio);
        assertThat("Value reverts as expected", revert.applyAsInt(expectedConvert), is(expectedRevert));
    }

    private static Stream<Arguments> getScenarios() {
        final int[] values = new int[] { -20, 30, 1024, 0, -16, -15 };

        final int blocksPerChunk = 16;
        final int chunksPerRegion = 32;
        final int blocksPerRegion = blocksPerChunk * chunksPerRegion;
        final double chunksPerBlock = 1.0 / blocksPerChunk;
        final double regionsPerBlock = 1.0 / blocksPerRegion;
        final double regionsPerChunk = 1.0 / chunksPerRegion;

        AtomicInteger index = new AtomicInteger();
        return Stream.generate(() -> {
            int currentIndex = index.getAndIncrement();
            int value = values[currentIndex / 6];

            switch (currentIndex % 6) {
                case 0:
                    // Block -> chunk -> block
                    return Arguments.of(value, chunksPerBlock,
                            (IntUnaryOperator) CoordinateConversions::blockToChunk,
                            (IntUnaryOperator) CoordinateConversions::chunkToBlock);
                case 1:
                    // Block -> region -> block
                    return Arguments.of(value, regionsPerBlock,
                            (IntUnaryOperator) CoordinateConversions::blockToRegion,
                            (IntUnaryOperator) CoordinateConversions::regionToBlock);
                case 2:
                    // Chunk -> block -> chunk
                    return Arguments.of(value, blocksPerChunk,
                            (IntUnaryOperator) CoordinateConversions::chunkToBlock,
                            (IntUnaryOperator) CoordinateConversions::blockToChunk);
                case 3:
                    // Chunk -> region -> chunk
                    return Arguments.of(value, regionsPerChunk,
                            (IntUnaryOperator) CoordinateConversions::chunkToRegion,
                            (IntUnaryOperator) CoordinateConversions::regionToChunk);
                case 4:
                    // Region -> block -> region
                    return Arguments.of(value, blocksPerRegion,
                                    (IntUnaryOperator) CoordinateConversions::regionToBlock,
                            (IntUnaryOperator) CoordinateConversions::blockToRegion);
                case 5:
                    // Region -> chunk -> region
                    return Arguments.of(value, chunksPerRegion,
                            (IntUnaryOperator) CoordinateConversions::regionToChunk,
                            (IntUnaryOperator) CoordinateConversions::chunkToRegion);
                default:
                    throw new IndexOutOfBoundsException("How could this happen to me? I've made my mistakes.");
            }
        }).limit(values.length * 6);
    }

    @DisplayName("Valid MCA file names should be converted to chunk coordinates")
    @ParameterizedTest
    @CsvSource({
            "r.0.0.mca,0,0", "r.-1.-5.mca,-32,-160", "r.5.-10.mca,160,-320",
            "r.-20.1.mca,-640,32", "r.11.22.mca,352,704"
    })
    void parseGoodMcaTest(String regionFileName, Integer expectedX, Integer expectedZ) {
        if (expectedX == null || expectedZ == null) {
            assertThrows(IllegalArgumentException.class, () -> CoordinateConversions.getRegionChunkCoords(regionFileName));
            return;
        }

        Pair<Integer, Integer> regionChunkCoords = CoordinateConversions.getRegionChunkCoords(regionFileName);
        assertThat("X value must parse correctly", regionChunkCoords.getLeft(), is(expectedX));
        assertThat("Z value must parse correctly", regionChunkCoords.getRight(), is(expectedZ));
    }

    @DisplayName("Invalid MCA file names should throw an exception")
    @ParameterizedTest
    @ValueSource(strings = { "bad filename", "r...mca", "r.--1.2.mca" })
    void parseBadMcaTest(String regionFileName) {
        assertThrows(IllegalArgumentException.class, () -> CoordinateConversions.getRegionChunkCoords(regionFileName));
    }

}
