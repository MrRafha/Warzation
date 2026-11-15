package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProvinceGraphTests {

    // a - b - c - d     e (ilhada)
    private final ProvinceGraph graph = new ProvinceGraph(
            List.of("a", "b", "c", "d", "e"),
            List.of(List.of("a", "b"), List.of("b", "c"), List.of("c", "d"), List.of("a", "c")));

    @Test
    void neighboursAreSymmetric() {
        assertThat(graph.neighbours("a")).containsExactlyInAnyOrder("b", "c");
        assertThat(graph.neighbours("c")).containsExactlyInAnyOrder("a", "b", "d");
        assertThat(graph.areAdjacent("b", "a")).isTrue();
        assertThat(graph.areAdjacent("a", "d")).isFalse();
    }

    @Test
    void routeIsTheShortestInBorders() {
        assertThat(graph.route("a", "d")).contains(List.of("a", "c", "d"));
        assertThat(graph.route("d", "a")).contains(List.of("d", "c", "a"));
        assertThat(graph.route("b", "b")).contains(List.of("b"));
    }

    @Test
    void routeBetweenDisconnectedProvincesIsEmpty() {
        assertThat(graph.route("a", "e")).isEmpty();
    }

    @Test
    void unknownProvinceIsRejected() {
        assertThatThrownBy(() -> graph.neighbours("z")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> graph.route("a", "z")).isInstanceOf(IllegalArgumentException.class);
        assertThat(graph.areAdjacent("z", "a")).isFalse();
    }

    @Test
    void connectionToUnknownProvinceIsRejected() {
        assertThatThrownBy(() -> new ProvinceGraph(List.of("a"), List.of(List.of("a", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detectsConnectedSubsets() {
        assertThat(graph.isConnectedSubset(List.of("a", "b", "c"))).isTrue();
        assertThat(graph.isConnectedSubset(List.of("a", "d"))).isFalse();
        assertThat(graph.isConnectedSubset(List.of("a", "b", "c", "d", "e"))).isFalse();
        assertThat(graph.isConnectedSubset(List.of())).isFalse();
    }
}
