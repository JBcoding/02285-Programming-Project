package karlMarx;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.PriorityQueue;

public abstract class Strategy {
    private HashSet<Node> explored;
    private final long startTime;

    public Strategy() {
        this.explored = new HashSet<Node>(131072, 0.5f);
        this.startTime = System.currentTimeMillis();
    }

    public void addToExplored(Node n) {
        this.explored.add(n);
    }

    public boolean isExplored(Node n) {
        return this.explored.contains(n);
    }

    public int countExplored() {
        return this.explored.size();
    }

    public String searchStatus() {
        return String.format("#Explored: %,6d, #Frontier: %,6d, #Generated: %,6d, Time: %3.2f s \t%s", this.countExplored(), this.countFrontier(), this.countExplored() + this.countFrontier(), this.timeSpent(), Memory.stringRep());
    }

    public float timeSpent() {
        return (System.currentTimeMillis() - this.startTime) / 1000f;
    }

    public abstract Node getAndRemoveLeaf();

    public abstract void addToFrontier(Node n);

    public abstract boolean inFrontier(Node n);

    public abstract int countFrontier();

    public abstract boolean frontierIsEmpty();

    @Override
    public abstract String toString();


}

class StrategyBestFirst extends Strategy {
    private Heuristic heuristic;
    private PriorityQueue<Node> frontier;
    private HashSet<Node> frontierSet;

    public StrategyBestFirst(Heuristic h) {
        super();
        heuristic = h;
        frontier = new PriorityQueue<Node>(131072, h);
        frontierSet = new HashSet<Node>(131072, 0.5f);
    }

    @Override
    public Node getAndRemoveLeaf() {
        Node n = frontier.poll();
        frontierSet.remove(n);
        return n;
    }

    @Override
    public void addToFrontier(Node n) {
        if (frontier.size() > 100000) {
            pruneFrontier();
        }
        frontier.add(n);
        frontierSet.add(n);
    }

    private void pruneFrontier() {
        Node[] newFrontier = new Node[frontier.size() / 10];
        for (int i = 0; i < newFrontier.length; i++) {
            newFrontier[i] = frontier.poll();
        }
        frontier.clear();
        frontierSet.clear();
        for (Node n : newFrontier) {
            addToFrontier(n);
        }
    }

    @Override
    public int countFrontier() {
        return frontier.size();
    }

    @Override
    public boolean frontierIsEmpty() {
        return frontier.isEmpty();
    }

    @Override
    public boolean inFrontier(Node n) {
        return frontierSet.contains(n);
    }

    @Override
    public String toString() {
        return "Best-first Search (PriorityQueue) using " + this.heuristic.toString();
    }
}
