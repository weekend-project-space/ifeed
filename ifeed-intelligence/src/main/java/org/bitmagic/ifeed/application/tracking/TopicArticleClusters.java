package org.bitmagic.ifeed.application.tracking;

import org.bitmagic.ifeed.domain.record.ArticleEmbeddingRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.HashMap;

/**
 * 用于向量聚类
 */
final class TopicArticleClusters {

    private final double similarityThreshold;
    private final Map<Long, double[]> vectorsById = new TreeMap<>();

    TopicArticleClusters(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    void add(List<Long> articleIds, List<ArticleEmbeddingRecord> embeddings) {
        Map<Long, float[]> vectors = embeddings.stream().collect(Collectors.toMap(
                ArticleEmbeddingRecord::id, ArticleEmbeddingRecord::embedding));
        for (Long articleId : articleIds) {
            vectorsById.putIfAbsent(articleId, normalize(vectors.get(articleId)));
        }
    }

    List<List<Long>> articleGroups() {
        List<Group> groups = new ArrayList<>();
        Map<Long, Group> anchors = new HashMap<>();
        Map<Integer, Node> indexes = vectorsById.entrySet().stream().filter(entry -> entry.getValue() != null)
                .collect(Collectors.groupingBy(entry -> entry.getValue().length)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> build(new ArrayList<>(entry.getValue()))));
        for (var entry : vectorsById.entrySet()) {
            Long articleId = entry.getKey();
            double[] vector = entry.getValue();
            Group closest = null;
            double bestSimilarity = similarityThreshold;
            if (vector != null) {
                for (Long candidate : candidates(indexes.get(vector.length), vector)) {
                    Group group = anchors.get(candidate);
                    if (group == null) continue;
                    double similarity = 0;
                    for (int dimension = 0; dimension < vector.length; dimension++) {
                        similarity += vector[dimension] * group.representative[dimension];
                    }
                    if (similarity > bestSimilarity || similarity == bestSimilarity && closest == null) {
                        bestSimilarity = similarity;
                        closest = group;
                    }
                }
            }
            if (closest == null) {
                closest = new Group(vector);
                groups.add(closest);
                anchors.put(articleId, closest);
            }
            closest.add(articleId, vector);
        }
        for (var entry : new TreeMap<>(anchors).entrySet()) {
            Group group = anchors.get(entry.getKey());
            if (group == null || group.articleIds.isEmpty() || group.representative == null) continue;
            for (Long candidate : candidates(indexes.get(group.representative.length), group.representative)) {
                Group other = anchors.get(candidate);
                if (other == null || other == group || other.articleIds.isEmpty() || other.representative == null)
                    continue;
                double similarity = 0;
                for (int dimension = 0; dimension < group.representative.length; dimension++) {
                    similarity += group.representative[dimension] * other.representative[dimension];
                }
                if (similarity >= similarityThreshold) {
                    group.merge(other);
                    anchors.put(candidate, group);
                }
            }
        }
        return groups.stream().filter(group -> !group.articleIds.isEmpty())
                .map(group -> group.articleIds.stream().sorted().toList())
                .sorted(Comparator.comparing(group -> group.getFirst())).toList();
    }

    private Node build(List<Map.Entry<Long, double[]>> entries) {
        if (entries.size() <= 16) return new Node(entries, 0, 0, null, null);
        int dimension = 0;
        double largestSpread = -1;
        for (int index = 0; index < entries.getFirst().getValue().length; index++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (var entry : entries) {
                min = Math.min(min, entry.getValue()[index]);
                max = Math.max(max, entry.getValue()[index]);
            }
            if (max - min > largestSpread) {
                largestSpread = max - min;
                dimension = index;
            }
        }
        int axis = dimension;
        entries.sort(Comparator.<Map.Entry<Long, double[]>>comparingDouble(entry -> entry.getValue()[axis])
                .thenComparingLong(Map.Entry::getKey));
        int middle = entries.size() / 2;
        double split = entries.get(middle).getValue()[axis];
        return new Node(List.of(), axis, split, build(new ArrayList<>(entries.subList(0, middle))),
                build(new ArrayList<>(entries.subList(middle, entries.size()))));
    }

    private List<Long> candidates(Node root, double[] vector) {
        PriorityQueue<Branch> pending = new PriorityQueue<>(Comparator.comparingDouble(Branch::distance)
                .thenComparing(Comparator.comparingInt(Branch::depth).reversed())
                .thenComparingLong(Branch::sequence));
        long sequence = 0;
        pending.add(new Branch(root, 0, 0, sequence++));
        List<Long> candidates = new ArrayList<>();
        while (!pending.isEmpty() && candidates.size() < 128) {
            Branch branch = pending.remove();
            Node node = branch.node();
            if (node.left() == null) {
                node.entries().forEach(entry -> candidates.add(entry.getKey()));
                continue;
            }
            double difference = vector[node.dimension()] - node.split();
            Node near = difference < 0 ? node.left() : node.right();
            Node far = difference < 0 ? node.right() : node.left();
            pending.add(new Branch(near, branch.distance(), branch.depth() + 1, sequence++));
            pending.add(new Branch(far, Math.max(branch.distance(), Math.abs(difference)), branch.depth() + 1, sequence++));
        }
        return candidates.stream().sorted().toList();
    }

    private record Node(List<Map.Entry<Long, double[]>> entries, int dimension, double split, Node left, Node right) {
    }

    private record Branch(Node node, double distance, int depth, long sequence) {
    }

    private double[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) return null;
        double squaredNorm = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) return null;
            squaredNorm += (double) value * value;
        }
        if (squaredNorm == 0) return null;
        double norm = Math.sqrt(squaredNorm);
        double[] normalized = new double[vector.length];
        for (int dimension = 0; dimension < vector.length; dimension++) {
            normalized[dimension] = vector[dimension] / norm;
        }
        return normalized;
    }

    private static final class Group {
        private double[] representative;
        private final double[] sum;
        private final List<Long> articleIds = new ArrayList<>();

        private Group(double[] representative) {
            this.representative = representative;
            this.sum = representative == null ? null : new double[representative.length];
        }

        private void add(Long id, double[] vector) {
            articleIds.add(id);
            if (vector == null) return;
            for (int dimension = 0; dimension < sum.length; dimension++) sum[dimension] += vector[dimension];
            updateCentroid();
        }

        private void merge(Group other) {
            articleIds.addAll(other.articleIds);
            for (int dimension = 0; dimension < sum.length; dimension++) sum[dimension] += other.sum[dimension];
            other.articleIds.clear();
            updateCentroid();
        }

        private void updateCentroid() {
            double norm = 0;
            for (double value : sum) norm += value * value;
            norm = Math.sqrt(norm);
            if (norm == 0) return;
            representative = new double[sum.length];
            for (int dimension = 0; dimension < sum.length; dimension++)
                representative[dimension] = sum[dimension] / norm;
        }
    }
}
