package karlMarx;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.PriorityQueue;

public abstract class Strategy {
    private HashSet<GeneralNode> explored;
    private final long startTime;

    public Strategy() {
        this.explored = new HashSet<GeneralNode>(131072, 0.5f);
        this.startTime = System.currentTimeMillis();
    }

    public void addToExplored(GeneralNode n) {
        this.explored.add(n);
    }

    public boolean isExplored(GeneralNode n) {
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

    public abstract GeneralNode getAndRemoveLeaf();

    public abstract void addToFrontier(GeneralNode n);

    public abstract boolean inFrontier(GeneralNode n);

    public abstract int countFrontier();

    public abstract boolean frontierIsEmpty();

    @Override
    public abstract String toString();


}

class StrategyBestFirst extends Strategy {
    private Heuristic heuristic;
    private PriorityQueue<GeneralNode> frontier;
    private HashSet<GeneralNode> frontierSet;

    public StrategyBestFirst(Heuristic h) {
        super();
        heuristic = h;
        frontier = new PriorityQueue<GeneralNode>(131072, h);
        frontierSet = new HashSet<GeneralNode>(131072, 0.5f);
    }

    @Override
    public GeneralNode getAndRemoveLeaf() {
        GeneralNode n = frontier.poll();
        frontierSet.remove(n);
        return n;
    }

    @Override
    public void addToFrontier(GeneralNode n) {
        if (frontier.size() > 100000) {
            pruneFrontier();
        }
        frontier.add(n);
        frontierSet.add(n);
    }

    private void pruneFrontier() {
        GeneralNode[] newFrontier = new GeneralNode[frontier.size() / 10];
        for (int i = 0; i < newFrontier.length; i++) {
            newFrontier[i] = frontier.poll();
        }
        frontier.clear();
        frontierSet.clear();
        for (GeneralNode n : newFrontier) {
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
    public boolean inFrontier(GeneralNode n) {
        return frontierSet.contains(n);
    }

    @Override
    public String toString() {
        return "Best-first Search (PriorityQueue) using " + this.heuristic.toString();
    }
}
