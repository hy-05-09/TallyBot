package com.tallybot.backend.tallybot_back.debtopt;

import java.util.*;

public class UnionFind {
    private final List<Integer> disjointSet;
    private final List<Integer> size;

    public UnionFind(int vertices) {
        disjointSet = new ArrayList<>(vertices);
        size = new ArrayList<>(Collections.nCopies(vertices, 1));
        for(int i = 0; i < vertices; i++) {
            disjointSet.add(i);
        }
    }

    public int find(int idx) {
        if (idx != disjointSet.get(idx)){
            disjointSet.set(idx, find(disjointSet.get(idx)));
        }
        return disjointSet.get(idx);
    }

    public void union(int a, int b) {
        a = find(a);
        b = find(b);

        if (a==b) return;

        if(size.get(a) < size.get(b)) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        disjointSet.set(b, a);
        size.set(a, size.get(a) + size.get(b));
    }

    public static List<Graph> splitGraph(Graph g) {
        UnionFind uf = new UnionFind(g.getVertexCount());

        for(int i = 0; i < g.getVertexCount(); i++) {
            for(Integer end : g.getAdjacencyList().get(i).keySet()) {
                uf.union(i, end);
            }
        }

        Map<Integer, Graph> m = new HashMap<>();

        for(int i = 0; i < g.getVertexCount(); i++) {
            if(g.getAdjacencyList().get(i).isEmpty()) continue;
            final int i2 = i;
            int parent = uf.find(i);
            for(Integer j : g.getAdjacencyList().get(i).keySet()) {
                m.compute(parent, (k, v) -> {
                    if(v == null)
                        v = new Graph(g.getVertexCount());
                    if(i2 < j)
                        v.addEdge(i2, j, g.getAdjacencyList().get(i2).get(j));
                    return v;
                });
            }
        }

        return m.values().stream().toList();
    }
}
