package com.warzation.application.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Grafo imutável de províncias (nós) e fronteiras terrestres (arestas). É a
 * consulta que movimento, abastecimento e rotas vão usar.
 */
public final class ProvinceGraph {

    private final Map<String, Set<String>> adjacency;

    public ProvinceGraph(Collection<String> provinces, Collection<List<String>> connections) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        provinces.forEach(id -> graph.put(id, new LinkedHashSet<>()));
        for (List<String> edge : connections) {
            String a = edge.get(0);
            String b = edge.get(1);
            if (!graph.containsKey(a) || !graph.containsKey(b)) {
                throw new IllegalArgumentException("Conexão com província inexistente: " + a + " - " + b);
            }
            graph.get(a).add(b);
            graph.get(b).add(a);
        }
        Map<String, Set<String>> frozen = new LinkedHashMap<>();
        graph.forEach((id, neighbours) -> frozen.put(id, Collections.unmodifiableSet(neighbours)));
        this.adjacency = Collections.unmodifiableMap(frozen);
    }

    public boolean contains(String province) {
        return adjacency.containsKey(province);
    }

    public Set<String> provinces() {
        return adjacency.keySet();
    }

    public Set<String> neighbours(String province) {
        Set<String> neighbours = adjacency.get(province);
        if (neighbours == null) {
            throw new IllegalArgumentException("Província inexistente: " + province);
        }
        return neighbours;
    }

    public boolean areAdjacent(String a, String b) {
        return contains(a) && neighbours(a).contains(b);
    }

    /** Menor rota em número de fronteiras atravessadas, incluindo origem e destino. */
    public Optional<List<String>> route(String from, String to) {
        neighbours(from);
        neighbours(to);
        Map<String, String> previous = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(List.of(from));
        previous.put(from, from);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(to)) {
                List<String> path = new ArrayList<>();
                for (String step = to; !step.equals(from); step = previous.get(step)) {
                    path.add(step);
                }
                path.add(from);
                Collections.reverse(path);
                return Optional.of(List.copyOf(path));
            }
            for (String next : adjacency.get(current)) {
                if (!previous.containsKey(next)) {
                    previous.put(next, current);
                    queue.add(next);
                }
            }
        }
        return Optional.empty();
    }

    /** Verdadeiro se todos os nós dados formam um único componente usando só arestas entre eles. */
    public boolean isConnectedSubset(Collection<String> subset) {
        Set<String> nodes = new LinkedHashSet<>(subset);
        if (nodes.isEmpty() || !adjacency.keySet().containsAll(nodes)) {
            return false;
        }
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>(List.of(nodes.iterator().next()));
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (seen.add(current)) {
                for (String next : adjacency.get(current)) {
                    if (nodes.contains(next) && !seen.contains(next)) {
                        stack.push(next);
                    }
                }
            }
        }
        return seen.equals(nodes);
    }
}
