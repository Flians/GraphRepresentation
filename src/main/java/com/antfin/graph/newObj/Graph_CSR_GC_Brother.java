package com.antfin.graph.newObj;

import com.antfin.arc.arch.message.graph.Edge;
import com.antfin.arc.arch.message.graph.Vertex;
import com.antfin.graph.Graph;
import com.antfin.util.BiHashMap;
import com.antfin.util.GraphHelper;

import java.util.*;

/**
 * @param <K>
 * @param <VV>
 * @param <EV>
 * @author Flians
 * @Description: Compare the corresponding method in refObj.
 * @Title: Graph_CSR_GC
 * @ProjectName graphRE
 * @date 2020/5/18 16:13
 */
public class Graph_CSR_GC_Brother<K, VV, EV> extends Graph<K, VV, EV> {
    private Map<K, Integer> dict_V;
    // store all targets. For i-th vertex in vertices, i = dict_V[sid] and targets[i][j] = dict_V[tid] - dict_V[sid]
    private List<List<byte[]>> targets;
    private List<Integer> csr;

    public Graph_CSR_GC_Brother() {
        this.dict_V = new BiHashMap<>();
        this.targets = new ArrayList<>();
        this.csr = new ArrayList<>();
    }

    public Graph_CSR_GC_Brother(List vg, boolean flag) {
        this();
        if (flag) {
            ((List<Vertex<K, VV>>) vg).forEach(this::addVertex);
        } else {
            ((List<Edge<K, EV>>) vg).forEach(this::addEdge);
        }
    }

    public Graph_CSR_GC_Brother(List<Vertex<K, VV>> vertices, List<Edge<K, EV>> edges) {
        this(vertices, true);
        edges.stream().forEach(this::addEdge);
    }

    @Override
    public void addVertex(Vertex<K, VV> vertex) {
        this.addVertex(vertex.getId());
    }

    public void addVertex(K id) {
        if (!this.dict_V.containsKey(id)) {
            String newId = new String((String) id);
            this.dict_V.put((K) newId, this.dict_V.size());
            this.csr.add(-1);
        }
    }

    @Override
    public void addEdge(Edge<K, EV> edge) {
        this.addVertex(edge.getSrcId());
        this.addVertex(edge.getTargetId());
        // the source vertex of edge has no edges.
        if (this.csr.get(this.dict_V.get(edge.getSrcId())) == -1) {
            this.csr.set(this.dict_V.get(edge.getSrcId()), this.targets.size());
            List<byte[]> item = new ArrayList<>();
            item.add(GraphHelper.intToByteArray(this.dict_V.get(edge.getTargetId()) - this.dict_V.get(edge.getSrcId())));
            this.targets.add(item);
        } else {
            int before = this.dict_V.get(edge.getSrcId());
            for (byte[] i : this.targets.get(this.csr.get(this.dict_V.get(edge.getSrcId())))) {
                before += GraphHelper.byteArrayToInt(i);
            }
            // the source vertex of edge has other edges.
            this.targets.get(this.csr.get(this.dict_V.get(edge.getSrcId()))).add(GraphHelper.intToByteArray(this.dict_V.get(edge.getTargetId()) - before));
        }
    }

    @Override
    public Object getVertex(Object key) {
        if (key instanceof Integer) {
            if (this.dict_V.containsValue(key)) {
                return ((BiHashMap) this.dict_V).getKey(key);
            }
        } else {
            if (this.dict_V.containsKey(key))
                return this.getDict_V().get(key);
        }
        return null;
    }

    @Override
    public List<byte[]> getEdge(K sid) {
        if (this.csr.get(this.dict_V.get(sid)) != -1)
            return this.getTargets().get(this.csr.get(this.dict_V.get(sid)));
        return null;
    }

    @Override
    public List<Object> getVertexList() {
        return new ArrayList<>(this.dict_V.keySet());
    }

    @Override
    public void clear() {
        this.dict_V.clear();
        this.dict_V = null;
        this.targets.clear();
        this.targets = null;
        this.csr.clear();
        this.csr = null;
    }

    public int reorder_BFS() {
        // Degree sort to put vertices with more out-edges in front
        List<Integer> id_V = new ArrayList<>(this.dict_V.size());
        for (int i = 0; i < this.dict_V.size(); ++i)
            id_V.add(i);
        Collections.sort(id_V, (o1, o2) -> {
            Integer d1 = 0, d2 = 0;
            if (csr.get(o1) != -1)
                d1 = targets.get(csr.get(o1)).size();
            if (csr.get(o2) != -1)
                d2 = targets.get(csr.get(o2)).size();
            if (d1 == d2) {
                return o1 < o2 ? -1 : 1;
            } else {
                return d1 < d2 ? 1 : -1;
            }
        });

        int[] id_new = new int[this.dict_V.size()];
        for (int i = 0; i < id_new.length; ++i) id_new[i] = -1;
        int cur_idx = 0;
        Queue<Integer> Vs = new LinkedList<>();
        for (Integer vid : id_V) {
            if (id_new[vid] == -1) {
                id_new[vid] = cur_idx++;
                Vs.add(vid);
                int levelSize = Vs.size();
                while (!Vs.isEmpty()) {
                    // Vertex has output edges
                    if (this.csr.get(Vs.peek()) != -1) {
                        Integer tarInt = Vs.peek();
                        for (byte[] tar : this.targets.get(this.csr.get(Vs.peek()))) {
                            tarInt += GraphHelper.byteArrayToInt(tar);
                            if (id_new[tarInt] == -1) {
                                id_new[tarInt] = cur_idx++;
                                Vs.add(tarInt);
                            }
                        }
                    }
                    Vs.poll();
                    --levelSize;
                    if (levelSize == 0) {
                        Collections.sort((List) Vs, (Comparator<Integer>) (o1, o2) -> {
                            Integer d1 = 0, d2 = 0;
                            if (csr.get(o1) != -1)
                                d1 = targets.get(csr.get(o1)).size();
                            if (csr.get(o2) != -1)
                                d2 = targets.get(csr.get(o2)).size();
                            if (d1 == d2) {
                                return o1 < o2 ? -1 : 1;
                            } else {
                                return d1 < d2 ? -1 : 1;
                            }
                        });
                        levelSize = Vs.size();
                    }
                }
            }
        }
        Vs = null;

        Iterator<String> it = (Iterator<String>) this.dict_V.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            // System.out.println("'" + key + "\t" + id_new[this.dict_V.get(key)]);
            this.dict_V.put((K) key, id_new[this.dict_V.get(key)]);
        }

        int maxGap = 0;
        for (int sid = 0; sid < this.csr.size(); sid++) {
            // sid <--> id_new[sid]
            id_V.set(id_new[sid], this.csr.get(sid));
            if (this.csr.get(sid) != -1)
                maxGap = Math.max(Math.abs(maxGap), Math.abs(this.adjustTarget(sid, this.targets.get(this.csr.get(sid)), id_new)));
        }
        id_new = null;
        this.csr.clear();
        this.csr = id_V;
        id_V = null;
        return maxGap;
    }

    private int adjustTarget(int sid_old, List<byte[]> tars, int[] id_new) {
        int maxGap = 0;
        int oldT = sid_old + GraphHelper.byteArrayToInt(tars.get(0));
        tars.set(0, GraphHelper.intToByteArray(id_new[oldT] - id_new[sid_old]));
        // System.out.println(GraphHelper.byteArrayToInt(tars.get(0)));
        for (int i = 1; i < tars.size(); ++i) {
            int oldTi = oldT + GraphHelper.byteArrayToInt(tars.get(i));
            tars.set(i, GraphHelper.intToByteArray(id_new[oldTi] - id_new[oldT]));
            oldT = oldTi;
            maxGap = Math.max(Math.abs(maxGap), Math.abs(GraphHelper.byteArrayToInt(tars.get(i))));
            // System.out.println(GraphHelper.byteArrayToInt(tars.get(i)));
        }
        return maxGap;
    }

    public Map<K, Integer> getDict_V() {
        return this.dict_V;
    }

    public List<List<byte[]>> getTargets() {
        return this.targets;
    }

    public List<Integer> getCsr() {
        return this.csr;
    }
}
