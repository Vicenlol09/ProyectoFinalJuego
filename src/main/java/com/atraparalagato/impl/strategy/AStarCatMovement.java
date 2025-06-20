package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AStarCatMovement extends CatMovementStrategy<HexPosition> {

    public AStarCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }

    @Override
    protected List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        // Devuelve las posiciones adyacentes no bloqueadas
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .collect(Collectors.toList());
    }

    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves,
                                                  HexPosition currentPosition,
                                                  HexPosition targetPosition) {
        if (possibleMoves.isEmpty()) return Optional.empty();
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);

        // Selecciona el movimiento con menor f(n) = g(n) + h(n)
        return possibleMoves.stream()
                .min(Comparator.comparing(move ->
                        getMoveCost(currentPosition, move) + heuristic.apply(move)
                ));
    }

    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        // Distancia hexagonal como heurística admisible
        return position -> (double) position.distanceTo(targetPosition);
    }

    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        int size = board.getSize();
        // El objetivo es cualquier celda en el borde del tablero
        return pos -> Math.abs(pos.getQ()) == size ||
                      Math.abs(pos.getR()) == size ||
                      Math.abs(pos.getS()) == size;
    }

    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        // Costo uniforme para adyacentes
        return 1.0;
    }

    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        // BFS para verificar si hay camino a cualquier objetivo
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        Predicate<HexPosition> isGoal = getGoalPredicate();

        queue.add(currentPosition);
        visited.add(currentPosition);

        while (!queue.isEmpty()) {
            HexPosition pos = queue.poll();
            if (isGoal.test(pos)) return true;
            for (HexPosition neighbor : getPossibleMoves(pos)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        // Implementación A* para encontrar el camino completo
        Function<HexPosition, Double> heuristic = getHeuristicFunction(targetPosition);
        Set<HexPosition> closedSet = new HashSet<>();
        Map<HexPosition, Double> gScore = new HashMap<>();
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));

        gScore.put(currentPosition, 0.0);
        openSet.add(new AStarNode(currentPosition, 0.0, heuristic.apply(currentPosition), null));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            if (current.position.equals(targetPosition)) {
                return reconstructPath(current);
            }
            closedSet.add(current.position);

            for (HexPosition neighbor : getPossibleMoves(current.position)) {
                if (closedSet.contains(neighbor)) continue;
                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);

                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    openSet.add(new AStarNode(neighbor, tentativeG, fScore, current));
                }
            }
        }
        return Collections.emptyList(); // No hay camino
    }

    // Clase auxiliar para nodos del algoritmo A*
    private static class AStarNode {
        public final HexPosition position;
        public final double gScore; // Costo desde inicio
        public final double fScore; // gScore + heurística
        public final AStarNode parent;

        public AStarNode(HexPosition position, double gScore, double fScore, AStarNode parent) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.parent = parent;
        }
    }

    // Método auxiliar para reconstruir el camino
    private List<HexPosition> reconstructPath(AStarNode goalNode) {
        List<HexPosition> path = new LinkedList<>();
        AStarNode current = goalNode;
        while (current != null) {
            path.add(0, current.position);
            current = current.parent;
        }
        return path;
    }

    // Hook methods - los estudiantes pueden override para debugging
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        super.beforeMovementCalculation(currentPosition);
    }

    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        super.afterMovementCalculation(selectedMove);
    }
}