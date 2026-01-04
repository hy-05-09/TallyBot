package com.tallybot.backend.tallybot_back.debtopt;

import org.springframework.data.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public final class FlattedGraph {
    private Pair<InfiniteIterator<WeightStrategy>, ThreeTuple<List<Integer>, Map<Integer, Integer>, List<Integer>>> data;

    public FlattedGraph(InfiniteIterator<WeightStrategy> ws, ThreeTuple<List<Integer>, Map<Integer, Integer>, List<Integer>> t) {
        this.data = Pair.of(ws, t);
    }

    public FlattedGraph(InfiniteIterator<WeightStrategy> ws, List<Integer> circuit, Map<Integer, Integer> weightFrequency, List<Integer> weights) {
        this(
                ws, new ThreeTuple<>(circuit, weightFrequency, weights)
        );
    }

    public FlattedGraph(FlattedGraph f) {
        this(f.getWeightStrategies(), new ArrayList<>(f.getCircuit()), new HashMap<>(f.getWeightFrequency()), new ArrayList<>(f.getWeights()));
    }

    public InfiniteIterator<WeightStrategy> getWeightStrategies() {
        return data.getFirst();
    }

    public void setWeightStrategies(InfiniteIterator<WeightStrategy> ws) {
        data = Pair.of(ws, data.getSecond());
    }

    public List<Integer> getCircuit() {
        return data.getSecond().first();
    }

    public void setCircuit(List<Integer> circuit) {
        data = Pair.of(data.getFirst(), new ThreeTuple<>(circuit, data.getSecond().second(), data.getSecond().third()));
    }

    /**
     * @deprecated Use {@link #setCircuit(List)} instead.
     */
    @Deprecated
    public void setChangeable(List<Integer> circuit) {
        setCircuit(circuit);
    }

    public Map<Integer, Integer> getWeightFrequency() {
        return data.getSecond().second();
    }

    public void setWeightFrequency(Map<Integer, Integer> weightFrequency) {
        data = Pair.of(data.getFirst(), new ThreeTuple<>(data.getSecond().first(), weightFrequency, data.getSecond().third()));
    }

    public List<Integer> getWeights() {
        return data.getSecond().third();
    }

    public void setWeights(List<Integer> weights) {
        data = Pair.of(data.getFirst(), new ThreeTuple<>(data.getSecond().first(), data.getSecond().second(), weights));
    }

    public ThreeTuple<List<Integer>, Map<Integer, Integer>, List<Integer>> getGraphData(){
        return data.getSecond();
    }

    /**
     * @deprecated Use {@link #getGraphData()} instead.
     */
    @Deprecated
    public ThreeTuple<List<Integer>, Map<Integer, Integer>, List<Integer>> getSecond() {
        return getGraphData();
    }
}
